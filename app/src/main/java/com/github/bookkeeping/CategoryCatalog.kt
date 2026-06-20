package com.github.bookkeeping

import androidx.annotation.StringRes

data class CategoryDef(
    val key: String,
    val direction: String,
    @param:StringRes val labelRes: Int,
    val labelResName: String,
    val order: Int
)

object CategoryCatalog {
    val expense = listOf(
        CategoryDef(CategoryKey.FOOD, Direction.EXPENSE, R.string.category_food, "category_food", 0),
        CategoryDef(CategoryKey.SHOPPING, Direction.EXPENSE, R.string.category_shopping, "category_shopping", 1),
        CategoryDef(CategoryKey.SUBSCRIPTION, Direction.EXPENSE, R.string.category_subscription, "category_subscription", 2),
        CategoryDef(CategoryKey.CONSUMPTION, Direction.EXPENSE, R.string.category_consumption, "category_consumption", 3),
        CategoryDef(CategoryKey.TRANSFER, Direction.EXPENSE, R.string.category_transfer, "category_transfer", 4),
        CategoryDef(CategoryKey.TRANSPORT, Direction.EXPENSE, R.string.category_transport, "category_transport", 5),
        CategoryDef(CategoryKey.HOUSING, Direction.EXPENSE, R.string.category_housing, "category_housing", 6),
        CategoryDef(CategoryKey.INSURANCE, Direction.EXPENSE, R.string.category_insurance, "category_insurance", 7),
        CategoryDef(CategoryKey.COMMUNICATION, Direction.EXPENSE, R.string.category_communication, "category_communication", 8),
        CategoryDef(CategoryKey.ENTERTAINMENT, Direction.EXPENSE, R.string.category_entertainment, "category_entertainment", 9),
        CategoryDef(CategoryKey.MEDICAL, Direction.EXPENSE, R.string.category_medical, "category_medical", 10),
        CategoryDef(CategoryKey.EDUCATION, Direction.EXPENSE, R.string.category_education, "category_education", 11),
        CategoryDef(CategoryKey.RED_PACKET, Direction.EXPENSE, R.string.category_red_packet, "category_red_packet", 12),
        CategoryDef(CategoryKey.TRAVEL, Direction.EXPENSE, R.string.category_travel, "category_travel", 13),
        CategoryDef(CategoryKey.INVESTMENT, Direction.EXPENSE, R.string.category_investment, "category_investment", 14),
        CategoryDef(CategoryKey.OTHER, Direction.EXPENSE, R.string.category_other, "category_other", 15),
    )

    val income = listOf(
        CategoryDef(CategoryKey.TRANSFER, Direction.INCOME, R.string.category_transfer, "category_transfer", 0),
        CategoryDef(CategoryKey.SALARY, Direction.INCOME, R.string.category_salary, "category_salary", 1),
        CategoryDef(CategoryKey.FINANCE, Direction.INCOME, R.string.category_finance, "category_finance", 2),
        CategoryDef(CategoryKey.RED_PACKET, Direction.INCOME, R.string.category_red_packet, "category_red_packet", 3),
        CategoryDef(CategoryKey.BORROW_IN, Direction.INCOME, R.string.category_borrow_in, "category_borrow_in", 4),
        CategoryDef(CategoryKey.RECEIPT, Direction.INCOME, R.string.category_receipt, "category_receipt", 5),
        CategoryDef(CategoryKey.REFUND, Direction.INCOME, R.string.category_refund, "category_refund", 6),
        CategoryDef(CategoryKey.OTHER, Direction.INCOME, R.string.category_other, "category_other", 7),
    )

    val all: List<CategoryDef> = expense + income

    fun forDirection(direction: String): List<CategoryDef> =
        if (direction == Direction.INCOME) income else expense

    @StringRes
    fun labelRes(key: String, direction: String = Direction.EXPENSE): Int {
        return all.firstOrNull { it.key == key && it.direction == direction }?.labelRes
            ?: all.firstOrNull { it.key == key }?.labelRes
            ?: R.string.category_other
    }
}
