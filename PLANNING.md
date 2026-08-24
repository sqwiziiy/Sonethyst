# Sonethyst Planning

Sonethyst is an independent Android music player fork based on Aurora.

## Permanent project decisions

- App name: Sonethyst
- Package/application ID: `com.mentality.sonethyst`
- Material 3 / Material You remains the core design language.
- Dynamic Color remains a first-class feature.
- Preserve Apache-2.0 attribution and applicable third-party licenses.
- Do not rely on proprietary fonts or proprietary build assets.
- Keep upstream Aurora configured as a read-only upstream remote.
- Prefer reproducible builds from a clean clone.

## Phase 0 — Fork foundation

- [x] Fork Aurora
- [x] Configure `origin` and `upstream`
- [x] Configure JDK 21 / Android SDK 35 / NDK 27 / CMake 3.22.1
- [x] Bootstrap external libFLAC dependency
- [x] Produce successful upstream debug build
- [x] Remove hard dependency on unavailable Circular Std
- [ ] Rename app to Sonethyst
- [ ] Change application ID to `com.mentality.sonethyst`
- [ ] Decide whether/when to migrate Kotlin namespace/packages
- [ ] Replace Aurora-specific URI schemes and integration identifiers
- [ ] Add Sonethyst branding and launcher icon
- [ ] Adopt permanent open-source typography
- [ ] Create secure release signing configuration
- [ ] Ensure secrets/keystores are never committed
- [ ] Verify Aurora and Sonethyst can be installed side by side
- [ ] Add build/setup documentation

## Design principles

- Material 3 / Material You
- Dynamic Color
- Light and Dark themes
- AMOLED-black option
- Optional custom accent
- UI should remain coherent under arbitrary system palettes
- Native Android interaction patterns
- Smooth Material motion and transitions
- Album-art-derived color may be used where appropriate

## Phase 1 — Playlist UX

- [x] Add "Add to playlist" to song menu
- [x] Playlist picker
- [x] Create playlist from picker
- [x] Refresh playlist/library UI immediately after playlist create/add/edit/delete/import
- [x] Keep playlist picker surfaces visually consistent with the Material 3 bottom sheet
- [ ] Add songs from playlist screen
- [x] Remove songs from playlist
- [ ] Multi-select
- [ ] Add selected songs to playlist / queue / likes
- [ ] Drag-and-drop reorder — drag tracks by a dedicated handle, animate surrounding rows while moving, preserve normal row tap behavior, and persist the final order
- [ ] Playlist cover management
  - [ ] Choose a custom image from the device
  - [ ] Choose artwork from any track already inside the playlist
  - [ ] First-track artwork mode — playlist cover automatically follows the current first track and updates after reorder — playlist cover automatically follows the current first track and updates after reorder
  - [ ] 2x2 collage artwork mode
  - [ ] Automatic/default artwork mode
  - [ ] Reset a custom cover back to automatic
  - [ ] Persist the selected cover mode and custom artwork
  - [ ] Do not overwrite an explicitly selected custom/track cover when playlist contents change
- [ ] Preserve M3U/M3U8 import/export


## Playlist / library performance

- [ ] Optimize scrolling performance for large song, album, artist and playlist lists
- [ ] Use stable LazyColumn/LazyGrid item keys everywhere
- [ ] Add appropriate `contentType` values for heterogeneous lazy lists
- [ ] Minimize unnecessary Compose recompositions while scrolling
- [ ] Avoid reloading complete library datasets when only one playlist/item changed
- [ ] Optimize album-art loading, resizing and caching for list thumbnails
- [ ] Avoid full-size artwork decoding for small list rows
- [ ] Review paging/incremental loading for very large song libraries
- [ ] Keep scroll position stable after likes, playlist mutations and metadata changes
- [ ] Profile scrolling for dropped frames and excessive allocations on a real device

## Phase 2 — Library

- [ ] Genres
- [ ] Ratings
- [ ] Custom tags
- [ ] Hide tracks/albums
- [ ] Folder blacklist
- [ ] Improved duplicate detection
- [ ] Better handling of multiple versions/edits of one song
- [ ] Playlist folders
- [ ] Pinning improvements

## Phase 3 — Metadata

- [ ] Batch tag editing
- [ ] Album Artist support
- [ ] Better multiple-artist handling
- [ ] Batch MusicBrainz matching
- [ ] Artwork search/replacement
- [ ] Metadata backup/restore
- [ ] Lyrics editor
- [ ] Synced-lyrics offset/editor
- [ ] Improve M3U matching for Unicode/non-Latin metadata
- [ ] Use local path/basename before fuzzy M3U matching where possible

## Phase 4 — Audio quality tooling

- [ ] Show codec
- [ ] Show bitrate
- [ ] Show sample rate
- [ ] Show bit depth
- [ ] Lossless indicator
- [ ] Hi-Res indicator
- [ ] Probable fake-lossless detection
- [ ] Batch ReplayGain
- [ ] Identify low-quality tracks worth replacing

## Phase 5 — Player UX

- [ ] Improved Now Playing
- [ ] Make the liked/favorite heart use the active track artwork-derived accent instead of a fixed theme color; fall back to MaterialTheme.colorScheme.primary when no track accent is available and preserve sufficient contrast
- [ ] Waveform/visualization improvements
- [ ] Artwork options
- [ ] Credits
- [ ] Detailed active audio-stream information
- [ ] Additional useful gestures

## Android / Samsung integration

- [ ] Correct Media3 system media session behavior
- [ ] Notification-shade controls
- [ ] Lock-screen media controls
- [ ] Samsung Now Bar / Live Notifications
- [ ] Correct artwork/title/artist in system UI
- [ ] Play/Pause/Previous/Next
- [ ] Seek/progress integration
- [ ] Bluetooth/headset controls
- [ ] System Media Output picker
- [ ] Android Auto
- [ ] Test on current One UI

## Phase 6 — Advanced audio

Only after the application foundation and UX are stable:

- [ ] DSP architecture work
- [ ] Media3 playback pipeline changes
- [ ] FFmpeg path
- [ ] USB DAC
- [ ] Bit-perfect path
- [ ] Native C/C++ audio code


## Repository presentation

- [ ] Redesign README as a Sonethyst product landing page
- [ ] Add Sonethyst logo / launcher artwork to README
- [ ] Add real app screenshots
- [ ] Add concise feature highlights and project status
- [ ] Move detailed build documentation to `docs/BUILDING.md`
- [ ] Move architecture/development notes to `docs/DEVELOPMENT.md`
- [ ] Move advanced audio / USB DAC documentation to `docs/AUDIO.md`
- [ ] Move troubleshooting details to `docs/TROUBLESHOOTING.md`
- [ ] Keep Aurora attribution and third-party credits clearly visible
- [ ] Add GitHub Releases installation section when release builds are available

## Technical debt / cleanup

- [ ] Deprecated Android Virtualizer API usage
- [ ] Deprecated Material icons -> AutoMirrored equivalents
- [ ] Review other compiler warnings
- [ ] Add CI build
- [ ] Add tests where practical

## Localization / Multilanguage

- [ ] Audit all user-visible hardcoded strings
- [ ] Move user-visible text to Android string resources
- [ ] Use `stringResource()` throughout Compose UI
- [ ] English as the base/fallback language
- [ ] Russian translation
- [ ] Add additional community translations later
- [ ] Per-app language selector
- [ ] Follow Android system language by default
- [ ] Correct plurals with `<plurals>`
- [ ] Locale-aware numbers, dates, times and durations
- [ ] RTL layout support and testing
- [ ] Test long translated strings and UI truncation
- [ ] Add pseudo-localization testing
- [ ] Keep artist, album and track metadata untranslated
- [ ] Keep the Sonethyst brand name untranslated
- [ ] Make new UI features localization-ready from the start
