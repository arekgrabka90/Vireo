# VIREO

**Your entertainment. One experience.**

VIREO is a multimedia application bringing live television, movies and series together in one coherent entertainment environment. The project focuses on a premium viewing experience, a clear interface, reliable playback and a flexible foundation for future development.

## Vision

VIREO aims to make discovering and watching content simple and consistent. Live channels, movies and series are presented within one application, supported by personalized profiles and a unified playback experience.

The product is being developed around its own identity, design language and technical architecture.

## Content & Entertainment

### Live TV
- Access to live television channels through supported IPTV sources.
- Channel information and logos when supplied by the source.
- Electronic Programme Guide (EPG), including current and upcoming programmes.
- Relevant programme information presented alongside live playback.

### Movies
- Movie catalogue enriched with metadata from TMDB.
- Titles, descriptions, release years, genres, ratings and runtime.
- Movie artwork and logos where available.
- Playback using supported VOD sources.

### Series
- Series catalogues with seasons and episodes.
- Episode-specific information, descriptions and artwork where available.
- Clear navigation between series, seasons and episodes.
- Playback using supported VOD sources.

## Premium Playback Experience

VIREO is being built around a shared playback foundation for supported content types. The interface provides contextual on-screen information while keeping the video visible.

The playback experience focuses on:
- A clean, transparent on-screen display.
- Content-aware controls and information.
- Clear playback state and timeline feedback when reliable data is available.
- Consistent behaviour across Live TV, movies and series.
- Stability and predictable playback-session handling.

## Profiles & Personalization

VIREO is designed to support individual user profiles. The long-term goal is to keep preferences and settings organized per profile, allowing each viewer to shape their own experience.

## Designed for Android

The project is being developed for Android, with a responsive experience planned for phones, tablets and Android TV. Interface behaviour will adapt to the capabilities and interaction patterns of each device.

## Architecture & Technology

VIREO uses a modular architecture intended to support continued development and maintainability.

Key technology areas include:
- **Android** — application platform.
- **Jetpack Compose** — modern declarative interface.
- **Media3** — media playback and playback-session integration.
- **TMDB** — movie and series metadata.
- **Xtream-compatible sources** — supported Live TV and VOD catalogues.
- **EPG/XMLTV** — live programme information.

Each source has a defined role: catalogue and stream providers supply available content and playback sources, TMDB enriches movie and series information, and EPG supplies programme schedules and descriptions for Live TV.

## Reliability & Quality

Reliability is a core development priority. Automated builds and tests help detect regressions and keep changes verifiable.

Development priorities include:
- Predictable playback and session handling.
- Clear separation of data-source responsibilities.
- Graceful handling of missing metadata and artwork.
- Maintainable code and focused changes.
- Automated verification through the project build pipeline.

## Development Roadmap

VIREO is under active development. Features are introduced in stages, with emphasis on a stable foundation before broader expansion.

The roadmap includes continued work on:
- Playback and the on-screen experience.
- Live TV, EPG and channel management.
- Movie and series catalogue experiences.
- Profile-based settings and personalization.
- Responsive layouts for different Android devices.
- Performance, reliability and overall product polish.

## Project Status

VIREO is a work in progress. Some capabilities are implemented, while others remain planned or are being refined. The repository and its development history reflect the current state of the project.

## Project Philosophy

VIREO is built around a straightforward idea: bring different forms of entertainment into one thoughtful, cohesive experience.

**VIREO — Your entertainment. One experience.**
