package com.github.bookkeeping.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.bookkeeping.R
import com.github.bookkeeping.SourceKey

@Composable
internal fun sourceName(source: String): String = stringResource(sourceNameRes(source))

@Composable
internal fun sourceNameOrNull(source: String): String? = if (isSourceKey(source)) sourceName(source) else null

@StringRes
internal fun sourceNameRes(source: String): Int = when (source) {
    SourceKey.WECHAT -> R.string.source_wechat
    SourceKey.ALIPAY -> R.string.source_alipay
    SourceKey.UNIONPAY -> R.string.source_unionpay
    SourceKey.VIVO_WALLET -> R.string.source_vivo_wallet
    SourceKey.BANK -> R.string.source_bank
    SourceKey.IMPORT -> R.string.source_import
    SourceKey.GENERIC -> R.string.source_generic
    else -> R.string.source_generic
}

internal fun isSourceKey(value: String): Boolean =
    value in setOf(
        SourceKey.WECHAT,
        SourceKey.ALIPAY,
        SourceKey.UNIONPAY,
        SourceKey.VIVO_WALLET,
        SourceKey.BANK,
        SourceKey.GENERIC,
        SourceKey.IMPORT
    )

internal fun sourceItems() = listOf(
    SourceUi(SourceKey.WECHAT, R.string.source_wechat, R.string.source_desc_wechat),
    SourceUi(SourceKey.ALIPAY, R.string.source_alipay, R.string.source_desc_alipay),
    SourceUi(SourceKey.UNIONPAY, R.string.source_unionpay, R.string.source_desc_unionpay),
    SourceUi(SourceKey.VIVO_WALLET, R.string.source_vivo_wallet, R.string.source_desc_vivo_wallet),
    SourceUi(SourceKey.BANK, R.string.source_bank, R.string.source_desc_bank),
    SourceUi(SourceKey.GENERIC, R.string.source_generic, R.string.source_desc_generic),
    SourceUi(SourceKey.IMPORT, R.string.source_import, R.string.source_desc_import)
)
