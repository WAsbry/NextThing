# Contributing to NextThing

Thank you for improving NextThing.

## Before you start

- Search existing issues before opening a new one.
- Do not commit API keys, keystores, tokens, models, native binaries, device
  logs, screenshots with personal information, or generated APKs.
- Use `local.properties.example` as the configuration reference; keep your real
  `local.properties` local.

## Development checks

Run the relevant checks before opening a pull request:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
```

For location, notification, camera, microphone, map, background behavior or
widget changes, also validate the feature on a real Android device.

## Pull requests

Keep one pull request focused on one user-visible behavior or one technical
concern. Include:

1. The problem and the intended outcome.
2. The commands/tests run and their results.
3. Before/after screenshots for UI changes, with personal data redacted.
4. Any impact on permissions, privacy, data migration or release notes.

By submitting a contribution, you agree to license it under this repository's
[MIT License](LICENSE).
