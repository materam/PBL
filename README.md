# Mindful Launcher

A free, text-only Android launcher that makes you pause before opening distracting apps.

**Website:** https://materam.github.io/PBL/
**Download:** [mindful-launcher.apk](https://github.com/materam/PBL/releases/latest/download/mindful-launcher.apk) (Android 8.0+)

## The problem

Most phone use isn't a decision. People unlock their phone to check one thing and open Instagram or YouTube out of habit. App timers and blockers are easy to dismiss, and they say nothing about *why* you reached for the app.

## What Mindful does

Mindful replaces your home screen. Apps are shown as plain words, with no icons, badges or colours. For apps you mark as distracting, Mindful adds a **mindful pause** before they open:

1. **Wait.** A breathing countdown (10 s by default) runs before anything happens.
2. **Write your reason.** The open button stays locked until you type why you're opening the app.
3. **Choose a session length.** 5, 10, 15 or 30 minutes.
4. **Get a reminder.** A notification tells you when your session is up.

Apps without a pause (calls, maps, banking) open instantly.

### Other features

- Home screen with a clock, the date and up to 7 apps as text
- Search-first app list: swipe up, type, press enter to open the top result
- Focus hours (for example 22:00–07:00), when pauses last 30 seconds or more
- Intention log: every reason you write is saved so you can review your habits
- Daily open counts for paused apps
- Hide apps from the list without uninstalling them

## Privacy

Mindful has **no internet permission**. Settings, reasons and counts are stored only on the device in SharedPreferences. There are no accounts, ads or analytics.

## Using it

1. Install the APK and open **Mindful**.
2. Swipe up to see all apps. Hold an app to add it to the home screen, turn on its pause, hide it, or uninstall it.
3. Hold an empty part of the home screen to open **Settings**, then tap **Set Mindful as your home app**.

## Building

The project uses Kotlin and the plain Android framework (no AndroidX), so the APK stays small and has no third-party dependencies.

```bash
# Requires JDK 17, the Android SDK (platform 34) and Gradle 8.7+
gradle assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

Every push to `main` builds the APK with GitHub Actions and publishes it to [Releases](https://github.com/materam/PBL/releases). The website lives in `docs/` and is served by GitHub Pages.

> The release APK is currently signed with a debug key for sideloading. Before publishing to the Play Store, create a private release keystore.

## Project structure

```
app/src/main/java/com/materam/mindful/
  HomeActivity.kt      Home screen: clock, date, favourite apps, gestures
  DrawerActivity.kt    Searchable app list
  GateActivity.kt      The mindful pause screen
  SessionReceiver.kt   Session-over reminder notifications
  SettingsActivity.kt  Pause apps, focus hours, hidden apps, logs
  Prefs.kt             On-device storage
  Apps.kt              Loading, launching and the long-press menu
docs/                  Website (GitHub Pages)
```

## Roadmap

- Optional on-device AI that reads your reason and gently challenges vague ones ("just bored")
- Weekly reflection screen built from the intention log
- Grayscale mode and a home-screen widget for today's screen time
- Play Store release

## Background

Mindful started as a Project-Based Learning (PBL) project at Sikkim Manipal Institute of Technology, exploring whether adding a moment of friction and a written intention reduces habitual phone use.

## License

[MIT](LICENSE) © 2026 Saksham
