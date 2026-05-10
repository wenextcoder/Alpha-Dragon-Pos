# Alpha Dragon POS — Project Brief

**Version:** 3.0  
**Platform:** Android (Native — Kotlin)  
**Distribution:** APK sideload (no Play Store)  
**Architecture:** Device-local only — fully offline, no cloud sync  
**Branding:** App name `Alpha Dragon` · Colors: red and black

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Core Principles](#2-core-principles)
3. [System Architecture](#3-system-architecture)
4. [First Launch & Admin Setup Flow](#4-first-launch--admin-setup-flow)
5. [Authentication System](#5-authentication-system)
6. [Module Specifications](#6-module-specifications)
7. [Customer Data on Orders](#7-customer-data-on-orders)
8. [Data Backup & Restore System](#8-data-backup--restore-system)
9. [Merchant API Integration](#9-merchant-api-integration)
10. [Payment Terminal SDK Integration](#10-payment-terminal-sdk-integration)
11. [Security Requirements](#11-security-requirements)
12. [Tech Stack](#12-tech-stack)
13. [Database Schema](#13-database-schema)
14. [OTA Update System](#14-ota-update-system)
15. [Development Phases](#15-development-phases)
16. [Coding Standards](#16-coding-standards)
17. [Testing Requirements](#17-testing-requirements)
18. [Folder Structure](#18-folder-structure)

---

## 1. Project Overview

Alpha Dragon is a production-grade, fully offline Android POS (Point of Sale) system designed for retail and hospitality environments. Every function operates entirely on the local device. There is no internet dependency, no cloud server, no remote sync, and no cross-device data sharing — by design.

Each physical device is a standalone, self-contained POS unit for a single shop. The data on that device belongs to that device only. A shop cannot access its data from another device, and no other device can access its data.

There is a single user role: **Admin**. The Admin has full access to everything. No other login role exists.

Customer data (name, phone, email) is optionally attached to an order at the time of sale for the purpose of receipt printing and order reference. There is no customer-facing screen and no customer account system.

Data portability is handled through a secure manual backup and restore system — encrypted export/import with images bundled — which the Admin controls entirely.

---

## 2. Core Principles

- **Device-local only.** All data lives on the device. No internet required for any POS operation. No server, no sync, no cloud.
- **One device, one shop.** The app is bound to the device it is installed on. Data cannot be transferred to or accessed from another device except via explicit Admin-initiated backup/restore.
- **Admin only.** One role. The Admin is the sole operator of the app. No login screen for any other user type.
- **Security-first.** All data is encrypted at rest. The database cannot be read outside the app. The APK is bound to the device hardware.
- **First-run setup.** On first launch, the app forces Admin account creation before anything else is accessible.
- **Clean architecture.** UI, business logic, and data layers are strictly separated for maintainability and testability.
- **Production-ready.** Code written to production standards from phase one. No shortcuts that require rewrites later.

---

## 3. System Architecture

The entire system is self-contained within the Android APK. There is no backend server.

```
┌──────────────────────────────────────────────────────────────┐
│                     Alpha Dragon POS APK                      │
│                                                              │
│   ┌──────────────┐    ┌──────────────┐    ┌───────────────┐  │
│   │   UI Layer   │    │ Domain Layer │    │  Data Layer   │  │
│   │  (Compose)   │◄──►│ (Use Cases)  │◄──►│ (Room/SQLite) │  │
│   └──────────────┘    └──────────────┘    └───────────────┘  │
│                                                  │            │
│                              ┌───────────────────┘            │
│                              │                                │
│               ┌──────────────▼──────────────┐                │
│               │       Security Layer         │                │
│               │  SQLCipher · Keystore · PIN  │                │
│               │  Device binding · Root check │                │
│               └─────────────────────────────┘                │
│                                                              │
│   ┌──────────────────────────────────────────────────────┐   │
│   │                 Merchant API Layer                    │   │
│   │   Worldpay · Fiserv · First Data · Clover            │   │
│   │   Cardnet · Elavon · Manya                           │   │
│   │   (outbound only — payment processing calls)         │   │
│   └──────────────────────────────────────────────────────┘   │
│                                                              │
│   ┌──────────────────────────────────────────────────────┐   │
│   │               Terminal Driver Layer                   │   │
│   │   Ingenico DX8000/DX4000 · Clover Flex               │   │
│   │   Castles S1000 · Castles S1F2                       │   │
│   └──────────────────────────────────────────────────────┘   │
│                                                              │
│   ┌──────────────────────────────────────────────────────┐   │
│   │                 Backup / Restore                      │   │
│   │   Export encrypted .adb file to local storage        │   │
│   │   Import encrypted .adb file (Admin only)            │   │
│   │   Database + images bundled in one file              │   │
│   └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### App layers

| Layer | Responsibility |
|-------|---------------|
| UI (Compose) | Render screens, receive input, observe ViewModel state |
| ViewModel | Manage UI state, delegate to use cases, no business logic |
| Domain (Use Cases) | All business logic — pure Kotlin, zero Android dependencies |
| Repository (interface) | Abstracts data access — defined in domain layer |
| Repository (impl) | Room/SQLite implementation — defined in data layer |
| Local Data | Room ORM + SQLCipher encrypted SQLite |
| Security | Android Keystore, device binding, PIN management, root detection |
| Merchant layer | One isolated module per merchant payment API |
| Terminal layer | One isolated driver per payment terminal model |
| Backup | Encrypted export/import of database and media |

---

## 4. First Launch & Admin Setup Flow

On first launch (detected by absence of any Admin record in the local database), the app must:

1. **Block the main POS interface completely.** No navigation, no menu, nothing is accessible until setup is done.
2. **Show a full-screen "Welcome / Setup" screen** with Alpha Dragon branding.
3. **Force Admin registration** by collecting:
   - Shop name
   - Admin full name
   - Admin username (alphanumeric, no spaces)
   - Admin PIN (6 digits, entered twice for confirmation)
   - Optional: shop logo image (can be skipped and added later in Settings)
4. **On submission:**
   - Validate all fields
   - Hash the PIN using bcrypt (cost factor 12) — never store plain PIN
   - Save Admin account to encrypted local database
   - Save shop profile to local database
   - Generate and store device fingerprint in Android Keystore
   - Set `setup_complete = true` in `app_config`
5. **Transition to the Login screen** — do not auto-login after setup. Require an explicit login so the Admin experiences the login flow from the start.

### Subsequent launches

```
App launch
    │
    ▼
Check: setup_complete in app_config?
    │
    ├── No  → First Launch Setup Screen
    │
    └── Yes → Login Screen
                  │
                  ▼
            Admin enters username + PIN
                  │
                  ▼
            Main POS Interface (full access)
```

### Login screen requirements

- Display shop name and optional shop logo at top
- Username field (text input)
- PIN field (6-digit, masked, numeric keyboard shown automatically)
- "Login" button
- No "forgot PIN" on the login screen — PIN reset requires being logged in as Admin
- Failed login: lock after 5 consecutive failures for 30 seconds, log each attempt
- No biometric login — PIN only

---

## 5. Authentication System

All authentication is entirely local. No network call is ever made for any auth operation.

### PIN storage

- PINs hashed using **bcrypt** (cost factor 12) before storage
- Hash stored in the encrypted SQLite database
- Plain PINs are never logged, displayed, or stored anywhere
- On login: entered PIN is bcrypt-compared against stored hash

### Session management

- On successful login, an in-memory session token is created (UUID + timestamp)
- Session stored in memory only — never persisted to disk
- Session expires after **8 hours of inactivity** (configurable in Settings, min 1 hour, max 24 hours)
- On session expiry: return to login screen; current cart state is preserved
- On app backgrounded for more than 5 minutes: require PIN re-entry on return (configurable in Settings)
- On app force-close: session cleared, login required on reopen

### PIN rules

- Must be exactly 6 digits (numeric only)
- Cannot be all same digit (e.g. `111111`)
- Cannot be sequential ascending or descending (e.g. `123456`, `654321`)
- Enforced on creation and on change

### PIN change

- Admin can change their own PIN: requires current PIN + new PIN (entered twice)
- Accessible from Settings while logged in

### Auth audit trail

All auth events logged to `audit_log`:
- Successful login (timestamp)
- Failed login attempt (timestamp)
- Account lockout triggered
- PIN changed (timestamp)
- Session expired

---

## 6. Module Specifications

### 6.1 Transaction module

**Sale flow:**
1. Admin selects products from product grid or searches by name/SKU/barcode scan
2. Items added to cart with quantity and line-item pricing
3. Discounts applied per item or on cart total (if enabled in Settings)
4. System calculates subtotal, tax (per product/category rule), discount, and grand total
5. Optionally: add customer name/phone/email to the order for receipt (see section 7)
6. Admin selects payment method: Cash, Card (terminal), or Split (cash + card)
7. If card: app checks internet connectivity
   - Offline → clear message shown, cash offered as alternative
   - Online → communicates with paired terminal SDK
8. Terminal handles card interaction, returns approval/decline
9. On approval: transaction written to local database immediately
10. Optional: print receipt via Bluetooth printer or display on-screen
11. Cart cleared, ready for next transaction

**Refund flow:**
1. Admin opens transaction history, finds original transaction by ID or date
2. Selects full or partial refund amount
3. Confirmation prompt (PIN re-entry for refunds, configurable in Settings)
4. Refund sent to merchant API (requires internet) or flagged as "pending refund" if offline
5. Refund record created and linked to original transaction ID

**Void flow:**
- Available on same-day transactions only (before end-of-day close)
- Requires PIN confirmation
- Void record linked to original transaction

**Transaction record fields:**

```
transaction_id        TEXT    UUID, primary key
device_fp             TEXT    device fingerprint at time of transaction
timestamp             INTEGER Unix timestamp UTC
customer_name         TEXT    optional — for receipt only
customer_phone        TEXT    optional — for receipt only
customer_email        TEXT    optional — for receipt only
items                 TEXT    JSON array of line items
subtotal              REAL
tax_total             REAL
discount_total        REAL    default 0
total                 REAL
payment_method        TEXT    ENUM: cash | card | split
merchant_id           TEXT    which merchant processed payment
terminal_id           TEXT    which terminal was used
approval_code         TEXT    from merchant on card approval
status                TEXT    ENUM: completed | refunded | partial_refund | voided | pending_refund
notes                 TEXT    optional admin note
receipt_ref           TEXT    optional printed receipt reference
```

### 6.2 Product management module

- Add, edit, delete products
- Product categories (max 2 levels: category → subcategory)
- Per-product and per-category tax rate override
- Barcode / SKU field
- Product image (stored locally, linked to product record)
- Stock quantity tracking — optional, toggleable per product
- Low-stock alert threshold (set in Settings, shown as badge on product in sale screen)
- Product active/inactive status (inactive products hidden from sale screen)
- Display/sort order per category
- Bulk import via CSV file

### 6.3 Reports module

All reports generated from local database — no internet required.

**Available reports:**
- Daily sales summary — total revenue, transaction count, average basket, cash vs card split
- Transaction log — filterable by date range, payment method, status
- Refund report — all refunds in selected period
- Product sales breakdown — units sold and revenue per product
- Category sales breakdown
- Hourly sales heatmap (transaction volume by hour of day)
- Tax summary — total tax collected per period (for accounting)
- End-of-day report — printable/exportable daily summary

**Export:** CSV saved to device Downloads folder

### 6.4 Settings module

- Shop profile: name, logo, address, currency, timezone
- Merchant API configuration (per merchant, keys stored in Android Keystore)
- Terminal pairing and configuration
- Tax rules — global default rate, per-category overrides
- Receipt template — shop name, logo, footer message, field visibility
- Session timeout duration (1–24 hours)
- PIN re-entry delay after background (1–30 minutes)
- PIN re-entry required for refunds toggle
- Low-stock alert threshold (global default)
- Backup reminder interval (default 7 days)
- App display: language, date format, currency symbol format
- Audit log viewer + clear audit log (the clear action is always logged first)
- Admin PIN change

---

## 7. Customer Data on Orders

There is no customer account system, no customer login, and no customer-facing screen.

Customer data is **optionally** attached to a transaction at the point of sale for the sole purpose of:
- Printing on the receipt (name, phone, and/or email)
- Identifying the order in transaction history (searchable by customer name)

### How it works

On the sale/cart screen, there is an optional "Add customer" field. Admin can:
- Type a customer name (and optionally phone/email)
- Or search previously entered customer names from past orders (autocomplete from `customer_name` field in `transactions` table — no separate customer table needed)

The customer data is stored directly on the transaction record. There is no standalone customer record, no customer profile, no customer history view beyond searching transaction logs.

### Customer fields on a transaction

```
customer_name   TEXT    optional free-text name
customer_phone  TEXT    optional
customer_email  TEXT    optional
```

### Receipt output with customer data

When a transaction has customer data, the receipt prints:

```
─────────────────────────
Alpha Dragon POS
[Shop Name]
─────────────────────────
Customer: John Smith
Phone:    +44 7700 900000
─────────────────────────
[line items]
─────────────────────────
Total: £24.50
Payment: Card
Approval: 123456
─────────────────────────
[footer message]
─────────────────────────
```

If no customer data is entered, the customer section is simply omitted from the receipt.

---

## 8. Data Backup & Restore System

Since there is no cloud sync, the backup system is the only mechanism for data safety. It must be robust, secure, and simple to use.

### Backup (export)

**What is backed up:**
- Full SQLite database (all transactions, products, settings, Admin account)
- All product images and shop logo stored locally
- App configuration

**Export format:**
- A single encrypted `.adb` file (Alpha Dragon Backup)
- AES-256 encrypted using a backup password the Admin sets at export time
- The backup password is separate from the Admin PIN — Admin must remember it to restore
- Saved to Admin-chosen location via Android file picker or share sheet (Downloads, USB, SD card, or shared via Google Drive / email / etc.)

**Export process:**
1. Admin → Settings → Backup & Restore → Export Backup
2. Admin enters and confirms a backup password
3. App generates the `.adb` file (database + images bundled and AES-256 encrypted)
4. File size and SHA-256 checksum displayed to Admin for verification
5. Admin selects save location (file picker / share sheet)
6. Export timestamp and filename recorded in `app_config`

**Backup reminder:**
- If no backup exported in more than N days (default 7, Admin-configurable): show a non-dismissible reminder banner on the Admin home screen
- Admin can snooze for 1 day

### Restore (import)

**Restore process:**
1. Admin → Settings → Backup & Restore → Restore from Backup
2. Warning: "Restoring will replace ALL current data on this device. This cannot be undone."
3. Admin confirms with their current Admin PIN
4. Admin selects the `.adb` file via Android file picker
5. Admin enters the backup password used at export time
6. App verifies SHA-256 checksum and decrypts
7. On success: current database replaced, images restored, app restarts to login screen
8. On failure (wrong password or corrupt file): current data is untouched, error displayed clearly

**Device transfer:**
- Backup file is not locked to the original device — it is protected by the backup password
- If a device is lost or replaced, Admin restores onto a new device using the file + password
- After restore on a new device, device fingerprint is regenerated automatically

### Image backup options

- Default: database + images bundled in one `.adb` file
- Optional "Database only": exports without images (smaller file, faster)

---

## 9. Merchant API Integration

Each merchant is a separate, isolated Gradle module implementing a shared `PaymentProvider` interface. The core app has no direct knowledge of any merchant SDK — it only calls the interface. Adding a new merchant requires only a new module with zero changes to core app code.

### PaymentProvider interface

```kotlin
interface PaymentProvider {
    val merchantId: String
    val displayName: String

    suspend fun initializeSDK(config: MerchantConfig): Result<Unit>
    suspend fun processPayment(request: PaymentRequest): Result<PaymentResponse>
    suspend fun processRefund(request: RefundRequest): Result<RefundResponse>
    suspend fun voidTransaction(transactionId: String): Result<VoidResponse>
    suspend fun getTransactionStatus(transactionId: String): Result<TransactionStatus>
    fun isAvailableOffline(): Boolean
}
```

### MerchantConfig

```kotlin
data class MerchantConfig(
    val merchantId: String,
    val apiKey: String,           // loaded from Android Keystore at runtime — never in SQLite
    val environment: Environment,  // SANDBOX | PRODUCTION
    val terminalId: String,
    val currencyCode: String,     // ISO 4217 e.g. "GBP"
    val countryCode: String       // ISO 3166-1 alpha-2 e.g. "GB"
)
```

### Merchant modules

| Module | Merchant | Integration type | Developer portal |
|--------|----------|-----------------|-----------------|
| `:merchant:worldpay` | Worldpay | REST API + Android SDK | developer.worldpay.com |
| `:merchant:fiserv` | Fiserv | REST API | developer.fiserv.com |
| `:merchant:firstdata` | First Data | REST API | developer.fiserv.com/product/FirstDataGateway |
| `:merchant:clover` | Clover | Android native SDK | developer.clover.com |
| `:merchant:cardnet` | Cardnet | REST API | Contact Cardnet UK directly |
| `:merchant:elavon` | Elavon | Converge REST + SDK | developer.elavon.com |
| `:merchant:manya` | Manya | REST API | Verify on approval |

### Offline behavior for card payments

Card payments require internet to reach the merchant API. If offline:
- App detects no connectivity before initiating the terminal
- Message shown: "No internet connection. Card payments unavailable. Accept cash or retry when connected."
- Cash is always available offline
- No silent failures

### Developer approval checklist (apply day one)

- [ ] Worldpay developer account
- [ ] Fiserv developer account
- [ ] First Data / Fiserv Gateway developer account
- [ ] Clover developer account
- [ ] Cardnet UK merchant portal
- [ ] Elavon developer account
- [ ] Manya (on approval)

All integrations built and tested in sandbox before any production credentials are used.

---

## 10. Payment Terminal SDK Integration

Each terminal is a separate, isolated Gradle module implementing a shared `TerminalDriver` interface. The core app calls only the interface — no direct terminal SDK imports in app code.

### TerminalDriver interface

```kotlin
interface TerminalDriver {
    val terminalModel: String
    val connectionType: ConnectionType  // USB | BLUETOOTH | TCPIP | BUILTIN

    suspend fun connect(config: TerminalConfig): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun startPayment(request: TerminalPaymentRequest): Result<TerminalPaymentResult>
    suspend fun startRefund(request: TerminalRefundRequest): Result<TerminalRefundResult>
    suspend fun cancelTransaction(): Result<Unit>
    fun getConnectionStatus(): TerminalConnectionStatus
    fun isConnected(): Boolean
}
```

### Terminal modules

| Module | Terminal(s) | OS | SDK |
|--------|-------------|----|-----|
| `:terminal:ingenico` | DX8000 + DX4000 | Android 10 / TETRA OS | Telium+ SDK |
| `:terminal:clover` | Clover Flex | Android | Clover SDK (built-in) |
| `:terminal:castles` | S1000 + S1F2 | Android | CTOS SDK |

### Terminal payment flow

```
Admin selects "Pay by card"
    │
    ▼
Check internet connectivity
    │
    ├── Offline → show message, offer cash only
    │
    └── Online → TerminalManager.getActiveDriver()
                    │
                    ▼
                Driver sends payment command to terminal
                    │
                    ▼
                Terminal handles EMV / NFC / MSR card interaction
                    │
                    ▼
                Driver returns TerminalPaymentResult
                    │
                    ├── Approved → write transaction, clear cart
                    │
                    └── Declined / Error → show message, offer retry or cash
```

---

## 11. Security Requirements

### Database encryption

- SQLite encrypted with **SQLCipher (AES-256)**
- Encryption key derived from device hardware fingerprint component + a key stored in Android Keystore
- Database file cannot be read by any external tool even with root access
- Key never stored in plain text — only in Android Keystore

### Android Keystore

Used for all sensitive key material:
- Database encryption key component
- All merchant API keys (one Keystore entry per merchant)
- Backup encryption key component

Keys are hardware-backed on devices with a secure element (Android 6+). Cannot be extracted even via ADB or APK decompilation.

### Device binding

- Device fingerprint generated on first launch: SHA-256 hash of `ANDROID_ID` + hardware serial + build fingerprint
- Stored in both the local database and Android Keystore
- Verified on every app launch — fingerprint recomputed and compared
- On mismatch: lock screen shown, event logged, Admin PIN required to proceed

### Root and tamper detection

- **RootBeer** library integrated
- Rooted device detected: warning shown to Admin on first login, event logged to audit trail
- Admin can acknowledge and continue (some legitimate POS devices have modified firmware)
- In production build: debuggable disabled, R8 obfuscation enabled

### Network security

- All merchant API calls over HTTPS / TLS 1.2 minimum
- **Certificate pinning** via OkHttp `CertificatePinner` on all merchant API clients
- `network_security_config.xml` blocks all plain HTTP traffic
- No analytics SDK or crash-reporting SDK that transmits data off-device

### Data leak prevention

- `android:allowBackup="false"` in manifest — prevents ADB backup from extracting app data
- `android:exported="false"` on all components that do not require external access
- `FLAG_SECURE` on login, PIN entry, Settings, and merchant config screens (prevents screenshots and screen recording)
- No `Log.*` calls in release builds — custom `Logger` wrapper strips all output in release
- R8 obfuscation and minification enabled in release builds

### Audit log

All security-relevant events logged to `audit_log`:
- App launch and device fingerprint verification result
- Login success and failure
- Account lockout
- PIN changed
- Settings changes (what changed, when)
- Backup exported or restored
- Merchant config changed
- Transaction voided or refunded

Audit log is viewable in Settings by Admin. Cannot be edited. Admin can clear it (the clear action itself is logged with timestamp before clearing).

---

## 12. Tech Stack

### Android app

| Component | Technology | Reason |
|-----------|-----------|--------|
| Language | Kotlin | Full terminal SDK compatibility, modern Android |
| Min SDK | API 24 (Android 7.0) | Covers all 5 target POS terminals |
| Target SDK | API 34 (Android 14) | Latest stable |
| UI framework | Jetpack Compose | Declarative, testable UI |
| Architecture | MVVM + Clean Architecture | Strict separation, testability |
| Dependency injection | Hilt | Standard Android DI |
| Local database | Room ORM | Type-safe SQLite abstraction |
| DB encryption | SQLCipher | AES-256 SQLite encryption |
| PIN hashing | bcrypt (jBCrypt) | One-way secure hashing |
| Networking | Retrofit + OkHttp | Merchant API calls |
| Serialization | Kotlinx Serialization | JSON parsing |
| Async | Kotlin Coroutines + Flow | Non-blocking operations |
| Secure storage | Android Keystore | Hardware-backed key storage |
| Root detection | RootBeer | Tamper detection |
| Image handling | Coil | Local image loading |
| File I/O | Storage Access Framework | Backup file save/load |
| Backup encryption | AES-256 (javax.crypto) | Encrypting backup files |
| Build | Gradle (Kotlin DSL) | Build system |

### No cloud dependencies

The following are explicitly **not used:**
- Firebase (any service)
- Any analytics SDK
- Any crash-reporting SDK that transmits data off-device
- Any remote configuration service
- Any push notification service

---

## 13. Database Schema

Single SQLCipher-encrypted SQLite database managed by Room.

### `app_config`

```sql
CREATE TABLE app_config (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
-- Keys: setup_complete, shop_name, shop_currency, shop_timezone,
--       shop_logo_path, session_timeout_minutes, bg_lock_timeout_minutes,
--       pin_required_for_refunds, last_backup_timestamp, backup_reminder_days
```

### `admin`

```sql
CREATE TABLE admin (
    id           TEXT PRIMARY KEY,   -- single record, UUID
    username     TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    pin_hash     TEXT NOT NULL,      -- bcrypt hash
    created_at   INTEGER NOT NULL,
    last_login   INTEGER
);
```

### `products`

```sql
CREATE TABLE products (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    sku             TEXT,
    barcode         TEXT,
    category_id     TEXT,
    price           REAL NOT NULL,
    tax_rate        REAL,            -- null = use category/global rule
    image_path      TEXT,
    track_stock     INTEGER DEFAULT 0,
    stock_qty       INTEGER DEFAULT 0,
    low_stock_alert INTEGER DEFAULT 5,
    is_active       INTEGER DEFAULT 1,
    sort_order      INTEGER DEFAULT 0,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);
```

### `categories`

```sql
CREATE TABLE categories (
    id            TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    parent_id     TEXT,              -- null = top-level category
    tax_rate      REAL,              -- null = use global rule
    display_order INTEGER DEFAULT 0,
    is_active     INTEGER DEFAULT 1
);
```

### `transactions`

```sql
CREATE TABLE transactions (
    id              TEXT PRIMARY KEY,
    device_fp       TEXT NOT NULL,   -- device fingerprint at time of sale
    timestamp       INTEGER NOT NULL,
    customer_name   TEXT,            -- optional, for receipt only
    customer_phone  TEXT,            -- optional, for receipt only
    customer_email  TEXT,            -- optional, for receipt only
    subtotal        REAL NOT NULL,
    tax_total       REAL NOT NULL,
    discount_total  REAL DEFAULT 0,
    total           REAL NOT NULL,
    payment_method  TEXT NOT NULL,   -- cash | card | split
    merchant_id     TEXT,
    terminal_id     TEXT,
    approval_code   TEXT,
    status          TEXT NOT NULL,   -- completed | refunded | partial_refund | voided | pending_refund
    notes           TEXT,
    receipt_ref     TEXT
);
```

### `transaction_items`

```sql
CREATE TABLE transaction_items (
    id             TEXT PRIMARY KEY,
    transaction_id TEXT NOT NULL,
    product_id     TEXT,             -- null if custom/manual item
    product_name   TEXT NOT NULL,    -- snapshot at time of sale
    unit_price     REAL NOT NULL,
    quantity       REAL NOT NULL,
    tax_rate       REAL NOT NULL,
    tax_amount     REAL NOT NULL,
    discount       REAL DEFAULT 0,
    line_total     REAL NOT NULL
);
```

### `refunds`

```sql
CREATE TABLE refunds (
    id              TEXT PRIMARY KEY,
    original_tx_id  TEXT NOT NULL,
    amount          REAL NOT NULL,
    timestamp       INTEGER NOT NULL,
    reason          TEXT,
    merchant_ref    TEXT,            -- merchant refund reference if online
    status          TEXT NOT NULL    -- completed | pending
);
```

### `merchant_configs`

```sql
CREATE TABLE merchant_configs (
    id             TEXT PRIMARY KEY,
    merchant_id    TEXT NOT NULL UNIQUE,
    display_name   TEXT NOT NULL,
    keystore_alias TEXT NOT NULL,    -- references Android Keystore entry for API key
    terminal_id    TEXT,
    currency_code  TEXT NOT NULL,
    country_code   TEXT NOT NULL,
    environment    TEXT NOT NULL,    -- SANDBOX | PRODUCTION
    is_active      INTEGER DEFAULT 1,
    created_at     INTEGER NOT NULL,
    updated_at     INTEGER NOT NULL
);
-- Actual API key lives in Android Keystore only, referenced by keystore_alias
```

### `terminal_configs`

```sql
CREATE TABLE terminal_configs (
    id              TEXT PRIMARY KEY,
    terminal_model  TEXT NOT NULL,
    display_name    TEXT,
    connection_type TEXT NOT NULL,   -- USB | BLUETOOTH | TCPIP | BUILTIN
    ip_address      TEXT,
    bluetooth_addr  TEXT,
    port            INTEGER,
    is_active       INTEGER DEFAULT 1
);
```

### `tax_rules`

```sql
CREATE TABLE tax_rules (
    id         TEXT PRIMARY KEY,
    name       TEXT NOT NULL,
    rate       REAL NOT NULL,
    is_default INTEGER DEFAULT 0,
    applies_to TEXT NOT NULL         -- all | category | product
);
```

### `audit_log`

```sql
CREATE TABLE audit_log (
    id        TEXT PRIMARY KEY,
    action    TEXT NOT NULL,
    detail    TEXT,                  -- JSON with action-specific context
    timestamp INTEGER NOT NULL
);
```

---

## 14. OTA Update System

Since the app is sideloaded, updates are delivered via a lightweight version-check on launch.

### Update check

- On app launch, if internet is available: single HTTP GET to a hosted `version.json` manifest
- Manifest URL hardcoded in `BuildConfig`
- If offline: skip silently, proceed to login

### Version manifest format

```json
{
  "latest_version_code": 12,
  "latest_version_name": "1.2.0",
  "minimum_version_code": 8,
  "apk_url": "https://your-host/alpha-dragon-1.2.0.apk",
  "apk_sha256": "abc123...",
  "release_notes": "Bug fixes and new report types",
  "force_update": false
}
```

### Update behavior

| Condition | Behavior |
|-----------|---------|
| `current >= latest` | No action |
| `current < latest`, `force_update = false` | Dismissible update banner on Admin home |
| `current < minimum` or `force_update = true` | Blocking update screen — app cannot proceed |

On download: SHA-256 checksum verified before install. Android package installer launched. Hosting is static file only — no server logic needed.

---

## 15. Development Phases

### Phase 1 — Core offline POS

**Goal:** Fully working POS for cash sales with Admin login, encrypted local storage, and device security.

**Deliverables:**
- Project scaffold: Clean Architecture + MVVM + Hilt + Room + SQLCipher
- First-launch Admin setup screen (shop name, username, PIN)
- Login screen (username + PIN, lockout after 5 failures)
- Session management (timeout, re-entry on background)
- Admin home dashboard
- Product management: add, edit, delete, categories, tax, images, stock tracking
- Sale screen: product grid, search/barcode scan, cart, quantity, discounts, cash payment
- Optional customer name/phone/email on order for receipt
- Transaction history with filters
- Daily summary report
- Red and black branding — Jetpack Compose baseline
- Device fingerprint binding + Android Keystore
- Root detection (RootBeer)
- `FLAG_SECURE` on all sensitive screens

**Exit criteria:** Admin completes setup on a fresh install, processes a complete cash sale with optional customer name, sale is stored encrypted and visible in transaction history — fully offline, zero network calls.

---

### Phase 2 — Merchant APIs + terminal integration

**Goal:** Card payments work through real hardware terminals and merchant processors.

**Deliverables:**
- `PaymentProvider` interface + module per merchant (sandbox first)
- `TerminalDriver` interface + driver per terminal model
- Card payment flow in sale screen with offline detection
- Full refund flow (full + partial, PIN confirmation)
- Void transaction (PIN confirmation)
- Merchant configuration screen (API key → Keystore)
- Terminal pairing screen
- Certificate pinning on all merchant API clients
- All integrations tested in sandbox

**Exit criteria:** Card payment processed end-to-end through a physical terminal against at least one merchant sandbox, stored locally, refund flow works.

---

### Phase 3 — Backup, reports, and settings

**Goal:** Admin has full data safety, complete reporting, and all settings functional.

**Deliverables:**
- Encrypted backup export (`.adb` file — database + images, AES-256)
- Backup restore flow (password verification, data replacement, new device support)
- Backup reminder banner
- All report types (section 6.3)
- CSV export to Downloads
- End-of-day printable/exportable summary
- Full settings screen (all items from section 6.4)
- Audit log viewer in Settings
- Tax rules management
- Receipt template customization
- Admin PIN change flow
- Bulk product import via CSV

**Exit criteria:** Admin exports full encrypted backup, restores on a clean install, all data intact. All reports generate correctly from local data.

---

### Phase 4 — QA, hardening, and production delivery

**Goal:** Production-ready signed APK tested on all real target hardware.

**Deliverables:**
- OTA update check system live
- R8 obfuscation + minification on release build
- `android:allowBackup="false"` confirmed in production manifest
- All debug logging stripped from release
- Full QA on all 5 POS terminal models
- Performance: sale flow completes under 2 seconds on target hardware
- Edge case testing: storage full, permission denied, terminal disconnected mid-transaction
- Production-signed APK + keystore backup secured
- APK delivered

**Exit criteria:** All features pass QA on real hardware. Production APK is signed, obfuscated, and installable on all 5 target terminals.

---

## 16. Coding Standards

### Kotlin

- Follow [Kotlin official coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- No mutable shared state — use `StateFlow` and `SharedFlow`
- All coroutine functions marked `suspend` — no blocking calls on main thread
- All fallible operations return `Result<T>` — no unhandled exceptions propagating to UI
- No `!!` non-null assertion — use safe calls or `requireNotNull` with a descriptive message
- Sealed classes for all UI state and domain error types
- No business logic inside Composable functions

### Architecture rules

- ViewModels import only `ViewModel`, `Application`, and domain layer classes — nothing else
- Use cases are pure Kotlin — zero Android, Compose, or data layer imports
- Repository interfaces defined in domain layer; implementations in data layer
- Data layer imports nothing from the UI layer
- Each merchant module and each terminal module is a separate Gradle module — no cross-module direct imports in `:app`

### Security rules in code

- No `Log.*` in release builds — custom `Logger` wrapper strips all output in release
- No API keys, PINs, or sensitive strings hardcoded anywhere (not in source, not in `strings.xml`, not in `BuildConfig`)
- All Keystore operations wrapped in try/catch with explicit failure handling
- All Room queries use parameterized queries — no raw SQL string concatenation

### Git workflow

- Branch naming: `feature/`, `fix/`, `chore/`, `release/`
- Features merged to `develop` via pull request
- `main` is production only — merged from `release/` branches
- Every PR: passing tests, zero lint errors, one peer review
- Commit messages: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`)

### Build variants

| Variant | Logging | Security checks | Credentials |
|---------|---------|-----------------|-------------|
| `debug` | Enabled | Warnings only | Sandbox |
| `staging` | Disabled | All active | Sandbox |
| `release` | Stripped | All active + obfuscation | Production via Keystore |

---

## 17. Testing Requirements

### Unit tests

- All use cases: 100% coverage
- All ViewModels: 100% coverage
- PIN hashing and verification: explicit cases including invalid PINs
- Backup encryption/decryption: full round-trip test
- Merchant module: request/response parsing with mocked HTTP responses
- Terminal driver: all connection state transitions

### Integration tests

- Room schema migrations
- SQLCipher: verify encrypted DB cannot be opened without key
- Android Keystore: key creation, retrieval, merchant config round-trip
- Device fingerprint: generation, match, and mismatch detection
- Backup export and restore: full round-trip

### UI tests (Espresso + Compose UI Test)

- First-launch setup flow: end-to-end
- Login: success, failure, lockout
- Sale flow with and without customer data: product selection → cart → payment → stored
- Refund flow: found in history, PIN confirmation, record updated

### Device QA (required before Phase 4 sign-off)

- Ingenico DX8000
- Ingenico DX4000
- Clover Flex
- Castles S1000
- Castles S1F2

---

## 18. Folder Structure

```
alpha-dragon-pos/
│
├── app/                                  # Main application module
│   └── src/main/
│       ├── java/com/alphadragon/pos/
│       │   ├── ui/
│       │   │   ├── setup/                # First-launch setup flow
│       │   │   ├── auth/                 # Login screen
│       │   │   ├── sale/                 # Sale screen, cart, customer field
│       │   │   ├── transactions/         # History, refund, void
│       │   │   ├── products/             # Product + category management
│       │   │   ├── reports/              # All report screens
│       │   │   ├── settings/             # All settings screens
│       │   │   └── components/           # Shared Compose components
│       │   │
│       │   ├── domain/                   # Pure Kotlin — zero Android deps
│       │   │   ├── model/
│       │   │   ├── repository/           # Repository interfaces
│       │   │   └── usecase/
│       │   │       ├── auth/
│       │   │       ├── sale/
│       │   │       ├── product/
│       │   │       ├── report/
│       │   │       └── backup/
│       │   │
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── dao/
│       │   │   │   ├── entity/
│       │   │   │   └── AlphaDragonDb.kt
│       │   │   ├── repository/           # Repository implementations
│       │   │   └── backup/               # Backup export/import logic
│       │   │
│       │   ├── security/
│       │   │   ├── DeviceFingerprint.kt
│       │   │   ├── KeystoreManager.kt
│       │   │   ├── PinHasher.kt
│       │   │   └── RootDetection.kt
│       │   │
│       │   ├── di/                       # Hilt modules
│       │   └── AlphaDragonApp.kt
│       │
│       └── res/
│           ├── values/colors.xml         # Red + black brand palette
│           └── xml/network_security_config.xml
│
├── merchant/                             # One Gradle module per merchant
│   ├── worldpay/
│   ├── fiserv/
│   ├── firstdata/
│   ├── clover/
│   ├── cardnet/
│   ├── elavon/
│   └── manya/
│
├── terminal/                             # One Gradle module per terminal
│   ├── ingenico/                         # DX8000 + DX4000 (same SDK)
│   ├── clover-flex/
│   └── castles/                          # S1000 + S1F2 (same SDK)
│
└── core/                                 # Shared across all modules
    ├── common/                           # Extensions, constants, shared models
    ├── payment-interface/                # PaymentProvider + TerminalDriver interfaces
    └── testing/                          # Shared test fakes and utilities
```

---

## Appendix A — Merchant developer portals

| Merchant | Developer portal |
|----------|-----------------|
| Worldpay | https://developer.worldpay.com |
| Fiserv | https://developer.fiserv.com |
| First Data | https://developer.fiserv.com/product/FirstDataGateway |
| Clover | https://developer.clover.com |
| Elavon | https://developer.elavon.com |
| Cardnet | Contact Cardnet UK merchant services directly |
| Manya | Obtain on account approval |

---

## Appendix B — Android permissions required

```xml
<!-- Network — merchant API calls only -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Terminal connectivity -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.USB_PERMISSION" />

<!-- Background work (OTA update check on launch) -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Barcode scanning -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Backup file save/load -->
<!-- Android 10+: Storage Access Framework — no permission needed -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

---

## Appendix C — Glossary

| Term | Definition |
|------|-----------|
| Admin | The sole operator of the app — one account, created at first launch, cannot be deleted |
| customer data | Optional name/phone/email attached to a transaction for receipt purposes only — not a separate account or profile |
| device fingerprint | SHA-256 hash of hardware identifiers used to bind the app to one physical device |
| `.adb` file | Alpha Dragon Backup — AES-256 encrypted bundle of database + images |
| bcrypt | One-way hashing algorithm used to store PINs securely |
| SQLCipher | AES-256 encryption layer on top of SQLite |
| Android Keystore | Hardware-backed secure storage for cryptographic keys — cannot be extracted |
| merchant module | Isolated Gradle module implementing PaymentProvider for one merchant |
| terminal driver | Isolated Gradle module implementing TerminalDriver for one terminal model |

---

*This document is the single source of truth for the Alpha Dragon POS development project.  
Version 3.0 — device-local · Admin-only · customer data on orders only · no cloud.*
