# Report System

Spring Boot 报表系统，部署在 TongWeb 服务器上。

## 项目信息

- **Java 版本**: 1.8
- **Spring Boot 版本**: 2.7.18
- **打包方式**: WAR
- **部署目标**: TongWeb

## 项目结构

```
reportSystem/
├── src/
│   ├── main/
│   │   ├── java/com/example/reportsystem/
│   │   │   ├── ReportSystemApplication.java
│   │   │   └── controller/
│   │   │       └── ReportController.java
│   │   ├── resources/
│   │   │   └── application.yml
│   │   └── webapp/WEB-INF/
│   │       └── web.xml
│   └── test/
└── pom.xml
```

## 编译打包

```bash
mvn clean package
```

编译成功后，会在 `target/` 目录下生成 `report-system.war` 文件。

## TongWeb 部署步骤

### 1. 准备部署包
```bash
mvn clean package
```

### 2. 部署到 TongWeb

**方式一：通过 TongWeb 管理控制台**
1. 登录 TongWeb 管理控制台
2. 选择"应用程序" -> "部署" -> "上传"
3. 选择生成的 `report-system.war` 文件
4. 设置上下文路径（默认为 `/report-system`）
5. 点击"部署"按钮

**方式二：手动部署**
1. 将 `target/report-system.war` 复制到 TongWeb 的 `webapps` 目录
2. 重启 TongWeb 服务器

### 3. 访问应用

部署成功后，可以通过以下地址访问：

- 主页: `http://<TongWeb服务器IP>:<端口>/report-system/`
- 健康检查: `http://<TongWeb服务器IP>:<端口>/report-system/health`
- 示例API: `http://<TongWeb服务器IP>:<端口>/report-system/api/report/hello`

## 注意事项

1. **Java 版本**: 确保 TongWeb 运行在 Java 1.8 环境下
2. **Servlet 版本**: 项目使用 Servlet 3.1，确保 TongWeb 支持
3. **端口配置**: 默认使用 8080 端口，可根据需要修改 `application.yml`
4. **上下文路径**: 可通过 `server.servlet.context-path` 调整

## 开发调试

本地开发时可以直接运行主类：
```bash
mvn spring-boot:run
```

或者运行 `ReportSystemApplication` 的 `main` 方法。

## 数据库迁移

为持久化生成记录的数据来源（API/手动），新增了 `report_generation.data_source` 字段。

- 初始化脚本更新位置：
  - [init.sql](file:///Users/huangyan/CodeBuddy/reportSystem/src/main/resources/sql/init.sql)
- 增量迁移脚本位置：
  - [20260128_add_data_source.sql](file:///Users/huangyan/CodeBuddy/reportSystem/src/main/resources/sql/migrations/20260128_add_data_source.sql)

执行示例（根据你的数据库环境调整）：

```bash
ksql -U SYSTEM -d report_system -f src/main/resources/sql/migrations/20260128_add_data_source.sql
```

应用启动后，服务会在生成记录中自动写入该字段，页面列表与详情也会使用该字段展示数据来源。
