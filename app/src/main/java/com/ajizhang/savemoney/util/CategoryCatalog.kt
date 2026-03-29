package com.ajizhang.savemoney.util

object CategoryCatalog {
    // 这里只保留首启时要灌入数据库的默认分类，后续由用户自己维护。
    val incomeDefaults: List<String> = listOf("工资", "奖金", "兼职", "红包", "其他收入")
    val expenseDefaults: List<String> = listOf("餐饮", "交通", "购物", "住房", "娱乐", "医疗", "其他支出")
}
