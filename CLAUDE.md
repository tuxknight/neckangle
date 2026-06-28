# NeckAngle — Project Context

## Project Overview
Android app that uses front camera + MediaPipe face landmarker to monitor neck forward-head posture in real-time. Detects bad posture, vibrates as alert, runs in background.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Camera**: CameraX (androidx.camera)
- **Face Detection**: MediaPipe Face Landmarker (com.google.mediapipe:tasks-vision)
- **Database**: Room (SQLite)
- **Background**: Foreground Service
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Build**: Gradle with Kotlin DSL

## Design & Style
- **Background**: Dark gradient (#0F0F1A → #1A1A2E)
- **Accent**: Cyan-blue gradient (#00D4FF → #7B2FF7)
- **Fonts**: Numbers in monospace (JetBrains Mono), body in system default
- **Cards**: Large rounded corners (24dp), plenty of whitespace
- **Icons**: Minimal line-art, uniform 2px stroke
- **Vibe**: Clean, spacious, Apple Health / Oura app aesthetic
- **All UI text**: Chinese (simplified) — labels, buttons, descriptions

## Package Structure
```
com.neckangle.app/
├── ui/
│   ├── monitor/       # Real-time monitoring screen
│   ├── stats/         # Statistics/history screen
│   ├── settings/      # Settings screen
│   ├── components/    # Reusable components (gauge, angle display, cards)
│   └── theme/         # Material3 theme, colors, typography
├── engine/
│   ├── camera/        # CameraX wrapper
│   ├── facedetect/    # MediaPipe Face Landmarker wrapper
│   ├── angle/         # Angle calculation + posture classifier
│   └── alert/         # Threshold detection + vibrator trigger
├── service/
│   └── NeckMonitorService.kt   # Foreground Service
├── data/
│   ├── db/            # Room database
│   ├── repository/    # StatsRepo / SettingsRepo
│   └── model/         # Entity + Data classes
└── NeckAngleApp.kt    # Application class
```

## Key Design Decisions

### Posture Classification
- Standing/sitting: compute **forward head angle** (head pitch relative to gravity via face landmarks + IMU)
- Lying down (supine/side): compute **neck bend angle** (head relative to torso)
- Use accelerometer to auto-detect posture mode

### Angle Calculation
- Face landmarks: nose tip, eyes, ears (if visible)
- Head pitch derived from nose-to-ear axis relative to phone plane
- Combined with phone IMU pitch → head relative to gravity
- Standing baseline: phone pitch ≈ 30-60° when held normally

### Background Mode
- Foreground Service with persistent notification
- Drop camera frame rate to 3-5fps when in background
- Notification shows current angle + cumulative bad posture duration

### Alert System
- Default threshold: 25° for 15 consecutive seconds
- Configurable: angle (10-45°), duration (5-120s), vibration pattern, cooldown (30s)
- Vibration uses Android Vibrator API, no audio alerts

### Privacy
- Camera frames processed locally only, never saved to disk or uploaded
- No network permissions, no internet access
- Face data discarded after each frame's angle computation
- Storage (Room DB) only stores: timestamp, angle, duration — no images

### Performance
- Camera preview at 15fps (front camera, low resolution ~480p)
- MediaPipe Face Landmarker with CPU delegate (GPU on supported devices)
- Background mode drops to 3-5fps
- Frame processing skipped if previous frame still being processed

## Data Model (Room)

```kotlin
@Entity(tableName = "records")
data class PostureRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,           // epoch millis
    val angle: Float,              // degrees
    val postureMode: String,       // "standing", "sitting", "lying", "side"
    val isBadPosture: Boolean,     // true if angle exceeded threshold
    val durationSeconds: Int       // consecutive bad posture duration
)

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val angleThreshold: Float = 25f,
    val durationThreshold: Int = 15,    // seconds
    val cooldownSeconds: Int = 30,
    val vibrationPattern: String = "double_short",  // single, double_short, long, pulse
    val frameRateMode: String = "balanced"  // power_save, balanced, precise
)
```

## Permissions Required
1. `CAMERA` — front camera for face detection
2. `FOREGROUND_SERVICE` — background monitoring
3. `FOREGROUND_SERVICE_CAMERA` — Android 14+ camera in foreground service
4. `VIBRATE` — alert vibration
5. `POST_NOTIFICATIONS` — Android 13+ notification permission

## Build Configuration
- AGP: 8.2.x
- Kotlin: 1.9.x
- Compose BOM: 2024.02.00
- MediaPipe: 0.10.13 (com.google.mediapipe:tasks-vision)
- Room: 2.6.x
- Navigation Compose: 2.7.x
- minSdk: 26, targetSdk: 34, compileSdk: 34
