# Stock Portfolio Tracker — Backend

> Version 1.0.0 · Spring Boot 3.4.4 · Java 17

REST API backend for the Stock Portfolio Tracker. Manages user portfolios and stock positions, fetches live market data from the TwelveData API, and caches quotes in PostgreSQL.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.4.4 |
| Language | Java 17 |
| Database | PostgreSQL 17 |
| ORM | Spring Data JPA / Hibernate 6 |
| HTTP client | Spring `RestTemplate` |
| Market data | TwelveData API |
| Logging | SLF4J / Logback |
| Build | Maven |
| Containerisation | Docker Compose |

---

## Project Structure

```
src/main/java/com/yassine/portfolio_tracker/
├── config/           # Spring beans, CORS, @ConfigurationProperties
├── controller/       # REST controllers + GlobalExceptionHandler
├── service/          # Business logic (PortfolioService, StockService)
├── repository/       # Spring Data JPA interfaces
├── model/            # JPA entities (Portfolio, Stock, StockCache)
└── dto/              # API response DTOs (StockQuote)
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker + Docker Compose (for PostgreSQL)
- A free [TwelveData](https://twelvedata.com) API key

---

## Getting Started

**1. Clone and configure environment variables**

Copy the example env file and fill in your values:

```bash
cp .env.example .env
```

```env
DB_URL=jdbc:postgresql://localhost:5433/portfolio_tracker
DB_USERNAME=yassine
DB_PASSWORD=your_db_password
DDL_AUTO=update
TWELVEDATA_API_KEY=your_api_key
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

**2. Start PostgreSQL**

```bash
docker compose up -d
```

**3. Run the backend**

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/user/{username}` | Get portfolio for a user |
| `GET` | `/api/user/stocks` | Get all stocks across portfolios |
| `POST` | `/api/user/stock/{username}` | Buy / add a stock to portfolio |
| `DELETE` | `/api/stocks/{id}` | Remove a stock from portfolio |
| `GET` | `/api/stocks/all` | Fetch live quotes for tracked symbols |
| `GET` | `/api/stocks/top-performer` | Get the best-performing stock today |

---

## Environment Variables Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5433/portfolio_tracker` | JDBC connection URL |
| `DB_USERNAME` | `yassine` | Database user |
| `DB_PASSWORD` | — | Database password (**required**) |
| `DDL_AUTO` | `update` | Hibernate DDL mode (`validate` recommended for prod) |
| `TWELVEDATA_API_KEY` | — | TwelveData API key (**required**) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed CORS origin(s) |

---

## Author

Developed by **Yassine Azzouz**
