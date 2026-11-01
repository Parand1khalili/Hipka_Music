# Verification record

Date: 2026-11-01. Final local verification completed successfully.

## Final result

`:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` — **BUILD SUCCESSFUL**.

- Debug APK: version 1.1.0 (version code 2), minimum API 24.
- Unit tests: 10 passed, 0 failures/errors, 0 skipped.
- Lint: 0 errors, 57 warnings. Remaining warnings include dependency update suggestions, unused legacy resources, version-catalog/style suggestions, and the intentionally exported media service (which checks controller trust).
- GitHub CI is configured, but its remote result is not claimed here.

## Executed checks

- Windows host with Android Studio bundled JDK, Android SDK 35, Gradle 8.11.1.
- Official repository resolution failed in this environment. The original project mirror (`HIPKA_MAVEN_MIRROR=https://maven.myket.ir`) was needed for dependency resolution. It is optional, not the default for other developers.
- `:app:testDebugUnitTest`: 10 tests passed in the final verification run (5 Audius mapping/response tests, 2 cancellation tests, 2 offline source tests, 1 pre-existing sample test).
- Live Audius checks: trending returned 20 tracks (4 downloadable), search returned 3 tracks, single-track response confirmed an object payload, and a byte-range streaming request returned HTTP 206 / `audio/mpeg` / 256 bytes. No full song was downloaded during these smoke tests.
- Source and resource files passed UTF-8 validation. `git diff --check` passed.
- ADB started successfully, but `adb devices` listed no connected device. Instrumentation, audible playback, screenshots, and airplane-mode UI behavior are not claimed as tested.

## Offline acceptance checklist (device required)

1. Download an artist-permitted track while online; wait for completion.
2. Enable airplane mode, close and reopen Hipka, and open Downloads.
3. Play the downloaded track, pause/resume, seek, and navigate a queue containing multiple downloads.
4. Lock the screen and check media controls and continued playback. Disconnect headphones and confirm playback pauses.
5. Check favorites, recent listening, and search of cached metadata while offline.
6. Try a never-downloaded track offline: show a recoverable playback error; do not silently mark it downloaded.
7. Interrupt a download, then reconnect: partial files must not appear as available offline.
8. Delete a download: it disappears from Downloads and must no longer be selected as a local playback source.
9. Test English/Persian, RTL, dark/light themes, large fonts, and landscape player scrolling.
10. Test service recreation and process death separately. In-process controller reconnect restores the retained queue; restoring playback after process death is not implemented.

## Remaining boundaries

- No production Supabase credentials or schema were available or required for the default music catalog. Legacy social/auth behavior has not been validated against a live backend.
- Playlist editing and cross-device synchronization are not implemented. The default Library exposes working local favorites, history, and downloads; the old editorial playlist code remains in the repository.
- Audius quotas, stream availability, and download permissions are provider-controlled. No commercial catalog completeness is promised.
- Existing pre-v3 Room upgrades still use the original destructive fallback; a schema-history-based migration requires separate validation. Current schema version is unchanged.
- Dependency review targeted a known Ktor advisory, not an exhaustive transitive security certification.
