# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BTC 涨跌预测市场 (BTC Price Prediction Market) is a web-based prediction market platform where users bet on BTC price movements (UP/DOWN) within specified time windows (1/5/15 minutes). The platform uses virtual points and automatically settles bets based on real-time prices from Binance.

**Tech Stack:**
- Backend: Spring Boot 3.5.14 (Java 17), Spring Data JPA, H2 in-memory database, JWT authentication
- Frontend: Vue 3.4.x, Vue Router 4.3.x, Vite 5.2.x, ECharts 6.1.0

## Development Commands

### Backend
```bash
# Run development server
./mvnw spring-boot:run

# Build
./mvnw clean package

# Run tests
./mvnw test
# Run specific test class
./mvnw test -Dtest=BtcMarketIntegrationTest

# H2 Console (when running): http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:btcmarket
```

### Frontend
```bash
cd frontend
npm install          # First time setup
npm run dev          # Development server (http://localhost:5173)
npm run build        # Production build
npm run preview       # Preview production build
```

## Architecture Overview

### Market Lifecycle (Core Domain)

The platform's core is the automated market lifecycle managed by `MarketSchedulerService`:

```
PENDING ──(start time reached, record open price)──> ACTIVE ──(end time reached, record close price)──> CLOSED ──(settlement complete)──> SETTLED
```

**Three scheduled tasks drive this:**
1. `createUpcomingMarkets()` - Every 30s, creates future markets based on templates
2. `openMarkets()` - Every 2s, activates markets whose start time arrived
3. `closeAndSettleMarkets()` - Every 2s, closes and settles markets

### Key Services

| Service | Responsibility |
|---------|----------------|
| `MarketService` | Market CRUD, listing, template management |
| `BetService` | Bet placement, validation, balance freezing |
| `SettlementService` | Market settlement, winner/loser calculation, payout distribution |
| `WalletService` | Balance management, transaction recording |
| `PriceService` | BTC price fetching from Binance with simulated fallback |

### Settlement Logic

- Closing price ≥ Opening price → UP wins
- Closing price < Opening price → DOWN wins
- If only one side has bets → All bets refunded
- Otherwise → Winners split losers' pool proportionally to their bet amounts

### Authentication

- JWT-based stateless authentication (12-hour expiration)
- `JwtAuthFilter` intercepts requests to protected endpoints
- BCrypt password hashing
- Role-based access: USER vs ADMIN
- User context stored in thread-local `UserContext`

### Package Structure

```
com.lazybeartoby.btcmarket/
├── common/           # Shared components (config, constants, exception handling, security)
├── controller/       # REST endpoints
├── service/          # Business logic
├── model/
│   ├── entity/       # JPA entities
│   ├── dto/          # Request DTOs
│   └── vo/           # Response VOs
├── repository/       # JPA repositories
└── init/             # Startup data initialization
```

### Frontend Structure

```
frontend/src/
├── views/            # Page components (Login, Markets, MarketDetail, UserCenter, Admin)
├── App.vue           # Root component
├── main.js           # Entry point
├── router.js         # Vue Router config with auth guards
├── store.js          # Global reactive state
├── api.js            # API client with token injection
└── style.css         # Global styles
```

## Important Design Constraints

1. **In-Memory Database**: H2 database loses data on restart. For persistence, switch to PostgreSQL or file-based H2.

2. **Price Fallback**: `PriceService` falls back to simulated prices if Binance API fails (2s timeout). This ensures demo availability even with network issues.

3. **Wallet Concurrency**: `Wallet` entity uses `@Version` for optimistic locking to prevent race conditions on balance updates.

4. **Frozen Balance**: When placing bets, points are frozen (`Wallet.frozenBalance`) and only released/reallocated after settlement.

5. **No Message Queue**: Unlike the production design (DESIGN.md), this demo uses synchronous processing. Scheduled tasks directly call services rather than publishing to RabbitMQ.

## Demo Accounts

- Admin: `admin` / `admin123` (1,000,000 initial points)
- User: `demo` / `demo123` (10,000 initial points)

These are created by `DataInitializer` on startup.

## Configuration

Key settings in `application.yml`:
- Server port: 8080
- JWT expiration: 720 minutes (12 hours)
- Min/max bet: 1 / 100,000 points
- Price fetch interval: 3000ms
- Market creation window: 10 minutes ahead

## Testing

Integration tests in `BtcMarketIntegrationTest.java` cover:
- Registration, login, and authentication flows
- Market lifecycle (creation → activation → settlement)
- Bet placement and validation
- Wallet balance consistency throughout transactions
- Settlement with various scenarios (single-side bets, proportional distribution)
