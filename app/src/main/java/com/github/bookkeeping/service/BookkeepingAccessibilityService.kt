package com.github.bookkeeping.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.github.bookkeeping.AccessibilityPackages
import com.github.bookkeeping.BookkeepingApplication
import com.github.bookkeeping.BookkeepingRepository
import com.github.bookkeeping.SettingsStore
import com.github.bookkeeping.recognition.PaymentTextParser
import java.util.concurrent.Executors

class BookkeepingAccessibilityService : AccessibilityService() {
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "bookkeeping-accessibility").apply { isDaemon = true }
    }

    // 主线程读写，但事件分发通常串行；加 @Volatile 以保证可见性。
    @Volatile
    private var lastFingerprint: String? = null
    @Volatile
    private var lastInsertAt: Long = 0L

    @Suppress("DEPRECATION")
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val store = SettingsStore(applicationContext)
        if (store.automaticPaused) return

        val packageName = event.packageName?.toString()
        // 关键防御：只识别已知支付应用的界面，且永远不抓本应用自身。
        // 否则记账界面上的金额、数字键盘等会被当成交易金额反复写入垃圾记录。
        if (!AccessibilityPackages.shouldProcess(packageName)) return

        // rootInActiveWindow 由本服务拥有，可以 recycle；event.source 由系统管理，不要 recycle。
        val root = rootInActiveWindow
        val visibleText = try {
            buildString {
                event.text.joinToString(" ").takeIf { it.isNotBlank() }?.let { append(it).append(' ') }
                root
                    ?.takeIf { AccessibilityPackages.shouldProcess(it.packageName?.toString()) }
                    ?.let { collectText(it, this, 0) }
            }.trim()
        } finally {
            root?.recycle()
        }
        if (visibleText.length < 6) return

        val candidate = PaymentTextParser.parseAccessibilityText(packageName, visibleText) ?: return
        if (!store.isSourceEnabled(candidate.sourceChannel)) return
        val now = System.currentTimeMillis()
        if (candidate.fingerprint == lastFingerprint && now - lastInsertAt < 30_000L) return
        lastFingerprint = candidate.fingerprint
        lastInsertAt = now

        executor.execute {
            BookkeepingRepository.insertCandidateBlocking(BookkeepingApplication.instance.database, candidate)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun collectText(node: AccessibilityNodeInfo, out: StringBuilder, depth: Int) {
        if (depth > 8) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.append(it).append(' ') }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectText(child, out, depth + 1)
                child.recycle()
            }
        }
    }
}
