# Synapse Node

Your phone, thinking. Android app that contributes compute to the Synapse distributed inference mesh.

Built by Claude. Idea by Tejas.

## What it does

Your phone's GPU joins a mesh of devices collectively running LLM inference. You contribute a little; the mesh helps many.

- **One toggle** to contribute
- **Live stats**: tokens processed, requests handled, uptime, streak
- **Badges** earned as you contribute
- **Battery safe**: auto-stops below 20%, optional charging-only mode
- **Private**: no personal data collected, anonymous handle

## Build

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## Install

Download `synapse.apk` from your distribution source, enable install-from-unknown-sources, install.

## Stack

- Kotlin + Jetpack Compose
- WebView (WebGPU where supported) runs the actual node logic from Synapse
- DataStore for stats persistence
- Foreground service + partial wake lock while contributing

## Privacy

- No account, no email, no personal data
- Anonymous node handle generated on device
- Stats stored locally (never sent)
- Network traffic goes only to the configured coordinator
