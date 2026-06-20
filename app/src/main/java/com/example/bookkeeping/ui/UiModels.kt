package com.example.bookkeeping.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.annotation.StringRes
import com.example.bookkeeping.CategoryKey
import com.example.bookkeeping.Direction
import com.example.bookkeeping.data.BillEntity

internal data class BillDraft(
    val existingId: Long? = null,
    val direction: String = Direction.EXPENSE,
    val amount: String = "",
    val categoryKey: String = CategoryKey.FOOD,
    val merchant: String = "",
    val account: String = "",
    val note: String = "",
    val transactionTime: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * 让 BillDraft 可被 rememberSaveable 保存，跨配置变化（如语言切换 recreate）
         * 不丢失正在录入的草稿。
         */
        val Saver: Saver<BillDraft?, Any> = listSaver(
            save = { draft ->
                draft?.let {
                    listOf(
                        it.existingId ?: -1L,
                        it.direction,
                        it.amount,
                        it.categoryKey,
                        it.merchant,
                        it.account,
                        it.note,
                        it.transactionTime
                    )
                } ?: emptyList()
            },
            restore = { values ->
                if (values.isEmpty()) null
                else BillDraft(
                    existingId = (values[0] as Long).takeIf { it >= 0 },
                    direction = values[1] as String,
                    amount = values[2] as String,
                    categoryKey = values[3] as String,
                    merchant = values[4] as String,
                    account = values[5] as String,
                    note = values[6] as String,
                    transactionTime = values[7] as Long
                )
            }
        )
    }
}

internal data class BillGroup(
    val dateLabel: String,
    val expense: Long,
    val income: Long,
    val items: List<BillEntity>
)

internal data class SourceUi(
    val key: String,
    @param:StringRes val title: Int,
    @param:StringRes val desc: Int
)
