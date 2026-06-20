package com.github.bookkeeping.recognition

import org.junit.Assert.assertTrue
import org.junit.Test

class PiiMaskerTest {
    @Test
    fun masksPhoneAndIdCardButKeepsAmountAndMerchant() {
        val raw = "微信支付向示例咖啡支付¥36.00，联系手机13912345678，证件110101199003071234"
        val masked = PiiMasker.mask(raw)
        assertTrue("金额保留", masked.contains("¥36.00"))
        assertTrue("商户保留", masked.contains("示例咖啡"))
        assertTrue("手机号脱敏", !masked.contains("13912345678"))
        assertTrue("身份证脱敏", !masked.contains("110101199003071234"))
    }

    @Test
    fun masksBankCardNumber() {
        val raw = "尾号 6225 7560 1234 5678 消费42.10元"
        val masked = PiiMasker.mask(raw)
        assertTrue("金额保留", masked.contains("42.10元"))
        assertTrue("卡号脱敏", !masked.contains("6225 7560 1234 5678"))
    }

    @Test
    fun keepsShortAmountsIntact() {
        val raw = "支付¥150.00元，订单号 20260608"
        val masked = PiiMasker.mask(raw)
        assertTrue(masked.contains("150.00"))
    }
}
