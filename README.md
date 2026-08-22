# 车机分区启动器 v2

## 手动打包
GitHub Actions 已改为 **workflow_dispatch 手动触发**，不会因为 push 自动打包。

进入：
Actions → Build Android APK → Run workflow

## 本版修改
- Java/Kotlin JVM 统一 17。
- 分隔条改为真正的横向拖动区域，不再使用旋转 SeekBar。
- 拖动时左右分区宽度实时变化。
- 增加“窗口控制权限”入口，可打开 Android 无障碍设置。
- 保留 127.0.0.1:5555 的车机 ADB 环境设计。

## 关于“第三方 APP 真正显示在框内”
普通 Android 第三方应用不能被另一个普通 Activity 直接嵌入 FrameLayout。
本版本因此保留独立 APP 启动，并加入无障碍服务基础，以便在你的车机上测试厂商是否允许窗口级控制。

如果车机提供系统级 WindowManager/ADB shell 权限，下一版可以针对实际权限做真正的窗口移动/缩放。
