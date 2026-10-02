<div align="center">

<!-- Typing intro -->
<img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&weight=500&size=26&pause=1000&color=D5FC2D&center=true&vCenter=true&width=480&lines=Real-Time+Music+Visualizer.;Nothing+Phone+Glyph+Sync.;Desktop+Companion+%2B+OpenRGB." alt="Typing SVG" />

<br/><br/>

<img src="assets/logo.png" width="120" height="120" alt="Glyphix Logo">

<br/><br/>

<!-- Action Badges -->
<a href="https://github.com/oliver-lebaigue-bright-bench/glyph-syncronator/releases"><img src="https://img.shields.io/badge/Download-Latest_APK-D5FC2D?style=for-the-badge&logo=android&logoColor=black" alt="Download APK" height="35" /></a>
<a href="https://discord.gg/cQ4hxNE8fX"><img src="https://img.shields.io/badge/Discord-Join_Community-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord" height="35" /></a>
<a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-888888?style=for-the-badge&logo=github&logoColor=white" alt="License" height="35" /></a>

<br/><br/>

[![Downloads](https://img.shields.io/github/downloads/oliver-lebaigue-bright-bench/glyph-syncronator/total?style=flat-square&logo=github&logoColor=white&label=Downloads&color=D5FC2D&labelColor=1a1a1a)](https://github.com/oliver-lebaigue-bright-bench/glyph-syncronator/releases)
[![Stars](https://img.shields.io/github/stars/oliver-lebaigue-bright-bench/glyph-syncronator?style=flat-square&logo=github&logoColor=white&label=Stars&color=ffffff&labelColor=1a1a1a)](https://github.com/oliver-lebaigue-bright-bench/glyph-syncronator/stargazers)
[![Nothing Phone](https://img.shields.io/badge/Nothing_Phone-1_·_2_·_2a_·_3_·_4-D5FC2D?style=flat-square&labelColor=1a1a1a)](#-supported-devices)

<sub>Read in: [हिन्दी](Docs/README_HI.md) · [मराठी](Docs/README_MR.md) · [Türkçe](Docs/README_TR.md) · [العربية](Docs/README_AR.md)</sub>

</div>

---

### 🚀 What is Glyphix?

**Glyphix** delivers real-time music visualization for Nothing Phone Glyphs, haptics, and flashlights. Powered by high-precision **FFT audio analysis**, it maps frequencies to individual zones for a pixel-perfect audio-visual experience at a smooth 60 FPS.

---

### 🎯 Why Choose Glyphix?

Stock visualizers are uniform and limited. Glyphix unleashes the true hardware potential of your phone by driving every single zone and segment independently.

| Feature | Stock Visualizer | **Glyphix** |
| :--- | :--- | :--- |
| **Light Depth** | ~3 basic levels | **4,096 levels (12-bit)** |
| **Frame Rate** | 20 FPS | **60 FPS** |
| **Precision** | Low / Unreliable | **FFT-Based & Deterministic** |
| **Control** | Full glyph strips | **Independent zone mapping** |

---

### ⚡ Quick Start

1. **Download & Install**: [Grab the latest APK release](https://github.com/oliver-lebaigue-bright-bench/glyph-syncronator/releases).
2. **Select Audio Source**: Choose your preferred capture source (Media Projection recommended).
3. **Hit Start**: Play your music and watch the lights come alive!

> **💡 Bluetooth Latency?** Head over to the **Audio** tab in the app to calibrate sync offsets. Advanced users can fine-tune frequency presets in `zones.config` (see the [Configuration Guide](Docs/ZONES_CONFIG.md)).

---

### 💻 Desktop Companion & OpenRGB

Want to sync your setup and stream audio from your computer? Glyphix features a lightweight **Desktop Companion script** with built-in **OpenRGB support**!

* **Platforms**: Fully supported on **Windows** and **Linux** (macOS support is currently in active development).
* **Format**: Pure Python script (`.py`), making it transparent, lightweight, and easy to run.
* **OpenRGB Integration**: Syncs your PC's RGB lighting gear (case fans, keyboards, mice, strips) right along with your phone's glyphs for a unified desktop light show.

---

### 📱 Supported Devices

<table>
  <tr>
    <td width="140" align="center"><strong>Full Glyph Support</strong></td>
    <td>
      <code>Nothing Phone (1)</code> · <code>Phone (2), (2a), (2a) Plus</code> · <code>Phone (3), (3a), (3a) Pro</code> · <code>Phone (4a), (4b), (4a) Pro</code>
    </td>
  </tr>
  <tr>
    <td align="center"><strong>Haptics & Flashlight</strong></td>
    <td>
      Any standard Android phone is supported for haptic and flashlight visualization modes.
    </td>
  </tr>
</table>

---

### ⚙️ Under the Hood

```text
Audio Stream (Mobile/Desktop) ➔ FFT Analysis (20ms window) ➔ Frequency Mapping ➔ Glyph Zones / OpenRGB / Haptic

```

* **Fast Fourier Transform (FFT)**: Breaks incoming audio down into precise frequency bins every single frame.
* **Smart Smoothing**: Downward-only smoothing keeps animations snappy and responsive without jitter.
* **Haptics & Flashlight**: Leverages bass amplitudes for continuous reactive glows, and derivative beat-detection for sharp pulses.

---

### 📖 Documentation & Community

* **[zones.config Guide](https://www.google.com/search?q=Docs/ZONES_CONFIG.md)** — Customize presets or add new device profiles.
* **[Python Script Wiki](https://github.com/oliver-lebaigue-bright-bench/glyph-syncronator/wiki/)** — Legacy bulk audio file processing tools.
* **[Discord Server](https://discord.gg/cQ4hxNE8fX)** — Chat with developers and the community.
* **[Issue Tracker](https://github.com/oliver-lebaigue-bright-bench/glyph-syncronator/issues)** — Report bugs or request features.

---

### 🔒 Privacy & Security

* **Screen Capture**: Used exclusively via Android Media Projection to read system audio streams. No video frames or screen content are ever stored or transmitted.
* **Audio Handling**: Processed strictly in real-time RAM buffers. Audio data is never recorded, saved, or uploaded.
* **Analytics**: Completely optional, anonymous usage stats to help patch crashes and boost stability.
* **Safety Scan**: [View VirusTotal Report](https://www.virustotal.com/gui/url/c92c1ff82b56eb60bfd1e159592d09f949f0ea2d195e01f7f5adbef0e0b0385b)

---

### 👥 Core Contributors

---

Built with care by the Glyphix community. © 2026
