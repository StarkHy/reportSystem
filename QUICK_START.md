# Groovy 脚本测试指南

本文档提供 Groovy 脚本在报告生成系统中的使用指南。

## 快速开始

### 1. 最简单的测试

复制 `simple_test.txt` 中的脚本到 Groovy 脚本输入框，点击生成即可。

### 2. 测试脚本

`test_groovy_scripts.txt` 包含 7 个不同场景的测试脚本：
- 基本文本替换
- 数字格式化
- 日期处理
- 表格数据处理
- 条件渲染
- 列表数据处理
- 复合数据处理

### 3. 带样式的脚本

`test_groovy_with_style.txt` 包含使用 poi-tl 1.12.1 样式 API 的示例：
- 带样式的文本
- 表格样式处理
- 图片处理
- 列表样式
- 段落样式
- 综合样式应用

## poi-tl 1.12.1 API

### 样式设置

```groovy
import com.deepoove.poi.data.style.Style
import com.deepoove.poi.data.TextRenderData

// 创建样式
def style = new Style()
style.setBold(true)
style.setFontSize(14)
style.setColor("FF0000")
style.setFontFamily("宋体")

// 应用样式到文本
def text = new TextRenderData("重要通知", style)
data.put("title", text)
```

### 常用样式属性

- `setBold(true)` - 加粗
- `setItalic(true)` - 斜体
- `setUnderline(true)` - 下划线
- `setFontSize(14)` - 字体大小
- `setColor("FF0000")` - 颜色（十六进制）
- `setFontFamily("宋体")` - 字体

### 图片处理

```groovy
import com.deepoove.poi.data.Pictures

// 本地图片
def picture = Pictures.ofLocal("/path/to/image.png").size(150, 80).build()
data.put("signature", picture)
```

## 内置变量

### data
数据映射对象，所有要填充到模板的数据都通过它设置。

```groovy
data.put("name", "张三")
data.put("age", 30)
```

### params
参数映射对象，包含外部传入的参数。

```groovy
def param = params.get("someParam")
```

### log
日志记录器，用于记录脚本执行日志。

```groovy
log.info("信息日志")
log.warn("警告日志")
log.error("错误日志")
log.debug("调试日志")
```

## 常用操作

### 字符串操作

```groovy
// 拼接
def fullName = firstName + " " + lastName

// 格式化
def formatted = String.format("¥%,.2f", amount)

// 替换
def replaced = text.replace("old", "new")
```

### 数字操作

```groovy
// 四舍五入
def rounded = value.round(2)

// 求和
def total = list.sum { it.value }

// 平均值
def avg = list.sum { it.value } / list.size()
```

### 日期操作

```groovy
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 当前时间
def now = LocalDateTime.now()

// 格式化
def formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

// 解析
def date = LocalDateTime.parse("2024-01-15", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
```

### 集合操作

```groovy
// 遍历
items.each { item ->
    println item.name
}

// 过滤
def filtered = items.findAll { it.score > 80 }

// 排序
def sorted = items.sort { it.score }

// 分组
def grouped = items.groupBy { it.department }
```

### 条件判断

```groovy
// if-else
if (score >= 90) {
    data.put("level", "优秀")
} else if (score >= 80) {
    data.put("level", "良好")
} else {
    data.put("level", "合格")
}

// 三元运算
def level = score >= 90 ? "优秀" : "良好"
```

## 错误处理

```groovy
try {
    // 可能出错的代码
    def result = someOperation()
} catch (Exception e) {
    log.error("操作失败: " + e.getMessage())
    // 设置默认值
    data.put("result", "N/A")
}
```

## 调试技巧

1. 使用日志输出查看中间结果
   ```groovy
   log.info("当前数据: ${data}")
   log.info("计算结果: ${result}")
   ```

2. 验证数据类型
   ```groovy
   log.info("score类型: ${score.getClass().getName()}")
   ```

3. 测试逐步执行
   - 先测试基本数据填充
   - 再添加逻辑处理
   - 最后添加样式和复杂逻辑

## 测试数据

`test_data.json` 提供了完整的员工考核数据，可以用来测试各种场景。

## 模板创建

参考 `test_template_instructions.txt` 了解如何创建 Word 模板。

## 注意事项

1. 变量名区分大小写
2. Groovy 是动态类型语言，注意数据类型转换
3. 使用日志记录关键步骤，方便调试
4. 复杂逻辑建议分步实现并测试
5. 样式在 Word 模板中设置更简单，脚本中主要用于动态样式
