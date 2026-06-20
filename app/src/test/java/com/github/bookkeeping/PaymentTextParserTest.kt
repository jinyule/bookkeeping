package com.github.bookkeeping

import com.github.bookkeeping.recognition.CsvBillImporter
import com.github.bookkeeping.recognition.PaymentTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentTextParserTest {
    @Test
    fun parsesChineseWeChatExpense() {
        val candidate = PaymentTextParser.parse(
            "com.tencent.mm",
            "微信支付",
            "微信支付凭证：你已向示例咖啡支付¥36.00，交易时间 2026-06-08 12:30"
        )
        assertNotNull(candidate)
        requireNotNull(candidate)
        assertEquals(SourceKey.WECHAT, candidate.sourceChannel)
        assertEquals(Direction.EXPENSE, candidate.direction)
        assertEquals(3600L, candidate.amountCents)
        assertEquals(CategoryKey.FOOD, candidate.categoryKey)
    }

    @Test
    fun parsesEnglishWeChatIncome() {
        val candidate = PaymentTextParser.parse(
            "com.tencent.mm",
            "WeChat Pay",
            "WeChat Pay received CNY 88.00 from Sample Client"
        )
        assertNotNull(candidate)
        requireNotNull(candidate)
        assertEquals(SourceKey.WECHAT, candidate.sourceChannel)
        assertEquals(Direction.INCOME, candidate.direction)
        assertEquals(8800L, candidate.amountCents)
    }

    @Test
    fun parsesChineseWeChatIncomeMerchant() {
        val candidate = PaymentTextParser.parse(
            "com.tencent.mm",
            "微信支付",
            "微信支付收款到账88.00元，来自示例客户"
        )
        assertNotNull(candidate)
        requireNotNull(candidate)
        assertEquals(Direction.INCOME, candidate.direction)
        assertEquals(8800L, candidate.amountCents)
        assertEquals("示例客户", candidate.merchant)
    }

    @Test
    fun ignoresWeChatMarketingCoupon() {
        val candidate = PaymentTextParser.parse(
            "com.tencent.mm",
            "微信支付",
            "微信支付优惠券到账，满20减5"
        )
        assertNull(candidate)
    }

    @Test
    fun parsesBankStyleNotificationWithoutSmsPermission() {
        val candidate = PaymentTextParser.parse(
            "com.example.bank",
            "银行交易提醒",
            "您尾号1234储蓄卡消费人民币42.10元，商户：示例便利店"
        )
        assertNotNull(candidate)
        requireNotNull(candidate)
        assertEquals(SourceKey.BANK, candidate.sourceChannel)
        assertEquals(Direction.EXPENSE, candidate.direction)
        assertEquals(4210L, candidate.amountCents)
    }

    @Test
    fun importsWeChatCsvRows() {
        val rows = CsvBillImporter.parseRows(
            """
            时间,方向,金额,商户,备注,分类,账户
            2026-06-08 12:30,支出,36.00,微信支付 示例咖啡,拿铁,餐饮,零钱
            2026-06-08 14:00,收入,88.00,微信支付 示例客户,收款,收款,零钱
            """.trimIndent()
        )
        assertEquals(2, rows.size)
        assertEquals(SourceKey.WECHAT, rows.first().sourceChannel)
        assertEquals(3600L, rows.first().amountCents)
        assertEquals("拿铁", rows.first().note)
        assertEquals("零钱", rows.first().account)
        assertEquals(CategoryKey.FOOD, rows.first().categoryKey)
    }

    @Test
    fun notificationAndAccessibilityCandidatesShareFingerprint() {
        val notification = PaymentTextParser.parse(
            "com.tencent.mm",
            "微信支付",
            "微信支付凭证：你已向示例咖啡支付¥36.00，交易时间 2026-06-08 12:30"
        )
        val accessibility = PaymentTextParser.parseAccessibilityText(
            "com.tencent.mm",
            "微信支付凭证：你已向示例咖啡支付¥36.00，交易时间 2026-06-08 12:30"
        )

        assertNotNull(notification)
        assertNotNull(accessibility)
        requireNotNull(notification)
        requireNotNull(accessibility)
        assertEquals(notification.fingerprint, accessibility.fingerprint)
    }

    @Test
    fun accessibilityRejectsNonCompletionText() {
        // 记账界面/待支付页等无完成态关键词的文本不应被无障碍识别为交易，
        // 防止自身界面数字被反复记账。
        val self = PaymentTextParser.parseAccessibilityText(
            "com.github.bookkeeping",
            "支出 收入 ¥ 金额 36.00 商户 账户 备注 7 8 9 删除"
        )
        assertNull(self)

        val pending = PaymentTextParser.parseAccessibilityText(
            "com.tencent.mm",
            "返回 订单 交易剩余时间23小时59分58秒 支付金额 4.90 支付方式"
        )
        assertNull(pending)
    }

    @Test
    fun accessibilityRejectsSelfPackageEvenWithCompletionText() {
        val candidate = PaymentTextParser.parseAccessibilityText(
            "com.github.bookkeeping",
            "交易成功 支出 收入 ¥ 金额 36.00 商户 账户 备注 7 8 9 删除"
        )
        assertNull(candidate)
    }

    @Test
    fun accessibilityRejectsUnknownPackages() {
        val candidate = PaymentTextParser.parseAccessibilityText(
            "com.example.bank",
            "交易成功 支付¥36.00"
        )
        assertNull(candidate)
    }

    @Test
    fun accessibilityAcceptsCompletionText() {
        val ok = PaymentTextParser.parseAccessibilityText(
            "com.tencent.mm",
            "微信支付 已支付¥5.00"
        )
        assertNotNull(ok)
        requireNotNull(ok)
        assertEquals(500L, ok.amountCents)
    }

    @Test
    fun doesNotMistakeDateTimeDigitsAsAmount() {
        // 无明确金额关键词与货币符号时，不应把日期/时间里的数字解析为金额
        val candidate = PaymentTextParser.parse(
            "com.tencent.mm",
            "微信支付",
            "账单日期 2026-06-08 12:30 已生成"
        )
        assertNull(candidate)
    }

    @Test
    fun refundDirectionBeatsExpenseKeyword() {
        // "支付退款" 同时命中支付(支出)与退款(收入)，应为收入
        val candidate = PaymentTextParser.parse(
            "com.tencent.mm",
            "微信支付",
            "微信支付退款到账¥36.00"
        )
        assertNotNull(candidate)
        requireNotNull(candidate)
        assertEquals(Direction.INCOME, candidate.direction)
        assertEquals(CategoryKey.REFUND, candidate.categoryKey)
    }

    @Test
    fun rejectsInvalidCalendarDate() {
        // 越界日期在 lenient=false 下应无法解析为时间，但不影响金额识别
        val candidate = PaymentTextParser.parse(
            "com.tencent.mm",
            "微信支付",
            "微信支付向示例咖啡支付¥36.00，交易时间 2026-13-40 12:30"
        )
        assertNotNull(candidate)
        requireNotNull(candidate)
        assertEquals(3600L, candidate.amountCents)
    }

    @Test
    fun importsCsvWithBomAndCRLF() {
        val text = "﻿时间,方向,金额,商户,备注,分类,账户\r\n" +
            "2026-06-08 12:30,支出,36.00,示例咖啡,拿铁,餐饮,零钱\r\n"
        val rows = CsvBillImporter.parseRows(text)
        assertEquals(1, rows.size)
        assertEquals(3600L, rows.first().amountCents)
        assertEquals("拿铁", rows.first().note)
    }

    @Test
    fun consecutiveSameMerchantAmountProduceDistinctFingerprints() {
        // 1 分钟桶：相隔 2 分钟的两笔同商户同金额应有不同 fingerprint
        val base = 1_748_380_000_000L
        val a = PaymentTextParser.parse("com.tencent.mm", "微信支付", "微信支付向地铁支付¥4.00")
        val b = PaymentTextParser.parse("com.tencent.mm", "微信支付", "微信支付向地铁支付¥4.00")
        assertNotNull(a)
        assertNotNull(b)
        requireNotNull(a)
        requireNotNull(b)
        // 时间桶不同则指纹不同；这里用显式时间构造更稳
        val fa = PaymentTextParser.parse("com.tencent.mm", "微信支付", "微信支付向地铁支付¥4.00，交易时间 2026-06-08 12:30")
        val fb = PaymentTextParser.parse("com.tencent.mm", "微信支付", "微信支付向地铁支付¥4.00，交易时间 2026-06-08 12:32")
        requireNotNull(fa)
        requireNotNull(fb)
        assertNotEquals(fa.fingerprint, fb.fingerprint)
    }
}
