# Changelog

All notable changes to Kvizo Android will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.1] - 2026-09-16

### Added
- **Website link** in About → Developer (opens the Kvizo site, hosted on Netlify)
- **Verified community fetch**: signed `manifest.json` is checked first, then every quiz against its
  SHA-256 from that manifest (with the older signed `community.json` bundle as a fallback)
- **Self-repair**: community quizzes imported by an older build are rebuilt in place on sync

### Fixed
- **Community quizzes marked every correct answer as wrong** — the import stored the answer *text*
  instead of its position, so `"b" == "Bjarne Stroustrup"` never matched. Now the answer position is
  stored and scored like any local quiz
- **Community quizzes could not load**: the app verifies against the live signing key
  (`kvizo-pub-2026-09`) and fetches from the free `raw.githubusercontent.com` / `jsDelivr` mirrors
  instead of the retired Cloudflare Worker
- Community fetch no longer runs on the main thread

### Changed
- Community **announcement notifications** removed (notifier, background worker, settings toggle and
  the unverified `notifications.json` fetch path)
- R8 optimization for improved startup time and smoother transitions
- APK size **3.3 MB** (from 20 MB debug) with resource shrinking

### Security
- Content that is not signed by the Kvizo Ed25519 key is dropped, whoever serves it
- Cached bundles are re-verified on every read, so a tampered cache is ignored rather than trusted

## [2.0.0] - 2026-09-09

### Added
- 80+ badges across 15 categories with unlock celebrations
- Community quizzes with one-tap import
- Quiz share codes — share any quiz offline via clipboard
- Quiz exit confirmation & auto-pause only on true backgrounding
- Brand-new app icon
- Smoother buttons, clipped ripples & press animations
- Full 5-language support with instant switching

## [1.5.0] - 2026-08-08

### Added
- Speedometer score sweep — settles exactly on your score
- One-shot confetti celebration on finish
- Settings redesign: segmented theme, cleaner controls
- Telegram-style dark mode colors
- Updater, changelog & about screens
- Backup & Restore with JSON import
- Quiz pauses automatically when you leave the app

## [1.4.0] - 2026-08-06

### Added
- Violet redesign — new UI everywhere
- Space Grotesk display font
- Dark mode improvements & SVG icons
- Press-glow buttons

## [1.0.0] - 2026-08-03

### Added
- The first Kvizo release
- Offline quizzes & local profiles

## Community Quizzes — How Publishing Works

There is no backend to run or pay for. Quizzes live in the `SirYadav1/kvizo-community` repository and
are served to the app as **signed static files** by free CDNs.

### Flow
1. **Publish**: quiz JSON lands in `quizzes/**` in `SirYadav1/kvizo-community` (plain commit, PR or
   the *Publish Quiz* GitHub Action)
2. **Sign**: the repository Action validates the quiz, rebuilds `manifest.json` + `community.json`
   and signs them with the Ed25519 private key that only exists as a repository secret
3. **Fetch**: the app downloads the bundle from `raw.githubusercontent.com`
   (mirror: `cdn.jsdelivr.net`)
4. **Verify**: `manifest.json` is verified against the public key embedded in the app, then every
   quiz is checked against the SHA-256 recorded in that signed manifest
5. **Show**: verified quizzes appear under *Community → Download quizzes*

### Security
- Ed25519 public key is embedded in the app; the private key never leaves the content repo secret
- A hijacked CDN, proxy or DNS answer cannot inject a quiz — the signature is verified in-app
- Cached bundles are re-verified on every read, so editing the cache on-device gains nothing
- Publishing is owner-only by construction: without the private key no valid bundle can be produced
