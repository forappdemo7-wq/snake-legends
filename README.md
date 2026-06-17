# 🐍 Snake Legends

Welcome to **Snake Legends**! An ultra-polished, fluid, and immersive 2D Snake game designed with modern Android architecture. Snake Legends takes the classic nostalgia of snake and reconstructs it with gorgeous 60FPS fluid gameplay, dark sci-fi/cyberpunk aesthetics, tactical class abilities (shield, freeze pulse, EMP, etc.), and multi-mode options.

---

## 🚀 Key Features

*   **🎮 Ultra-Fluid gameplay**: Centered on responsive virtual joysticks & custom analog controls tuned for 60FPS vertical-screen play.
*   **📡 Private Rooms & Real-time Multiplayer**: Build private lobbies, chat with other participants, configure lag compensation, tick rates (up to 60Hz), and matchmake on global custom servers.
*   **🛠️ Tactical Class Abilities**: Select your custom playstyle:
    *   🛡️ **Aegis Shield**: Instant invulnerability to crash-out and reset positioning safely.
    *   ❄️ **Sub-Zero Blast**: Freeze pulse to slow down surrounding rivals.
    *   ⚡ **Disruptor EMP**: Stall nearby snake boosts.
    *   🔥 **Drive Burst**: Immediate nitro boost over enemies.
    *   🌌 **Quantum Ghost**: Ghost phase directly through blocks or other snakes.
*   **🏆 Seasonal Battle Pass & Daily Rewards**: Progress through Season 7 "Cyber Rebirth" to unlock premium reward tracks, coins, and legendary cosmetics.
*   **📈 Ranked Ladders & Match History**: Climb from **Bronze** up to **Legend** based on competitive match performances. Full historic card statistics with complete XP metrics.
*   **🏪 In-game Shop**: Exchange coins gained from daily missions & matches for custom trail designs and premium skins.

---

## 🛠️ Tech Stack & Architecture

This codebase is crafted matching official Android development best practices:

*   **Kotlin & Jetpack Compose**: 100% declarative UI with highly optimized recomposition states.
*   **Material Design 3 (M3)**: Beautiful, dark-themed responsive UI conforming to deep, accessibility-friendly designs.
*   **MVVM & State Management**: Clean segregation of responsibilities using `ViewModel` and thread-safe Kotlin `StateFlow` streams.
*   **Asynchronous Flows**: Structured concurrency utilizing Kotlin Coroutines.
*   **Offline Local Storage**: Local database caching powered by **Room** to log match details, profile settings, and currency states.
*   **Dynamic Multiplayer Engine**: Integrated high-speed WebSocket networking with custom tick configurations and low-latency feedback.

---

## 💻 Getting Started (Local Development)

To run, build, or modify **Snake Legends** on your machine, follow these instructions:

### Prerequisites

*   **Java Development Kit (JDK)**: JDK 17 or higher.
*   **Android SDK**: Minimum SDK 26, Target SDK 34 (handled automatically by Gradle).
*   **Android Studio**: Hedgehog (or newer) is highly recommended.

### Building & Running

Clone the repository and run the application via the Gradle wrapper scripts:

```bash
# Clone the repository
git clone https://github.com/forappdemo7-wq/snake-legends.git
cd snake-legends

# Grant executable permission to gradlew (macOS / Linux)
chmod +x gradlew

# Build the debug APK
./gradlew assembleDebug

# Run the local unit tests
./gradlew :app:testDebugUnitTest
```

---

## 🧪 Testing and Verifications

Snake Legends features a comprehensive unit-testing suite to ensure code health and robust application lifecycle handling:

*   **JVM Unit Tests**: Fast, secure verification of state mechanics, profile level tiers, and multiplayer ping/packet logic.
*   **To run tests**:
    ```bash
    ./gradlew :app:testDebugUnitTest
    ```

---

## 📦 Continuous Integration (CI)

We have configure a robust **GitHub Actions workflow** to automatically verify every pull request and push to the codebase. It ensures code quality by:
1.  Setting up JDK 17 environment.
2.  Caching Gradle dependencies for ultra-fast builds.
3.  Compiling and verifying the entire Android build.
4.  Executing the standard unit test suite.
