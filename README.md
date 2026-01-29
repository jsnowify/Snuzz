# SNUZZ: ANDROID-BASED OFFICE’S NOISE DETECTION APPLICATION.

**Snuzz** is not just a decibel meter — it’s a **context-aware noise guardian** designed for classrooms, offices, and libraries.

Unlike standard noise-monitoring apps that trigger alerts purely based on volume, Snuzz uses an embedded **TensorFlow Lite (YAMNet)** model to understand **what** the sound is.  
It ignores safe noises (like air conditioners or typing) but instantly alerts you to **critical events** (such as screaming or glass breaking), even when the volume is lower than the set threshold.

---

## 🔴 Live Monitoring Features
- Real-time noise monitoring
- Context-aware Info Card details
- Historical logs & visual graphs

---

## 🧠 Smart “Traffic Light” Logic

At the core of SnuzzNoise is an intelligent decision engine that processes audio in real time using a **3-stage filtering system**:

| Zone | Sound Type | Logic Applied |
|-----|-----------|---------------|
| 🟢 **GREEN (Safe)** | Fan, Rain, Typing, AC, Heartbeat | **Ignore Mode** — Threshold raised to 95 dB. Prevents false alarms from machinery or ambient noise. |
| 🟡 **YELLOW (Normal)** | Talking, Laughter, Music, Footsteps | **Standard Mode** — Uses user-defined threshold (e.g., 60 dB). Sound must persist for 5 seconds to trigger an alert. |
| 🔴 **RED (Critical)** | Screaming, Glass Breaking, Baby Crying | **Panic Mode** — Threshold drops to 50 dB. Triggers an alert within 1 second, prioritizing safety over settings. |

---

## ✨ Key Features

- **Real-Time Decibel Metering**  
  Uses `AudioSource.UNPROCESSED` to bypass Android’s automatic gain control for raw, accurate readings.

- **AI Sound Classification**  
  Identifies over **500+ sound types** locally on-device using TensorFlow Lite (YAMNet).

- **Dynamic UI Feedback**  
  Pulsing interface reacts to sound intensity.  
  Info Card icons change based on sound source (e.g., ❤️ Heartbeat, ❄️ Air Conditioner).

- **Background Monitoring**  
  Runs as a **Foreground Service**, continuing detection even when the phone is locked.

- **Smart Audio Alerts**  
  - Uses **SoundPool** for zero-latency alerts  
  - Includes a **“Deafening Loop” prevention system** that pauses listening while alert sounds play

- **Historical Data Logging**  
  Noise events are logged with timestamps and sound classifications for later review.

---

## 🛠 Tech Stack

**Language:** Kotlin  
**UI:** Jetpack Compose (Material 3)  
**Architecture:** MVVM + Clean Architecture  
**Dependency Injection:** Hilt  

### AI / ML
- TensorFlow Lite (YAMNet model)

### Concurrency
- Kotlin Coroutines
- Kotlin Flow

### Local Data
- Room Database (noise history)
- DataStore (user settings)

### Audio Engine
- `AudioRecord` — raw PCM audio capture  
- `SoundPool` — low-latency alert playback  
- `MediaMetadataRetriever` — dynamic audio duration handling

---

## 🚀 Installation

```bash
git clone https://github.com/jsnowify/Snuzz.git


