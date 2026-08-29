# NextThing Privacy Notice

Last updated: 2026-08-29

NextThing is a personal task-management application. This notice describes the
data categories used by the Android application as implemented in this source
repository. It is not a substitute for the privacy terms of a self-hosted or
third-party backend chosen by a deployer.

## Data stored on the device

The application stores task data, categories, settings, notification strategy
settings, locally created places, statistics required by the app, and locally
cached weather/location results in its app storage. Export files are created
only when the user explicitly starts an export flow.

## Optional permissions and their purpose

| Permission or capability | Purpose | If not granted |
| --- | --- | --- |
| Location, including background location | Current-location display, map/place selection and arrival/departure geofence reminders | Core task management remains available; location-based features cannot work |
| Microphone | Voice task entry and local speech processing | Voice entry is unavailable |
| Camera | Adding a photo attachment from the camera | Camera capture is unavailable |
| Notifications, alarms and vibration | Task reminders, countdown reminders and briefing notifications | The app cannot deliver the relevant notifications |
| Network | Map/POI services, weather, configured backend synchronization and remote AI functions | Network-backed features are unavailable or show an error |

Android may show additional system settings for exact alarms, background
location, battery restrictions, or notifications. These are controlled by the
user in Android system settings.

## When data may leave the device

Data leaves the device only when a network-backed feature is used:

- **Map, search and location:** location coordinates, search terms and related
  map requests are processed by the configured AMap SDK/service.
- **Weather:** location parameters are sent to the configured weather endpoint.
- **Account and synchronization:** account credentials/tokens and task or
  category synchronization payloads are sent to the configured backend.
- **Remote AI:** content submitted to an enabled remote AI flow is sent to the
  configured backend endpoint for that request.

The default backend URL is defined in the Android build configuration. Anyone
building a fork should set `BACKEND_BASE_URL` to a service they control and
publish that service's own privacy notice.

## Local models

Optional voice-model assets are loaded from the device package or app runtime.
The repository does not include model assets. Whether audio stays local depends
on the selected runtime and backend configuration; do not enable a remote voice
or AI service without reviewing its terms.

## Your controls

You can deny or revoke runtime permissions in Android settings, turn off
location/geofence functionality, avoid logging in or syncing, and export data
from the app. Cloud deletion and retention are determined by the backend used
by your deployment; this repository does not publish a hosted-service retention
policy.

## Security reports

Do not include credentials, personal task content, precise locations, or raw
logs in public issues. See [SECURITY.md](SECURITY.md) for responsible disclosure.
