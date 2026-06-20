# 简记账

简记账是一个本地优先的 Android 记账 App，界面和核心流程参考 vivo 钱包记账，保留手动记账、报表、搜索、导入和自动记账能力，排除营销、广告、贷款、理财推荐等非记账功能。

当前版本：`0.1.4`
应用包名：`com.example.bookkeeping`

## 功能

- 手动记账：支持支出/收入、金额、分类、商户/对象、账户/来源、备注和时间录入。
- 账单管理：按日期分组展示，支持详情、编辑、删除和搜索。
- 收支报表：支持周报、月报、年报和自定义统计，展示支出趋势、分类构成、日均支出和结余。
- 自动记账：支持通知识别和无障碍识别两条路径，覆盖微信支付、支付宝、云闪付/银联、vivo Pay、银行/短信样式通知和通用支付文本。
- 数据导入：支持 CSV 或结构化文本导入，自动跳过重复或无效记录。
- 多语言：支持简体中文、English 和跟随系统。

## 自动记账边界

自动记账只在本机解析支付通知或可见账单文本，并生成待确认账单；不会点击按钮、执行付款、转账或输入密码。

无障碍识别使用包名白名单，当前只处理：

- `com.tencent.mm`
- `com.eg.android.AlipayGphone`
- `com.unionpay`
- `com.vivo.wallet`

本应用自身和未知应用的无障碍文本会被拒绝，避免把首页金额、统计数字或数字键盘误识别为交易。

## 隐私

- 数据保存在本机 Room 数据库。
- 不包含广告、云同步、分析上传或营销 SDK。
- 原始识别文本会做基础脱敏，降低手机号、银行卡号、身份证号等敏感信息落库风险。
- 本地验证截图、UI XML、构建产物和设备本地配置已通过 `.gitignore` 排除，不进入 Git 仓库。

## 构建

要求：

- JDK 17
- Android SDK，`compileSdk 36`
- 可用的 Android 设备或模拟器用于安装验证

常用命令：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest
```

安装 Debug APK：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

运行 instrumentation：

```bash
adb shell am instrument -w -r com.example.bookkeeping.test/androidx.test.runner.AndroidJUnitRunner
```

## 权限开启

通知识别需要在系统设置中开启通知使用权。

无障碍识别需要在系统设置中开启“简记账无障碍识别”。部分 vivo 系统会拦截 ADB 写入无障碍授权，最终验证应以系统设置页手动开启后的状态为准。

## 验证

详细验证记录见：

- [docs/verification-report.md](docs/verification-report.md)
- [docs/vivo-reference-inventory.md](docs/vivo-reference-inventory.md)

本仓库不提交 `reference/` 下的本地截图和 UI XML，因为这些验证材料可能包含真实商户、金额、账户或设备信息。

## 目录

```text
app/src/main/java/com/example/bookkeeping/
  data/          Room 数据库、DAO 和实体
  recognition/   通知、无障碍和导入文本解析
  service/       通知监听和无障碍服务
  ui/            Compose 页面和组件
docs/            验证报告和参考采集说明
```
