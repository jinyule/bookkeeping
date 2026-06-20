package com.github.bookkeeping

object Direction {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

object ReviewStatus {
    const val ACCEPTED = "ACCEPTED"
    const val REVIEW = "REVIEW"
}

object SourceKey {
    const val MANUAL = "MANUAL"
    const val WECHAT = "WECHAT"
    const val ALIPAY = "ALIPAY"
    const val UNIONPAY = "UNIONPAY"
    const val VIVO_WALLET = "VIVO_WALLET"
    const val BANK = "BANK"
    const val GENERIC = "GENERIC"
    const val IMPORT = "IMPORT"
    const val ACCESSIBILITY = "ACCESSIBILITY"
    const val NOTIFICATION = "NOTIFICATION"
}

/**
 * 无障碍服务仅识别这些支付应用的界面文本，避免抓取本应用自身或其它无关应用
 * 界面上的数字（否则会被误判为交易金额而反复写入垃圾记录）。
 * 银行通知走通知监听服务，不在此列。
 */
object AccessibilityPackages {
    const val SELF = "com.github.bookkeeping"

    val paymentPackages = setOf(
        "com.tencent.mm",               // 微信
        "com.eg.android.AlipayGphone",  // 支付宝
        "com.unionpay",                 // 云闪付
        "com.vivo.wallet"               // vivo 钱包
    )

    /** 是否为应当识别的支付应用包名。 */
    fun shouldProcess(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName == SELF) return false
        return paymentPackages.any { pkg -> packageName == pkg || packageName.startsWith("$pkg.") }
    }
}

object CategoryKey {
    const val FOOD = "food"
    const val SHOPPING = "shopping"
    const val SUBSCRIPTION = "subscription"
    const val CONSUMPTION = "consumption"
    const val TRANSFER = "transfer"
    const val TRANSPORT = "transport"
    const val HOUSING = "housing"
    const val INSURANCE = "insurance"
    const val COMMUNICATION = "communication"
    const val ENTERTAINMENT = "entertainment"
    const val MEDICAL = "medical"
    const val EDUCATION = "education"
    const val RED_PACKET = "red_packet"
    const val TRAVEL = "travel"
    const val INVESTMENT = "investment"
    const val OTHER = "other"
    const val SALARY = "salary"
    const val FINANCE = "finance"
    const val RECEIPT = "receipt"
    const val BORROW_IN = "borrow_in"
    const val REFUND = "refund"
}
