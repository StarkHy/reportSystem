# Word 模板创建详细指南

## 问题：表格数据不显示

这是最常见的 poi-tl 使用问题。请按照以下步骤仔细检查。

## 正确的 Word 模板创建步骤

### 1. 创建表格模板（重要！）

#### 步骤1.1：插入表格

1. 打开 Word
2. 点击"插入" > "表格"
3. 选择 2 行 4 列的表格（1行表头 + 1行数据）

#### 步骤1.2：填写表头

在第一行填写列标题：
```
| 考核维度 | 分数 | 权重 | 评价 |
```

#### 步骤1.3：填写数据行占位符（关键！）

**这是最容易出错的地方！**

在第二行的第一个单元格，输入：
```
{{#dimensions}}dimension
```

**注意：**
- `{{#dimensions}}` 必须在单元格的最开始
- `{{#dimensions}}` 和 `dimension` 之间**不要有空格**
- `dimensions` 是 Groovy 脚本中的变量名
- `dimension` 是数据对象的属性名

在第二行的其他单元格分别输入：
```
单元格2：{{score}}
单元格3：{{weight}}
单元格4：{{comment}}
```

**完成的表格应该看起来像：**
```
| 考核维度          | 分数 | 权重 | 评价        |
|-------------------|------|------|-------------|
| {{#dimensions}}dimension | {{score}} | {{weight}} | {{comment}} |
```

### 2. 创建列表模板（重要！）

#### 步骤2.1：插入列表

1. 点击"开始" > "项目符号"
2. 输入一个列表项

#### 步骤2.2：添加循环标记（关键！）

在第一个列表项的开头输入：
```
{{*strengths}}优势1
```

**注意：**
- `{{*strengths}}` 必须在列表项的最开始
- `{{*strengths}}` 和文字之间**不要有空格**
- `strengths` 是 Groovy 脚本中的变量名
- 列表项的文字可以是任意的，会被自动替换

**完成的列表应该看起来像：**
```
- {{*strengths}}优势1（会被替换）
- 优势2（会被删除或保留为最后一项）
```

### 3. 常见错误示例

#### 错误1：循环标记位置错误

```
❌ 错误：
| 考核维度 | 分数 |
| dimension | {{score}} |  ← 缺少 {{#dimensions}}

❌ 错误：
| {{#dimensions}} |  ← 标记单独占一列
| dimension | {{score}} |

✅ 正确：
| {{#dimensions}}dimension | {{score}} |
```

#### 错误2：变量名不匹配

**Groovy 脚本：**
```groovy
def data = [
    [dimension: "A", score: 90],
    [dimension: "B", score: 85]
]
put("myTable", data)  // 变量名是 myTable
```

**Word 模板：**
```
❌ 错误：
| {{#data}}dimension | {{score}} |  // 变量名错误

❌ 错误：
| {{#dimensions}}dimension | {{score}} |  // 变量名错误

✅ 正确：
| {{#myTable}}dimension | {{score}} |  // 变量名必须一致
```

#### 错误3：属性名不匹配

**Groovy 脚本：**
```groovy
def data = [
    [dimension: "A", score: 90, comment: "good"]
]
```

**Word 模板：**
```
❌ 错误：
| {{dimension}} | {{scoore}} |  // 拼写错误
| {{dimension}} | {{Score}} |  // 大小写错误

✅ 正确：
| {{dimension}} | {{score}} |  // 属性名必须一致
```

#### 错误4：多余空格

```
❌ 错误：
| {{ #dimensions }}dimension | {{ score }} |  // 有多余空格

❌ 错误：
| {{#dimensions}} dimension |  // 有空格

✅ 正确：
| {{#dimensions}}dimension | {{score}} |  // 无多余空格
```

## 完整示例

### Groovy 脚本
```groovy
// 设置基本数据
data.put("title", "员工考核报告")
data.put("name", "张三")

// 设置表格数据（注意变量名：dimensions）
def dimensions = [
    [
        dimension: "工作业绩",
        score: 90,
        weight: "35%",
        comment: "出色完成"
    ],
    [
        dimension: "专业能力",
        score: 88,
        weight: "25%",
        comment: "能力强"
    ]
]
data.put("dimensions", dimensions)

// 设置列表数据（注意变量名：strengths）
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
姓名：{{name}}

考核维度表格：
| 考核维度          | 分数 | 权重 | 评价    |
|-------------------|------|------|---------|
| {{#dimensions}}dimension | {{score}} | {{weight}} | {{comment}} |

优势列表：
- {{*strengths}}优势1
```

## 调试步骤

### 步骤1：使用调试脚本

使用 `debug_table_script.txt` 进行测试，它会输出详细的日志。

### 步骤2：检查日志

查看执行日志，确认：
```
INFO  dimensions 类型: class java.util.ArrayList
INFO  dimensions 大小: 2
INFO  第一项: [dimension:工作业绩, score:90, ...]
INFO  dimensions 数据已设置到 data
```

### 步骤3：检查 Word 模板

1. 在 Word 中打开模板文件
2. 定位到表格的第一行数据行（第2行）
3. 点击第一个单元格，光标放在最前面
4. 逐个字符检查：
   - 是否有 `{`
   - 是否有 `#`
   - 是否有 `dimensions`
   - 是否有 `}`
   - 是否有 `dimension`
5. 使用 Ctrl+A 全选内容，检查是否有隐藏字符

### 步骤4：生成测试

1. 使用调试脚本生成报告
2. 下载生成的 Word 文档
3. 检查：
   - 基本数据是否显示（{{title}}、{{name}}）
   - 表格是否有数据
   - 列表是否有数据

### 步骤5：对比

如果基本数据显示，但表格/列表不显示：
- ✓ 脚本没问题
- ✓ 基本占位符格式正确
- ✗ 表格/列表循环标记有问题

如果所有数据都不显示：
- ✗ 脚本有问题
- ✗ 模板占位符格式有问题
- ✗ 变量名不匹配

## 快速检查清单

在使用模板前，检查以下项目：

- [ ] 表格第一行的第一个单元格包含 `{{#variable}}`
- [ ] `{{#variable}}` 后面紧跟属性名，无空格
- [ ] 变量名 `variable` 与 `data.put("variable", data)` 一致
- [ ] 属性名与数据对象的属性名一致（大小写敏感）
- [ ] 列表第一项包含 `{{*variable}}`
- [ ] 没有多余的空格或隐藏字符
- [ ] Groovy 脚本中数据是 List 类型
- [ ] 表格数据是对象列表（Map 或 POJO）
- [ ] 列表数据是字符串列表或其他列表

## 使用辅助工具

### 1. 调试脚本

使用 `debug_table_script.txt`，它会输出详细的调试信息。

### 2. 最小化测试

先创建一个最简单的模板：
```
标题：{{title}}

| 项目 | 分数 |
|------|------|
| {{#test}}项目 | {{score}} |
```

使用最简单的脚本：
```groovy
data.put("title", "测试")
data.put("test", [[project:"A", score:90]])
```

### 3. 逐步添加

从最简单的模板开始，逐步添加内容，每次测试：
1. 只测试基本数据（{{title}}）
2. 添加一个表格（1个变量，1列）
3. 添加多列
4. 添加多行数据
5. 添加列表

## 获取帮助

如果问题仍然存在：

1. 提供以下信息：
   - Groovy 脚本内容（特别是数据设置部分）
   - Word 模板的截图（特别是表格部分）
   - 执行日志（完整内容）
   - 生成的 Word 文档截图

2. 检查官方文档：
   - poi-tl 文档：http://deepoove.com/poi-tl/

3. 查看本项目的其他示例：
   - `debug_table_script.txt` - 调试脚本
   - `test_template_correct.html` - 正确格式的模板说明
