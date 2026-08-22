# APP窗口嵌入测试 V3

这是专门用于你的车机做“哪一种方式能把第三方 APP 放进框里”的测试工程。

## 测试顺序
1. 安装 APK。
2. 点击“选择 APP”，选一个容易测试的第三方 APP。
3. 分别点击：
   - A 普通 Activity
   - B 多窗口
   - C ADB 启动
   - D ADB wm/am
   - E 无障碍
   - 读取窗口信息
4. 把每一种方案的实际现象和日志发回来。

## 重要
普通 Android APP 无法仅靠 FrameLayout 把另一个独立 APP 直接嵌入。
本版本的目的不是假装已经实现嵌入，而是一次性验证车机到底开放了哪些窗口控制能力。

## GitHub Actions
已经改成手动触发：
Actions → Build Android APK → Run workflow

不会因为 push 自动打包。
