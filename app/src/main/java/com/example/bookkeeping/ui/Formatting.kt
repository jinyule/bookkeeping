package com.example.bookkeeping.ui

import com.example.bookkeeping.Direction
import com.example.bookkeeping.data.BillEntity
import android.text.format.DateFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun amountText(cents: Long): String = String.format(Locale.US, "%.2f", cents / 100.0)

internal fun parseAmountCents(text: String): Long? {
    val normalized = text.trim().replace(",", "")
    if (normalized.isBlank()) return null
    return runCatching {
        BigDecimal(normalized)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()
}

internal fun normalizeAmountInput(text: String): String {
    val cleaned = text.filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    return if (dot >= 0) cleaned.take(dot + 3) else cleaned.trimStart('0').ifBlank { "0" }
}

internal fun groupedBills(bills: List<BillEntity>): List<BillGroup> {
    return bills
        .groupBy { startOfDay(it.transactionTime) }
        .entries
        .sortedByDescending { it.key }
        .map { (dayStart, items) ->
            BillGroup(
                dateLabel = dayLabel(dayStart),
                expense = items.filter { it.direction == Direction.EXPENSE }.sumOf { it.amountCents },
                income = items.filter { it.direction == Direction.INCOME }.sumOf { it.amountCents },
                items = items.sortedWith(compareByDescending<BillEntity> { it.transactionTime }.thenByDescending { it.id })
            )
        }
}

internal fun timeText(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(millis)

internal fun dateShortText(millis: Long): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMMd")
    return SimpleDateFormat(pattern, locale).format(millis)
}

internal fun dateTimeText(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(millis)

internal fun isToday(millis: Long): Boolean = sameDay(Calendar.getInstance(), millis)

internal fun isThisMonth(millis: Long): Boolean {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.MONTH) == then.get(Calendar.MONTH)
}

internal fun isThisYear(millis: Long): Boolean {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    return now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
}

/** 当前自然周（按周一为一周起始）内。 */
internal fun isThisWeek(millis: Long): Boolean {
    val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    // 周一作为一周第一天
    val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    val offset = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    val weekStart = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -offset) }
    val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
    return millis >= weekStart.timeInMillis && millis < weekEnd.timeInMillis
}

internal fun currentDayOfMonth(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

internal fun dailyExpenseValues(bills: List<BillEntity>): List<Long> {
    val days = (1..currentDayOfMonth()).associateWith { 0L }.toMutableMap()
    val now = Calendar.getInstance()
    bills.filter { isThisMonth(it.transactionTime) }.forEach {
        val day = Calendar.getInstance().apply { timeInMillis = it.transactionTime }.get(Calendar.DAY_OF_MONTH)
        days[day] = (days[day] ?: 0L) + it.amountCents
    }
    return days.values.toList()
}

private fun sameDay(now: Calendar, millis: Long): Boolean {
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
}

private fun startOfDay(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun dayLabel(dayStartMillis: Long): String {
    val locale = Locale.getDefault()
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = dayStartMillis }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    // 用系统最佳模式，避免硬编码中文格式在日韩等 locale 下出现中文。
    val skeleton = if (sameYear) "MMMdE" else "yyyyMMMdE"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(dayStartMillis)
}
