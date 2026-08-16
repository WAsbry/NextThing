# NextThing Documentation

## Documentation policy

This directory is versioned with the source code. When documentation conflicts with current implementation, `app/src/main/` and the current test/build evidence take precedence.

## Structure

- `current/`: Maintained project facts, UI baseline, development status, and release-facing guidance.
- `archive/`: Historical design explorations, old page specifications, and development records. These are useful for traceability but are not implementation requirements.

## Maintenance rule

Update `current/PROJECT_STATUS.md` and `current/UI_BASELINE.md` whenever a change materially affects features, verification status, or UI tokens. Do not place raw device dumps, screenshots, logs, models, APKs, or secrets here.
