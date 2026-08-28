# Pocket Bansuri 🎶

**Pocket Bansuri** is a premium Android application designed to help bansuri (Indian bamboo flute) practitioners perfect their pitches and practice Hindustani classical Ragas. The app features a real-time pitch tuner mapped to Indian music notation (Swaras) and an interactive Raga library.

---

## 🌟 Core Features

### 1. Landscape 7-Hole Flute Visualizer
*   **Realistic Bamboo Design**: Rendered using custom Canvas graphics with realistic wood-grain gradients, traditional thread bindings, and a blowing hole (embouchure).
*   **State-Driven Fingering Animations**: Smoothly transitions between finger configurations for each Swara (**Sa, Re, Ga, Ma, Pa, Dha, Ni, High Sa**). Supports open (○), fully closed (●), and half-closed (◐) states.

### 2. Practice Station (Double-Tab Layout)
*   **Tuner Tab (Hands-Free Auto-Detecting Chromatic Tuner)**:
    *   **Live Pitch Detection Engine**: Replaced native mock stubs with a fully functional audio pipeline. It captures raw PCM input at 44.1 kHz using Android's `AudioRecord` API, performing high-performance signal processing in a dedicated background coroutine.
    *   **Autocorrelation with Center-Clipping**: Utilizes center-clipping to eliminate strong second/third harmonics typical of flutes, and employs parabolic interpolation on the autocorrelation peak for sub-sample Hz precision.
    *   **Comparative Note Display (Target vs Played)**: 
        *   **Wish to Play (Target Shuddh Swara)**: Automatically infers the shuddh note you are aiming to play by finding the closest Shuddh Swara (`Sa, Re, Ga, Ma, Pa, Dha, Ni`) relative to the current scale.
        *   **Actually Played (Chromatic Note)**: Performs a 12-semitone chromatic lookup to detect the exact note being blown—including flat, sharp, komal (`re, ga, dha, ni`), and tivra (`ma'`) notes, and their Western equivalents (e.g., `C#4`, `F#5`).
        *   **False Note Feedback**: If your pitch drifts, the tuner visually displays exactly what "false note" you are hitting (e.g., playing a flat `Re` and hitting `re` Komal Re), making it a powerful tool for intonation correction.
    *   **Multi-Octave Alignment**: Automatically detects the played octave (`LOW`, `MID`, `HIGH`, `V.HIGH`) and aligns the target note's octave dynamically to match the played octave, preventing the deviation gauge from pegging to the margins when jumping octaves.
    *   **Visual Precision Gauge**: Displays the live frequency in Hz along with a responsive visual tuner bar showing precise deviation ($\pm20$ Hz) and status feedback (perfect match, flat, sharp, or false-note indicators).
*   **Riyaaz Tab**:
    *   **Plain Scale Table**: Configures the key scale, base practice octave, and a custom timer value to step through notes individually.
    *   **Minimal Raga Practice (Autoplay Raga Scales)**:
        *   **4-Row Layout**: Each Raga card is strictly constrained to 4 rows of text, using a large, clear font to prevent vertical overflow and maximize readability on landscape devices.
        *   **Core Details**: Displays the Raga name, parent Thaat category tag, and primary notes (Vadi & Samvadi).
        *   **Sequential Autoplay Engine**: Includes a play/stop button that triggers a coroutine sequence. It cycles through the Aaroh (ascent) and Avroh (descent) notes sequentially at 700ms intervals, generating synthesized flute tones.
        *   **Live Pitch Highlighting**: Highlights the currently playing note in real-time within the Aaroh/Avroh text displays using dynamic size-scaling and color accents.
        *   **Visualizer Synchronization**: Automatically updates the 7-hole bansuri visualizer configuration on-screen to match the active note being synthesized, demonstrating correct fingerings in real-time.
        *   **20 Classical Ragas**: Loaded with 20 traditional Hindustani classical Ragas grouped by parent Thaats.

---

## 🛠️ Technical Stack & Algorithms

*   **UI Framework**: Kotlin & Jetpack Compose (Material Design 3)
*   **Audio Pipeline**: Android `AudioRecord` API with Kotlin Coroutines for asynchronous audio processing.
*   **DSP Pitch Detection**:
    *   *Center-Clipping*: Suppresses overtones and highlights the fundamental frequency.
    *   *Autocorrelation*: Measures signal self-similarity to calculate period lengths.
    *   *Parabolic Interpolation*: Fits a quadratic curve around the peak autocorrelation index to resolve frequency up to fractions of a Hertz.
    *   *Silence Gating*: Restricts analysis below a noise floor threshold and filters out signals under 20 Hz.
*   **Target Android API**: SDK 36 (Android 16 preview)
*   **Build Tool**: Gradle 8.9 (Kotlin DSL)

---

## 📂 Project Architecture & Key Files

The project is structured with a clean separation of Compose UI views and native component modules:

*   [`MainActivity.kt`](file:///run/media/anonymous/Study/Projects/PocketBansuri/app/src/main/java/com/pocketbansuri/MainActivity.kt): Entry point of the application. Forces Landscape mode and requests runtime microphone (`RECORD_AUDIO`) permissions.
*   [`AudioEngine.kt`](file:///run/media/anonymous/Study/Projects/PocketBansuri/app/src/main/java/com/pocketbansuri/AudioEngine.kt): Core audio acquisition loop and digital signal processing (DSP) logic.
*   [`Swara.kt`](file:///run/media/anonymous/Study/Projects/PocketBansuri/app/src/main/java/com/pocketbansuri/model/Swara.kt): Defines Swara frequencies across multiple scales and octaves, MIDI mappings, and finger configurations.
*   [`TunerScreen.kt`](file:///run/media/anonymous/Study/Projects/PocketBansuri/app/src/main/java/com/pocketbansuri/ui/screens/TunerScreen.kt): High-density, auto-detecting chromatic tuner screen displaying wish-to-play vs actually-played notes, status color codes, and deviation meter.
*   [`BansuriVisualizer.kt`](file:///run/media/anonymous/Study/Projects/PocketBansuri/app/src/main/ui/components/BansuriVisualizer.kt): Performs smooth, custom Canvas drawings of the flute holes based on the active Swara.

---

## 🚀 Getting Started

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/MihirrajDixit/PocketBansuri.git
    cd PocketBansuri
    ```
2.  **Open in Android Studio**:
    *   Launch Android Studio (Ladybug or newer recommended).
    *   Select **Open An Existing Project** and navigate to the `PocketBansuri` folder.
    *   The IDE will automatically resolve dependencies and build a `local.properties` file with your local Android SDK/NDK path.
3.  **Run on Device**:
    *   Connect your Android device or start an emulator.
    *   Build and run the project (`Shift + F10` or click **Run**).
    *   Accept the microphone permission prompt to enable real-time tuner updates.

---

## 🗺️ Roadmap & Current Status

*   [x] **Phase 1**: Initial project scaffolding, Jetpack Compose Material 3 theme, and NDK build configurations.
*   [x] **Phase 2**: Animated custom Canvas flute visualizer, interactive Raga list, and landscape 40/60 layout.
*   [x] **Phase 3**: Real-time Kotlin AudioEngine utilizing autocorrelation, center-clipping, silence gating, and parabolic interpolation for ultra-precise pitch detection.
*   [x] **Phase 4**: Dual-note comparative chromatic tuner automatically identifying target shuddh swaras and live chromatic notes (including komal/tivra) with false note visual feedback.
*   [ ] **Phase 5**: Native Google Oboe C++ integration for lower-latency capture and SoundFont playback support.
