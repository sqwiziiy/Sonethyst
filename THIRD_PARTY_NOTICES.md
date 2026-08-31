# Sonethyst third-party notices

Sonethyst is licensed under GPL-3.0-only. Third-party components remain under
their respective licenses; the full GPL, LGPL, Apache, and BSD texts are in
`app/src/main/assets/licenses/` and are packaged under `assets/licenses/`.

This release distributes the native components listed below. The same
attribution is packaged in the APK under `assets/licenses/`.

## Chromaprint and internal resampler

`app/src/main/cpp/chromaprint/` is derived from Chromaprint, copyright Lukas
Lalinsky and contributors. Chromaprint's own source is MIT-licensed. The
copied `avresample/resample2.c` and related headers are FFmpeg-derived
LGPL-2.1-or-later code compiled into `libsonethyst_fp.so`, so that component
must be treated as LGPL-covered.

The build selects bundled KissFFT (`USE_KISSFFT`) and compiles
`kiss_fft.c` and `tools/kiss_fftr.c`; its BSD-3-Clause notice is packaged as
`assets/licenses/kissfft-COPYING.txt`; the full LGPL text is in
`assets/licenses/LGPL-2.1-or-later.txt`.

## Jellyfin AndroidX Media3 FFmpeg decoder

The APK includes `libffmpegJNI.so` from
`org.jellyfin.media3:media3-ffmpeg-decoder:1.5.0+1`, published from the
`jellyfin/jellyfin-androidx-media` tag `v1.5.0+1` (commit
`9bbeb8b73a05ec3f09751a6589cc87ce651585d6`). The
cached POM declares GPL-3.0. The artifact does not contain a reproducible
configuration report proving `--enable-gpl` or `--enable-nonfree`; the
published artifact is nevertheless declared GPL-3.0.

Upstream source/build references:

- https://github.com/jellyfin/jellyfin-androidx-media/tree/v1.5.0%2B1
- https://github.com/jellyfin/jellyfin-androidx-media/blob/v1.5.0%2B1/build.sh

## decent USB audio and libFLAC

The vendored `decent/` modules are MIT-licensed. Their notice, including the
bundled libFLAC BSD notice, is in `decent/NOTICE.md` and is packaged as
`assets/licenses/decent-NOTICE.md`.

## Status

FFmpeg source provenance is upstream commit
`b98349b2055a93b2a22381bc1a4c09c229f2b3cb` on release/6.0; the corresponding
LGPL text is packaged separately. The Jellyfin artifact is distributed under
its GPL-3.0 declaration.
