package com.a1hd.movies.db.repository

import com.a1hd.movies.db.dao.PlaybackProgressDao
import com.a1hd.movies.db.entity.PlaybackProgressEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps [PlaybackProgressDao] and owns the resume rules. Mirrors the iOS
 * `PlaybackProgressRepository`.
 *
 * A record stops being "resumable" once playback is past 85% of the runtime OR within
 * the final 15s (whichever hits first) — so a nearly-finished item drops out of the
 * Continue Watching row and re-opening it starts from 0. The record itself is only
 * deleted at the true end-tail.
 */
@Singleton
class PlaybackProgressRepository @Inject constructor(
    private val dao: PlaybackProgressDao
) {

    /** Fired after a local save / clear, so a future Firebase sync layer can upload. */
    var onProgressSaved: ((PlaybackProgressEntity) -> Unit)? = null
    var onProgressCleared: ((String) -> Unit)? = null

    suspend fun save(
        contentLink: String,
        positionMs: Long,
        durationMs: Long,
        title: String? = null,
        thumbnail: String? = null,
        contentType: String? = null
    ) {
        if (contentLink.isBlank()) return
        // Saving near the very end clears the record — don't resume into the credits.
        if (durationMs > 0 && positionMs >= durationMs - END_TAIL_MS) {
            clear(contentLink)
            return
        }
        val existing = dao.get(contentLink)
        val entity = PlaybackProgressEntity(
            contentLink = contentLink,
            positionMs = positionMs,
            durationMs = if (durationMs > 0) durationMs else existing?.durationMs ?: 0L,
            updatedAt = System.currentTimeMillis(),
            title = title ?: existing?.title,
            thumbnail = thumbnail ?: existing?.thumbnail,
            contentType = contentType ?: existing?.contentType
        )
        dao.upsert(entity)
        onProgressSaved?.invoke(entity)
    }

    suspend fun get(contentLink: String): PlaybackProgressEntity? = dao.get(contentLink)

    /** Position to seek to when re-opening, or null if the record isn't resumable. */
    suspend fun resumePosition(contentLink: String): Long? {
        val record = dao.get(contentLink) ?: return null
        return if (isResumable(record.positionMs, record.durationMs)) record.positionMs else null
    }

    suspend fun clear(contentLink: String) {
        dao.delete(contentLink)
        onProgressCleared?.invoke(contentLink)
    }

    /** Partially-watched movies (have title/thumbnail and are still resumable), newest first. */
    suspend fun inProgressMovies(): List<PlaybackProgressEntity> =
        dao.getAll()
            .filter { !it.title.isNullOrBlank() && isResumable(it.positionMs, it.durationMs) }
            .sortedByDescending { it.updatedAt }

    fun isResumable(positionMs: Long, durationMs: Long): Boolean {
        if (positionMs <= MIN_RESUME_MS) return false
        if (durationMs <= 0) return true // unknown duration — resume anything past the min
        val fractionCutoff = (durationMs * FINISHED_FRACTION).toLong()
        val tailCutoff = durationMs - END_TAIL_MS
        return positionMs < minOf(fractionCutoff, tailCutoff)
    }

    companion object {
        private const val MIN_RESUME_MS = 5_000L
        private const val END_TAIL_MS = 15_000L
        private const val FINISHED_FRACTION = 0.85
    }
}
