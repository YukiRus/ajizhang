# Save Money

中文 | English

## 中文简介

`Save Money` 是一个使用 Kotlin 开发的原生 Android 攒钱记账应用，面向个人日常收支、储蓄目标和投资记录管理。

当前项目主要特性：

- 以攒钱目标为核心，展示目标金额、已攒金额、还差金额和预计完成时间
- 投资与存款分开展示，其中投资计入总已攒金额
- 收支明细支持按月、季度、年份分组查看
- 支持退款记录，支出可标记退款金额并参与净支出统计
- 分类可由用户自行增删，并提供默认分类
- 数据本地持久化，基于 Room / SQLite
- 提供兼容 OpenAI 接口的大模型设置，可在新增支出时通过语音识别辅助填写表单

技术栈：

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Room / SQLite
- DataStore

## English

`Save Money` is a native Android savings and expense tracking app built with Kotlin. It is designed for personal finance tracking around saving goals, investments, deposits, and daily income/expense records.

Current features include:

- Goal-first dashboard showing target amount, saved amount, remaining amount, and expected completion time
- Separate investment and deposit sections, with investment included in total saved amount
- Transaction list grouped by month, quarter, or year
- Refund support for expenses, with refunded amount tracked in net spending
- User-manageable categories with built-in defaults
- Local persistence powered by Room / SQLite
- OpenAI-compatible LLM settings for voice-assisted expense entry on new expense records

Tech stack:

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Room / SQLite
- DataStore

## License

This project is licensed under the WTFPL.

