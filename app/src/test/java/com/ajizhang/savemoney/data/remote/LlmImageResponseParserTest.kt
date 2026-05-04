package com.ajizhang.savemoney.data.remote

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LlmImageResponseParserTest {

    @Test
    fun `parse single expense item`() {
        val json = """[{"amount":"12.50","category":"餐饮","note":"午饭","date":"2025-05-05","time":"12:30"}]"""
        val items = ImageExpenseParser.parseExpenseArray(json)
        assertThat(items).hasSize(1)
        assertThat(items[0].amountText).isEqualTo("12.50")
        assertThat(items[0].category).isEqualTo("餐饮")
        assertThat(items[0].note).isEqualTo("午饭")
        assertThat(items[0].dateText).isEqualTo("2025-05-05")
        assertThat(items[0].timeText).isEqualTo("12:30")
    }

    @Test
    fun `parse multiple expense items`() {
        val json = """[
            {"amount":"12.50","category":"餐饮","note":"午饭","date":"2025-05-05","time":"12:30"},
            {"amount":"35.00","category":"交通","note":"打车","date":"2025-05-04","time":"08:15"}
        ]"""
        val items = ImageExpenseParser.parseExpenseArray(json)
        assertThat(items).hasSize(2)
        assertThat(items[0].amountText).isEqualTo("12.50")
        assertThat(items[1].amountText).isEqualTo("35.00")
    }

    @Test
    fun `parse empty array`() {
        val items = ImageExpenseParser.parseExpenseArray("[]")
        assertThat(items).isEmpty()
    }

    @Test
    fun `parse empty string`() {
        val items = ImageExpenseParser.parseExpenseArray("")
        assertThat(items).isEmpty()
    }

    @Test
    fun `parse with missing optional fields`() {
        val json = """[{"amount":"12.50","category":"餐饮"}]"""
        val items = ImageExpenseParser.parseExpenseArray(json)
        assertThat(items).hasSize(1)
        assertThat(items[0].amountText).isEqualTo("12.50")
        assertThat(items[0].category).isEqualTo("餐饮")
        assertThat(items[0].note).isEqualTo("")
        assertThat(items[0].dateText).isEqualTo("")
        assertThat(items[0].timeText).isEqualTo("")
    }

    @Test
    fun `parse with empty amount`() {
        val json = """[{"amount":"","category":"餐饮","note":"午饭"}]"""
        val items = ImageExpenseParser.parseExpenseArray(json)
        assertThat(items).hasSize(1)
        assertThat(items[0].amountText).isEmpty()
    }

    @Test
    fun `strip markdown code fence`() {
        val text = "```json\n[{\"amount\":\"20.00\",\"category\":\"购物\"}]\n```"
            .replace("\\n", "\n")
        val stripped = ImageExpenseParser.stripCodeFence(text)
        val items = ImageExpenseParser.parseExpenseArray(stripped)
        assertThat(items).hasSize(1)
        assertThat(items[0].amountText).isEqualTo("20.00")
    }

    @Test
    fun `strip code fence without json prefix`() {
        val text = "```\n[{\"amount\":\"5.00\",\"category\":\"咖啡\"}]\n```"
            .replace("\\n", "\n")
        val stripped = ImageExpenseParser.stripCodeFence(text)
        val items = ImageExpenseParser.parseExpenseArray(stripped)
        assertThat(items).hasSize(1)
        assertThat(items[0].amountText).isEqualTo("5.00")
    }

    @Test
    fun `parse non-json text returns empty list`() {
        val items = ImageExpenseParser.parseExpenseArray("这是一段不是JSON的文本")
        assertThat(items).isEmpty()
    }

    @Test
    fun `parse malformed json returns empty items`() {
        val items = ImageExpenseParser.parseExpenseArray("[{amount: bad}")
        assertThat(items).hasSize(1)
        assertThat(items[0].amountText).isEmpty()
        assertThat(items[0].category).isEmpty()
    }
}
