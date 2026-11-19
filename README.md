# JewelERP — Spring Boot + React

Jewellery showroom management: daily metal rates, product masters, piece-level stock with HUID and barcode, POS billing with making/wastage/GST, A4 and 80mm invoices, customers with loyalty points, and old gold exchange.

- `backend/` — Java 17+, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, MySQL
- `frontend/` — React 18 + Vite

The pricing engine (`backend/src/main/java/com/jewelerp/pricing/JewelleryPricing.java`) is a line-for-line port of the original PHP calculator, cross-checked on 3,000 random bills.

## Prerequisites

- JDK 17 or newer
- Maven 3.9+ (or use IntelliJ's bundled Maven)
- Node.js 18+
- MySQL 8 (XAMPP's MySQL/MariaDB works)

## 1. Database

Start MySQL. The database `jewel_erp` is created automatically on first run, and tables are created by Hibernate.

## 2. Backend

```bash
cd backend
cp .env.example .env
```

Edit `.env`: set `DB_PASSWORD` if your MySQL root user has one, and replace `JWT_SECRET` with any random string of 32+ characters.

```bash
mvn spring-boot:run
```

The API starts on http://localhost:8080. On the first run it loads demo data (branches, staff, rates, products, stock, customers, one sample invoice). Set `SEED_DEMO_DATA=false` in `.env` to start empty.

Run the tests (uses an in-memory H2 database, MySQL not needed):

```bash
mvn test
```

## 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173.

## Demo logins

All passwords are `password`.

| Role | Email | Can do |
| --- | --- | --- |
| Admin | admin@jewelerp.in | Everything, including audit logs |
| Manager | manager@jewelerp.in | Everything except audit logs; approves rates and old gold, cancels invoices |
| Cashier | cashier@jewelerp.in | POS, customers, loyalty, old gold intake |
| Salesperson | ananya@jewelerp.in | POS and customers |
| Artisan | rahim@jewelerp.in | Dashboard, rates, stock and invoices (view); can submit rate changes for approval |

## API overview

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/login`, `GET /api/auth/me` |
| Dashboard | `GET /api/dashboard` |
| Rates | `GET/POST /api/rates`, `POST /api/rates/{id}/approve`, `DELETE /api/rates/{id}`, `POST /api/rates/sync` |
| Products & stock | `GET/POST /api/products`, `GET/POST /api/inventory`, `GET /api/inventory/{id}/movements` |
| POS | `GET /api/pos/catalog`, `POST /api/pos/calculate`, `POST /api/pos/checkout` |
| Invoices | `GET /api/invoices`, `GET /api/invoices/{id}`, `POST /api/invoices/{id}/cancel` |
| Customers | `GET/POST /api/customers`, `GET /api/customers/{id}`, `POST /api/customers/{id}/loyalty` |
| Old gold | `GET/POST /api/old-gold`, `POST /api/old-gold/calculate`, `/approve`, `/refinery` |

## Not built yet (from the original PHP version)

Gold savings schemes, karigar workshop / custom orders / repairs, purchases and suppliers, accounts and expenses, staff attendance and payroll, stock transfers between branches, hallmark and diamond registers, label printing, marketing campaigns, sales returns.

## Credits

Based on [webotapp-academy/jewellery-software](https://github.com/webotapp-academy/jewellery-software) (MIT licence).
