# 🎮 Color Rush

An addictive, hyper-casual, fast-paced arcade reaction game built with Kotlin and Jetpack Compose. Players must test their reflexes by matching and tapping falling blocks against a dynamic target color!

---

## 📸 Interactive Mockup Showcase

Since emulator screenshots vary based on your target device aspect ratio, here is an executive design preview of the mobile screens engineered for this application:

### 🏠 1. Start & Registration Screen
```text
┌──────────────────────────────────────────┐
│  10:00 AM                         🔋 99% │
├──────────────────────────────────────────┤
│                                          │
│                [🟥🟩🟦]                  │
│               COLOR RUSH                 │
│         - Arcade Reflex Game -           │
│                                          │
│         ┌──────────────────────┐         │
│         │ ENTER YOUR NAME      │         │
│         │  N I N J A           │         │
│         └──────────────────────┘         │
│                                          │
│         ┌──────────────────────┐         │
│         │ 🏆 NINJA RECORD      │         │
│         │      High Score: 42  │         │
│         └──────────────────────┘         │
│                                          │
│         ┌──────────────────────┐         │
│         │ 💡 HOW TO PLAY:      │         │
│         │ Tap only the blocks  │         │
│         │ matching target color│         │
│         └──────────────────────┘         │
│                                          │
│              ┌────────────┐              │
│              │ 🚀 PLAY !  │              │
│              └────────────┘              │
│                                          │
└──────────────────────────────────────────┘
```

### 🕹️ 2. Active Gameplay Screen
```text
┌──────────────────────────────────────────┐
│  Target: [ 🟡 GOLD ]        Score: 12    │
├──────────────────────────────────────────┤
│ 🔋 ▇▇▇▇▇▇▇▇▇▇ (100%)       Combo: x3      │
├──────────────────────────────────────────┤
│                                          │
│                                          │
│     [🔴 RED]                             │
│                                          │
│                  [🔵 BLUE]               │
│                                          │
│            [🟡 GOLD]  <--- TAP THIS!     │
│                                          │
│     [🟣 PURPLE]                          │
│                                          │
│                                          │
└──────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack & Architecture

This application utilizes modern Android design patterns and Google’s latest framework recommendations:

*   **Language:** Kotlin (100% type-safe, expressive, and optimized).
*   **UI Framework:** Jetpack Compose (Modern declarative UI with smooth recompositions).
*   **Design System:** Material Design 3 (M3) utilizing edge-to-edge window processing, energetic cyber-colors, a dark cosmic slate theme, and tactile ripple feedbacks.
*   **State Management:** Model-View-ViewModel (MVVM) architecture driven by `GameViewModel` exposing safe `StateFlow` streams.
*   **Local Persistence:** Standard Android `SharedPreferences` to capture and record persistent high scores partitioned on a per-player name basis without bloated database overhead.
*   **Threading & Coroutines:** Kotlin Coroutines to drive high-performance frame loops (up to 60 FPS) and smooth game-loop tick evaluations.
*   **Modern Custom Launcher Icon:** Custom adaptive vector icons (`ic_launcher_background` & `ic_launcher_foreground`) designed with an isometric cascade of glowing neon gemstone diamonds representing the cascading color cubes.

---

## 📦 How to Compile and Run Locally

To test, run, or continue developing **Color Rush** inside Android Studio or with the command line:

### Prerequisites
*   Android Studio (Ladybug or newer)
*   JDK 17 or higher
*   Android SDK 34 (Upside Down Cake)

### Build Commands
1.  **Sync and Compile Kotlin Files:**
    ```bash
    gradle compileDebugKotlin
    ```
2.  **Generate a Debug Package (APK):**
    ```bash
    gradle assembleDebug
    ```
    The output apk will be generated at `./app/build/outputs/apk/debug/app-debug.apk`.

---

## 🚀 Step-by-Step Play Store Publishing Guide

Ready to publish **Color Rush** to millions of players? Follow this production-ready roadmap to submit your app to the Google Play Store:

### Step 1: Set Up Your Google Play Developer Account
1.  Navigate to the [Google Play Console](https://play.google.com/console/signup).
2.  Register for a Developer Account (requires a one-time $25 registration fee).
3.  Complete developer profile verification with valid identity documents.

### Step 2: Configure Your App Launcher Identity
Ensure your app naming is finalized and your custom logo is loaded:
*   We have renamed the app label to `Color Rush` inside standard resource strings.
*   We have updated `AndroidManifest.xml` to point to our custom neon adaptive diamond icon `@mipmap/ic_launcher`.

### Step 3: Generate a Signed Release Bundle (AAB)
Google Play requires publishing using the **Android App Bundle (.aab)** format instead of raw APKs:
1.  Open Project in Android Studio.
2.  In the top menu, select **Build > Generate Signed Bundle / APK...**
3.  Choose **Android App Bundle** and click **Next**.
4.  Create a secure keystore file (`.jks`) to digitally sign your app:
    *   *Tip:* Keep this key safe! If lost, you cannot update your game on the Play Store.
5.  Select Build Variant as **release** and execute.
6.  The platform generates your bundle inside `/app/release/app-release.aab`.

### Step 4: Create a New App in Play Console
1.  In the Play Console, click **Create app**.
2.  Fill in the initial configurations:
    *   **App Name:** Color Rush
    *   **Default Language:** English (US)
    *   **App or Game:** Game
    *   **Free or Paid:** Free
3.  Accept the Developer Program Policies and Export Laws.

### Step 5: Complete Main Store Listing Questionnaire
Google requires fulfilling store listing metadata:
*   **Short Description:** A fast-paced reactive arcade game. Color-match falling blocks!
*   **Full Description:** Fuel your reflexes in Color Rush! A high-voltage casual arcade challenge where you tap falling blocks matching the active target color. Track personal records under custom usernames, score chains of combos, and avoid matching the wrong colors before your timer ends. Perfectly optimized for all form factors!
*   **Graphics Assets Required:**
    *   **App Icon:** 512 x 512 px (PNG, 32-bit color, max 1MB).
    *   **Feature Graphic:** 1024 x 500 px (PNG or JPEG, max 1MB) representing your game brand.
    *   **Phone Screenshots:** 2 to 8 screenshots (Aspect ratio 16:9 or 9:16, between 320px and 3840px). *Tip: Take screenshot of the game active screen and game-over page from your mobile device!*

### Step 6: Define Content Ratings & Target Audience
1.  Answer the content rating questionnaire. Color Rush qualifies for a **PEGI 3** (suitable for all ages) rating since it contains no violent, offensive, or sensitive content.
2.  Set the Target Audience to age groups (e.g., Teens, Adults, or Everywhere).

### Step 7: Push to Closed Testing or Production Release
1.  Go to **Release > Production** in the sidebar.
2.  Click **Create new release**.
3.  Upload the `.aab` file created in Step 3.
4.  Write descriptive release notes (e.g., *"Initial production release introducing multi-player personal arcade high score persistence and adaptive material dynamic colors!"*).
5.  Click **Save**, then click **Review release** and click **Start roll-out to Production**!
