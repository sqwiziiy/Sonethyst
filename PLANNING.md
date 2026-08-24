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

- [ ] Add "Add to playlist" to song menu
- [ ] Playlist picker
- [ ] Create playlist from picker
- [ ] Add songs from playlist screen
- [ ] Remove songs from playlist
- [ ] Multi-select
- [ ] Add selected songs to playlist / queue / likes
- [ ] Drag-and-drop reorder
- [ ] Custom playlist covers
- [ ] First-track artwork mode
- [ ] 2x2 collage artwork mode
- [ ] Preserve M3U/M3U8 import/export

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

## Technical debt / cleanup

- [ ] Deprecated Android Virtualizer API usage
- [ ] Deprecated Material icons -> AutoMirrored equivalents
- [ ] Review other compiler warnings
- [ ] Add CI build
- [ ] Add tests where practical
