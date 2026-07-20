# 🌐 Orbit Mesh

<div align="center">

![Orbit Mesh Banner](https://img.shields.io/badge/Orbit%20Mesh-Bluetooth%20Mesh%20Messenger-5865F2?style=for-the-badge&logo=bluetooth&logoColor=white)

[![Android](https://img.shields.io/badge/Platform-Android%208%2B-35ED7E?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![BLE](https://img.shields.io/badge/Transport-Bluetooth%20LE-00B0F4?style=flat-square&logo=bluetooth&logoColor=white)](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview)
[![License](https://img.shields.io/badge/License-MIT-EC48BD?style=flat-square)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/hackergovind/Orbit?style=flat-square&color=5865F2)](https://github.com/hackergovind/Orbit/stargazers)

**Decentralized, encrypted peer-to-peer messaging over Bluetooth — no internet, no servers, no surveillance.**

[Download APK](#-installation) · [Architecture](#-architecture) · [Contributing](#-contributing)

</div>

---

## 🚀 What is Orbit Mesh?

**Orbit Mesh** is an open-source Android messenger built on the **BMTP (Bluetooth Mesh Transport Protocol)** — a custom-designed protocol for encrypted, serverless communication over Bluetooth Low Energy (BLE).

> No SIM card. No WiFi. No cloud. Just pure peer-to-peer Bluetooth mesh.

Perfect for:
- 🏕️ Off-grid communication (camping, hiking, festivals)
- 🌐 Censorship-resistant messaging
- 🔒 Privacy-first local communication
- 🛸 Disaster recovery scenarios where internet is unavailable

---

## ✨ Features

| Feature | Description |
|---|---|
| 📡 **BLE Mesh Discovery** | Auto-discovers nearby devices via Bluetooth LE advertising using the BMTP service UUID |
| 💬 **Direct Messages** | End-to-end encrypted 1:1 messaging over GATT write characteristics |
| 👥 **Group Chats** | Encrypted local mesh group conversations |
| 🎙️ **Push-to-Talk Voice** | Hold-to-record voice notes transmitted over the mesh |
| 📎 **File Transfers** | Share files peer-to-peer without any cloud storage |
| ✏️ **Custom Display Name** | Set your identity — broadcasts with every BLE advertisement |
| 📊 **Network Diagnostics** | Live telemetry: signal strength (RSSI), hop count, peer count |
| 🔵 **No Internet Required** | 100% Bluetooth — works anywhere your phone has BT |

---

## 📸 Screenshots

> *Coming soon — install and see for yourself!*

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│              Orbit Mesh App             │
│  ┌──────────┐  ┌──────────┐  ┌───────┐ │
│  │ Mesh     │  │  Chat    │  │ Diag  │ │
│  │ Radar    │  │  List    │  │ nostic│ │
│  └────┬─────┘  └────┬─────┘  └───┬───┘ │
│       └─────────────┴────────────┘     │
│              Jetpack Compose UI         │
│  ┌──────────────────────────────────┐  │
│  │    MeshViewModel + ChatViewModel  │  │
│  │         (AndroidViewModel)        │  │
│  └──────────────────┬───────────────┘  │
└─────────────────────│───────────────────┘
                       │
┌─────────────────────▼───────────────────┐
│         BleTransportManager             │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │ BLE Scanner │  │  BLE Advertiser  │  │
│  │ (discovery) │  │  (your identity) │  │
│  └─────────────┘  └──────────────────┘  │
│  ┌─────────────────────────────────┐    │
│  │   GATT Server (receive msgs)    │    │
│  │   GATT Client (send msgs)       │    │
│  └─────────────────────────────────┘    │
│  Service UUID: a1b2c3d4-e5f6-7890-...   │
└─────────────────────────────────────────┘
```

### Key Components

| Component | Role |
|---|---|
| `BleTransportManager` | Singleton managing BLE scan, advertise, GATT server/client |
| `MeshViewModel` | Collects live BLE discovery events, exposes `MeshState` |
| `ChatViewModel` | Collects incoming GATT messages, handles send via `BleTransportManager` |
| `AppNavigation` | Jetpack Compose NavHost with Bottom Navigation |
| `MeshRadarScreen` | Live view of discovered BLE peers with RSSI + hop count |
| `ConversationScreen` | Real-time chat UI with PTT voice and file attach |
| `DiagnosticsScreen` | Network telemetry dashboard |

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + StateFlow
- **Transport**: Bluetooth Low Energy (BLE) GATT
- **Protocol**: BMTP (Bluetooth Mesh Transport Protocol) — custom designed
- **Build**: Gradle 9.3 + Android SDK 34
- **Min SDK**: Android 8.0 (API 26)

---

## 📦 Installation

### Option 1 — Download APK directly
```
Coming soon on GitHub Releases
```

### Option 2 — Build from source

```bash
# Clone the repo
git clone https://github.com/hackergovind/Orbit.git
cd Orbit

# Build debug APK
./gradlew :app-android:assembleDebug

# APK will be at:
# app-android/build/outputs/apk/debug/app-android-debug.apk
```

> **Requirements**: Android Studio Hedgehog+, JDK 17, Android SDK 34

---

## 🔐 Permissions Required

| Permission | Why |
|---|---|
| `BLUETOOTH_SCAN` | Discover nearby Orbit Mesh devices |
| `BLUETOOTH_ADVERTISE` | Broadcast your presence and display name |
| `BLUETOOTH_CONNECT` | Send/receive messages via GATT |
| `ACCESS_FINE_LOCATION` | Required by Android for BLE scanning (OS policy) |
| `RECORD_AUDIO` | Push-to-Talk voice messages |

> ⚠️ All communication stays **on-device and peer-to-peer**. No data ever leaves your Bluetooth radio.

---

## 🚀 How to Use

1. Install on **two or more Android devices**
2. Open the app — grant **Bluetooth + Location** permissions when prompted
3. Tap **⚙️** → set your display name (e.g. "Govind")
4. Tap **"Start Discovery"** on the Mesh Radar screen
5. Both devices will appear on each other's radar within ~10 seconds
6. Tap a device → start messaging!

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

```bash
# Fork the repo, then:
git checkout -b feature/your-feature-name
git commit -m "feat: add your feature"
git push origin feature/your-feature-name
# Open a Pull Request
```

### Areas we'd love help with:
- 🔒 Implementing full E2EE (currently preparing for Signal Protocol integration)
- 📡 Multi-hop mesh routing (packets relayed through intermediate nodes)
- 🗃️ Persistent message storage (Room database)
- 🍎 iOS companion app (BLE is cross-platform!)
- 🧪 Unit + integration tests

---

## 📜 License

```
MIT License — free to use, modify, and distribute.
```

---

## 👨‍💻 Author

**Govind** — [@hackergovind](https://github.com/hackergovind)

> *Built with the conviction that communication should be free, private, and resilient.*

---

<div align="center">

**⭐ Star this repo if you believe in decentralized communication!**

[![Star on GitHub](https://img.shields.io/github/stars/hackergovind/Orbit?style=social)](https://github.com/hackergovind/Orbit)

</div>
