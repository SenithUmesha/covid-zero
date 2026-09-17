# Covid Zero engineering notes

Covid Zero is a native Android/Java project from the 2021–2022 pandemic period. It grew into a surprisingly broad prototype: live COVID statistics, account/profile flows, health information, news, vaccine-center maps, vaccination-token generation, QR scanning, check-ins, visit history and an isolation countdown.

This document describes the codebase as it actually exists. It does not retrofit a modern architecture onto it, and it does not treat historical health rules or datasets as current guidance.

## 1. High-level shape

The app is primarily a collection of Android `Activity` classes backed by XML layouts. Activities talk directly to Firebase, REST APIs, the bundled SQLite database and Android platform APIs.

```text
SplashScreen
    │
    ▼
SignIn / SignUp
    │
    ├── Firebase Authentication
    ├── Realtime Database
    └── Firebase Storage
    │
    ▼
MainActivity
    │
    ├── COVID dashboard ── disease.sh + Volley
    ├── News ───────────── NewsAPI + Retrofit
    ├── Vaccine centers ── bundled SQLite + Google Maps
    ├── Vaccination flow ─ Realtime Database + Storage + QR/token image
    ├── Safe check-in ──── camera/QR + Realtime Database
    ├── Isolation timer ── CountDownTimer + SharedPreferences
    ├── Symptoms/info ──── bundled SQLite + external links
    └── Profile/settings ─ Firebase
```

There is no repository/use-case/ViewModel layer. Most state lives directly inside Activities and adapters.

## 2. Toolchain snapshot

The project intentionally keeps a toolchain close to the period in which it was built:

```text
compileSdk 31
minSdk 26
targetSdk 31
Java 8
```

The app module uses AndroidX plus a mixture of libraries that reflect the original feature experiments:

- Volley for the COVID dashboard request
- Retrofit + Gson for NewsAPI
- Firebase Auth / Realtime Database / Storage / Analytics
- Google Maps and Play Services Location
- EazeGraph and MPAndroidChart for visualisation
- SQLiteAssetHelper for the bundled database
- ZXing / CAMView for QR scanning
- Glide and Picasso for image loading
- Lottie for animated UI

This is not currently a dependency-upgrade project. A full migration would be a separate exercise because several old libraries and APIs would need replacements rather than simple version bumps.

## 3. COVID dashboard

`MainActivity.java` owns the dashboard.

The selected region is represented by an integer flag:

```text
1 -> Sri Lanka
2 -> Global
```

The Activity chooses one of two disease.sh endpoints:

```text
https://disease.sh/v3/covid-19/countries/lk
https://disease.sh/v3/covid-19/all
```

Volley fetches the JSON response and extracts:

```text
active
recovered
deaths
```

Those values are formatted into labels and rendered as three slices in an EazeGraph pie chart.

### Limitations

The screen owns networking, loading UI, parsing and visualisation in one Activity. Failure handling is also uneven: a failed request logs the Volley error but the loading UI can remain active.

A modern implementation would expose a `DashboardState` from a ViewModel/repository and make loading/success/error explicit rather than controlling a `ProgressDialog` directly from the request callback.

The external API is also pandemic-era infrastructure. The repository should not imply that this endpoint or its semantics will remain stable indefinitely.

## 4. Accounts and profile data

The authentication flow supports email/password and Google sign-in through Firebase Authentication.

User-related code also reads/writes Firebase Realtime Database and Firebase Storage. The original project commonly passes values between screens with `Intent` extras and caches some display information in `SharedPreferences` for the dashboard header.

The profile picture URL is then loaded through Glide.

### Architectural limitation

Authentication, profile persistence, UI validation and navigation are tightly coupled inside Activity classes. A more maintainable design would put Firebase calls behind an `AuthRepository` / `ProfileRepository` and expose typed state to the UI.

## 5. News flow

`News_RequestManager.java` builds a Retrofit client for:

```text
https://newsapi.org/v2/
```

The request uses `top-headlines` with:

```text
q=covid
sortBy=publishedAt
language=en
```

### Credential cleanup

The original project stored the NewsAPI key as an Android string resource. That is not an appropriate place for a credential and it also made the value part of the public source.

The current project instead reads:

```properties
NEWS_API_KEY=...
MAPS_API_KEY=...
```

from local `secrets.properties`, which is ignored by Git. Gradle exposes the NewsAPI value through `BuildConfig.NEWS_API_KEY` and the Maps key through a manifest placeholder.

`News_RequestManager` now also fails gracefully when the local NewsAPI value is empty instead of referring to the removed string resource.

### Important limitation

Moving a key out of Git prevents accidentally publishing it in source control, but an API key compiled into an Android app is still recoverable from the APK. For a genuinely sensitive or privileged API, the request should go through a backend that owns the secret.

For this historical project, the local-secret setup is mainly repository hygiene.

## 6. Vaccine-center data

The repository ships a SQLite asset:

```text
app/src/main/assets/databases/HealthPlusPlus.db
```

`DatabaseAdapter.java` opens it through `SQLiteAssetHelper`.

Two tables are referenced by the code:

```text
sick_info
vaccine_centers
```

The vaccine-center query reads:

```text
center
district
police_area
lat
long
```

The same center data is used by the list and map fragments.

The current adapter clears its in-memory result lists before each full-table query. The previous implementation kept appending into the same ArrayLists, which could duplicate rows if a method was called more than once on the same adapter instance.

### Data freshness

The center list is bundled into the APK, so it is a snapshot rather than a live government dataset. That made sense as a learning exercise but is the wrong model for time-sensitive public-health locations.

A current product would use a remotely managed dataset with a visible `lastUpdated` timestamp, cache it locally, and degrade gracefully when offline.

## 7. Google Maps

The map flow uses the stored coordinates from the local vaccine-center database and Google Maps SDK.

The manifest no longer contains a literal Maps credential. Instead:

```xml
android:value="${MAPS_API_KEY}"
```

is populated by Gradle from local `secrets.properties`.

Anyone cloning the repository must provide their own key and should restrict that key in Google Cloud to the intended Android package/signing certificate.

## 8. Vaccination-token prototype

The vaccination module is much more ambitious than the old README suggested.

`GetVaccined.java` allows a signed-in user to enter identity details and works with data under a structure shaped like:

```text
vaccinationTokens/{firebaseUid}/{nic}
```

The stored model includes identity and dose information such as:

```text
firstNameT
lastNameT
postalCodeT
nicT
phoneNumberT
genderT
dateOfBirthT
indigenousT
issuedDateT
validDateT
tokenImageUrl
used
dose1
dose2
dose3
```

Other screens generate/display token imagery, update later-dose state, upload media to Firebase Storage and expose previously generated tokens through a RecyclerView.

### What this feature is not

This is a prototype data model inside a student/side project. It is not a cryptographically verifiable vaccination credential.

A production credential system would require trusted issuance, signed claims, revocation, tamper resistance, strict authorization, minimized personal data and a threat model very different from “store fields under the signed-in user's UID.”

### Callback timing issue

Some original validation methods kick off a Firebase read and then immediately return a boolean to the click handler. The Firebase result arrives later, so flags such as “NIC already has a token” can be stale when the button handler evaluates them.

That is a classic async-state bug. A modern implementation would continue the flow *inside* the completion callback/coroutine after the remote check resolves, rather than mutating a shared boolean and hoping it is ready before the next line runs.

## 9. QR token scanning

The project contains QR scanning in more than one feature area.

The vaccination side includes token scanning/verification UI, while the safe-check-in side uses a camera scanner to read a business code.

For check-in, `CheckInScanner.java` looks up:

```text
checkInBusinesses/{code}
```

in Firebase Realtime Database. A missing node is treated as an invalid code. A valid node opens the add-people flow and passes the scanned code plus a formatted timestamp.

This is an application-level existence check, not a secure signature verification scheme.

## 10. Safe check-in experiment

The check-in module includes:

```text
CheckInHome
CheckInScanner
CheckInAddPeople
CheckInCreateNewGuest
CheckInFrequentGuests
CheckInHistory
CheckInFinish
```

The flow can scan a business code, add the signed-in user and guests, reuse frequent guests, record the check-in and display visit history.

One write path stores a visit under a structure shaped like:

```text
checkInBusinesses/{businessCode}/visitors/{date}/{uid}/{time}
```

The app also maintains user-side history for rendering previous check-ins.

### Privacy model

This feature handles location/visit history and guest information, so a real deployment would need a clear retention policy, strict Firebase rules, consent boundaries and a strong minimisation strategy.

Those production privacy guarantees are not demonstrated by this repository. The code is best understood as a Firebase data-flow prototype.

## 11. Self-isolation countdown

`IsolationCountDown.java` contains a hard-coded duration:

```text
14 days = 1,209,600,000 ms
```

The Activity uses `CountDownTimer` while visible and stores:

```text
millisLeft
timerRunning
endTime
endDate
```

in `SharedPreferences` during lifecycle transitions.

When reopened, it reconstructs the remaining time from the persisted end timestamp.

### Historical health rule

The 14-day value reflects the period when this project was built. It should not be presented as current isolation guidance.

For a health application, policy-driven values like isolation duration should be remotely configurable, jurisdiction-specific, dated and sourced.

### Notification limitation

The original reminder scheduling is tied closely to Activity/timer lifecycle behavior. It is an experiment rather than a robust background scheduling subsystem. A current Android implementation would use lifecycle-safe scheduling (often WorkManager/AlarmManager depending on exact timing requirements), immutable PendingIntents and modern notification-permission handling.

## 12. Connectivity handling

Several screens manually inspect `ConnectivityManager` and show a blocking no-connection dialog.

The project uses the older `NetworkInfo` API and distinguishes Wi-Fi/mobile connections directly.

This demonstrates the original intent clearly, but a better UX would keep locally available content readable and represent connectivity as a smaller state/banner rather than restarting Activities when connectivity returns.

## 13. Android manifest cleanup

The current manifest explicitly requests only the capabilities used by the app's feature set:

```text
INTERNET
ACCESS_NETWORK_STATE
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
CAMERA
READ_EXTERNAL_STORAGE
WRITE_EXTERNAL_STORAGE (maxSdkVersion 28)
```

Internal Activities are marked `android:exported="false"`; only `SplashScreen` is exported as the launcher.

The Maps key is injected through the `MAPS_API_KEY` manifest placeholder instead of being committed directly.

The project still reflects Android 11/12-era storage and permission assumptions. A target-SDK migration would require revisiting scoped storage, notification permissions, media permissions and several deprecated APIs.

## 14. Repository credential hygiene

The root `.gitignore` now excludes:

```text
app/google-services.json
secrets.properties
*.jks
*.keystore
build outputs
IDE/local-machine state
```

`secrets.properties.example` documents the two local values expected by the Gradle build.

A Firebase `google-services.json` file is configuration rather than a server secret, but keeping the original project-specific file out of a public historical repo is still good hygiene and makes it explicit that contributors should attach their own Firebase project.

No production Firebase rules are included here. A clone should not be treated as secure merely because client code checks authentication state.

## 15. The main architectural lesson

The project has a lot of feature surface for one Android codebase, but almost all orchestration happens in Activities:

```text
Activity
  ├── reads views
  ├── validates input
  ├── calls Firebase/API
  ├── owns progress dialogs
  ├── parses results
  ├── mutates lists
  └── navigates to next Activity
```

That makes each feature approachable when learning, but state becomes difficult to reason about as soon as callbacks, configuration changes, retries and partial failures interact.

A modern rebuild would look closer to:

```text
Compose / Views
      │
ViewModel + immutable UI state
      │
use cases
      │
repositories
  ├── CovidStatsRepository
  ├── AuthRepository
  ├── ProfileRepository
  ├── VaccineCenterRepository
  ├── CheckInRepository
  └── NewsRepository
      │
API / Firebase / local database
```

## 16. If I rebuilt it now

The biggest changes would be:

- split UI state from data access
- replace callback flags with lifecycle-aware async flows
- treat every loading/error/empty state explicitly
- move remotely changing health content out of the APK
- attach source and update timestamps to health data
- scope Firebase reads/writes and commit/test security rules
- minimize sensitive identity/check-in data
- use a backend for privileged operations and sensitive credentials
- replace the vaccination-token prototype with signed/verifiable claims if such a feature were genuinely required
- make vaccine-center data remotely refreshable with an offline cache
- replace old connectivity, storage and permission APIs
- add unit tests for validation and data mapping
- add Firebase Emulator tests for authorization-sensitive flows
- use a single navigation/state architecture instead of Activity-to-Activity state passing

Covid Zero is worth keeping public because it shows the breadth of what I was experimenting with early on. The goal of the current cleanup is not to disguise its age — it is to make the repo accurate, safe to browse, reproducible enough to study, and clear about what was prototype code versus something that could be trusted in a real health product.
