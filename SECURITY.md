# Security Policy

## Audit Focus

When reviewing this project, please pay special attention to:

- Android permissions declared in `app/src/main/AndroidManifest.xml`.
- Any network access or new dependency that could send data off device.
- Local data handling in `app/src/main/java/com/baohao/esimkeeper/data`.
- Calendar insertion behavior in `app/src/main/java/com/baohao/esimkeeper/ui/ESimKeeperApp.kt`.
- Build reproducibility through the Gradle wrapper and GitHub Actions workflow.

## Current Security Notes

- The app does not request `INTERNET` permission.
- eSIM records are stored locally with Room/SQLite.
- System calendar support uses an insert intent so the user confirms the calendar event.
- Android cloud backup is disabled for the app.
- Dependency repositories are limited to official Google, Maven Central, and Gradle Plugin Portal endpoints.

## Reporting Issues

Please open a GitHub issue with:

- What file or behavior is affected.
- Why it may be a security, privacy, or build reproducibility problem.
- Steps to reproduce, if applicable.
