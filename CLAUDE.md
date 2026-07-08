# 1HD Movies — Android app

A Kotlin app that browses and streams movies / TV shows by **scraping the website `1hd.art`** (no official API). Content is parsed from HTML with **Jsoup**; playback streams (`.m3u8`) are sniffed out of the embed pages at runtime with a hidden `WebView`, then played with **ExoPlayer (media3)**.

It was built to reach **feature + design parity with the iOS app** at `/Users/gleb/ios/1HDMovies` (see that repo's `CLAUDE.md` for the canonical feature spec). Runs on **both phone and Android TV** (and tablets) from one codebase via resource qualifiers.

## Tech stack
- **Kotlin**, MVVM with `ViewModel` + `LiveData`. Views are Fragments/Activities with **ViewBinding** (no Compose).
- **Hilt** for DI (`@HiltAndroidApp App`, `@AndroidEntryPoint`, `@HiltViewModel`). Modules in `etc/module/` (`PreferencesModule`, `FirebaseModule`, `GsonModule`, `DatabaseModule`).
- **Custom navigation** — a `Router` sealed class + `NavigationRouter` (fragment `replace()` + manual back stack). **No** Jetpack Navigation / nav graph.
- **Persistence:** SharedPreferences (favorites, watching-shows, sync timestamps) **and Room** (`AppDatabase`, db file `1hd-movies.db`) for playback/watched/notification data.
- **ExoPlayer / media3** in a separate `VideoPlayerActivity`. **Jsoup** for scraping. **Glide** for images. **Gson**. **OkHttp** + **Conscrypt** (modern TLS).
- **Firebase** Auth + Firestore for cross-device sync; **Google Sign-In**.
- Base site URL is `BuildConfig.BASE_URL` = `https://1hd.art` (set in `app/build.gradle` `buildConfigField`). Package root: `com.a1hd.movies`.
- `minSdk 26`, `compileSdk 36`, Kotlin 2.3, AGP 9.1, **KSP2**.

## Build / run
```bash
./gradlew :app:assembleDebug          # from the project root (/Users/gleb/android/1HDMovies)
```
- If `./gradlew` isn't found, the shell cwd drifted — `cd /Users/gleb/android/1HDMovies` first.
- Use `assembleDebug` (or `:app:compileDebugKotlin` for a faster compile-only check) to verify changes. There are no meaningful unit tests.

## Project layout (`app/src/main/java/com/a1hd/movies/`)
- `App.kt` — `@HiltAndroidApp`. Inits Conscrypt + Firebase; wires **sync hooks** (favorites, playback progress, watched movie/episode, notifications → `FirebaseSyncService` uploads/deletes, guarded on signed-in); on startup runs `syncService.syncAll()` (if signed in) then `newEpisodeService.checkForNewEpisodes()`.
- `ui/MainActivity.kt` — single `fragment_container`; inits `NavigationRouter`, shows Splash then Dashboard after 2s; sets **landscape for TV/tablet, portrait for phone**; disables SSL cert check (`disableSSLCheck`). `Context.isTvOrientation()` / `isTabletOrientation()` helpers live here.
- `ui/navigation/` — `NavigationRouter` + `route/Router.kt` (sealed routes + `toFragment()`/`toRouter()`). Routes: Splash, Dashboard, MovieDetails, WatchMovie, AllMovies, AllTvShows, MovieByGenre, **Tag(title,url)**, Search, Favorites, **Watched**, **Notifications**, Account, Filter.
- `api/` — `RestHttpClient` (Jsoup/OkHttp GET with spoofed UA). `api/repository/` — **scraping repos** (`ParseJsonDashboardRepository`, `ParseJsonMostPopularRepository`, `ParseJsonMoviesRepository`, `ParseJsonTvShowsRepository`, `ParseJsonMoviesGenresRepository`, `ParseJsonMovieDetailsRepository`, `ParseJsonSearchRepository`, `ParseJsonFilterRepository`, `ParseJsonYouMayAlsoLikeRepository`). **Data models are declared inline** in these files: `MoviesDataModel`, `MoviesDetailsDataModel` (+ `TagRef`), `MovieSeasonDataModel`, `MovieEpisodesDataModel`, `MostPopularMoviesDataModel`, `MovieType` enum, `GenresEnum`.
- `db/` — Room. `entity/Entities.kt` (`PlaybackProgressEntity`, `WatchedMovieEntity`, `WatchedEpisodeEntity`, `ShowEpisodeSnapshotEntity`, `ShowNotificationEntity`), `dao/Daos.kt`, `AppDatabase`. `db/repository/` — `PlaybackProgressRepository`, `WatchedRepository`, `WatchedEpisodeRepository`, `NotificationRepository`, `ContinueWatchingRepository`, `WatchingShowRepository`.
- `services/` — `AuthenticationService` (Google/Firebase), `FirebaseSyncService`, `NewEpisodeService`.
- `ui/sections/` — the screens. `dashboard/` (+ `viewpager/ViewPagerAdapter` hero slider, `adapter/` rows, `manager/TVLinearLayoutManager`), `movie/` (details + `watch/` player), `search/`, `filter/`, `favorite/` (`repository/FavoriteRepository`, SharedPreferences), `watched/`, `notifications/`, `account/`, `allmovies/`, `alltvshows/`, `genre/`.
- `ui/views/` — `VideoWebView` (stream/subtitle/server sniffer), `SubtitleModels` (VTT parser).
- `etc/extensions/` — `PrefsExtention` (SharedPreferences delegates incl. `mutableList` JSON via Gson), `CoroutinesExtension` (`ViewModel.launch`, `io {}`).

## Form factors (phone / TV / tablet)
- Two layout sets chosen by resource qualifier: **`res/layout/`** (phone: portrait, side/top button bar, grids of `@integer/grid_span_count`) and **`res/layout-television/`** (TV: landscape, 5-col grids, left side menu). TV is selected by the leanback feature; phones/tablets use the default `layout/`.
- **Manifest:** `android.software.leanback` is `required="false"` (so it installs on phones too); `touchscreen` `required="false"`. Both `LEANBACK_LAUNCHER` + `LAUNCHER` categories.
- **Tablets:** `res/layout-sw600dp/fragment_movie_details.xml` is an iPad-style side-by-side details layout (poster left, info right; seasons/episodes/You-May-Also-Like full width below). Sizing comes from **`values`/`values-sw600dp`/`values-sw720dp/dimens.xml`** (`details_*`, `player_*`) and **`integers.xml`** (`grid_span_count` = 2 / 4 / 6). When adding a tablet-only design, prefer dimension/integer resources over new layout files.
- **ViewBinding across variants:** an id referenced non-null from a fragment/activity **must exist in every layout variant that view uses** (phone + TV, and tablet if present), otherwise the generated binding field becomes `@Nullable` and won't compile. When adding a view, add it to all relevant variants (or use `view.findViewById` for optional ones — `BaseFragment` wires an optional `@+id/btnBack` this way).

## Key flows
- **Browsing:** `DashboardViewModel` loads the hero slider (`MostPopular`), Continue Watching, New Releases (same data as slider but opens details), Top Movies/Shows, genre rows (incl. Animation), and the notification badge count. Rows are horizontal `RecyclerView`s.
- **Details:** `MovieDetailsViewModel.fetchDetails` scrapes the page; for TV it fetches seasons then episodes via an AJAX endpoint. Lands on the **current-watching season** (not always the last). Clickable metadata chips (genre/cast/country/production/year) open `Router.Tag` listings via `ParseJsonMoviesGenresRepository.fetchMoviesByUrl`.
- **Playback:** `WatchMovieFragment` loads the embed page in a hidden `VideoWebView` that sniffs the `.m3u8` + subtitles + server list, then launches `VideoPlayerActivity` for result. The player: resume, per-episode watched marking, speed, subtitle/server pickers, prev/next episode, edge & double-tap ±10s seek.
- **Continue Watching** (`ContinueWatchingRepository`): TV shows come from **favorites OR `WatchingShowRepository`** (a show is "remembered" when you open an episode — `MovieDetailsViewModel.rememberWatchingShow()`), excluding show-level-watched ones; proposes the resume/next episode. Movies come from `PlaybackProgressRepository.inProgressMovies()`.
- **Firebase sync** (`FirebaseSyncService.syncAll`, launch when signed in): favorites + `playbackProgress` + `watchedMovies` + `watchedEpisodes` + `episodeSnapshots` + `showNotifications`, all **last-writer-wins by a date field**. Stable keys (URLs) are **Base64-URL-encoded into Firestore doc ids**. Single-item uploads fire from repo mutation hooks (wired in `App.kt`).
- **New episodes** (`NewEpisodeService`, launch, 6h throttle; Notifications screen refresh forces it): diffs fresh episode links against `ShowEpisodeSnapshot`; new episodes → `ShowNotification` (bell badge) and refresh the stored favorite. A show-level-watched favorite that gets new episodes is un-watched (resurfaces).

## Implemented features (parity with iOS — so future sessions know they exist)
Continue Watching row; resume playback (per episode & movie, stable content-link key, 85% cutoff); per-episode watched (5-min threshold + long-press toggle); separate **Watched** screen (eye icon in Favorites; Favorites excludes watched); **Notifications** (bell + red badge, snapshot diff); New Releases row; Animation genre row; clickable cast/genre/country/production/year chips; **Trailer** button (YouTube search); full-screen poster; favorite/watched icon toggles on details; open show on current season; Firebase sync of all of the above; iOS-styled hero slider (bottom-left title/quality/description + pagination dots, no gradient scrim — text shadow instead); black + red (`#FF3B30`) palette; custom player UI (centered play/pause, edge ±10s, skip-episode, speed, subtitles, server, close, double-tap seek).

## Gotchas learned the hard way
- **Room must be 2.8.x.** Kotlin 2.3 uses KSP2; Room < 2.7 fails `kspDebugKotlin` with `IllegalStateException: unexpected jvm signature V`. Use `fallbackToDestructiveMigration(dropAllTables = true)`.
- **Never put `android:background` in the app theme** — a theme's `android:background` bleeds onto every view that lacks its own background (it caused a black box behind bg-less views like the episode number). Use **`android:windowBackground`** for the app background.
- **media3 overrides the `exo_rew`/`exo_ffwd` button icons at runtime**, so a custom `android:src` on them is ignored. The −10/+10 buttons therefore use **custom ids** (`btnRewind`/`btnForward`) with the seek wired manually (`seekBy(±10_000)`); only `exo_play_pause`, `exo_progress`, `exo_position`, `exo_duration` keep exo ids.
- **Scraping selectors are fragile.** The details **title moved h3→h2**, so it's selected by class only: `.heading-xl`. When something shows blank/wrong, `curl` the live page with the spoofed UA and re-check selectors before touching Kotlin.
- **Resume key** is the stable content link (episode link / movie watch URL), threaded `WatchMovieFragment.movieUrl → VideoPlayerActivity EXTRA_CONTENT_LINK` — **never the per-session `.m3u8`**. Episode progress stores no title/thumbnail (episodes get theirs from the favorite/watching-show); movie progress stores title/thumbnail/type so its card can render.
- **Continue Watching for TV shows needs the show's episode list**, which comes from favorites or `WatchingShowRepository`. A never-opened, non-favorited show won't appear (nothing to build the "next episode" card from).
- **Episode watched is marked only after 5 min, and only for shows** (movies rely on progress + the 85% cutoff in `PlaybackProgressRepository.isResumable`).
- **Season/episode chips** use `@drawable/chip_background` (default gray, `state_selected`/`state_focused` → red). Adapters map focus→`isSelected` so TV focus turns the chip red; the currently-playing item is shown **bold**.
- **`isFocusedByDefault = true`** on RecyclerView items can trigger Android's default focus highlight — avoid it on phone lists.
