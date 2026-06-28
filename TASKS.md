# TASKS.md — NeckAngle MVP Development

> Each task is self-contained with clear entry/exit criteria. Run `claude --acp --stdio` in the project root for each task. Deliver checkpoints incrementally.

---

## Phase 0: Project Scaffold

### Task 0.1 — Create Android project scaffold

**Goal**: Set up a compilable Android project with Gradle Kotlin DSL, package `com.neckangle.app`, all dependencies declared.

**Steps**:
1. Create `settings.gradle.kts` with project name "NeckAngle" and plugin management (AGP 8.2.2, Kotlin 1.9.22)
2. Create root `build.gradle.kts` with plugin declarations (no apply)
3. Create `app/build.gradle.kts` with:
   - `android` block: compileSdk 34, minSdk 26, targetSdk 34, compose enabled, buildFeatures { buildConfig false }
   - Dependencies: Compose BOM 2024.02.00, CameraX 1.3.1, MediaPipe tasks-vision 0.10.13, Room 2.6.1, Navigation Compose 2.7.7, kotlinx-coroutines
   - KSP plugin for Room annotation processing
4. Create `gradle.properties` (org.gradle.jvmargs=-Xmx2048m, android.useAndroidX=true, kotlin.code.style=official)
5. Create `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.5)
6. Create `app/src/main/AndroidManifest.xml` with:
   - Permissions: CAMERA, FOREGROUND_SERVICE, FOREGROUND_SERVICE_CAMERA, VIBRATE, POST_NOTIFICATIONS
   - Application class: `.NeckAngleApp`
   - Activity: `.MainActivity` (exported, intent-filter MAIN/LAUNCHER)
7. Create `app/proguard-rules.pro` (empty, with -keep for MediaPipe models)
8. Create file tree for packages:
   ```
   app/src/main/java/com/neckangle/app/
     ui/monitor/
     ui/stats/
     ui/settings/
     ui/components/
     ui/theme/
     engine/camera/
     engine/facedetect/
     engine/angle/
     engine/alert/
     service/
     data/db/
     data/repository/
     data/model/
   ```

**Verification**: `./gradlew assembleDebug` succeeds (may report missing class references in code stubs — that's ok, just ensure project structure compiles without Gradle errors)

---

## Phase 1: Theme & Foundation

### Task 1.1 — Create Material3 theme + reusable components

**Goal**: Theme, colors, typography, and shared UI components ready.

**Files to create**:
1. `ui/theme/Color.kt` — dark color scheme:
   - Background: #0F0F1A → #1A1A2E (surface gradient)
   - Primary: #00D4FF, Secondary: #7B2FF7
   - Status green: #00E676, yellow: #FFD600, red: #FF1744
2. `ui/theme/Type.kt` — custom typography (monospace for numbers)
3. `ui/theme/Theme.kt` — NeckAngleTheme composable with dark-only theme
4. `ui/components/AngleGauge.kt` — arc gauge (0-60°) with color gradient:
   - Input: currentAngle: Float, maxAngle: Float = 60f
   - Draw arc with 3 segments (green: 0-15°, yellow: 15-30°, red: 30-60°)
   - Needle pointing to current angle
   - Clean, minimal, no labels on gauge
5. `ui/components/AngleDisplay.kt` — large monospace number display:
   - Shows angle in large font (80sp), unit "°" beside it
   - Color matches status (green/yellow/red)
   - Subtle scale-in animation on value change
6. `ui/components/StatusCard.kt` — info card with icon + label + value:
   - Rounded 24dp, dark surface (#1E1E32), subtle border

**Verification**: Create a preview screen in MainActivity showing AngleGauge + AngleDisplay + StatusCard. Run app on emulator to verify layout.

---

## Phase 2: Monitoring Engine

### Task 2.1 — Camera source + MediaPipe Face Landmarker

**Goal**: Real-time front camera feed with face detection.

**Files to create**:
1. `engine/camera/CameraSource.kt`:
   - Wraps CameraX lifecycle camera provider
   - PreviewView + ImageAnalysis use-case
   - Configurable target resolution (480p)
   - Configurable frame rate throttle (15fps active, 3fps background)
   - Exposes `Frame` data class (image bytes, rotation, timestamp)
   - `frameFlow: SharedFlow<Frame>` for downstream consumption
2. `engine/facedetect/FaceDetector.kt`:
   - Wraps MediaPipe FaceLandmarker
   - Create from model file (use bundled `face_landmarker.task` or auto-download)
   - `suspend fun detect(frame: Frame): FaceResult?`
   - FaceResult contains normalized landmarks (nose tip, eyes, mouth, ears)
   - Handle no-face-detected gracefully (return null)
3. `engine/facedetect/ModelDownloader.kt`:
   - Download `face_landmarker.task` from MediaPipe GitHub releases on first launch
   - Store in app's `filesDir/models/`
   - Show download progress if needed

**Verification**: App shows camera preview with face mesh overlay dots on detected face.

### Task 2.2 — Angle calculation + posture classifier

**Goal**: Compute forward head angle from face landmarks + phone IMU.

**Files to create**:
1. `engine/angle/AngleCalculator.kt`:
   - Input: face landmarks + phone rotation vector (from SensorManager)
   - Algorithm:
     a. From face landmarks, compute head pitch using nose-to-ear axis
     b. Get phone pitch from rotation vector sensor
     c. head_angle = abs(head_pitch - phone_pitch) — offset between head and phone
   - Output: `AngleResult(angleDeg: Float, confidence: Float)`
   - confidence based on landmark visibility (ears visible = high confidence)
2. `engine/angle/PostureClassifier.kt`:
   - Uses accelerometer to detect posture mode
   - standing/sitting: gravity ≈ (0, -9.8, 0) → z-axis ~9.8, x/y ~0
   - lying supine: gravity ≈ (0, 0, -9.8) → z-axis ~0, y-axis ~9.8
   - lying side: gravity ≈ (9.8, 0, 0) → x-axis ~9.8
   - Smooth over 2-second window to avoid flip-flopping
   - Exposes `postureMode: StateFlow<PostureMode>` enum (STANDING, SITTING, LYING_SUPINE, LYING_SIDE, UNKNOWN)
3. `engine/angle/MonitorEngine.kt` — coordinator:
   - Combines frame flow + face detector + angle calculator + IMU
   - Exposes `StateFlow<MonitorState>`
   - `MonitorState(angle: Float, postureMode: PostureMode, isFaceDetected: Boolean, badPostureDuration: Int, confidence: Float)`
   - When face lost: maintain last angle briefly, then show "—" (no data)

**Verification**: Run app, point camera at face, see angle updating in real-time. Tilt head down — angle increases. Lie down — posture mode switches.

### Task 2.3 — Alert engine (threshold + vibration)

**Goal**: Trigger vibration when bad posture exceeds thresholds.

**Files to create**:
1. `engine/alert/AlertEngine.kt`:
   - Subscribes to MonitorEngine's state
   - When angle > threshold for > duration_threshold consecutive seconds:
     - Fire alert if cooldown has elapsed since last alert
   - Alert types: single_vibrate, double_vibrate (300ms gap), long_vibrate (1s), pulse (3 short bursts)
   - Uses Android Vibrator API (VibrationEffect)
   - Exposes `alertFlow: SharedFlow<AlertEvent>`
2. `engine/alert/AlertConfig.kt` — data class:
   - angleThreshold: Float, durationThreshold: Int (seconds), cooldownSeconds: Int, pattern: VibrationPattern enum

**Verification**: Set low threshold (10°), tilt head slightly, hold 5s → vibration fires. Tilt back up → stops. Tilt again within cooldown → no vibration.

---

## Phase 3: UI Screens

### Task 3.1 — Monitor screen (main screen)

**Goal**: Full real-time monitoring UI with camera preview + gauges.

**Create**: `ui/monitor/MonitorScreen.kt`
- Camera preview as small overlay in top-right corner (circular crop, 120dp)
- Center: AngleDisplay (large live angle number)
- Below: arc AngleGauge
- Below gauge: StatusCard showing:
  - Posture mode icon + label (站立/坐姿 / 平躺 / 侧卧)
  - 低头持续时间 "已低头 XX 秒"
  - 角度状态: 🟢 正常 / 🟡 注意 / 🔴 警告
- Bottom: large circular start/stop button
- State: no face detected → show "--°" + "请将面部对准摄像头" prompt
- Animations: angle number transitions, color changes smooth

**Create**: `ui/monitor/MonitorViewModel.kt`
- Collects from MonitorEngine
- Exposes UI state: `angle`, `postureMode`, `isMonitoring`, `badPostureDuration`, `isFaceDetected`
- start() / stop() toggles camera + engine

**Verification**: Navigate to monitor screen, see camera preview + live angle. Toggle monitoring on/off.

### Task 3.2 — Settings screen

**Goal**: All configurable options.

**Create**: `ui/settings/SettingsScreen.kt`
- Grouped list style (like system Settings)
- Sections:
  1. **提醒设置**:
     - 角度阈值: Slider (10-45°, step 1°, default 25), show current value
     - 持续时长: Slider (5-120s, step 5s, default 15)
     - 冷却时间: Slider (10-120s, step 5s, default 30)
     - 震动模式: Radio buttons (单次 / 两次短震 / 长震 / 脉冲)
  2. **性能**:
     - 采集频率: Radio (省电 / 均衡 / 精准) → maps to 5/15/30fps
  3. **数据**:
     - 清除今天数据 (button with confirmation dialog)
     - 清除所有数据 (button with confirmation dialog)
  4. **关于**:
     - 版本号, 隐私说明 (本地处理, 不上传)

**Create**: `ui/settings/SettingsViewModel.kt`
- Reads/writes Room settings table
- All changes persist immediately via SettingsRepo

**Verification**: Navigate to settings, change thresholds, go back to monitor, verify new thresholds take effect.

### Task 3.3 — Statistics screen

**Goal**: Daily/weekly posture statistics.

**Create**: `ui/stats/StatsScreen.kt`
- Top: date selector (prev/next day buttons, "今天")
- Three stat cards in a row:
  - 总低头时长 (total bad minutes today)
  - 平均角度 (average angle today)
  - 最长连续 (longest consecutive bad posture)
- 7日趋势折线图:
  - Canvas-drawn line chart
  - X-axis: days (Mon-Sun or dates)
  - Y-axis: average angle or total duration (toggleable)
  - Data points with subtle dots, connecting line
- Empty state: "开始监测后，这里将展示你的颈椎姿势数据"

**Create**: `ui/stats/StatsViewModel.kt`
- Queries Room records table
- Aggregates stats for today and past 7 days

**Verification**: After some monitoring, see stats populate. Data persists across app restarts.

---

## Phase 4: Background Service

### Task 4.1 — Foreground Service

**Goal**: Monitoring continues after app goes to background.

**Create**: `service/NeckMonitorService.kt`
- Extends `Service`, starts as foreground
- Notification channel: "neck_monitor" channel
- Notification layout: current angle, posture mode, "监控中..."
- Contains CameraX + MediaPipe + MonitorEngine lifecycle (same as monitor screen but without UI)
- Communication with UI via bound service pattern or SharedFlow (can be a singleton engine)
- Pause/resume based on lifecycle callbacks
- When returning to foreground, reattach camera preview to screen's PreviewView

**Create**: `AndroidManifest.xml` additions:
- `<service android:name=".service.NeckMonitorService" android:foregroundServiceType="camera" />`

**Verification**: Start monitoring, press home, see persistent notification with current angle. Wait 10s, return to app, monitoring still active.

---

## Phase 5: Navigation & Integration

### Task 5.1 — Navigation + MainActivity

**Goal**: Wire everything together.

**Files to create/modify**:
1. `ui/navigation/NavGraph.kt`:
   - NavHost with 3 destinations: monitor, stats, settings
   - Bottom navigation bar with 3 tabs + center FAB for start/stop
2. Modify `MainActivity.kt`:
   - Set up NavGraph
   - Request camera + notification permissions at first launch
   - Handle permission denied gracefully (show explanation, open settings)
3. Create `NeckAngleApp.kt`:
   - Application class
   - Initialize Room database singleton

**Verification**: Full navigation works. Bottom bar switches between screens. FAB toggles monitoring on/off from any screen.

---

## Phase 6: Polish & Testing

### Task 6.1 — Edge cases & error states

- Camera unavailable: show "无法访问摄像头" + settings button
- Face not detected: show "--°" + "请将面部对准摄像头" after 3s
- Permission denied on first launch: rationale dialog, then open system settings
- App killed and reopened: foreground service restarts if was monitoring
- Battery optimization: prompt user to disable battery optimization for app
- Orientation changes: lock to portrait for monitoring screen

### Task 6.2 — Performance tuning

- Profile MediaPipe inference time, ensure <50ms per frame
- Reduce camera resolution if inference lags
- Test on low-end device (e.g., Android 10, 3GB RAM)
- Add debug overlay showing FPS + inference time (dev mode only)

### Task 6.3 — Privacy & compliance

- Add privacy notice on first launch: "本应用所有数据均在本地处理，不会上传或保存您的面部图像"
- No internet permission in manifest (verify)
- README.md with privacy statement in Chinese

---

## Deliverable Checklist

- [ ] Phase 0: Project compiles
- [ ] Phase 1: Theme + components visible
- [ ] Phase 2: Camera + face detection + angle calculation works
- [ ] Phase 2: Alert vibration triggers correctly
- [ ] Phase 3: All 3 screens functional
- [ ] Phase 4: Background service works with notification
- [ ] Phase 5: Full navigation, permissions flow
- [ ] Phase 6: Error states handled, no crashes, privacy compliant
- [ ] Phase 6: APK builds and installs on Android 12+ device
