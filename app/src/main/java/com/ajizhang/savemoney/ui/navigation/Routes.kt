package com.ajizhang.savemoney.ui.navigation

object Routes {
    const val HOME: String = "home"
    const val BUDGET: String = "budget"
    const val TREND: String = "trend"
    const val ARG_TRANSACTION_ID: String = "transactionId"
    const val NEW_TRANSACTION_ID: Long = -1L
    const val TRANSACTION_EDITOR: String = "transaction_editor"
    const val TRANSACTION_EDITOR_ROUTE: String =
        "$TRANSACTION_EDITOR?$ARG_TRANSACTION_ID={$ARG_TRANSACTION_ID}"

    fun transactionEditor(transactionId: Long = NEW_TRANSACTION_ID): String =
        "$TRANSACTION_EDITOR?$ARG_TRANSACTION_ID=$transactionId"
}
