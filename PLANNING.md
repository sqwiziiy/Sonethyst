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
- [x] Rename app to Sonethyst
- [x] Change application ID to `com.mentality.sonethyst`
- [x] Migrate Kotlin namespace/packages to `com.mentality.sonethyst`
- [x] Replace Aurora-specific URI schemes and integration identifiers
- [x] Add Sonethyst branding and launcher icon
- [x] Adopt permanent open-source typography
- [ ] Create secure release signing configuration
- [x] Ensure secrets/keystores are never committed
- [ ] Verify Aurora and Sonethyst can be installed side by side
- [x] Add build/setup documentation

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
- [x] Add songs from playlist screen
- [x] Remove songs from playlist
- [x] Multi-select
- [x] Add selected songs to playlist / queue / likes
- [x] Drag-and-drop reorder — drag tracks by a dedicated handle, preserve normal row tap behavior, and persist the final order
- [x] Playlist cover management
  - [x] Choose a custom image from the device
  - [x] Choose artwork from any track already inside the playlist
  - [x] First-track artwork mode — playlist cover automatically follows the current first track and updates after reorder
  - [x] 2x2 collage artwork mode
  - [x] Automatic/default artwork mode
  - [x] Reset a custom cover back to automatic
  - [x] Persist the selected cover mode and custom artwork
  - [x] Do not overwrite an explicitly selected custom/track cover when playlist contents change
- [x] Preserve M3U/M3U8 import/export


## Playlist / library performance

- [x] Optimize scrolling performance for large song, album, artist and playlist lists
- [x] Use stable LazyColumn/LazyGrid item keys everywhere
- [x] Add appropriate `contentType` values for heterogeneous lazy lists
- [x] Minimize unnecessary Compose recompositions while scrolling
- [x] Avoid reloading complete library datasets when only one playlist/item changed
- [x] Optimize album-art loading and caching for list thumbnails
- [x] Avoid full-size artwork decoding for small list rows
- [x] Review paging/incremental loading for very large song libraries
- [x] Keep scroll position stable after likes, playlist mutations and metadata changes
- [x] Profile scrolling for dropped frames on a real device
- [ ] Profile excessive allocations with desktop tooling — deferred; PC/ADB profiling not performed

## Phase 2 — Library

- [x] Genres
- [ ] Ratings
- [ ] Custom tags
- [ ] Hide tracks/albums
- [x] Local music folder management in Settings
  - [x] Settings → Local music → Music folders
  - [x] Show detected music/source folders
  - [x] Exclude a folder and all descendants from the Sonethyst library
  - [x] Re-enable previously excluded folders
  - [x] Persist folder exclusions between restarts
  - [x] Rescan/update the local library after changes
  - [x] Optional include-only folder mode
  - [x] Folder management must never delete or modify the actual music files
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
- [ ] Refresh edited track metadata immediately across the UI without requiring screen re-entry
- [x] Improve M3U matching for Unicode/non-Latin metadata
- [x] Use local path/basename before fuzzy M3U matching where possible

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

## Playback transitions

- [x] Crossfade between songs
  - [x] Add the setting under Settings → Playback near gapless playback
  - [x] Disabled by default
  - [x] Adjustable crossfade duration
  - [x] Smoothly fade out the outgoing track while fading in the next track
  - [x] Avoid audible volume jumps at the transition
  - [x] Prebuffer the incoming track before the overlap begins
  - [x] Keep the outgoing tail continuous without replaying or skipping it
  - [x] Continue the incoming track after the overlap without restarting its intro
  - [ ] Preserve true gapless playback where crossfade would be undesirable
  - [ ] Define behavior for manual Next/Previous separately from natural track completion
  - [ ] Ensure shuffle, repeat and queue transitions work correctly
  - [ ] Keep Media3/system playback state and seek position correct during overlap
  - [ ] Test with local lossy and lossless files

## Phase 5 — Player UX

- [ ] Improved Now Playing
- [x] Make the liked/favorite heart use the active track artwork-derived accent instead of a fixed theme color; fall back to MaterialTheme.colorScheme.primary when no track accent is available and preserve sufficient contrast
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


## About / project links

- [x] Add a GitHub repository link to Settings → About
  - [x] Show the Sonethyst GitHub repository as a dedicated row/action
  - [x] Open `https://github.com/sqwiziiy/Sonethyst` in the browser/GitHub app
  - [x] Keep Aurora attribution and licensing visible separately

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
