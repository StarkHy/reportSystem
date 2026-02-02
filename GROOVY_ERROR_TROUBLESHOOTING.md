# Groovy 脚本错误排查指南

本文档帮助您快速排查和解决 Groovy 脚本中的常见错误。

## 常见错误类型

### 1. 语法错误

#### 错误：Unexpected input

**症状**：编译失败，提示意外的输入

**原因**：
- 复制脚本时包含了分隔线（如 `===`、`---`）
- 字符串引号不匹配
- 特殊字符未转义

**解决方法**：
```groovy
// 错误：包含分隔线
// ====================================
data.put("name", "张三")
// ====================================

// 正确：移除所有分隔线和注释（除必要的外）
data.put("name", "张三")
```

**复制脚本时注意事项**：
- 只复制 `// ===` 之后到下一个分隔符之前的实际代码
- 不要复制分隔线本身
- 不要复制注释块（除非是代码内的注释）

#### 错误：Unclosed string literal

**症状**：提示字符串未闭合

**原因**：字符串引号不匹配

**解决方法**：
```groovy
// 错误
data.put("name", "张三)

// 正确
data.put("name", "张三")

// 错误：混合使用不同引号
data.put('name", "张三")

// 正确：使用相同引号
data.put("name", "张三")
// 或
data.put('name', '张三')
```

### 2. 类找不到错误

#### 错误：unable to resolve class

**症状**：提示找不到某个类

**原因**：类名拼写错误或使用了不存在的类

**poi-tl 1.12.1 正确的类名**：
```groovy
// 正确
import com.deepoove.poi.data.style.Style
import com.deepoove.poi.data.TextRenderData
import com.deepoove.poi.data.Pictures

// 错误：这些类在 1.12.1 中不存在
import com.deepoove.poi.data.style.TextStyle
import com.deepoove.poi.data.style.ParagraphStyle
import com.deepoove.poi.data.style.TableStyle
import com.deepoove.poi.data.style.BorderStyle
import com.deepoove.poi.data.style.PictureStyle
```

**解决方法**：
使用 poi-tl 1.12.1 的正确 API：
```groovy
// 正确的样式设置方式
import com.deepoove.poi.data.style.Style

def style = new Style()
style.setBold(true)
style.setFontSize(14)
style.setColor("FF0000")
```

### 3. 空指针错误

#### 错误：NullPointerException

**症状**：脚本执行失败，提示空指针异常

**原因**：访问了 null 对象的属性或方法

**解决方法**：
```groovy
// 错误：未检查就使用
data.put("name", info.get("name"))

// 正确：先检查
def info = data.get("info")
if (info != null) {
    data.put("name", info.get("name"))
}

// 或使用安全导航操作符
data.put("name", info?.get("name"))

// 提供默认值
data.put("name", info?.get("name") ?: "未知")
```

### 4. 类型转换错误

#### 错误：ClassCastException

**症状**：类型转换失败

**原因**：尝试将一个类型强制转换为不兼容的类型

**解决方法**：
```groovy
// 错误：直接转换
def score = (Integer) data.get("score")

// 正确：先检查类型
def scoreValue = data.get("score")
if (scoreValue instanceof Integer) {
    score = scoreValue
} else if (scoreValue instanceof String) {
    score = Integer.parseInt(scoreValue)
}

// 或使用 toInteger()
def score = data.get("score")?.toString()?.toInteger()
```

### 5. 数据不显示

#### 症状：模板占位符显示为空白

**可能原因**：
1. 变量名拼写错误
2. 大小写不匹配
3. 数据未正确设置

**排查步骤**：
```groovy
// 添加日志检查数据
log.info("当前数据: ${data}")

// 检查变量是否设置
log.info("name: ${data.get('name')}")

// 检查变量类型
log.info("name类型: ${data.get('name')?.getClass()}")
```

### 6. 表格不渲染

#### 症状：表格显示为空白

**可能原因**：
1. 标签位置错误
2. 数据格式不正确
3. 列名不匹配

**解决方法**：
```groovy
// 确保数据是 List 类型
def items = [
    [name: "A", score: 90],
    [name: "B", score: 85]
]

log.info("items类型: ${items.getClass()}")
log.info("items内容: ${items}")

data.put("items", items)

// 确保列名匹配
// 模板中的列名必须与对象的属性名一致
// {{name}} 对应 item.name
```

### 7. API 调用失败

#### 症状：提示 API 调用失败

**可能原因**：
1. API URL 配置错误
2. API 返回数据格式不正确
3. 网络问题

**解决方法**：
```groovy
// 检查 API 数据
log.info("API返回数据: ${params}")

// 检查特定字段
def employee = params.get("employee")
if (employee != null) {
    log.info("员工数据: ${employee}")
    data.putAll(employee)
} else {
    log.warn("API返回的员工数据为空")
    // 设置默认值
    data.put("name", "未知")
}
```

## 调试技巧

### 1. 逐步执行

```groovy
// 步骤1：测试基本数据
data.put("name", "张三")
log.info("步骤1完成")

// 步骤2：添加复杂逻辑
def score = 85
if (score >= 90) {
    data.put("level", "优秀")
}
log.info("步骤2完成")

// 步骤3：添加样式
// ...
log.info("步骤3完成")
```

### 2. 使用日志输出

```groovy
// 输出变量值
log.info("name = ${data.get('name')}")

// 输出整个数据结构
log.info("data = ${data}")

// 输出类型信息
log.info("score type = ${data.get('score').getClass()}")

// 输出异常堆栈
try {
    // 可能出错的操作
} catch (Exception e) {
    log.error("操作失败: ${e.getMessage()}", e)
}
```

### 3. 验证假设

```groovy
// 验证数据是否为 List
def items = data.get("items")
if (items instanceof List) {
    log.info("items是List类型，大小: ${items.size()}")
} else {
    log.warn("items不是List类型，类型: ${items?.getClass()}")
}
```

## 常用代码片段

### 安全获取数据

```groovy
// 使用安全导航和 Elvis 运算符
def name = data.get("name")?.toString() ?: "未知"
def score = data.get("score")?.toString()?.toInteger() ?: 0
```

### 数据验证

```groovy
// 验证必填字段
def required = ["name", "department", "score"]
def missing = required.findAll { !data.containsKey(it) }

if (missing) {
    log.warn("缺少必填字段: ${missing}")
    missing.each { field ->
        data.put(field, "N/A")
    }
}
```

### 列表处理

```groovy
// 空列表处理
def items = data.get("items") ?: []
if (items.isEmpty()) {
    log.warn("items为空，使用默认值")
    items = [[name: "默认", score: 0]]
}
data.put("items", items)
```

## 获取帮助

如果以上方法都无法解决问题：

1. 查看完整的错误堆栈信息
2. 记录重现问题的步骤
3. 提供脚本代码和错误日志
4. 检查 poi-tl 和 Groovy 的官方文档

## 版本兼容性

- poi-tl: 1.12.1
- Groovy: 3.0.9
- Java: 17

注意：不同版本的 API 可能有差异，确保使用正确的类名和方法。
