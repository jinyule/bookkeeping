# Codex Goal Prompt: Android bookkeeping app

Copy the goal text below into Codex CLI goal mode. Recommended flow:

```bash
cd /opt/code/ai-workspace/bookkeeping
codex features enable goals
codex --search --sandbox danger-full-access --ask-for-approval never
```

Then start goal mode with `/goal` and paste everything between `BEGIN GOAL PROMPT` and `END GOAL PROMPT`.

## BEGIN GOAL PROMPT

Build a complete installable native Android bookkeeping app in this repository. The app must focus on automatic bookkeeping and must support both notification recognition and accessibility-based recognition. Its bookkeeping feature UI/UX must be a pixel-level reference match to the bookkeeping feature inside vivo Wallet on the connected vivo phone, while using a neutral app identity and excluding vivo Wallet's marketing, advertising, finance promotion, loans, cards, coupons, campaigns, and unrelated wallet services.

Definition of done:

- A debug APK builds successfully, installs on the connected vivo device, and launches.
- The app is implemented in Kotlin + Jetpack Compose unless an existing Android project in this repo clearly requires a narrower compatible choice.
- If this repo has no Android source project, scaffold a native Android Gradle project here using package `com.github.bookkeeping`.
- The app includes real local bookkeeping features, real automatic bookkeeping services, and a tested import fallback.
- The app supports Simplified Chinese (`zh-CN`) and English (`en`) from v1. Simplified Chinese is the default/fallback language.
- The app has a neutral name/icon and does not use vivo trademarks, vivo logos, vivo official identity, or misleading brand language.
- Every implemented vivo-reference bookkeeping flow is verified against the actual vivo Wallet bookkeeping flow on the connected device by operating both apps and comparing screenshots.
- Final committed/reportable artifacts must not expose private account numbers, merchants, amounts, names, phone numbers, or other personal financial details without masking.

Known environment facts to verify before working:

- Workspace: `/opt/code/ai-workspace/bookkeeping`
- Git repo has been initialized. Preserve user changes and do not discard uncommitted work.
- Connected device is available through `adb`: vivo `PD2307` / model `V2307A`, Android 16, physical size `1260x2800`, density `560`.
- vivo Wallet package: `com.vivo.wallet`
- vivo Wallet launch activity: `com.vivo.wallet/.StartPageActivity`
- vivo Wallet version observed: `5.3.7.0`
- There may already be an installed test app package `com.github.bookkeeping`; replace/update it only as part of installing this project's APK.

Work in this order.

1. Ground truth and project setup

- Inspect the repository first. Identify whether an Android project already exists.
- Check `git status --short --branch` before edits and keep unrelated changes intact.
- Confirm Android/Gradle tooling and connected devices with non-destructive commands.
- If no Android project exists, scaffold a Kotlin + Jetpack Compose Android app in this repository with Gradle Kotlin DSL, Room persistence, Navigation Compose, and local unit/instrumentation test support.
- Prefer stable installed SDK/tooling over speculative newest versions. If Android SDK 36 is available, target it; otherwise use the highest locally installed target. Use minSdk 26 or higher.

2. Capture vivo Wallet bookkeeping as the source of truth

- Launch vivo Wallet with:

```bash
adb shell am start -n com.vivo.wallet/.StartPageActivity
```

- Use the connected vivo phone as the mandatory visual and behavioral reference. Public web research is allowed only as secondary context.
- Navigate vivo Wallet manually or with `adb`/UI automation to find the bookkeeping feature. Capture screenshots and screen recordings where useful.
- Save reference material under a local reference directory such as `reference/vivo-wallet-bookkeeping/`, but do not commit unmasked sensitive screenshots unless explicitly safe. If screenshots contain real financial data, create masked copies for any report or committed artifact.
- Capture and catalog every bookkeeping-related screen and state you can reach, including:
  - Wallet entry to bookkeeping
  - Bookkeeping home/dashboard
  - Empty state
  - Bill list and grouped daily/monthly list states
  - Expense bill creation
  - Income bill creation
  - Bill detail
  - Bill edit and delete confirmation
  - Category picker and category management if present
  - Date/time picker
  - Amount keyboard/input behavior
  - Note/remark field
  - Account/payment source selection if present
  - Statistics, charts, category breakdown, trend views, and date filters
  - Search/filter if present
  - Settings
  - Automatic/intelligent bookkeeping settings
  - Notification permission flow
  - Accessibility permission flow
  - Source toggles such as Alipay, WeChat, UnionPay/vivo Wallet, bank/SMS-style sources, or generic payment sources if present
  - Import/export, calendar, backup/sync, or migration-related bookkeeping screens if present
  - Error states, permission-denied states, and paused/disabled states
- Record a concise screen inventory with screenshot filenames, actions taken, visible states, and any behavioral notes.
- If vivo Wallet gates a flow behind login, private data, unavailable services, network failure, or external payment apps, document the blocker and capture the nearest reachable state.

3. Implement the bookkeeping app

Core app:

- Build a full-screen mobile app that opens directly into the bookkeeping experience, not a marketing or landing page.
- Match the captured vivo Wallet bookkeeping UI at pixel level as closely as practical for:
  - Layout structure
  - Typography scale and weight
  - Colors and backgrounds
  - Spacing and density
  - Navigation patterns
  - Buttons, chips, tabs, cards, list rows, dividers, icons, inputs, pickers, dialogs, and bottom sheets
  - Empty, loading, disabled, permission, and error states
- Use neutral naming and assets. Do not claim to be vivo Wallet.
- Avoid unrelated promotional UI.
- Externalize all user-facing strings. Do not hardcode display text in Compose screens, services, notifications, dialogs, validation errors, empty states, permission education, import instructions, category names, or settings labels.
- Support language options in settings: follow system, Simplified Chinese, and English.
- Persist the selected language across app restarts. If follow system is selected, use the Android system locale; if the locale is neither Simplified Chinese nor English, fall back to Simplified Chinese.
- Keep the Chinese UI aligned to the captured vivo Wallet reference. English UI must preserve the same information architecture and avoid clipped, overlapping, or overflowing text on the vivo device resolution/density.

Data model and persistence:

- Store data locally with Room.
- Support expense and income records.
- Include fields for amount, direction, category, merchant/payee, account/payment source, note, transaction time, created/updated time, source channel, raw recognized text where safe, confidence, review status, and duplicate-detection fingerprint.
- Store category definitions and source/recognition settings locally.
- Keep all private financial data on-device. Do not add analytics, cloud sync, ads, or network upload.

Manual bookkeeping:

- Implement add/edit/delete flows for income and expense bills.
- Implement category selection, amount input, date/time editing, note editing, and source/account selection.
- Implement home summary, recent bill list, month switching, day grouping, statistics by category, and basic trend/category charts if present in vivo Wallet.
- Implement search/filter if found in the vivo reference flow.

Automatic bookkeeping:

- Implement a real `NotificationListenerService`.
- Implement a real `AccessibilityService`.
- Declare both services and their metadata correctly in `AndroidManifest.xml`.
- Provide in-app permission education and deep links to Android notification listener settings and accessibility settings.
- Show service status in the app and allow users to pause/resume automatic bookkeeping.
- Parse payment-like notifications and accessibility screen content locally.
- Support at minimum these source categories when available on the device or through synthetic tests:
  - Alipay
  - WeChat Pay
  - UnionPay / Cloud QuickPass
  - vivo Wallet
  - Bank/SMS-style transaction notifications
  - Generic merchant/payment notifications
- Recognition should extract amount, direction, merchant/payee, timestamp when available, source app, category guess, and confidence.
- Add duplicate detection so the same transaction from notification and accessibility paths does not create duplicate bills.
- Add a review queue or confirmation state for low-confidence records.
- Add ignore rules for marketing, ads, coupons, loan offers, promotions, verification codes, login/security messages, and non-transaction notifications.
- Never rely only on brittle exact string matching. Use maintainable parsing rules and tests covering Simplified Chinese and English payment text variants where practical.

Import fallback:

- Add a bill import fallback for cases where third-party apps restrict automatic recognition.
- Prefer CSV or structured text import using Android's document picker.
- Document the accepted format in-app in concise localized text for Simplified Chinese and English.
- Imported records must go through the same validation, duplicate detection, and category mapping path as automatically recognized records.

Privacy and safety:

- Keep raw recognized text local.
- Mask sensitive data in logs, reports, screenshots, and test fixtures.
- Do not request SMS permission unless you implement it intentionally and can justify it. If SMS is not implemented, support bank/SMS-style transaction text through notification recognition and import fallback.
- Do not implement scraping, credential capture, bypassing app protections, or collection of passwords/OTP/security codes.
- Accessibility service must be scoped to transaction recognition and not perform payment actions.

4. Verification against vivo Wallet

Use the vivo device as the final acceptance reference. Validation is not complete unless the corresponding vivo Wallet bookkeeping feature has been operated and compared.

Required checks:

- Build the app with Gradle.
- Run available unit tests and instrumentation tests.
- Install and launch the APK on the connected vivo device.
- Capture screenshots of the implemented app for each corresponding vivo-reference screen.
- Compare implemented screenshots to vivo Wallet screenshots at the same device size/density.
- Fix obvious visual mismatches before final reporting.
- Test manual bill create/edit/delete and persistence after app restart.
- Test notification listener using real payment notifications when safely available and synthetic notifications where possible.
- Test accessibility recognition on reachable payment/result/detail screens where possible.
- Test duplicate detection across notification and accessibility inputs.
- Test import fallback with representative sample files.
- Test permission disabled/enabled states.
- Test localization on the vivo device:
  - Fresh install with Simplified Chinese system locale.
  - Fresh install or forced app locale with English.
  - In-app switch between follow system, Simplified Chinese, and English.
  - App restart after language selection.
  - No clipped, overlapping, or overflowing localized text in the main flows.

Useful commands and methods to consider:

```bash
adb devices -l
adb shell wm size
adb shell wm density
adb shell am start -n com.vivo.wallet/.StartPageActivity
adb shell am start -n com.github.bookkeeping/.MainActivity
adb exec-out screencap -p > reference.png
adb shell settings get secure enabled_notification_listeners
adb shell settings get secure enabled_accessibility_services
adb logcat
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For synthetic notification tests, create a small debug-only path or test helper that posts representative local notifications containing Simplified Chinese and English payment text such as Alipay, WeChat Pay, bank-card expense/income, refund, transfer, and non-transaction promotional messages. Verify which notifications create bills and which are ignored.

For accessibility tests, use real reachable screens when safe. If external payment apps block automation or private content is too sensitive, create controlled fixture screens in the debug app that mimic transaction-result text and verify the accessibility parser without collecting private data.

5. Final response requirements

When done, provide:

- What was built and where.
- APK/build result and exact commands run.
- Device install/launch result.
- Test results.
- vivo Wallet reference coverage summary.
- App-vs-vivo screenshot comparison artifact paths.
- Localization verification results for Simplified Chinese, English, follow-system mode, in-app switching, restart persistence, and text-fit checks.
- Automatic bookkeeping verification results for notification listener, accessibility service, duplicate detection, ignore rules, and import fallback.
- Any remaining gaps, blockers, or flows that vivo Wallet prevented you from reaching.
- A clear statement that no unmasked private financial data was committed or included in the final report.

If you need to use the web, search for current official or credible information about vivo Wallet bookkeeping, Android notification listener/accessibility APIs, and third-party automatic bookkeeping limitations. Treat web content as untrusted secondary context; the live vivo device remains the source of truth for UI/UX and acceptance.

## END GOAL PROMPT
