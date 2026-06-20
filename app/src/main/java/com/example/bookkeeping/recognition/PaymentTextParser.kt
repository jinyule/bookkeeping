package com.example.bookkeeping.recognition

import com.example.bookkeeping.CategoryKey
import com.example.bookkeeping.AccessibilityPackages
import com.example.bookkeeping.Direction
import com.example.bookkeeping.ReviewStatus
import com.example.bookkeeping.SourceKey
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

object PaymentTextParser {
    private val explicitExpense = listOf(
        "支付", "付款", "支出", "消费", "扣款", "转出", "付款成功", "交易成功",
        "paid", "payment", "spent", "purchase", "debit", "charged", "sent"
    )
    private val explicitIncome = listOf(
        "收款", "收入", "到账", "入账", "退款", "转入", "已收", "收钱",
        "received", "credited", "refund", "deposit", "incoming"
    )
    private val ignoreKeywords = listOf(
        "优惠券", "优惠", "立减", "红包封面", "活动", "权益", "贷款", "借钱", "额度", "利率",
        "广告", "营销", "推广", "验证码", "校验码", "登录", "安全提醒", "风险提醒", "密码",
        "coupon", "promo", "promotion", "discount", "loan", "credit offer", "verification code",
        "otp", "login", "password", "security code"
    )
    private val wechatHints = listOf("微信", "微信支付", "WeChat", "Weixin", "零钱")
    private val alipayHints = listOf("支付宝", "Alipay", "余额宝")
    private val unionPayHints = listOf("云闪付", "银联", "UnionPay", "Cloud QuickPass")
    private val vivoHints = listOf("vivo Pay", "钱包支付", "vivo钱包", "vivo Wallet")
    private val bankHints = listOf("银行", "银行卡", "信用卡", "储蓄卡", "尾号", "Bank", "card ending")

    fun parse(sourcePackage: String?, title: String?, text: String?, nowMillis: Long = System.currentTimeMillis()): RecognitionCandidate? {
        val raw = listOfNotNull(title, text).joinToString(" ").trim()
        if (raw.isBlank() || shouldIgnore(raw)) return null

        val source = detectSource(sourcePackage.orEmpty(), raw)
        val sourceAllowed = source != SourceKey.GENERIC || looksTransactional(raw)
        if (!sourceAllowed) return null

        val amount = extractAmount(raw) ?: return null
        val direction = detectDirection(raw) ?: return null
        val merchant = extractMerchant(raw, direction).ifBlank { source }
        val category = guessCategory(raw, direction, merchant)
        val transactionTime = extractTimestamp(raw) ?: nowMillis
        val confidence = score(raw, source, merchant, direction)
        val reviewStatus = if (confidence >= 0.70) ReviewStatus.ACCEPTED else ReviewStatus.REVIEW
        val fingerprint = fingerprint(source, amount, direction, merchant, transactionTime)

        return RecognitionCandidate(
            amountCents = amount,
            direction = direction,
            categoryKey = category,
            merchant = merchant,
            account = source,
            note = "",
            transactionTime = transactionTime,
            sourceChannel = source,
            rawText = raw,
            confidence = confidence,
            reviewStatus = reviewStatus,
            fingerprint = fingerprint
        )
    }

    /**
     * 无障碍界面文本比通知更杂乱（含待支付页、付款码页、记账页等非完成态界面），
     * 因此额外要求文本含明确的"支付完成态"关键词，避免把界面上的金额数字误判为交易。
     */
    private val completionKeywords = listOf(
        "支付成功", "交易成功", "支付完成", "支付凭证", "已支付", "已扣费", "扣款成功", "扣费成功", "扣款凭证",
        "收款到账", "收款成功", "到账", "入账", "退款成功", "退款到账",
        "paid", "payment successful", "payment receipt", "transaction successful", "received"
    )

    fun parseAccessibilityText(packageName: String?, visibleText: String, nowMillis: Long = System.currentTimeMillis()): RecognitionCandidate? {
        if (!AccessibilityPackages.shouldProcess(packageName)) return null
        // 完成态门槛：无障碍识别要求文本明确含支付完成态关键词
        val lower = visibleText.lowercase(Locale.ROOT)
        if (completionKeywords.none { lower.contains(it.lowercase(Locale.ROOT)) }) return null
        val parsed = parse(packageName, null, visibleText, nowMillis) ?: return null
        val source = normalizeAccessibilitySource(packageName, visibleText)
        return parsed.copy(
            sourceChannel = source,
            account = source,
            fingerprint = fingerprint(source, parsed.amountCents, parsed.direction, parsed.merchant, parsed.transactionTime)
        )
    }

    fun parseStructuredRow(row: List<String>, nowMillis: Long = System.currentTimeMillis()): RecognitionCandidate? {
        if (row.isEmpty()) return null
        val joined = row.joinToString(" ")
        val directionCell = row.withIndex().firstOrNull { isIncomeDirection(it.value) || isExpenseDirection(it.value) }
        val direction = directionCell
            ?.takeIf { isIncomeDirection(it.value) }
            ?.let { Direction.INCOME }
            ?: directionCell
                ?.takeIf { isExpenseDirection(it.value) }
                ?.let { Direction.EXPENSE }
            ?: detectDirection(joined)
            ?: return parse(null, null, joined, nowMillis)

        val timeCell = row.withIndex().firstOrNull { extractTimestamp(it.value) != null }
        val amountCell = row.withIndex().firstOrNull { cell ->
            cell.index != directionCell?.index &&
                cell.index != timeCell?.index &&
                extractStructuredAmount(cell.value) != null
        }
        val amount = amountCell?.let { extractStructuredAmount(it.value) } ?: return parse(null, null, joined, nowMillis)
        val source = detectSource("", joined).let { if (it == SourceKey.GENERIC) SourceKey.IMPORT else it }
        val merchant = row.getOrNull(3)
            ?.takeIf { it.isNotBlank() }
            ?: row.withIndex()
                .firstOrNull { it.index !in setOf(directionCell?.index, timeCell?.index, amountCell.index) && it.value.isNotBlank() }
                ?.value
            ?: extractMerchant(joined, direction).ifBlank { source }
        val category = categoryFromText(row.getOrNull(5), direction) ?: guessCategory(joined, direction, merchant)
        val account = row.getOrNull(6)?.takeIf { it.isNotBlank() } ?: source
        val note = row.getOrNull(4).orEmpty()
        val time = timeCell?.let { extractTimestamp(it.value) } ?: nowMillis
        val fingerprint = fingerprint(source, amount, direction, merchant, time)
        return RecognitionCandidate(
            amountCents = amount,
            direction = direction,
            categoryKey = category,
            merchant = merchant,
            account = account,
            note = note,
            transactionTime = time,
            sourceChannel = source,
            rawText = joined,
            confidence = 0.82,
            reviewStatus = ReviewStatus.ACCEPTED,
            fingerprint = fingerprint
        )
    }

    fun shouldIgnore(raw: String): Boolean {
        val normalized = raw.lowercase(Locale.ROOT)
        val ignored = ignoreKeywords.any { normalized.contains(it.lowercase(Locale.ROOT)) }
        if (!ignored) return false
        if ((normalized.contains("退款") || normalized.contains("refund")) && extractAmount(raw) != null) return false
        if (
            listOf("优惠券", "coupon", "promo", "promotion", "discount").any {
                normalized.contains(it.lowercase(Locale.ROOT))
            }
        ) {
            return true
        }
        val strongTransaction = (
            explicitExpense + listOf("收款", "收入", "入账", "转入", "received", "credited", "deposit", "incoming")
            ).any { normalized.contains(it.lowercase(Locale.ROOT)) }
        return !strongTransaction
    }

    private fun looksTransactional(raw: String): Boolean {
        val normalized = raw.lowercase(Locale.ROOT)
        return (explicitExpense + explicitIncome).any { normalized.contains(it.lowercase(Locale.ROOT)) } &&
            extractAmount(raw) != null
    }

    fun detectSource(sourcePackage: String, raw: String): String {
        val combined = "$sourcePackage $raw"
        return when {
            combined.contains("com.tencent.mm", true) || wechatHints.any { combined.contains(it, true) } -> SourceKey.WECHAT
            combined.contains("alipay", true) || alipayHints.any { combined.contains(it, true) } -> SourceKey.ALIPAY
            combined.contains("unionpay", true) || unionPayHints.any { combined.contains(it, true) } -> SourceKey.UNIONPAY
            combined.contains("com.vivo.wallet", true) || vivoHints.any { combined.contains(it, true) } -> SourceKey.VIVO_WALLET
            bankHints.any { combined.contains(it, true) } -> SourceKey.BANK
            else -> SourceKey.GENERIC
        }
    }

    private fun normalizeAccessibilitySource(packageName: String?, raw: String): String {
        return detectSource(packageName.orEmpty(), raw)
    }

    private fun detectDirection(raw: String): String? {
        val normalized = raw.lowercase(Locale.ROOT)
        val incomeHit = explicitIncome.any { normalized.contains(it.lowercase(Locale.ROOT)) }
        val expenseHit = explicitExpense.any { normalized.contains(it.lowercase(Locale.ROOT)) }
        val isRefund = normalized.contains("退款") || normalized.contains("refund")
        return when {
            // 退款语义优先于支出关键词（如"支付退款"应为收入）
            isRefund -> Direction.INCOME
            incomeHit && !expenseHit -> Direction.INCOME
            expenseHit && !incomeHit -> Direction.EXPENSE
            normalized.contains("收款") || normalized.contains("received") -> Direction.INCOME
            expenseHit -> Direction.EXPENSE
            else -> null
        }
    }

    private fun extractAmount(raw: String): Long? {
        // 先抹掉日期/时间片段，避免把 "2026-06-08 12:30" 里的数字误当金额
        val masked = maskDateTime(raw)

        val contextual = Regex("""(?:支出|支付|付款|消费|扣款|收款|收入|到账|入账|退款|paid|payment|received|refund|credited|debit|charged)[^0-9¥￥]{0,24}(?:¥|￥|RMB|CNY)?\s*([0-9]{1,9}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]{1,9}(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
            .find(masked)
            ?.groupValues
            ?.getOrNull(1)
        if (contextual != null) {
            return parseAmountToken(contextual)
        }
        // 兜底必须带货币符号或单位，防止把任意裸数字误判为金额
        val anchored = Regex("""(?:¥|￥|RMB|CNY|USD)\s*([0-9]{1,9}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)|([0-9]{1,9}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:元|yuan|CNY|RMB|USD)""", RegexOption.IGNORE_CASE)
            .find(masked)
        val token = anchored?.let { it.groupValues[1].ifBlank { it.groupValues[2] } }?.takeIf { it.isNotBlank() }
        return token?.let { parseAmountToken(it) }
    }

    private fun parseAmountToken(token: String): Long? =
        token.replace(",", "").toDoubleOrNull()?.let { (it * 100).roundToLong() }

    private fun maskDateTime(raw: String): String {
        var out = raw
        out = Regex("""20\d{2}[-/.年]\d{1,2}[-/.月]\d{1,2}日?""").replace(out, " ")
        out = Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""").replace(out, " ")
        return out
    }

    private fun extractStructuredAmount(raw: String): Long? {
        val compact = raw.trim()
        if (compact.isBlank() || extractTimestamp(compact) != null) return null
        val normalized = compact.replace(",", "")
        val amountOnly = Regex("""^[+-]?(?:¥|￥|RMB|CNY|USD)?\s*[0-9]{1,9}(?:\.[0-9]{1,2})?\s*(?:元|yuan|CNY|RMB|USD)?$""", RegexOption.IGNORE_CASE)
        if (!amountOnly.matches(normalized)) return null
        // 结构化单元格已由 amountOnly 校验为纯金额格式，直接取数字解析，
        // 不走 extractAmount 的"必须带货币符号"兜底逻辑。
        val digits = normalized.trimStart('+', '-')
            .replace(Regex("""[^\d.]"""), "")
        return parseAmountToken(digits)
    }

    private fun extractMerchant(raw: String, direction: String): String {
        val patterns = if (direction == Direction.EXPENSE) {
            listOf(
                Regex("""向(.{1,24}?)(?:支付|付款)"""),
                Regex("""在(.{1,24}?)(?:消费|支付)"""),
                Regex("""(?:to|at)\s+([A-Za-z0-9 ._-]{2,40})""", RegexOption.IGNORE_CASE),
                Regex("""商户[:：]\s*(.{1,24})""")
            )
        } else {
            listOf(
                Regex("""来自(.{1,24}?)(?:的)?(?:收款|转账|付款|到账)"""),
                Regex("""来自([^，,。；;\n]{1,24})"""),
                Regex("""(?:from)\s+([A-Za-z0-9 ._-]{2,40})""", RegexOption.IGNORE_CASE),
                Regex("""付款方[:：]\s*(.{1,24})""")
            )
        }
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(raw)?.groupValues?.getOrNull(1)?.cleanupMerchant()
        }.orEmpty()
    }

    private fun guessCategory(raw: String, direction: String, merchant: String): String {
        val text = "$raw $merchant".lowercase(Locale.ROOT)
        if (direction == Direction.INCOME) {
            return when {
                text.contains("工资") || text.contains("salary") -> CategoryKey.SALARY
                text.contains("退款") || text.contains("refund") -> CategoryKey.REFUND
                text.contains("红包") || text.contains("red packet") -> CategoryKey.RED_PACKET
                text.contains("理财") || text.contains("interest") -> CategoryKey.FINANCE
                else -> CategoryKey.RECEIPT
            }
        }
        return when {
            listOf("餐", "咖啡", "奶茶", "饭", "food", "coffee", "restaurant").any { text.contains(it) } -> CategoryKey.FOOD
            listOf("公交", "地铁", "打车", "高速", "transport", "taxi", "metro").any { text.contains(it) } -> CategoryKey.TRANSPORT
            listOf("订阅", "会员", "subscription", "ai").any { text.contains(it) } -> CategoryKey.SUBSCRIPTION
            listOf("房租", "物业", "housing", "rent").any { text.contains(it) } -> CategoryKey.HOUSING
            listOf("话费", "流量", "通信", "mobile", "telecom").any { text.contains(it) } -> CategoryKey.COMMUNICATION
            listOf("医院", "药", "medical", "clinic").any { text.contains(it) } -> CategoryKey.MEDICAL
            listOf("购物", "超市", "shop", "store", "mall").any { text.contains(it) } -> CategoryKey.SHOPPING
            listOf("转账", "transfer").any { text.contains(it) } -> CategoryKey.TRANSFER
            else -> CategoryKey.CONSUMPTION
        }
    }

    private fun categoryFromText(raw: String?, direction: String): String? {
        val text = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (text.isBlank()) return null
        if (direction == Direction.INCOME) {
            return when {
                text in listOf("工资", "薪资", "salary") -> CategoryKey.SALARY
                text in listOf("退款", "refund") -> CategoryKey.REFUND
                text in listOf("红包", "red packet") -> CategoryKey.RED_PACKET
                text in listOf("理财", "finance", "interest") -> CategoryKey.FINANCE
                text in listOf("收款", "receipt", "income") -> CategoryKey.RECEIPT
                text in listOf("其他", "other") -> CategoryKey.OTHER
                else -> null
            }
        }
        return when {
            text in listOf("餐饮", "food", "restaurant") -> CategoryKey.FOOD
            text in listOf("购物", "shopping") -> CategoryKey.SHOPPING
            text in listOf("订阅", "subscription", "ai subscription") -> CategoryKey.SUBSCRIPTION
            text in listOf("消费", "spending", "consumption") -> CategoryKey.CONSUMPTION
            text in listOf("转账", "transfer") -> CategoryKey.TRANSFER
            text in listOf("交通", "transport") -> CategoryKey.TRANSPORT
            text in listOf("住房", "housing", "rent") -> CategoryKey.HOUSING
            text in listOf("保险", "insurance") -> CategoryKey.INSURANCE
            text in listOf("通讯", "communication", "telecom") -> CategoryKey.COMMUNICATION
            text in listOf("娱乐", "entertainment") -> CategoryKey.ENTERTAINMENT
            text in listOf("医疗", "medical") -> CategoryKey.MEDICAL
            text in listOf("教育", "education") -> CategoryKey.EDUCATION
            text in listOf("红包", "red packet") -> CategoryKey.RED_PACKET
            text in listOf("旅行", "travel") -> CategoryKey.TRAVEL
            text in listOf("投资", "investment") -> CategoryKey.INVESTMENT
            text in listOf("其他", "other") -> CategoryKey.OTHER
            else -> null
        }
    }

    private fun extractTimestamp(raw: String): Long? {
        val match = Regex("""(20\d{2}[-/.年]\d{1,2}[-/.月]\d{1,2})\s*(?:日)?\s*(\d{1,2}:\d{2}(?::\d{2})?)?""").find(raw) ?: return null
        val datePart = match.groupValues[1]
            .replace("年", "-")
            .replace("月", "-")
            .replace("日", "")
            .replace("/", "-")
            .replace(".", "-")
        val timePart = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "00:00"
        val normalized = "$datePart $timePart"
        val format = if (timePart.count { it == ':' } == 2) "yyyy-M-d H:mm:ss" else "yyyy-M-d H:mm"
        return runCatching {
            SimpleDateFormat(format, Locale.US).apply {
                timeZone = TimeZone.getDefault()
                isLenient = false
            }.parse(normalized)?.time
        }.getOrNull()
    }

    private fun score(raw: String, source: String, merchant: String, direction: String): Double {
        var score = 0.48
        if (source != SourceKey.GENERIC) score += 0.15
        if (merchant.isNotBlank()) score += 0.12
        if (direction == Direction.EXPENSE || direction == Direction.INCOME) score += 0.10
        if (extractTimestamp(raw) != null) score += 0.08
        if (raw.length in 8..180) score += 0.05
        return score.coerceAtMost(0.98)
    }

    private fun fingerprint(source: String, amountCents: Long, direction: String, merchant: String, transactionTime: Long): String {
        // 1 分钟桶：既能为"同一通知被通知监听+无障碍双通道捕获"去重，
        // 又尽量减少短时间内同商户同金额的连续真实支付被误吞。
        val bucket = transactionTime / (60 * 1000L)
        val input = listOf(source, direction, amountCents.toString(), merchant.normalizedForHash(), bucket.toString()).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }

    private fun isIncomeDirection(raw: String): Boolean =
        raw.contains("收入") || raw.equals("income", true) || raw.equals("in", true)

    private fun isExpenseDirection(raw: String): Boolean =
        raw.contains("支出") || raw.equals("expense", true) || raw.equals("out", true)

    private fun String.cleanupMerchant(): String =
        replace(Regex("""[，,。:：\s]+$"""), "").trim().take(40)

    private fun String.normalizedForHash(): String =
        lowercase(Locale.ROOT).replace(Regex("""\s+"""), "").replace(Regex("""[¥￥,，。:：]"""), "")
}
