package com.a1hd.movies.db.repository

import com.a1hd.movies.db.dao.ShowEpisodeSnapshotDao
import com.a1hd.movies.db.dao.ShowNotificationDao
import com.a1hd.movies.db.entity.ShowEpisodeSnapshotEntity
import com.a1hd.movies.db.entity.ShowNotificationEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the episode-snapshot and show-notification DAOs. Snapshots are diffed against fresh
 * episode data to detect new episodes; notifications carry the unread new-episode counts.
 * Mirrors the persistence side of the iOS `NewEpisodeService`.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: ShowNotificationDao,
    private val snapshotDao: ShowEpisodeSnapshotDao
) {

    var onNotificationChanged: ((ShowNotificationEntity) -> Unit)? = null

    // --- notifications ---
    suspend fun getAll(): List<ShowNotificationEntity> = notificationDao.getAll()

    suspend fun unreadCount(): Int = notificationDao.unreadCount()

    suspend fun markAllRead() = notificationDao.markAllRead()

    suspend fun get(showLink: String): ShowNotificationEntity? = notificationDao.get(showLink)

    suspend fun upsert(notification: ShowNotificationEntity) {
        notificationDao.upsert(notification)
        onNotificationChanged?.invoke(notification)
    }

    // --- snapshots ---
    suspend fun getSnapshot(showLink: String): ShowEpisodeSnapshotEntity? = snapshotDao.get(showLink)

    suspend fun saveSnapshot(showLink: String, episodeLinks: List<String>) {
        snapshotDao.upsert(
            ShowEpisodeSnapshotEntity(
                showLink = showLink,
                episodeLinksJson = episodeLinks.joinToString(SEPARATOR),
                lastCheckedAt = System.currentTimeMillis()
            )
        )
    }

    fun parseSnapshotLinks(snapshot: ShowEpisodeSnapshotEntity): List<String> =
        if (snapshot.episodeLinksJson.isEmpty()) emptyList()
        else snapshot.episodeLinksJson.split(SEPARATOR)

    companion object {
        private const val SEPARATOR = "\n"
    }
}
