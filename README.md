# ₿ BTC 涨跌预测市场

> 基于 BTC 实时价格的短期涨跌预测平台 Demo · Spring Boot 3 + Vue3 · 零外部依赖

---

## 📖 项目简介

这是一个 BTC 涨跌预测市场 Demo。用户使用**积分**对 BTC 在指定时间窗口（1/5/15 分钟）内的涨跌方向进行押注，市场收盘后根据真实价格自动结算，盈亏直接反映在积分余额中。

### 核心玩法

```
开盘价记录 → 用户押注(涨/跌) → 收盘价记录 → 自动结算
     │              │                │              │
  市场激活      扣除并冻结积分      判定涨跌      赢家按比例分输方池
```

- 收盘价 ≥ 开盘价 → **涨方**获胜
- 收盘价 < 开盘价 → **跌方**获胜
- 获胜方按押注比例瓜分输方押注池

### Demo 特点

- ✅ **零外部依赖**：使用 H2 内存数据库，无需安装 PostgreSQL / Redis / RabbitMQ
- ✅ **真实价格源**：集成 Binance API 获取实时 BTC 价格，网络不可用时自动回退模拟价格
- ✅ **全自动市场**：定时任务自动创建、开盘、收盘、结算市场
- ✅ **完整闭环**：注册 → 登录 → 押注 → 结算 → 查看盈亏，全流程可体验

---

## 🛠 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.x | Web 框架 |
| Spring Data JPA | - | 数据访问层 |
| Spring Validation | - | 参数校验 |
| spring-security-crypto | - | BCrypt 密码加密 |
| H2 Database | - | 内存数据库（PostgreSQL 兼容模式） |
| JJWT | 0.12.6 | JWT 认证 |
| Java | 17 | 运行时 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.x | UI 框架 |
| Vue Router | 4.3.x | 单页应用路由 |
| Vite | 5.2.x | 构建工具与开发服务器 |

---

## 📁 项目结构

```
btcmarket/
├── src/main/java/com/lazybeartoby/btcmarket/
│   ├── BtcmarketApplication.java          # 主入口（@EnableScheduling）
│   ├── common/
│   │   ├── config/WebConfig.java          # CORS 与 Web 配置
│   │   ├── constant/                      # 常量与枚举
│   │   │   ├── AppConstants.java          # 应用常量
│   │   │   ├── BetDirection.java          # 押注方向(UP/DOWN)
│   │   │   ├── BetResult.java             # 押注结果(WIN/LOSS/REFUNDED)
│   │   │   ├── MarketResult.java          # 市场结果(UP/DOWN)
│   │   │   ├── MarketStatus.java          # 市场状态(PENDING/ACTIVE/CLOSED/SETTLED)
│   │   │   └── TransactionType.java       # 流水类型
│   │   ├── exception/                     # 全局异常处理
│   │   ├── result/R.java                  # 统一响应封装
│   │   └── security/                      # JWT 认证与用户上下文
│   ├── controller/                        # REST 控制器
│   │   ├── AuthController.java            # 注册/登录
│   │   ├── MarketController.java          # 市场列表/详情
│   │   ├── BetController.java             # 押注/注单查询
│   │   ├── WalletController.java          # 钱包/流水
│   │   ├── PriceController.java          # 实时价格
│   │   ├── UserController.java            # 用户资料/统计
│   │   └── AdminController.java           # 管理后台
│   ├── service/                           # 业务逻辑层
│   │   ├── AuthService.java               # 认证服务
│   │   ├── MarketService.java             # 市场服务
│   │   ├── MarketSchedulerService.java    # 市场定时任务（创建/开盘/收盘/结算）
│   │   ├── BetService.java                # 押注服务
│   │   ├── SettlementService.java         # 结算服务
│   │   ├── WalletService.java             # 钱包服务
│   │   ├── PriceService.java              # 价格服务（Binance + 模拟回退）
│   │   ├── UserService.java               # 用户服务
│   │   └── AdminService.java              # 管理服务
│   ├── model/
│   │   ├── entity/                        # JPA 实体
│   │   ├── dto/                           # 请求 DTO
│   │   └── vo/                            # 响应 VO
│   ├── repository/                        # JPA 仓库
│   └── init/DataInitializer.java         # 启动数据初始化
├── src/main/resources/
│   └── application.yml                    # 应用配置
├── frontend/                              # Vue3 前端
│   ├── src/
│   │   ├── views/                         # 页面组件
│   │   │   ├── Login.vue                  # 登录/注册
│   │   │   ├── Markets.vue                # 首页（实时价格+市场列表）
│   │   │   ├── MarketDetail.vue           # 市场详情+押注
│   │   │   ├── UserCenter.vue             # 用户中心
│   │   │   └── Admin.vue                  # 管理后台
│   │   ├── App.vue                        # 根组件
│   │   ├── api.js                         # API 客户端
│   │   ├── store.js                       # 全局状态
│   │   ├── router.js                      # 路由
│   │   └── style.css                      # 全局样式
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── DESIGN.md                              # 详细设计文档
├── prd.md                                 # 产品需求文档
└── pom.xml
```

---

## 🚀 快速开始

### 环境要求

- **JDK 17** 或更高版本
- **Maven 3.8+**（或使用项目自带 `mvnw`）
- **Node.js 18+** 与 **npm**

### 1. 启动后端

```bash
cd btcmarket
./mvnw spring-boot:run
```

后端启动后：
- API 服务：http://localhost:8080
- H2 控制台：http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:btcmarket`
  - 用户名: `sa`，密码留空

### 2. 启动前端

```bash
cd btcmarket/frontend
npm install
npm run dev
```

前端启动后访问：http://localhost:5173

> Vite 已配置 `/api` 代理转发至后端 `8080` 端口，无需处理跨域。

### 3. 演示账号

| 角色 | 用户名 | 密码 | 初始积分 |
|------|--------|------|----------|
| 管理员 | `admin` | `admin123` | 1,000,000 |
| 普通用户 | `demo` | `demo123` | 10,000 |

> 启动时由 [DataInitializer.java](src/main/java/com/lazybeartoby/btcmarket/init/DataInitializer.java) 自动创建，并初始化 1/5/15 分钟三种市场模板。

---

## 🎮 功能演示

### 用户视角

1. **注册/登录**：访问首页，使用演示账号登录，或注册新账号（注册赠送 10,000 积分）
2. **查看市场**：首页展示实时 BTC 价格和所有活跃市场卡片
3. **押注**：点击市场卡片进入详情，选择「涨」或「跌」，输入金额后确认下注
4. **等待结算**：市场到期后自动结算，可在用户中心查看盈亏

### 管理员视角

1. 使用 `admin / admin123` 登录
2. 访问「管理后台」查看平台数据（用户数、市场数、注单数、平台流水）
3. 可对任意用户发放或扣除积分
4. 查看所有用户列表及余额

---

## 🔌 API 接口

所有接口返回统一格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1781771201924
}
```

需要认证的接口请在请求头携带：`Authorization: Bearer <token>`

### 认证

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 注册（赠送积分） | 否 |
| POST | `/api/auth/login` | 登录，返回 JWT | 否 |

### 价格

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/price/btc` | 获取 BTC 实时价格 | 否 |

### 市场

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/market/active` | 活跃市场列表 | 否 |
| GET | `/api/market/history` | 已结算历史 | 否 |
| GET | `/api/market/{id}` | 市场详情 | 否 |
| GET | `/api/market/template/list` | 市场模板列表 | 否 |

### 押注

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/bet/place` | 下注 | 是 |
| GET | `/api/bet/my` | 我的注单（分页） | 是 |
| GET | `/api/bet/market/{marketId}` | 指定市场注单 | 否 |

### 钱包

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/wallet` | 我的钱包 | 是 |
| GET | `/api/wallet/transactions` | 积分流水（分页） | 是 |

### 用户

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/user/profile` | 个人资料 | 是 |
| GET | `/api/user/stats` | 押注统计 | 是 |

### 管理后台

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/dashboard` | 平台仪表盘 | 管理员 |
| GET | `/api/admin/users` | 用户列表 | 管理员 |
| POST | `/api/admin/grant` | 发放积分 | 管理员 |
| POST | `/api/admin/deduct` | 扣除积分 | 管理员 |

### 接口示例

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}'

# 获取实时价格
curl http://localhost:8080/api/price/btc

# 下注（需替换为有效 token 和 marketId）
curl -X POST http://localhost:8080/api/bet/place \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"marketId":1,"direction":"UP","amount":100}'
```

---

## ⚙️ 配置说明

核心配置位于 [application.yml](src/main/resources/application.yml)：

```yaml
app:
  jwt:
    secret: <base64编码的密钥>
    access-token-expire-minutes: 720      # token 有效期 12 小时
  market:
    platform-fee-rate: 0.02               # 平台费率（预留）
    min-bet: 1                            # 最小押注
    max-bet: 100000                       # 最大押注
    create-ahead-minutes: 10              # 提前创建市场的时间窗口
  price:
    source: binance                       # 价格源
    fetch-interval-ms: 3000               # 价格刷新间隔
    fallback-simulated: true             # 网络失败时回退模拟价格
```

---

## 🧠 核心机制

### 市场生命周期

由 [MarketSchedulerService.java](src/main/java/com/lazybeartoby/btcmarket/service/MarketSchedulerService.java) 的三个定时任务驱动：

```
PENDING ──(到达开始时间,记录开盘价)──> ACTIVE ──(到达结束时间,记录收盘价)──> CLOSED ──(结算完成)──> SETTLED
```

| 定时任务 | 频率 | 职责 |
|----------|------|------|
| `createUpcomingMarkets` | 每 30 秒 | 按模板预创建未来市场 |
| `openMarkets` | 每 2 秒 | 到点市场记录开盘价并激活 |
| `closeAndSettleMarkets` | 每 2 秒 | 到点市场记录收盘价并结算 |

### 押注与结算

**押注流程**（[BetService.java](src/main/java/com/lazybeartoby/btcmarket/service/BetService.java)）：

1. 校验金额范围与市场状态（必须为 `ACTIVE`）
2. 创建注单记录
3. 钱包扣除积分并冻结
4. 更新市场涨/跌押注池

**结算流程**（[SettlementService.java](src/main/java/com/lazybeartoby/btcmarket/service/SettlementService.java)）：

1. 比较收盘价与开盘价，判定市场结果（涨/跌）
2. 若无对手盘（全涨或全跌），退还所有押注
3. 否则，赢家按押注比例瓜分输方池：
   - 单注盈利 = 输方池 × (该注金额 / 赢方池总额)
   - 赢家返还本金 + 盈利
   - 输家扣除本金
4. 所有资金变动记录到钱包流水

### 价格服务

[PriceService.java](src/main/java/com/lazybeartoby/btcmarket/service/PriceService.java) 集成 Binance 24h 行情 API：

- 请求超时 2 秒，防止网络问题阻塞定时任务
- 失败时自动回退到模拟价格（带随机波动），保证 Demo 可用性
- 响应中 `source` 字段标识数据来源（`BINANCE` / `simulated`）

---

## 🗄 数据模型

| 实体 | 说明 |
|------|------|
| `User` | 用户（用户名、密码哈希、角色、状态） |
| `Wallet` | 钱包（余额、冻结余额、累计盈亏） |
| `WalletTransaction` | 积分流水（类型、金额、备注） |
| `MarketTemplate` | 市场模板（名称、标的、周期） |
| `Market` | 市场实例（开盘价、收盘价、押注池、状态） |
| `Bet` | 注单（方向、金额、赔率、结果） |
| `PriceTick` | 价格快照（历史记录） |

---

## ⚠️ 与生产架构的差异

本 Demo 为快速演示进行了简化，与 [DESIGN.md](DESIGN.md) 中的生产设计存在以下差异：

| 项目 | 生产设计 | Demo 实现 |
|------|----------|-----------|
| 数据库 | PostgreSQL | H2 内存库（PostgreSQL 兼容模式） |
| 缓存 | Redis | 无（直接查库） |
| 消息队列 | RabbitMQ | 无（同步处理） |
| 实时推送 | WebSocket | 前端轮询（3-5 秒） |
| 部署 | Nginx 反向代理 | Vite 开发代理 |

> 数据存储在内存中，**服务重启后数据会丢失**。如需持久化，可将 H2 切换为文件模式或更换为 PostgreSQL。

---

## 📄 相关文档

- [DESIGN.md](DESIGN.md) — 软件详细设计文档
- [prd.md](prd.md) — 产品需求文档

---

## 📜 License

仅供学习演示用途。
