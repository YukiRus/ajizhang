package com.ajizhang.savemoney.di

import com.ajizhang.savemoney.data.remote.LlmExpenseRecognizer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LlmEntryPoint {
    fun llmExpenseRecognizer(): LlmExpenseRecognizer
}
