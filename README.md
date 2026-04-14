# Synapse Node ⬢

**Your phone, thinking.** Android app that contributes phone GPU compute to the Synapse distributed-inference mesh.

> Built by Claude. Idea by Tejas. AI-generated, human-directed.

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![min SDK](https://img.shields.io/badge/min%20SDK-28-blue)
![target SDK](https://img.shields.io/badge/target%20SDK-35-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-purple)

---

## What it does

Your phone's GPU joins a mesh of devices that *collectively* run a transformer model. Each device runs a slice of the layers; the mesh routes activations between them and produces tokens. You contribute a little; the mesh helps many.

- **One toggle** to start contributing
- **Live stats**: tokens processed, requests handled, uptime, streak
- **Badges** for milestones (first spark, kilo thinker, week in the mesh)
- **Battery-safe**: auto-stops below 20%, optional charging-only mode, foreground service + wake lock only while active
- **Private**: no account, no email, no personal data — anonymous node handle generated on device

This is a public-good project. There's nothing to monetize, no ads, no tracking. If it costs you battery, you decide when to run it.

## Why this exists

LLM inference today is gated by data centers. We think a phone-shaped mesh — pockets, classrooms, idle tablets — can do real work. The bigger the mesh, the more interesting the model that can run on it. Hardware doesn't need to scale; participation does.

This app is *one node*. The coordinator and protocol live in the [Synapse repo](https://github.com/tejasphatak/Synapse).

## Install

**Latest signed APK:** [synapse.apk](https://chat.webmind.sh/synapse.apk) (sideload — Play Store coming once we have multi-coordinator failover)

1. Download
2. Allow install from your browser
3. Open → tap **start contributing**
4. Watch the [mesh status](https://chat.webmind.sh) tick up

Verify checksum (SHA256) printed on the [download page](https://chat.webmind.sh).

## Build from source

```bash
git clone https://github.com/tejasphatak/synapse-node-app.git
cd synapse-node-app
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease
# APK at: app/build/outputs/apk/release/app-release.apk
```

You'll need: Android SDK 35, JDK 17, Gradle 8.7+. Build is signed; supply your own keystore for distribution (see `app/build.gradle.kts`).

## Stack

- **Kotlin + Jetpack Compose** — UI
- **WebView** — runs the actual node WebGPU code from the Synapse coordinator
- **DataStore** — local stats persistence
- **Foreground service + wake lock** — keeps the node alive while contributing
- Min SDK 28 / target SDK 35

## Privacy

- No account, no email, no telemetry sent off-device beyond the protocol traffic to the coordinator (which carries activation tensors, not personal data)
- Anonymous node handle generated locally
- Stats stay on your device
- Open source — read the code

## Contributing

Issues + PRs welcome. Some good places to start:

- New badge ideas / animations
- Better battery awareness (e.g., respect Doze, charging-state-driven throttle)
- A11y polish (TalkBack, RTL, dynamic font sizes)
- i18n — add a language
- Different rendering for the network mesh visualization

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Related repos

- [Synapse](https://github.com/tejasphatak/Synapse) — the coordinator + protocol + WebGPU node code that runs inside this app's WebView
- Live mesh status: https://chat.webmind.sh

## License

[MIT](LICENSE)
