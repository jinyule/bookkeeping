package com.example.bookkeeping.recognition

/**
 * 对原始识别文本做最小化脱敏后再落库，降低手机号、银行卡号、身份证号等
 * 敏感信息长期留存于本地数据库的风险。金额与商户等记账必要信息保留。
 */
internal object PiiMasker {
    // 用前后非数字断言替代 \b，避免中文环境下 \b 边界判定不可靠导致漏匹配。
    // 11 位手机号
    private val phone = Regex("""(?<![0-9])1[3-9]\d{9}(?![0-9])""")
    // 18 位身份证号（最后一位可为 X）
    private val idCard = Regex("""(?<![0-9])\d{17}[\dXx](?![0-9])""")
    // 长串数字（≥16 位连续数字，多为银行卡/流水号）
    private val longDigits = Regex("""(?<![0-9])\d{16,}(?![0-9])""")
    // 银行卡常见 16-19 位但分隔显示如 1234 5678 1234 5678，先处理这种带空格形式
    private val spacedCard = Regex("""(?<![0-9])\d{4} \d{4} \d{4} \d{4}(?: \d{1,4})?(?![0-9])""")

    fun mask(raw: String): String {
        var out = raw
        out = idCard.replace(out) { "${it.value.take(6)}********${it.value.takeLast(4)}" }
        out = phone.replace(out) { "${it.value.take(3)}****${it.value.takeLast(4)}" }
        out = spacedCard.replace(out) { "**** **** **** ${it.value.filter { c -> c.isDigit() }.takeLast(4)}" }
        out = longDigits.replace(out) { "****${it.value.takeLast(4)}" }
        return out
    }
}
