# 验证报告

## 构建和测试

- 包名迁移目标：源码、Gradle `namespace`、`applicationId`、测试包、Room schema 路径和文档命令均已迁移到 `com.github.bookkeeping`。
- `rg -n "com\.example\.bookkeeping" --glob '!**/build/**'`：无匹配。
- `./gradlew testDebugUnitTest`：通过。
- `./gradlew assembleDebug assembleDebugAndroidTest`：通过。
- vivo V2307A 真机安装：
  - `adb install -r -t -g app/build/outputs/apk/debug/app-debug.apk`：通过。
  - `adb install -r -t -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`：通过。
- `adb shell am instrument -w -r com.github.bookkeeping.test/androidx.test.runner.AndroidJUnitRunner`：通过，`OK (1 test)`。
- `./gradlew connectedDebugAndroidTest`：通过，V2307A 上 1/1 tests completed，0 failed。
- 真机端到端启动：
  - `adb shell pm list packages --user 0 | rg 'com\.(github|example)\.bookkeeping'`：仅返回 `package:com.github.bookkeeping`。
  - `adb shell am start -W -n com.github.bookkeeping/.MainActivity`：`Status: ok`，冷启动 `TotalTime: 798` ms。
  - `dumpsys window`：`mFocusedApp=... com.github.bookkeeping/.MainActivity`，`isKeyguardShowing=false`。
  - 启动后 logcat 过滤 `FATAL EXCEPTION|ANR in com.github.bookkeeping|AndroidRuntime`：无匹配。
- 备注：首次运行 `connectedDebugAndroidTest` 时 vivo 系统安装器拒绝了 Gradle/UTP 的安装会话；手动 ADB 安装主 APK 和 androidTest APK 后，直接 instrumentation 与再次运行 Gradle connected test 均通过。

## 2026-06-20 无障碍误识别回归

- 修复严重问题：开启无障碍后，本应用界面上的金额、统计数字和数字键盘不再会被当成交易数字持续写入。
- 防线：
  - `BookkeepingAccessibilityService` 在读取窗口文本前只接受支付应用包名，并再次校验 `rootInActiveWindow.packageName`，拒绝采集本应用窗口。
  - `PaymentTextParser.parseAccessibilityText` 也按同一包名范围拒绝本应用和未知应用，即使文本里出现“交易成功”等完成态关键词也不会生成候选账单。
  - `bookkeeping_accessibility_service.xml` 增加 `android:packageNames="com.tencent.mm,com.eg.android.AlipayGphone,com.unionpay,com.vivo.wallet"`，在系统派发层减少无关无障碍事件。
- 回归测试：
  - `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest`：通过。
  - `aapt2 dump xmltree --file res/xml/bookkeeping_accessibility_service.xml app/build/outputs/apk/debug/app-debug.apk` 已确认 APK 内包含上述 `android:packageNames`。
- 真机覆盖安装和实测：
  - 已连接 vivo `V2307A`（序列号已脱敏）。
  - 修复版已提升为 `0.1.4` / `versionCode=5` 并覆盖安装成功；设备当前 `versionName=0.1.4`，`lastUpdateTime=2026-06-20 08:02:43`。
  - vivo 安装器会弹出外部来源风险确认页；自动化已勾选“已了解应用的风险检测结果”并点击“继续安装”，主 APK 和 androidTest APK 均安装成功。
  - `adb shell am instrument -w -r com.github.bookkeeping.test/androidx.test.runner.AndroidJUnitRunner`：通过，`OK (1 test)`。测试改为使用 `am start -W` + `dumpsys window` 验证前台 Activity，避免 vivo/Compose 下 `rootInActiveWindow` 偶发为空。
  - 已通过 `settings put secure enabled_accessibility_services` 启用真实 `BookkeepingAccessibilityService`，`dumpsys accessibility` 显示 `Bound services` 包含“简记账无障碍识别”。
  - 开启无障碍并停留在本应用首页 5 秒：账单数保持 8，未新增。
  - 开启无障碍后打开“记一笔”并输入 `123.45`、再次在 `0.1.4` 输入 `9.99`，均未点击保存；账单数和最大 id 均不变，验证数字键盘/金额字段不会被当成交易。
  - 开启无障碍后切换“记账/报表/自动/设置”等本应用页面，账单数保持不变。
  - 通知监听正向验证：投递微信支付样式通知“回归咖啡 ¥7.89”后新增 1 条 `WECHAT` 账单；验证完成后删除该临时样例。
  - App 内“无障碍识别测试”正向验证：样例新增微信支出 `¥36.00` 和微信收入 `¥88.00` 两条，优惠券样例被忽略；`0.1.4` 已修复收入商户解析，`来自示例客户` 不再被截断为 `示`。验证完成后删除临时样例。
- 脏数据清理：
  - Claude Code 历史中已确认旧版 bug：开启无障碍后数据库从清理后的 14 条涨到 66 条，其中 58 条含本应用界面特征词。
  - 本轮复查真机数据库仍为 66 条：`GENERIC/EXPENSE` 59 条，`WECHAT/EXPENSE` 6 条，`ALIPAY/EXPENSE` 1 条；最新 `GENERIC` 记录原始文本包含“记账日报”“今日支出(元)”“本月支出(元)”等本应用 UI 文本，金额膨胀到异常巨额。
  - 已在应用私有目录备份 `databases/bookkeeping-before-dirty-cleanup.db`，随后删除 58 条明确由本应用界面数字膨胀产生的 `GENERIC/GENERIC` 脏数据。
  - 清理后真机数据库为 8 条：`WECHAT` 6 条、`ALIPAY` 1 条、`GENERIC` 1 条。剩余 `GENERIC` 文本为“支付成功 交易成功 ¥4.90 支付方式”，不含本应用界面文本，暂保留。
  - 清理后多次复查账单数仍为 8 条；最终状态下 `enabled_accessibility_services` 包含 `com.github.bookkeeping/com.github.bookkeeping.service.BookkeepingAccessibilityService`，本 app 无障碍服务已启用且未继续产生垃圾记录。

## 真机验证

- 已安装包：`com.github.bookkeeping`，版本 `0.1.4`。
- 最终首页干净状态：`reference/app-screenshots/27-final-clean-home.png`。
- 手动 CRUD：
  - 新增支出、首页汇总、详情页和重启持久化已验证。
  - 编辑保存链路已验证，临时账单金额发生更新。
  - 删除确认和删除后空状态已验证。
- 通知自动记账：
  - 已通过 `cmd notification allow_listener` 授权真实 `BookkeepingNotificationListenerService`。
  - 投递微信支付样式测试通知后，首页生成 `微信支付自动记账` 账单。
  - 重复投递同一通知后，自动账单数量未重复增长。
- 无障碍识别：
  - 真实 `BookkeepingAccessibilityService` 已在 manifest 中声明，使用 `BIND_ACCESSIBILITY_SERVICE`。
  - vivo 系统会过滤 ADB 写入的无障碍授权，实际开启需要用户在系统设置页手动确认。
  - App 内“无障碍识别测试”使用同一微信可见文本解析路径，生成微信支出/收入账单，并忽略微信优惠券营销文本。
- 语言：
  - 默认/回退为简体中文。
  - English 切换、重启持久化已验证。
  - 英文首页标题和底部导航改为 `Ledger`，语言选项改为三列适配，避免截断。
- 导入：
  - 设置页提供系统文件选择器导入 CSV/结构化文本。
  - 单元测试覆盖微信 CSV 行导入和重复/无效行跳过逻辑。
- 崩溃扫描：
  - 最近 logcat 未发现 `com.github.bookkeeping` 的 FATAL/ANR。

## 关键截图

以下截图是本地验证产物，已通过 `.gitignore` 排除，不纳入 Git 提交。

- 首页：`reference/app-screenshots/00-home.png`
- 记一笔面板：`reference/app-screenshots/01-add-expense-011.png`
- 报表：`reference/app-screenshots/09-report.png`
- 自动记账页：`reference/app-screenshots/10-automation.png`
- 设置页：`reference/app-screenshots/11-settings.png`
- 英文适配修复：首页 `reference/app-screenshots/15-home-english-fixed.png`，设置 `reference/app-screenshots/16-settings-english-fixed.png`
- 微信通知自动记账：`reference/app-screenshots/20-home-notification-auto-restart.png`
- 无障碍示例识别：`reference/app-screenshots/26-home-fixture-result.png`
