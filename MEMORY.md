# NeckAngle — 项目记忆

> 手机前置摄像头实时监测颈椎前倾角度，不良姿势震动提醒。

---

## 1. 需求

**原始需求（2026-06-28）：** 基于手机姿态计算脖子前倾角度 → 摄像头 + IMU 实时监测 → 后台持续运行 → 超阈值震动提醒。

**讨论结论：**
- 单靠手机 IMU（陀螺仪+加速度计）不够准，缺少参照系
- 最佳方案：前置摄像头人脸检测 + IMU 姿态合成
- 躺着看手机时「前倾角」概念不成立，需切换到「头颈弯曲角」
- 必须本地处理，不上传不存储不联网

**目标用户：** 长时间低头看手机的上班族、学生、手游玩家。

---

## 2. 技术方案

| 项 | 选择 |
|---|---|
| 平台 | Android (Kotlin) |
| UI | Jetpack Compose + Material3 |
| 相机 | CameraX |
| 人脸检测 | MediaPipe Face Landmarker |
| 数据库 | Room (SQLite) |
| 后台 | Foreground Service |
| 最低版本 | Android 8.0 (API 26) |
| 目标版本 | Android 14 (API 34) |
| 架构 | MVVM (StateFlow + ViewModel) |

**包结构：**
```
com.neckangle.app/
├── ui/          ── Compose 页面 + ViewModel + 组件 + 导航 + 主题
├── engine/      ── CameraX / MediaPipe / 角度计算 / 姿态分类 / 震动提醒
├── service/     ── NeckMonitorService
└── data/        ── Room 数据库 / DAO / Repository / Entity
```

---

## 3. 项目看板

| Backlog | Todo | In Progress | Review | Done |
|---|---|---|---|---|
| — | — | — | — | P0 脚手架 |
| — | — | — | — | P1 主题组件 |
| — | — | — | — | P2 监测引擎 |
| — | — | — | — | P3 UI 页面 |
| — | — | — | — | P4 后台服务 |
| — | — | — | — | P5 导航集成 |

**当前阻塞**: GitHub push (详见阻塞项)

---

## 4. Backlog（优先级排序）

| # | 事项 | 类型 | 优先级 | 预估工时 | 依赖 |
|---|---|---|---|---|---|
| 1 | 首次编译验证 + 修复编译错误 | 工程 | 🔴 P0 | ClaudeCode 20轮 | GitHub push 解封 |
| 2 | MediaPipe 模型自动下载/降级逻辑验证 | 工程 | 🟡 P1 | 5轮 | — |
| 3 | 低端设备性能压测（发热、降帧、耗电） | 测试 | 🟡 P2 | — | APK 可用后 |
| 4 | 状态栏色标切换动画流畅度优化 | 改进 | 🔵 P3 | 3轮 | — |
| 5 | 隐私合规声明页面（首次启动弹窗） | 合规 | 🔵 P3 | 5轮 | — |
| 6 | 提醒冷却期倒计时在 UI 上显示 | 改进 | 🔵 P3 | 3轮 | — |

**唯一阻塞：GitHub push 失败**
- 原因：RPi 上 Git HTTPS 走 SSL 握手失败，SSH key 未配置到 GitHub，fine-grained PAT 缺 contents:write 权限
- 需要 Fuyuan：
  1. 找回 GitHub 账号
  2. 在 https://github.com/settings/tokens 加 `contents: write` 权限
  3. 在 https://github.com/settings/keys 加 SSH key

**替代方案：** 代码在 RPi 上打包为 zip → 传输到本机 → Android Studio 直接编译运行

---

## 5. CI/CD

```
.github/workflows/build.yml
  ├── trigger: push/PR to main
  ├── ubuntu-latest + JDK 17
  ├── ./gradlew assembleDebug
  └── upload app-debug.apk as artifact
```

目标：Fuyuan 只需 push → Actions 自动编出 APK → 下载安装

---

## 6. 待办 / 已知改进点

- [ ] 编译验证后修复编译错误（Claude Code 写的代码需初次编译调试）
- [ ] MediaPipe face_landmarker.task 模型文件的自动下载逻辑（当前在 FaceDetector.kt 中有降级处理）
- [ ] 低端设备性能压测（降帧、发热）
- [ ] 隐私合规声明页面

---

*最后更新: 2026-06-28*
