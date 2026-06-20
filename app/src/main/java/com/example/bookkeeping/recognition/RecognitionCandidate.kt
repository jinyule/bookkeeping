package com.example.bookkeeping.recognition

import com.example.bookkeeping.Direction
import com.example.bookkeeping.ReviewStatus

data class RecognitionCandidate(
    val amountCents: Long,
    val direction: String,
    val categoryKey: String,
    val merchant: String,
    val account: String,
    val note: String,
    val transactionTime: Long,
    val sourceChannel: String,
    val rawText: String,
    val confidence: Double,
    val reviewStatus: String,
    val fingerprint: String
) {
    val isExpense: Boolean get() = direction == Direction.EXPENSE
    val needsReview: Boolean get() = reviewStatus == ReviewStatus.REVIEW
}

data class ImportResult(
    val imported: Int,
    val skipped: Int
)
