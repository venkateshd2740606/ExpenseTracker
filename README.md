# ExpenseTracker Pro

Production-ready Android expense-sharing app — split bills, track balances, settle debts, and visualize spending.

## Features

- **Authentication**: Email/password and email OTP (on-device verification, no SMS or email provider)
- **Expenses**: Add, edit, delete, duplicate with equal/unequal/percentage/shares/exact splits
- **Groups & Friends**: Unlimited groups, friend management, invite links
- **Settlements**: Cash, UPI, bank transfer, manual — with smart debt simplification
- **Dashboard**: Health score, streaks, charts, heat maps, activity feed
- **Analytics**: Pie/bar charts, monthly trends, spending insights
- **Offline**: Room database with sync-on-reconnect via Firestore
- **Export**: CSV, Excel (XML), PDF
- **AdMob**: App open, banner, interstitial, rewarded, rewarded interstitial (test IDs in debug)
- **Themes**: Light, dark, system + accent color customization

## Requirements

- Android 8+ (API 26)
- Android Studio Ladybug or newer
- JDK 17

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Firebase Setup

Replace `app/google-services.json` with your Firebase project config. Enable Authentication (Email, Phone) and Firestore.

## Play Store

See `docs/PLAY_STORE.md` for listing copy, ASO keywords, and compliance checklist.

## Architecture

MVVM + Clean Architecture + Hilt DI + Room + Jetpack Compose + Material 3
