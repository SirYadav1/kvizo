<p align="center">
  <img src="banner.svg" width="100%" alt="Kvizo banner" />
</p>

<h1 align="center">Kvizo</h1>
<p align="center"><strong>Forge knowledge. Earn XP. Play anywhere.</strong></p>
<p align="center">
  <img src="https://img.shields.io/badge/Version-2.0.0-purple?style=flat-square" />
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-green?style=flat-square&logo=android" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue?style=flat-square&logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" />
</p>

<p align="center">
  <a href="https://github.com/SirYadav1/kvizo/releases/tag/v2.0.0"><strong>⬇ Download Kvizo v2.0.0 (APK)</strong></a>
</p>

---

## What is Kvizo?

Kvizo is an offline-first Android quiz app built with Kotlin and Jetpack Compose. Create your own quizzes, take them in normal or timed mode, earn XP, keep daily streaks, unlock 80+ badges, and pull community quizzes straight into the app. No account, no server needed — everything lives on your device.

Community quizzes are served as static files from GitHub — no backend needed, the app works fully offline.

## Features

- **Custom quizzes** — categories, Easy/Medium/Hard/Expert difficulty, tags, per-quiz time limits
- **Timed mode** — race the clock or take it slow
- **XP & levels** — XP for completion, correct answers, perfect runs and streaks; level titles up to Grandmaster
- **80+ badges** — milestones, streaks, accuracy tiers, speed runs, creator and community badges with unlock celebrations
- **Community quizzes** — one-tap import from the community feed
- **Share codes** — share any quiz offline via clipboard; import with one paste
- **Stats & heatmap** — accuracy trends, per-category breakdowns, activity calendar, CSV/PDF export
- **Backup & restore** — full JSON export/import of profiles, stats, badges and quizzes
- **5 languages** — English, Hindi, Chinese, Spanish, French with instant switching
- **Dark mode** — full dark theme with segmented control

## Install

1. Download `Kvizo-v2.0.0.apk` from the [v2.0.0 release](https://github.com/SirYadav1/kvizo/releases/tag/v2.0.0)
2. Open it on your phone and allow "install unknown apps" when asked
3. Play. No login needed.

## Build from source

```bash
git clone https://github.com/SirYadav1/kvizo.git
cd kvizo
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/usr/lib/android-sdk
./gradlew assembleRelease   # signed APK in app/build/outputs/apk/release/
```

Requires JDK 17 and Android SDK (compileSdk 34, minSdk 26).

## Project layout

```
app/src/main/java/com/kvizo/app/
├── data/        # Room-less SQLite (DbHelper), repositories, community fetch
├── logic/       # XpEngine — XP, levels, streaks
├── parsing/     # Quiz text importers
├── ui/screens/  # Compose screens (dashboard, quiz, stats, profile…)
├── ui/components/ # Forge design-system components
└── util/        # Backup, export, share codes, crash logger

```

## License

MIT — see [LICENSE](LICENSE).
