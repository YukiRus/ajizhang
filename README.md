- 全部AI写的，含人量为0
- 感受威胁
- 总之很适合我自己
- 因为含人量为0%，所以许可证是你他妈想干嘛就干嘛许可证。
- 感谢看我的废话。
- 以上是人写的
- 就这样
- 以下是ai写的
- 另外，用的Codex，重置额度真爽

# 攒钱记账

一个使用 Kotlin 和 Jetpack Compose 编写的原生 Android 个人记账应用。项目围绕“攒钱目标”组织日常收支、存款、投资、预算和趋势统计，并支持通过兼容 OpenAI Chat Completions 的大模型接口辅助录入支出。

## 功能概览

- 攒钱目标：设置目标名称、目标金额和预计完成日期，自动计算已攒金额、剩余金额和建议月攒金额。
- 收支记录：记录收入和支出，支持金额、分类、日期、备注、退款金额和编辑删除。
- 分类管理：收入和支出分类可在录入页维护，并提供默认分类初始化。
- 月度预算：设置每月总预算，添加子预算，查看子预算已用、剩余和关联支出明细。
- 预算复制：可将当前月份的子预算结构复制到其他月份。
- 趋势分析：按月、季度、年份查看收入、支出、净结余折线趋势，并支持排除指定支出分类。
- 投资余额：单独维护投资金额，投资和存款共同计入已攒金额。
- 智能录入：支持按住说话识别支出，也支持从图片、小票、账单截图中识别多笔支出。
- 本地持久化：核心数据存储在 Room / SQLite，模型设置存储在 DataStore。

## 技术栈

- Kotlin 2.0
- Android Gradle Plugin 8.7
- Jetpack Compose + Material 3
- Navigation Compose
- Hilt
- Room / SQLite
- DataStore Preferences
- Kotlin Coroutines / Flow
- JUnit4 + Truth

## 项目结构

```text
app/src/main/java/com/ajizhang/savemoney/
├── data/
│   ├── local/          # Room 数据库、DAO、Entity、类型转换
│   ├── model/          # UI 与仓储层使用的数据模型
│   ├── remote/         # 大模型语音/图片识别请求与响应解析
│   ├── repository/     # 目标、交易、预算、分类、投资、设置仓储
│   └── voice/          # WAV 录音封装
├── di/                 # Hilt 依赖注入模块
├── domain/             # 汇总、趋势、目标计划计算逻辑
├── ui/
│   ├── budget/         # 月度预算与子预算页面
│   ├── editor/         # 收支录入与编辑页面
│   ├── home/           # 首页、目标、余额、交易列表、设置弹窗
│   ├── navigation/     # 路由定义
│   ├── settings/       # 大模型设置状态
│   ├── theme/          # Compose 主题
│   └── trend/          # 收支趋势页面
└── util/               # 金额、日期、分类等工具
```

## 运行要求

- Android Studio Ladybug 或更新版本
- JDK 17
- Android SDK 35
- 最低运行系统：Android 8.0，API 26

## 构建与测试

在项目根目录执行：

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

如果在类 Unix 环境中运行：

```bash
./gradlew test
./gradlew assembleDebug
```

## 大模型识别配置

应用内打开“设置”后填写：

- API 地址：兼容 Chat Completions 的服务地址，例如 `https://api.example.com/v1`
- API Key：Bearer token
- 模型名称：服务端支持的模型 ID

代码会自动把 API 地址补全到 `/chat/completions`。语音识别会发送 WAV 音频；图片识别会发送 base64 data URL。接口需要支持 `input_audio` 或 `image_url` 这类多模态消息内容。

## 数据说明

- 交易金额以“分”为内部单位保存，界面按元展示。
- 退款金额只用于支出记录，统计时按 `支出金额 - 退款金额` 计算净支出。
- 子预算通过 `subBudgetId` 与支出记录关联，删除子预算时会解除已有交易关联。
- Room 数据库当前版本为 5，包含目标、投资、交易、分类、月度预算和子预算表。

## 权限

- `INTERNET`：调用大模型接口。
- `RECORD_AUDIO`：录制语音支出。

## License

This project is licensed under the WTFPL.
