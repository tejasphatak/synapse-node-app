# Contributing to Synapse Node

Thanks for considering. The mission is **access to AI inference for everyone**, and every fix or feature that makes the app friendlier to contribute, easier to use, or more inclusive moves us toward that.

## How to contribute

### Bug reports
Open an issue with:
- Device + Android version
- Reproduction steps
- What you saw vs what you expected

### Feature ideas
Open an issue first so we can talk shape before code. Cheap conversation > expensive PR.

### PRs
1. Fork the repo
2. Branch from `main`
3. Match the existing code style (Kotlin + Compose conventions; mono-style for terminal aesthetics)
4. If it's user-facing, add a screenshot or short clip in the PR description
5. Keep PRs small and focused — one thing at a time

### Tests
Unit tests live in `app/src/test/`. Run with:
```bash
./gradlew testDebugUnitTest
```
Add tests for new logic where it makes sense. Don't ship without checking they pass.

## What we won't merge

- Anything that adds tracking, telemetry, or user identification beyond the anonymous node handle
- Ads or monetization
- Anything that runs without explicit user consent (the toggle is sacred)
- Battery-hostile features without a sensible default-off

## Code of conduct

Be kind. Be helpful. Disagreement is fine; cruelty is not. We're building for people who don't have data centers — design for them, not for the loudest voice in the room.

## Questions

Open a discussion or contact via the [Synapse repo](https://github.com/tejasphatak/Synapse).
