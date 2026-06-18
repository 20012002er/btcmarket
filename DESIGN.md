# BTC 涨跌预测市场 - 软件详细设计文档

> 版本: v1.0 | 日期: 2026-06-05 | 作者: LazyBearAI 🐾

---

## 一、产品概述

### 1.1 产品定义
基于 BTC 实时价格的短期涨跌预测平台。用户使用**积分**对 BTC 在指定时间窗口内的涨跌方向进行押注，结算后盈亏直接反映在积分余额中。

### 1.2 核心玩法
- 每个市场有固定的时间窗口（如5分钟、15分钟、1小时）
- 市场开始时记录 BTC 开盘价，结束时记录收盘价
- 收盘价 ≥ 开盘价 → "涨"方获胜；收盘价 < 开盘价 → "跌"方获胜
- 获胜方获得所有押注积分（按比例分配）

### 1.3 积分体系
- 积分是平台内部虚拟资产，1积分 = 最小押注单位
- 管理后台可给任意用户赠送积分
- 押注时扣除积分，结算后赢取/亏损积分
- 暂不开放充值/兑换/提现，仅后台赠送

---

## 二、系统架构

### 2.1 整体架构图

```
┌──────────────────────────────────────────────────────┐
│                      前端应用                          │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌───────────┐ │
│  │ 首页/   │ │ 市场详情  │ │ 交易面板│ │ 用户中心  │ │
│  │ 市场列表│ │ 图表/盘口 │ │ 押注UI  │ │ 历史记录  │ │
│  └────┬────┘ └─────┬────┘ └────┬────┘ └─────┬─────┘ │
│       └────────────┴───────────┴─────────────┘       │
│                         │ HTTP/WebSocket               │
└─────────────────────────┼────────────────────────────┘
                          │
┌─────────────────────────┼────────────────────────────┐
│                    Nginx / 反向代理                     │
└─────────────────────────┼────────────────────────────┘
                          │
┌─────────────────────────┼────────────────────────────┐
│               Spring Boot 后端服务                      │
│  ┌──────────────────────────────────────────────────┐ │
│  │                Controller 层                      │ │
│  │  AuthController  MarketController  BetController│ │
│  │  UserController  AdminController  WalletCtrl   │ │
│  └──────────────────┬───────────────────────────────┘ │
│  ┌──────────────────┼───────────────────────────────┐ │
│  │                Service 层                        │ │
│  │  MarketService   BetService    WalletService    │ │
│  │  UserService     PriceService  SettlementService │ │
│  │  AdminService    WebSocketService               │ │
│  └──────────────────┬───────────────────────────────┘ │
│  ┌──────────────────┼───────────────────────────────┐ │
│  │                基础设施层                           │ │
│  │  Redis          RabbitMQ       ScheduledTasks  │ │
│  │  PriceFeed      Data Access                     │ │
│  └──────────────────────────────────────────────────┘ │
└──────────┬──────────────┬──────────────┬──────────────┘
           │              │              │
    ┌──────┴──────┐ ┌─────┴─────┐ ┌─────┴─────┐
    │  PostgreSQL  │ │   Redis   │ │  RabbitMQ │
    │  主数据库     │ │ 缓存/锁   │ │ 消息队列   │
    └─────────────┘ └───────────┘ └───────────┘
```

### 2.2 外部依赖

| 依赖 | 用途 | 备选 |
|------|------|------|
| Binance/Kraken API | BTC/USD 实时价格 | CoinGecko, OKX |
| JWT | 用户认证 | - |
| WebSocket | 实时推送价格/订单 | SSE fallback |

---

## 三、数据库设计

### 3.1 ER 关系

```
user 1──N wallet_transaction
user 1──N bet
user 1──N user_profile
market 1──N bet
market N──1 market_template
```

### 3.2 表结构详细设计

#### 3.2.1 用户表 `t_user`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 用户ID |
| username | VARCHAR(32) UNIQUE | 用户名 |
| password_hash | VARCHAR(128) | 密码哈希(BCrypt) |
| avatar_url | VARCHAR(512) | 头像URL |
| status | TINYINT DEFAULT 1 | 1=正常 0=禁用 |
| role | VARCHAR(16) DEFAULT 'USER' | USER / ADMIN |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 3.2.2 用户积分/钱包表 `t_wallet`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 钱包ID |
| user_id | BIGINT FK→t_user.id | 用户ID(UNIQUE) |
| balance | DECIMAL(18,2) NOT NULL DEFAULT 0 | 当前积分余额 |
| frozen_balance | DECIMAL(18,2) NOT NULL DEFAULT 0 | 冻结积分(押注中) |
| version | INT DEFAULT 0 | 乐观锁版本号 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

> **设计要点**：
> - `balance` 可用余额，`frozen_balance` 正在押注中冻结的金额
> - 实际可用 = `balance`（扣注时同时减 balance 加 frozen_balance）
> - 结算后从 frozen_balance 扣除，赢的部分加回 balance
> - 使用 `version` 字段做乐观锁，防止并发扣款超卖

#### 3.2.3 积分流水表 `t_wallet_transaction`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 流水ID |
| user_id | BIGINT FK→t_user.id NOT NULL | 用户ID |
| type | VARCHAR(32) NOT NULL | BET_FREEZE / BET_WIN / BET_LOSS / ADMIN_GRANT / ADMIN_DEDUCT |
| amount | DECIMAL(18,2) NOT NULL | 金额(正数) |
| direction | TINYINT NOT NULL | 1=收入 2=支出 |
| balance_after | DECIMAL(18,2) NOT NULL | 操作后余额 |
| frozen_after | DECIMAL(18,2) NOT NULL | 操作后冻结余额 |
| reference_id | BIGINT | 关联ID(bet_id 或 admin_log_id) |
| reference_type | VARCHAR(32) | BET / ADMIN_GRANT |
| remark | VARCHAR(256) | 备注 |
| created_at | TIMESTAMP | 创建时间 |

> 索引: `idx_user_id_created_at (user_id, created_at)`

#### 3.2.4 市场模板表 `t_market_template`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 模板ID |
| name | VARCHAR(64) NOT NULL | 模板名称(如"BTC 5分钟涨跌") |
| asset_symbol | VARCHAR(16) NOT NULL | 资产代码(BTC/ETH/SOL) |
| asset_name | VARCHAR(32) | 资产名称 |
| duration_seconds | INT NOT NULL | 时间窗口(秒) |
| settlement_rule | VARCHAR(512) | 结算规则描述 |
| status | TINYINT DEFAULT 1 | 1=启用 0=停用 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 3.2.5 市场表 `t_market`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 市场ID |
| template_id | BIGINT FK→t_market_template.id | 模板ID |
| market_no | VARCHAR(32) UNIQUE | 市场编号(如 BTC-5M-20260605-0400) |
| title | VARCHAR(128) | 展示标题(如"BTC 5分钟涨跌 16:00-16:05") |
| asset_symbol | VARCHAR(16) | 资产代码 |
| start_time | TIMESTAMP NOT NULL | 市场开始时间 |
| end_time | TIMESTAMP NOT NULL | 市场结束时间 |
| open_price | DECIMAL(24,8) | 开盘价(开始时记录) |
| close_price | DECIMAL(24,8) | 收盘价(结束时记录) |
| result | VARCHAR(8) | UP / DOWN / PENDING / CANCELLED |
| total_bet_up | DECIMAL(18,2) DEFAULT 0 | 涨方总押注积分 |
| total_bet_down | DECIMAL(18,2) DEFAULT 0 | 跌方总押注积分 |
| bet_count | INT DEFAULT 0 | 押注人次 |
| status | TINYINT DEFAULT 0 | 0=即将开始 1=进行中(可押注) 2=已截止(结算中) 3=已结算 |
| settled_at | TIMESTAMP | 结算时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

> 索引: `idx_template_start_time (template_id, start_time)`, `idx_status (status)`

#### 3.2.6 押注/订单表 `t_bet`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 押注ID |
| bet_no | VARCHAR(32) UNIQUE | 押注编号 |
| market_id | BIGINT FK→t_market.id NOT NULL | 市场ID |
| user_id | BIGINT FK→t_user.id NOT NULL | 用户ID |
| direction | VARCHAR(4) NOT NULL | UP / DOWN |
| amount | DECIMAL(18,2) NOT NULL | 押注积分 |
| odds | DECIMAL(6,4) NOT NULL DEFAULT 1.0000 | 押注时赔率 |
| win_amount | DECIMAL(18,2) | 赢得积分(结算后填充) |
| result | VARCHAR(8) | WIN / LOSS / PENDING / REFUNDED |
| status | TINYINT DEFAULT 1 | 1=有效 0=已取消 |
| ip_address | VARCHAR(45) | 下注IP |
| created_at | TIMESTAMP | 下注时间 |
| settled_at | TIMESTAMP | 结算时间 |

> 索引: `idx_market_id (market_id)`, `idx_user_id (user_id)`, `idx_user_market (user_id, market_id)`

> **赔率计算公式**：
> - UP赔率 = 1 + (total_bet_down / total_bet_up) × (1 - platform_fee_rate)
> - DOWN赔率 = 1 + (total_bet_up / total_bet_down) × (1 - platform_fee_rate)
> - platform_fee_rate 平台抽水比例，默认 0.02 (2%)

#### 3.2.7 价格记录表 `t_price_tick`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 记录ID |
| asset_symbol | VARCHAR(16) NOT NULL | 资产代码 |
| price | DECIMAL(24,8) NOT NULL | 价格 |
| timestamp | TIMESTAMP NOT NULL | 时间戳 |
| source | VARCHAR(32) | 数据源(binance/okx等) |

> 索引: `idx_symbol_timestamp (asset_symbol, timestamp)`
> 说明: 用于记录收盘价快照和历史价格查询，可按日分区

#### 3.2.8 管理员操作日志 `t_admin_log`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 日志ID |
| admin_id | BIGINT FK→t_user.id | 操作管理员ID |
| action | VARCHAR(64) NOT NULL | 操作类型(GRANT_POINTS/DEDUCT_POINTS/...) |
| target_user_id | BIGINT FK→t_user.id | 目标用户ID |
| amount | DECIMAL(18,2) | 操作金额 |
| remark | VARCHAR(256) | 操作备注 |
| ip_address | VARCHAR(45) | 操作IP |
| created_at | TIMESTAMP | 操作时间 |

---

## 四、后端设计 (Spring Boot)

### 4.1 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.x |
| ORM | MyBatis-Plus | 3.5+ |
| 数据库 | PostgreSQL | 15+ |
| 缓存 | Redis (Lettuce) | 7.x |
| 消息队列 | RabbitMQ | 3.x |
| 定时任务 | Spring @Scheduled + XXL-JOB (可选) | - |
| 认证 | Spring Security + JWT | - |
| 文档 | SpringDoc OpenAPI | - |
| 实时推送 | Spring WebSocket / STOMP | - |
| 价格源 | OkHttp 调用交易所API | - |

### 4.2 工程结构

```
btc-prediction-market/
├── pom.xml
├── src/main/java/com/btcmath/prediction/
│   ├── PredictionApplication.java
│   │
│   ├── common/                          # 公共模块
│   │   ├── config/
│   │   │   ├── SecurityConfig.java       # Spring Security配置
│   │   │   ├── RedisConfig.java         # Redis序列化配置
│   │   │   ├── RabbitMQConfig.java      # MQ交换机/队列定义
│   │   │   ├── WebSocketConfig.java     # WebSocket STOMP配置
│   │   │   └── MyBatisPlusConfig.java   # MP分页/填充配置
│   │   ├── exception/
│   │   │   ├── BizException.java        # 业务异常
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── result/
│   │   │   └── R.java                  # 统一响应体
│   │   ├── constant/
│   │   │   ├── MarketStatus.java        # 市场状态枚举
│   │   │   ├── BetDirection.java        # 押注方向枚举
│   │   │   ├── TransactionType.java     # 流水类型枚举
│   │   │   └── RedisKeys.java           # Redis Key常量
│   │   └── util/
│   │       ├── JwtUtil.java
│   │       └── PriceFormatUtil.java
│   │
│   ├── model/                           # 实体层
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── Wallet.java
│   │   │   ├── WalletTransaction.java
│   │   │   ├── MarketTemplate.java
│   │   │   ├── Market.java
│   │   │   ├── Bet.java
│   │   │   ├── PriceTick.java
│   │   │   └── AdminLog.java
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── BetRequest.java
│   │   │   ├── MarketListRequest.java
│   │   │   └── GrantPointsRequest.java
│   │   ├── vo/
│   │   │   ├── UserVO.java
│   │   │   ├── WalletVO.java
│   │   │   ├── MarketVO.java
│   │   │   ├── MarketDetailVO.java
│   │   │   ├── BetVO.java
│   │   │   ├── BetHistoryVO.java
│   │   │   └── AdminDashboardVO.java
│   │   └── enums/
│   │       ├── MarketStatusEnum.java
│   │       ├── BetDirectionEnum.java
│   │       ├── TransactionTypeEnum.java
│   │       └── UserRoleEnum.java
│   │
│   ├── mapper/                          # MyBatis Mapper
│   │   ├── UserMapper.java
│   │   ├── WalletMapper.java
│   │   ├── WalletTransactionMapper.java
│   │   ├── MarketTemplateMapper.java
│   │   ├── MarketMapper.java
│   │   ├── BetMapper.java
│   │   ├── PriceTickMapper.java
│   │   └── AdminLogMapper.java
│   │
│   ├── service/                         # 业务层
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   ├── WalletService.java           # ⭐ 积分核心
│   │   ├── MarketService.java           # ⭐ 市场核心
│   │   ├── BetService.java              # ⭐ 押注核心
│   │   ├── PriceService.java            # ⭐ 价格核心
│   │   ├── SettlementService.java       # ⭐ 结算核心
│   │   ├── AdminService.java            # ⭐ 管理后台
│   │   ├── MarketSchedulerService.java  # ⭐ 市场调度
│   │   └── WebSocketPushService.java    # 实时推送
│   │
│   ├── mq/                              # 消息队列消费者
│   │   ├── MarketCreateConsumer.java    # 创建市场
│   │   ├── MarketSettleConsumer.java    # 结算市场
│   │   └── PriceTickConsumer.java       # 价格快照
│   │
│   ├── controller/                       # 接口层
│   │   ├── AuthController.java
│   │   ├── MarketController.java
│   │   ├── BetController.java
│   │   ├── WalletController.java
│   │   ├── UserController.java
│   │   └── AdminController.java
│   │
│   └── websocket/                        # WebSocket
│       ├── PriceWebSocketHandler.java   # 价格推送
│       └── MarketWebSocketHandler.java # 市场状态推送
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── mapper/                          # XML映射文件
│   └── db/
│       └── schema.sql                   # 建表SQL
```

### 4.3 核心业务流程设计

#### 4.3.1 市场生命周期状态机

```
                    ┌──────────────┐
            定时创建 │   PENDING     │ 市场已创建，等待开始
            ────────→│   (即将开始)   │
                    └──────┬───────┘
                           │ 到达 start_time
                           ▼
                    ┌──────────────┐
     用户可以下注    │   ACTIVE     │ 开盘价已记录，接受押注
                    │   (进行中)     │
                    └──────┬───────┘
                           │ 到达 end_time
                           ▼
                    ┌──────────────┐
                    │   CLOSED     │ 停止接受押注
                    │   (已截止)     │
                    └──────┬───────┘
                           │ 结算完成
                           ▼
                    ┌──────────────┐
                    │   SETTLED    │ 已结算，积分已分发
                    │   (已结算)     │
                    └──────────────┘
```

#### 4.3.2 市场自动创建流程

```
MarketSchedulerService (Spring @Scheduled, 每分钟执行)
    │
    ├── 1. 查询所有启用的 MarketTemplate
    ├── 2. 对每个模板，计算下一个应创建的市场时间段
    ├── 3. 如果该时间段的市场尚未存在 → 发送 MQ 消息
    └── MQ Consumer:
         ├── 生成 market_no (如 BTC-5M-20260605-1600)
         ├── 插入 t_market (status=PENDING, open_price=NULL)
         └── 提前推送到前端(WebSocket)

到达 start_time 时:
    ├── 1. 获取当前 BTC 实时价格 → 记录 open_price
    ├── 2. 更新 market.status = ACTIVE
    └── 3. WebSocket 广播: 市场开始，展示开盘价

到达 end_time 时:
    ├── 1. 获取当前 BTC 实时价格 → 记录 close_price
    ├── 2. 判断结果: close >= open → UP, else → DOWN
    ├── 3. 更新 market.status = CLOSED, market.result = UP/DOWN
    └── 4. 发送结算 MQ 消息
```

#### 4.3.3 押注核心流程（⭐ 事务关键）

```
BetService.placeBet(userId, marketId, direction, amount)
    │
    ├── 1. 校验市场状态 (必须是 ACTIVE)
    ├── 2. 校验用户冻结余额 (frozen_balance + amount <= wallet.balance)
    ├── 3. 校验最小/最大押注额
    │
    ├── 4. 【Redis 分布式锁】key: bet:lock:user:{userId}
    │       防止同一用户并发押注导致超卖
    │
    ├── 5. 【数据库事务 BEGIN】@Transactional
    │   ├── 5a. 更新 wallet:
    │   │       balance = balance - amount
    │   │       frozen_balance = frozen_balance + amount
    │   │       WHERE id = ? AND version = ?  (乐观锁)
    │   │       失败 → 抛异常回滚
    │   │
    │   ├── 5b. 计算当前赔率:
    │   │       UP赔率 = 1 + (total_bet_down / total_bet_up) * (1 - fee)
    │   │       DOWN赔率 = 1 + (total_bet_up / total_bet_down) * (1 - fee)
    │   │
    │   ├── 5c. 插入 t_bet (direction, amount, odds)
    │   │
    │   ├── 5d. 更新 t_market:
    │   │       total_bet_up += amount (如果是UP)
    │   │       total_bet_down += amount (如果是DOWN)
    │   │       bet_count += 1
    │   │
    │   ├── 5e. 插入 t_wallet_transaction:
    │   │       type=BET_FREEZE, direction=支出, amount=amount
    │   │
    │   └── 5f. 缓存更新:
    │         Redis: market:{marketId} 的涨跌比例/总金额
    │
    ├── 6. 【释放锁】
    │
    └── 7. WebSocket 广播: 押注更新(新赔率、涨跌池金额)
```

#### 4.3.4 结算核心流程

```
SettlementService.settleMarket(marketId)
    │
    ├── 1. 加载市场 + 所有有效押注
    ├── 2. 确认 result (UP/DOWN)
    │
    ├── 3. 遍历赢方押注:
    │   for each winning_bet:
    │       ├── win_amount = bet.amount × bet.odds
    │       ├── 更新 bet.result = WIN, bet.win_amount = win_amount
    │       ├── 更新 wallet:
    │       │       frozen_balance -= bet.amount
    │       │       balance += win_amount
    │       └── 插入流水: BET_WIN, 收入, win_amount
    │
    ├── 4. 遍历输方押注:
    │   for each losing_bet:
    │       ├── 更新 bet.result = LOSS, bet.win_amount = 0
    │       ├── 更新 wallet:
    │       │       frozen_balance -= bet.amount
    │       └── 插入流水: BET_LOSS, 支出, bet.amount
    │
    ├── 5. 特殊处理: 如果某一方无人押注 → 退款
    │   for each bet:
    │       ├── win_amount = bet.amount (全额退回)
    │       └── 插入流水: REFUNDED, 收入, bet.amount
    │
    ├── 6. 更新 market.status = SETTLED
    │
    └── 7. WebSocket 广播: 结算完成
```

#### 4.3.5 积分赠送流程（管理员）

```
AdminService.grantPoints(adminId, targetUserId, amount, remark)
    │
    ├── 1. 权限校验 (role == ADMIN)
    ├── 2. 参数校验 (amount > 0, 用户存在)
    │
    ├── 3. 【数据库事务】
    │   ├── 3a. 更新 wallet.balance += amount
    │   ├── 3b. 插入流水: ADMIN_GRANT, 收入, amount
    │   └── 3c. 插入 t_admin_log (操作记录)
    │
    └── 4. WebSocket 通知用户: 收到积分
```

### 4.4 定时任务设计

| 任务 | 频率 | 说明 |
|------|------|------|
| MarketCreateTask | 每分钟 | 检查并创建即将开始的市场(提前5分钟创建) |
| MarketOpenTask | 每秒轮询 | 将到达 start_time 的 PENDING 市场切换为 ACTIVE，记录开盘价 |
| MarketCloseTask | 每秒轮询 | 将到达 end_time 的 ACTIVE 市场切换为 CLOSED，记录收盘价 |
| MarketSettleTask | 每秒轮询 | 结算 CLOSED 状态的市场 |
| PriceSyncTask | 每1-3秒 | 拉取交易所实时BTC价格，推送到Redis + WebSocket |

> **优化建议**: MarketOpen/Close/Settle 可改用延迟消息队列（RabbitMQ Delayed Message），减少轮询开销

### 4.5 Redis 缓存设计

```
# 1. 实时价格
price:BTC                     → String (最新价格, 如 "63052.71")
price:BTC:timestamp            → String (价格时间戳)

# 2. 市场实时数据缓存
market:detail:{marketId}       → Hash (total_bet_up, total_bet_down, bet_count, current_odds_up, current_odds_down)
market:list:active             → Set (当前进行中的市场ID集合, TTL=60s)

# 3. 押注分布式锁
bet:lock:user:{userId}         → String (SET NX EX 5, 防并发)

# 4. 用户会话
session:{token}                 → Hash (userId, username, role, TTL=24h)

# 5. 热门数据
leaderboard:daily               → ZSet (用户ID:当日盈亏积分, 用于排行榜)
leaderboard:weekly              → ZSet (用户ID:当周盈亏积分)
```

### 4.6 RabbitMQ 队列设计

```
Exchange: market.topic (Topic Exchange)

Queue                    RoutingKey              说明
─────────────────────────────────────────────────────────
market.create.queue      market.create           市场创建任务
market.open.queue        market.open             市场开盘任务
market.close.queue       market.close            市场收盘任务
market.settle.queue      market.settle           市场结算任务
market.settle.dlq        market.settle.dlq       结算失败死信队列

price.tick.queue         price.tick.btc          BTC价格快照
```

### 4.7 WebSocket 推送频道

```
/topic/price/BTC              # BTC实时价格推送 (每1-3秒)
    payload: { "price": 63052.71, "ts": 1717584000000 }

/topic/market/{marketId}       # 单个市场状态推送
    payload: {
        "marketId": 1001,
        "status": "ACTIVE",
        "openPrice": 63052.71,
        "totalUp": 15000,
        "totalDown": 12000,
        "oddsUp": 1.96,
        "oddsDown": 2.40,
        "betCount": 156,
        "countdown": 245      # 剩余秒数
    }

/topic/market/{marketId}/settle # 结算结果推送
    payload: {
        "marketId": 1001,
        "result": "UP",
        "closePrice": 63105.22,
        "profitRate": "+0.083%"
    }

/topic/user/{userId}/wallet    # 个人积分变动推送
    payload: {
        "type": "BET_WIN",
        "amount": 392.00,
        "balance": 5621.50,
        "message": "恭喜！BTC 5分钟涨跌 16:00-16:05 押涨赢得 392 积分"
    }
```

### 4.8 API 接口设计

#### 4.8.1 认证模块

```
POST /api/auth/register          # 注册
POST /api/auth/login             # 登录 → JWT token
POST /api/auth/refresh           # 刷新token
POST /api/auth/logout            # 登出
```

#### 4.8.2 市场模块

```
GET  /api/market/list            # 市场列表(支持筛选: asset, duration, status)
GET  /api/market/{id}            # 市场详情
GET  /api/market/{id}/chart      # 市场图表数据(价格走势 + 涨跌比例)
GET  /api/market/{id}/history    # 历史市场(同模板)
GET  /api/market/template/list   # 市场模板列表
```

#### 4.8.3 押注模块

```
POST /api/bet/place              # 下注 { marketId, direction: UP/DOWN, amount }
GET  /api/bet/my                 # 我的押注记录(分页)
GET  /api/bet/history            # 历史押注(已结算)
GET  /api/bet/market/{marketId}  # 某市场的所有押注(按方向聚合, 公开数据)
```

#### 4.8.4 钱包/积分模块

```
GET  /api/wallet                # 我的钱包 { balance, frozen_balance }
GET  /api/wallet/transactions   # 积分流水(分页, 支持筛选type)
```

#### 4.8.5 用户模块

```
GET  /api/user/profile           # 个人信息
PUT  /api/user/profile           # 更新信息(头像/昵称)
GET  /api/user/leaderboard       # 排行榜(日/周/总)
```

#### 4.8.6 管理后台模块

```
POST /api/admin/grant            # 赠送积分 { userId, amount, remark }
POST /api/admin/deduct           # 扣减积分 { userId, amount, remark }
GET  /api/admin/users            # 用户列表(分页, 支持搜索)
GET  /api/admin/users/{id}/detail # 用户详情(含流水)
GET  /api/admin/markets          # 市场管理列表
PUT  /api/admin/market/template  # 管理市场模板
GET  /api/admin/dashboard        # 仪表盘(总押注量/活跃用户/等)
GET  /api/admin/logs              # 操作日志
```

### 4.9 权限控制

```
/api/admin/** → 需要 ADMIN 角色
/api/bet/place → 需要登录 + 活跃用户
/api/wallet/** → 需要登录
其他GET接口 → 登录可选(未登录时部分数据不可见)
```

---

## 五、前端设计

### 5.1 页面列表与功能

#### 5.1.1 首页 / 市场列表页

```
┌──────────────────────────────────────────────────┐
│  [Logo]  BTC涨跌预测          [排行榜] [用户] [登录] │
├──────────────────────────────────────────────────┤
│                                                  │
│  📊 实时BTC价格面板                               │
│  ┌──────────────────────────────────────────────┐│
│  │  BTC/USD          ¥630,527.10                ││
│  │  24h +0.83%       ▲ +521.30                  ││
│  │  ┌─迷你K线图───────────────────────────────┐││
│  │  │  ~~~~~~/\/\/\__                          │││
│  │  └─────────────────────────────────────────┘││
│  └──────────────────────────────────────────────┘│
│                                                  │
│  🏆 当前活跃市场                                 │
│  ┌───────────┬───────────┬───────────┐          │
│  │ BTC 5min  │ BTC 15min │ BTC 1h   │          │
│  │ 16:00-05  │ 16:00-15  │ 16:00-17 │          │
│  │ ⬆1.96 ⬇2.40│ ⬆1.85 ⬇2.50│ ⬆1.70 ⬇2.80│ │
│  │ 池:27k积分 │ 池:45k积分│ 池:120k  │          │
│  │ [立即押注] │ [立即押注]│ [立即押注]│          │
│  └───────────┴───────────┴───────────┘          │
│                                                  │
│  📋 即将开始                                      │
│  ┌──────────────────────────────────────────┐  │
│  │ BTC 5min  16:05-10  距开始 02:15           │  │
│  │ BTC 5min  16:10-15  距开始 07:15           │  │
│  │ ...                                        │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  📅 历史结算                                     │
│  ┌──────────────────────────────────────────┐  │
│  │ 15:55-16:00  ⬆涨 ✅  开盘63052 → 63105  │  │
│  │ 15:50-15:55  ⬇跌 ✅  开盘63110 → 63080  │  │
│  │ ...                                        │  │
│  └──────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

#### 5.1.2 市场详情/交易页（核心页面）

```
┌──────────────────────────────────────────────────┐
│  [← 返回]   BTC 5分钟涨跌  16:00-16:05           │
│              开盘价: ¥630,527 | 收盘价: --       │
│              [分享] [收藏]                         │
├──────────────────────────────────────────────────┤
│                                                  │
│  ╔═══════════════════╗  ╔═══════════════════╗    │
│  ║                   ║  ║                    ║   │
│  ║   实时价格走势图    ║  ║   涨跌押注比例      ║   │
│  ║   (TradingView)   ║  ║   (环形图/进度条)    ║   │
│  ║                   ║  ║                    ║   │
│  ║   ←────→          ║  ║  ⬆ 涨 55%          ║   │
│  ║   时间窗口        ║  ║  ⬇ 跌 45%          ║   │
│  ║                   ║  ║                    ║   │
│  ╚═══════════════════╝  ╚═══════════════════╝    │
│                                                  │
│  ┌────── 倒计时 02:45 ──────┐                   │
│  │ ████████████░░░░░░░░░░  │                   │
│  └─────────────────────────┘                    │
│                                                  │
│  ┌────────────────┬────────────────┐            │
│  │   ⬆ 看涨 (UP)   │  ⬇ 看跌 (DOWN)  │            │
│  │                 │                │            │
│  │  赔率: 1.96×    │  赔率: 2.40×   │            │
│  │  总池: 15,000   │  总池: 12,000   │            │
│  │                 │                │            │
│  │  押注金额:       │  押注金额:       │            │
│  │  [  100  ] [-][+]│  [  100  ] [-][+]│            │
│  │                 │                │            │
│  │  可赢: 196积分  │  可赢: 240积分  │            │
│  │                 │                │            │
│  │  [🟢 确认押涨]  │  [🔴 确认押跌]  │            │
│  └────────────────┴────────────────┘            │
│                                                  │
│  ── 押注记录 ─────────────────────               │
│  用户A   ⬆涨 500积分  赔率1.96  2分钟前          │
│  用户B   ⬇跌 200积分  赔率2.40  3分钟前          │
│  ...                                             │
│                                                  │
│  ── 市场规则 ─────────────────────               │
│  • 开始时记录BTC价格，结束时再次记录               │
│  • 收盘价 ≥ 开盘价 = 涨方赢                       │
│  • 数据源: Binance BTC/USD                       │
└──────────────────────────────────────────────────┘
```

#### 5.1.3 用户中心

```
┌──────────────────────────────────────────────────┐
│  用户中心                                        │
├──────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────┐  │
│  │ 👤 头像  用户名                    [设置]   │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  💰 我的积分                                     │
│  ┌──────────────────────────────────────────┐  │
│  │  可用余额:         5,230.00 积分           │  │
│  │  冻结中:           1,200.00 积分           │  │
│  │  累计赢取:        12,450.00 积分           │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  📊 押注统计                                     │
│  ┌──────────────────────────────────────────┐  │
│  │  总场次: 86  胜率: 62%  盈利: +3,230      │  │
│  │  [本月] [本周] [今日]                     │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  📋 押注历史                                     │
│  ┌──────────────────────────────────────────┐  │
│  │ BTC 5min 16:00  ⬆涨 500积分 ✅赢 +480    │  │
│  │ BTC 15min 15:30 ⬇跌 200积分 ❌输 -200    │  │
│  │ BTC 5min 15:00  ⬆涨 100积分 ✅赢 +95     │  │
│  │ ...                                        │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  💳 积分流水                                     │
│  ┌──────────────────────────────────────────┐  │
│  │ +500  管理员赠送      06-05 14:20         │  │
│  │ +480  押涨获胜         06-05 16:05         │  │
│  │ -200  押跌亏损         06-05 15:45         │  │
│  │ +1000 管理员赠送      06-04 10:00         │  │
│  └──────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

#### 5.1.4 排行榜

```
┌──────────────────────────────────────────────────┐
│  🏆 排行榜   [日榜] [周榜] [总榜]                   │
├──────────────────────────────────────────────────┤
│  🥇  用户Alpha    今日盈利 +2,340积分  胜率71%     │
│  🥈  用户Beta     今日盈利 +1,890积分  胜率68%     │
│  🥉  用户Gamma    今日盈利 +1,560积分  胜率65%     │
│  4.  用户Delta    今日盈利 +1,200积分  胜率60%     │
│  5.  ...                                         │
│                                     [我的排名 #23]  │
└──────────────────────────────────────────────────┘
```

#### 5.1.5 管理后台

```
┌──────────────────────────────────────────────────┐
│  管理后台    [仪表盘] [用户管理] [市场管理] [日志]   │
├──────────────────────────────────────────────────┤
│                                                  │
│  📊 仪表盘                                       │
│  ┌────────┬────────┬────────┬────────┐           │
│  │总用户数 │今日活跃 │今日押注量│今日结算  │           │
│  │  1,234 │   456  │ 89,000 │ 52,000 │           │
│  └────────┴────────┴────────┴────────┘           │
│                                                  │
│  👤 用户管理                                      │
│  ┌──────────────────────────────────────────┐  │
│  │ 搜索: [用户名/ID____] [搜索]              │  │
│  │ 用户ID │ 用户名 │余额│冻结│今日盈亏│操作    │  │
│  │  001   │ Alpha │5230│1200│+2340 │[赠送]  │  │
│  │  002   │ Beta  │1890│ 300│+1890 │[赠送]  │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  🎁 赠送积分弹窗                                  │
│  ┌──────────────────────────────────────────┐  │
│  │  目标用户: [Alpha_________]              │  │
│  │  积分数量: [1000_________]               │  │
│  │  操作类型: ○ 赠送  ○ 扣减                │  │
│  │  备    注: [活动奖励______]               │  │
│  │              [取消]  [确认]                 │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  ⚙️ 市场模板管理                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ 模板名称    │资产 │时长  │状态  │操作       │  │
│  │ BTC 5min   │BTC  │300s │✅启用│[编辑][停用]│  │
│  │ BTC 15min  │BTC  │900s │✅启用│[编辑][停用]│  │
│  │ ETH 5min   │ETH  │300s │⬜停用│[编辑][启用]│  │
│  │ [+ 新增模板]                                │  │
│  └──────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

### 5.2 前端数据流

```
用户操作
  │
  ├── HTTP API 调用 (登录/押注/查询等)
  │     → RESTful: GET/POST/PUT
  │     ← JSON 响应
  │
  ├── WebSocket 连接 (实时数据)
  │     ← /topic/price/BTC         → 更新价格显示
  │     ← /topic/market/{id}       → 更新赔率/倒计时/池金额
  │     ← /topic/market/{id}/settle → 显示结算结果弹窗
  │     ← /topic/user/{id}/wallet  → 更新积分余额
  │
  └── 本地状态管理
        ├── 用户登录态 (localStorage / Vuex / Zustand)
        ├── 市场列表缓存
        ├── 当前市场状态
        └── 押注确认状态
```

### 5.3 关键交互流程

#### 押注流程 (前端)

```
1. 用户选择方向 (UP/DOWN)
2. 输入/调整押注金额
3. 前端实时计算: 可赢 = amount × odds
4. 点击 "确认押注"
5. 前端校验: 已登录 + 余额充足
6. POST /api/bet/place
7. 成功 → Toast提示 + 更新UI余额 + 弹出押注成功确认
8. 失败 → 显示错误原因(余额不足/市场已关闭/网络错误)
9. WebSocket推送新赔率 → 自动更新显示
```

#### 结算通知流程 (前端)

```
WebSocket 收到 /topic/market/{id}/settle
    │
    ├── 如果用户有该市场的押注:
    │   ├── WIN → 弹出🎉庆祝动画 + "恭喜赢得 XXX 积分"
    │   └── LOSS → 弹出冷静提示 + "很遗憾，输了 XXX 积分"
    │
    ├── 更新用户积分余额 (header显示)
    │
    └── 市场卡片状态更新 → "已结算: ⬆涨"
```

---

## 六、非功能性设计

### 6.1 安全

- JWT认证，Access Token有效期2小时，Refresh Token 7天
- 管理员接口二次验证（可选）
- 押注接口限流：单用户每秒最多1次押注
- SQL注入防护：MyBatis参数化查询
- XSS防护：前端输入转义

### 6.2 并发与一致性

- **积分扣款**: Redis分布式锁 + 数据库乐观锁双保险
- **市场结算**: 串行处理，同一市场同时只有一个结算线程
- **价格记录**: 最终一致性，允许秒级延迟

### 6.3 监控与告警

- 价格源异常告警（连续3次拉取失败）
- 结算延迟告警（市场关闭后30秒未完成结算）
- 积分余额负数告警（理论上不应出现）
- WebSocket连接数监控

---

## 七、开发阶段建议

| 阶段 | 时间 | 交付内容 |
|------|------|---------|
| P0-MVP | 2-3周 | 用户注册登录 + 积分体系 + BTC价格接入 + 市场创建/结算 + 基本押注 + 管理后台赠送积分 |
| P1-体验 | 1-2周 | 实时图表 + WebSocket推送 + 倒计时 + 排行榜 + 押注记录 |
| P2-社交 | 1周 | 分享功能 + 评论区(可选) + 多资产(ETH/SOL) |
| P3-运营 | 持续 | 数据统计大盘 + 用户行为分析 + 活动系统 |

---

*文档结束 - 如需对某个模块深入展开或开始编码，随时告诉我 🐾*
