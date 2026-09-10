# 高并发实时活动与交易平台

[简体中文](README.md) | [English](README.en.md)

基于 Java 21、Spring Boot、MySQL、Redis 和 RabbitMQ 构建的高并发交易后端。项目以限时抢购为核心场景，重点解决库存正确性、重复请求、流量保护、可靠消息投递和异步订单一致性问题。

## 项目亮点

- **MySQL 交易事实源**：16 个库存桶分散单商品写热点，条件扣减与唯一约束防止超卖和重复下单；Redis 不保存最终交易状态。
- **短事务与过载保护**：幂等查询位于事务外，公平信号量限制数据库在途事务，并在过载时快速返回 `429`。
- **可靠异步订单**：库存预留、请求记录和 Outbox 事件在同一事务中提交。
- **消息最终一致性**：Outbox 租约批量发布、RabbitMQ Confirm、持久化消息、消费端批量事务、失败队列和自动重试覆盖异常路径。
- **请求幂等**：同一用户重复提交同一抢购请求时返回同一个请求 ID，不重复扣减库存。
- **分层流量控制**：一次基于 Redis `TIME` 的 Lua 令牌桶调用完成用户、单商品和全局三级准入检查。
- **可观测性**：独立管理端口暴露 Prometheus 指标，包括连接池、预留耗时、异步完成延迟、入口拒绝和 Outbox 积压。
- **安全边界**：Token 鉴权、管理员权限、验证码原子消费、接口限流和请求结束身份清理。
- **自动化验证**：包含 26 项默认测试、真实依赖集成测试和可复现的 k6 写链路压测。

## 技术栈

- Java 21、Spring Boot 3.5
- Spring Security、MyBatis-Plus
- MySQL 8、Redis、RabbitMQ
- Maven、Docker Compose
- JUnit 5、H2、Mockito、Testcontainers、k6

## 核心订单链路

```mermaid
flowchart TD
  A[用户提交抢购请求] --> B[Redis Lua 三级流量准入]
  B --> C{已有请求或订单}
  C -- 是 --> D[返回原请求 ID]
  C -- 否 --> E[条件扣减一个 MySQL 库存桶]
  E --> F[事务写入 PENDING 请求与 Outbox]
  F --> G[提交事务并返回请求 ID]

  H[Outbox 批量扫描并租约事件] --> I[整批发布持久消息]
  I --> J[等待 Broker Confirm]
  J -- 失败或退回 --> K[记录错误并延迟重试]
  K --> H
  J -- 已确认 --> L[RabbitMQ 批量消费者]
  L --> M[整批锁定请求并批量创建订单]
  M --> N[批量标记请求与 Outbox 完成]
  N --> O[事务提交后 ACK]
  L -- 重试耗尽 --> P[失败队列]
```

Broker Confirm 只代表 RabbitMQ 已接收消息。消费者事务完成后，数据库中的 Outbox 事件才会被标记为完成；重复投递不会重复创建订单。

## 已实现功能

### 账户与安全

- 手机验证码登录和 Token 鉴权。
- Token 滑动过期及主动登出。
- 管理接口角色校验。
- 验证码发送、校验和来源 IP 限流。
- 图片类型、大小、像素及所有者校验。

### 交易与一致性

- 优惠券及限时抢购。
- 数据库条件扣减库存。
- 单商品 16 个 MySQL 库存桶分散行锁竞争。
- 用户、单商品和全局三级流量准入。
- 公平在途事务限制和快速过载拒绝。
- 同一用户、同一优惠券的请求幂等。
- 订单请求状态查询：`PENDING`、`COMPLETED`。
- Transactional Outbox 批量发布和定时补发。
- RabbitMQ 持久化、Confirm、Return、并发消费、重试和失败队列。
- Prometheus 订单、连接池和 Outbox 指标。

### 缓存与业务功能

- 商户查询缓存、空值缓存和逻辑过期。
- 商户分类查询。
- 笔记、点赞、关注和签到。
- 图片上传、读取和删除。

## 快速启动

### 环境要求

- Java 21
- Maven 3.6.3+
- Docker Desktop

### 1. 配置环境变量

在项目根目录创建 `.env`：

```properties
MYSQL_URL=jdbc:mysql://127.0.0.1:3307/event_trading?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
MYSQL_USER=root
MYSQL_PASSWORD=替换为本地密码

REDIS_HOST=127.0.0.1
REDIS_PORT=6380

RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5673
RABBITMQ_USER=event_app
RABBITMQ_PASSWORD=替换为本地密码
```

`.env` 已被 Git 忽略，请勿提交真实凭据。

### 2. 启动基础设施

```powershell
docker compose up -d
docker compose ps
```

默认端口：MySQL `3307`、Redis `6380`、RabbitMQ `5673`、RabbitMQ 管理界面 `15673`。

### 3. 启动应用

```powershell
mvn test
mvn '-Dspring-boot.run.profiles=local' spring-boot:run
```

服务地址：`http://127.0.0.1:8081`。`local` profile 会返回开发验证码，只能用于本机调试。

## 测试

默认测试不连接个人数据库：

```powershell
mvn test
```

当前默认测试结果：**26 项通过，0 失败**。

使用真实 MySQL、Redis 和 RabbitMQ 运行隔离集成测试：

```powershell
mvn -Pinfrastructure verify
```

## 并发压测

`loadtest/` 包含真实下单固定到达率脚本、隔离优惠券数据和自动过期的合成用户令牌。准备隔离数据后可执行：

```powershell
$env:RATE='1500'
$env:DURATION_SECONDS='10'
$env:VOUCHER_ID='9900021600'
$env:BASE_URL='http://127.0.0.1:8081'
k6 run .\loadtest\order-capacity.js
```

容量测试启动应用时将单商品/全局限流上限临时提高到 `5000`，避免保护阈值掩盖系统边界；日常默认值仍为单商品 `420/s`、全局 `800/s`。本地单实例测试环境：Windows 11、Java 21、Docker MySQL 8.4、Redis 7.4、RabbitMQ 4.1、k6 v2.2.0。每次请求调用真实鉴权下单接口并使用不同合成用户；RabbitMQ 消费与入口并行运行，测试后核对库存、请求、最终订单、重复订单和 Outbox。

10 秒固定到达率结果：

- 1000 目标 RPS：P95 34.69 ms，10001 个请求和最终订单，0 拒绝、0 异常、0 dropped iterations、重复订单 0、Outbox 0。
- 1200 目标 RPS：P95 12.77 ms，12001 个请求和最终订单，0 拒绝、0 异常、0 dropped iterations、重复订单 0、Outbox 0。
- 1400 目标 RPS：P95 17.1 ms，14001 个请求和最终订单，0 拒绝、0 异常、0 dropped iterations、重复订单 0、Outbox 0。
- 1500 目标 RPS：P95 14.82 ms，15001 个请求和最终订单，0 拒绝、0 异常、0 dropped iterations、重复订单 0、Outbox 0。
- 1600 目标 RPS：P95 109.13 ms，16001 次请求中 15848 次受理、153 次受控 `429`，0 非预期响应、0 dropped iterations；受理请求最终全部成单，重复订单 0、Outbox 0。

按 P95 小于 1 秒、无 `429`、无非预期响应、无 dropped iteration、无超卖/重复单且 Outbox 完全排空的严格口径，当前热机稳定档位为 1500 目标 RPS，失败边界位于 1500～1600 目标 RPS。该结果是本地单实例、10 秒短时容量基线，不代表生产环境 SLA 或 10～30 分钟长稳结果。

## 项目结构

```text
event-trading-platform/
├─ src/main/java/com/eventplatform/
│  ├─ config/          # 安全、数据库和消息配置
│  ├─ controller/      # HTTP API
│  ├─ order/           # 订单事务与 Outbox
│  ├─ security/        # Token、验证码和限流
│  ├─ service/         # 业务逻辑
│  └─ upload/          # 图片存储
├─ src/main/resources/
│  ├─ db/              # 建库及升级脚本
│  └─ mapper/          # MyBatis XML
├─ src/test/           # 单元、回归和集成测试
├─ docs/               # 架构与技术说明
├─ loadtest/           # k6 写链路压测与隔离数据
├─ postman/            # API 请求集合
├─ compose.yaml
└─ pom.xml
```

## 运行边界

- MySQL 是库存和订单的最终事实源；Redis 用于缓存、会话和流量准入。
- 同一商品库存分散到 16 个 MySQL 行桶，查询库存时汇总各桶；现有数据库使用 `db/performance-upgrade.sql` 迁移。
- 抢购接口返回请求 ID，最终订单由 RabbitMQ 消费者异步创建。
- 管理端口 `127.0.0.1:8082` 仅暴露健康检查与 Prometheus 指标。
- 本地 Compose 用于开发和验证，不代表生产部署环境。

## 相关文档

- [安全及一致性说明](docs/SECURITY-FIXES.md)
- [领域模型](docs/architecture/DOMAIN-MODEL.md)
- [业务状态机](docs/architecture/STATE-MACHINES.md)
- [API 契约](docs/architecture/API-CONTRACT.md)
