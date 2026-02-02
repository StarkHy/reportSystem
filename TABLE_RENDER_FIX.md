# 表格渲染错误修复指南

## 错误信息

```
java.lang.RuntimeException: 生成报告失败: Unable to render template {{#dimensions}}
```

## 问题原因

poi-tl 1.12.1 的表格循环格式有严格要求。之前的文档可能提供了错误的格式。

## 正确的表格循环格式

### 错误格式 ❌

在 Word 中，**不要**这样写：

```
| 考核维度 | 分数 |
|----------|------|
| {{#dimensions}}dimension | {{score}} |  ← 错误！循环标记和属性连在一起
```

### 正确格式 ✅

在 Word 中，应该这样写：

```
| 序号 | 考核维度 | 分数 | 权重 | 评价 |
|------|----------|------|------|------|
| {{@index+1}} | {{#dimensions}}{{dimension}} | {{score}} | {{weight}} | {{comment}} |
```

**关键点：**
1. `{{#dimensions}}` 循环标记必须**单独**在第一个数据单元格
2. 其他单元格只写属性名：`{{dimension}}`, `{{score}}` 等
3. 不要将循环标记和属性名写在一起
4. 可以使用 `{{@index}}` 或 `{{@index+1}}` 显示序号

## 在 Word 中创建表格（详细步骤）

### 步骤1：插入表格

1. 打开 Word
2. 点击"插入" > "表格"
3. 选择 2 行 5 列（1行表头 + 1行数据）

### 步骤2：填写表头

在第一行输入列标题：

```
| 序号 | 考核维度 | 分数 | 权重 | 评价 |
```

### 步骤3：填写数据行（关键！）

在第二行的每个单元格分别输入：

**单元格1（序号）：**
```
{{@index+1}}
```

**单元格2（考核维度）：**
```
{{#dimensions}}{{dimension}}
```

**单元格3（分数）：**
```
{{score}}
```

**单元格4（权重）：**
```
{{weight}}
```

**单元格5（评价）：**
```
{{comment}}
```

### 步骤4：最终表格应该看起来像：

```
| 序号 | 考核维度              | 分数 | 权重  | 评价    |
|------|-----------------------|------|-------|---------|
| {{@index+1}} | {{#dimensions}}{{dimension}} | {{score}} | {{weight}} | {{comment}} |
```

## 列表循环格式

### 正确格式 ✅

```
- {{*strengths}}
```

**在 Word 中：**
1. 创建项目符号列表
2. 在第一个列表项输入：`{{*strengths}}`
3. 可以删除或保留后续的列表项（poi-tl 会自动替换）

## 完整示例

### Groovy 脚本

```groovy
// 设置表格数据
def dimensions = [
    [dimension: "工作业绩", score: 90, weight: "35%", comment: "优秀"],
    [dimension: "专业能力", score: 88, weight: "25%", comment: "良好"],
    [dimension: "团队协作", score: 90, weight: "20%", comment: "优秀"]
]
data.put("dimensions", dimensions)

// 设置列表数据
def strengths = [
    "技术能力强",
    "代码质量高",
    "团队协作好"
]
data.put("strengths", strengths)
```

### Word 模板

```
标题：{{title}}

考核维度表格：
| 序号 | 考核维度              | 分数 | 权重  | 评价    |
|------|-----------------------|------|-------|---------|
| {{@index+1}} | {{#dimensions}}{{dimension}} | {{score}} | {{weight}} | {{comment}} |

优势列表：
- {{*strengths}}
```

## 测试步骤

### 1. 使用简单测试脚本

使用 `simple_table_test.txt` 进行测试。

### 2. 创建 Word 模板

参考 `word_template_v2.html` 的详细说明创建模板。

### 3. 在 Word 中操作

按照上面的详细步骤创建表格。

### 4. 生成报告

上传模板，使用测试脚本生成报告。

### 5. 验证结果

检查生成的 Word 文档：
- ✓ 基本数据显示
- ✓ 表格数据显示
- ✓ 列表数据显示

## 常见错误和解决方法

### 错误1：Unable to render template {{#dimensions}}

**原因：** 循环标记格式不正确

**解决：**
- 检查 `{{#dimensions}}` 是否单独在单元格中
- 不要与属性名连在一起

### 错误2：表格为空

**原因：** 属性名不匹配

**解决：**
- 检查 Word 中的属性名（`{{dimension}}`, `{{score}}` 等）
- 确保与 Groovy 脚本中的数据对象属性名一致
- 检查大小写

### 错误3：只显示一行数据

**原因：** 表格结构不正确

**解决：**
- 确保表格只有 1 行数据行（第2行）
- poi-tl 会自动复制这一行来渲染所有数据
- 不要手动添加多行

### 错误4：序号显示不正确

**原因：** 序号语法错误

**解决：**
- 使用 `{{@index}}` 从 0 开始
- 使用 `{{@index+1}}` 从 1 开始

## 使用辅助文件

### 1. word_template_v2.html
详细的正确格式说明，包含示例和步骤说明。

### 2. simple_table_test.txt
简化的测试脚本，专门用于测试表格渲染。

### 3. debug_table_script.txt
详细的调试脚本，输出大量日志信息。

## 检查清单

在上传模板前，检查以下项目：

### 表格
- [ ] 表格有表头行和数据行
- [ ] 数据行第一列是序号 `{{@index+1}}`
- [ ] 数据行第二列是 `{{#dimensions}}{{dimension}}`
- [ ] 其他列是 `{{score}}`, `{{weight}}`, `{{comment}}` 等
- [ ] `{{#dimensions}}` 单独在单元格中，没有与属性名连在一起
- [ ] 变量名 `dimensions` 与脚本中的 `data.put("dimensions", ...)` 一致
- [ ] 属性名与数据对象属性名一致（大小写敏感）

### 列表
- [ ] 列表第一项是 `{{*strengths}}`
- [ ] 变量名 `strengths` 与脚本中的 `data.put("strengths", ...)` 一致
- [ ] 列表数据是 List 类型

### 脚本
- [ ] 数据是 List 类型
- [ ] 使用 `data.put("variable", dataList)` 设置数据
- [ ] 数据对象的属性名与模板一致

## 获取帮助

如果问题仍然存在：

1. 使用 `debug_table_script.txt` 查看详细日志
2. 检查日志中的错误信息
3. 确保 Word 模板格式完全符合要求
4. 参考 `word_template_v2.html` 中的详细说明

## 快速参考

### 表格循环语法
```
{{@index+1}} | {{#tableVar}}{{property1}} | {{property2}} | ...
```

### 列表循环语法
```
- {{*listVar}}
```

### 基本占位符
```
{{variableName}}
```

### 序号占位符
```
{{@index}}     # 从 0 开始
{{@index+1}}   # 从 1 开始
```
