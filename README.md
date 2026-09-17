# Covid Zero 🦠

A very 2021–2022 Android side project: COVID stats, vaccine-center maps, QR check-ins, vaccination-token experiments, news, and a self-isolation timer — all living in one rather ambitious app.

I built this while learning how far I could push a native Android app with public APIs, Firebase, maps, camera/QR features, a bundled SQLite database and a lot of Activities.

> **Historical project:** this repository reflects a pandemic-era learning project. Its health guidance, isolation period, vaccine-center data and external COVID services should be treated as historical/demo data, not current medical or government guidance.

## what it actually does

### 📊 COVID dashboard

The home screen switches between Sri Lanka and global statistics using the `disease.sh` API and renders active, recovered and deceased counts in an animated pie chart.

### 🗺️ vaccine centers

Vaccine-center information is bundled in the app's SQLite database and can be viewed as a list or on Google Maps. The project uses stored latitude/longitude values for map markers and directions.

### 💉 vaccination-token prototype

There is a multi-step flow for recording vaccine-dose details, storing vaccination records in Firebase Realtime Database, generating a digital token/QR image, uploading token media to Firebase Storage and reopening previous tokens.

This was a prototype feature — **not an official vaccination credential system**.

### 📷 safe check-in experiment

The check-in module scans a business QR code, validates the code against Firebase, lets the user add people/frequent guests, and stores visit history for both the user and business side of the prototype.

### ⏳ self-isolation countdown

A 14-day countdown is persisted with `SharedPreferences` so it can resume after leaving the screen. The app also contains a local completion-notification experiment.

The 14-day value is part of the original 2021–2022 project and should not be interpreted as current health advice.

### 📰 COVID news

A Retrofit client requests COVID-related headlines from NewsAPI. The API key is no longer committed to the repository; local builds read it from `secrets.properties`.

### 👤 accounts & profile

Firebase Authentication handles email/password and Google sign-in flows. User/profile information is stored in Firebase Realtime Database, with profile media handled through Firebase Storage.

## screenshots

![Covid Zero screens](https://user-images.githubusercontent.com/90299964/232234193-f6762f42-9b66-4bfd-ba72-5420cc10d967.png)

## built with

`Java` · `Android XML` · `Firebase Auth` · `Realtime Database` · `Firebase Storage` · `Google Maps` · `Volley` · `Retrofit` · `SQLite` · `ZXing / CAMView` · `EazeGraph` · `MPAndroidChart` · `Glide` · `Picasso`

The current project snapshot targets the Android toolchain from that period rather than pretending this is a freshly modernized app:

```text
compileSdk 31
minSdk 26
targetSdk 31
Java 8
```

## running it

This repo intentionally does **not** include Firebase or API credentials.

1. Clone the project and open it in Android Studio.
2. Create your own Firebase project and add an Android app using package `com.myhealthplusplus.app`.
3. Put your own `google-services.json` in `app/`.
4. Copy `secrets.properties.example` to `secrets.properties`.
5. Add your own NewsAPI and Google Maps keys:

```properties
NEWS_API_KEY=your_newsapi_key
MAPS_API_KEY=your_google_maps_key
```

6. Sync Gradle and run the app.

Some pandemic-era APIs, URLs or Firebase assumptions may no longer behave exactly as they did when the project was built.

## a few things i'd change now

This project predates the way I build mobile apps today. The UI is Activity-heavy, screens talk directly to Firebase, network/loading state is handled manually, several flows depend on callback timing, and the app combines a lot of unrelated responsibilities.

If I rebuilt it now I'd split the data layer from the UI, model screen state explicitly, use lifecycle-aware async APIs, make the health content remotely configurable, move sensitive/privileged operations behind a backend, test Firebase rules, and treat offline/error states as first-class UI states.

I left the original structure recognizable because that's the interesting part of keeping this public: you can see both what I was trying to build and the engineering lessons it exposed.

For a deeper walkthrough of the architecture, data flows, security cleanup and known limitations, see [`docs/engineering.md`](docs/engineering.md).

## project status

**Archived in spirit, maintained as a portfolio/code-history project.**

I may clean up documentation and obvious correctness/security problems, but I'm not turning Covid Zero into a current health product.

---

made by [Senith Umesha](https://github.com/SenithUmesha)

### disclaimer

Covid Zero is a learning/demo project. It is not a medical device, official public-health application, vaccination credential, contact-tracing service, or source of current health guidance.
