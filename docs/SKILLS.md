# SKILLS.md — Alpha Dragon POS

Agent skill reference. Use the correct skill for each task area.

---

## Auth & setup
**Files:** `ui/setup/`, `ui/auth/`, `domain/usecase/auth/`, `security/PinHasher.kt`

- First-launch: check `app_config.setup_complete` → show setup screen if false
- PIN hashing: always use `PinHasher.hash()` (bcrypt, cost 12). Never store plain PIN.
- Login: `PinHasher.verify(input, storedHash)`. Lock after 5 failures × 30s.
- Session: in-memory UUID token only. Expire after inactivity timeout from `app_config`.
- Device fingerprint: `DeviceFingerprint.generate()` on setup, `verify()` on every launch.

---

## Sale & cart
**Files:** `ui/sale/`, `domain/usecase/sale/`

- Cart state lives in `SaleViewModel`. Not persisted until payment confirmed.
- Tax calculation: product tax_rate → category tax_rate → global default (fallback chain).
- Customer fields (name/phone/email) are optional strings on the transaction. No lookup table.
- Payment method: `cash | card | split`. Card requires connectivity check before terminal call.
- On approval: call `RecordTransactionUseCase` → writes to Room immediately.

---

## Products & categories
**Files:** `ui/products/`, `domain/usecase/product/`

- Max 2 category levels (category → subcategory). `parent_id` null = top-level.
- Images stored as local file paths in `products.image_path`. Load with Coil.
- Stock tracking optional per product (`track_stock` flag). Show low-stock badge when `stock_qty <= low_stock_alert`.
- CSV import: parse → validate → bulk insert via Room transaction.

---

## Merchant API integration
**Files:** `merchant/<name>/`, `core/payment-interface/PaymentProvider.kt`

- Every merchant implements `PaymentProvider` interface. Core app uses interface only.
- API key: never in SQLite. Store/retrieve via `KeystoreManager.getApiKey(alias)`.
- `MerchantConfig.environment`: SANDBOX in debug/staging, PRODUCTION in release.
- All clients use OkHttp with `CertificatePinner` configured.
- Offline guard: check `NetworkHelper.isConnected()` before any merchant call.

---

## Terminal drivers
**Files:** `terminal/<name>/`, `core/payment-interface/TerminalDriver.kt`

- Every terminal implements `TerminalDriver` interface. Core app uses interface only.
- `TerminalManager` holds the active driver. Swappable via Settings.
- Connection types: USB | BLUETOOTH | TCPIP | BUILTIN (Clover Flex).
- Always handle `TerminalConnectionStatus.DISCONNECTED` gracefully — show reconnect prompt.

---

## Security
**Files:** `security/`

| Task | Use |
|------|-----|
| Store API key | `KeystoreManager.storeApiKey(alias, key)` |
| Load API key | `KeystoreManager.getApiKey(alias)` |
| Hash PIN | `PinHasher.hash(pin)` |
| Verify PIN | `PinHasher.verify(input, hash)` |
| Device fingerprint | `DeviceFingerprint.generate()` / `.verify()` |
| Root check | `RootDetection.isRooted(context)` — warn Admin, log to audit |
| Sensitive screens | Set `window.addFlags(FLAG_SECURE)` in Activity/Fragment |

---

## Backup & restore
**Files:** `data/backup/`

- Export: bundle DB file + images → AES-256 encrypt with Admin-supplied password → `.adb` file.
- Checksum: SHA-256 of plaintext bundle, stored in file header.
- Restore: decrypt → verify checksum → replace DB → restore images → restart app.
- "Database only" option: skip image bundling.
- Last backup timestamp in `app_config.last_backup_timestamp`. Banner if overdue.

---

## Reports
**Files:** `ui/reports/`, `domain/usecase/report/`

- All queries run on local Room DB. No network.
- Date filtering: use Unix timestamps. Store and compare in UTC.
- CSV export: write to `Environment.DIRECTORY_DOWNLOADS` via `FileOutputStream`. Notify Admin on completion.

---

## Audit log
**Files:** `data/local/dao/AuditLogDao.kt`

- Log every security-relevant event: login, PIN change, backup, merchant config change, void, refund.
- Use `AuditLogger.log(action, detail)`. Detail is JSON string.
- Log the "clear" action itself before clearing the table.

---

## Settings
**Files:** `ui/settings/`

- All settings read/write via `app_config` table (key-value).
- Merchant and terminal config screens require extra care — changes logged to audit.
- PIN change: verify current PIN first, then `PinHasher.hash(newPin)` and update `admin.pin_hash`.

---

## Conventions
- Branch: `feature/`, `fix/`, `chore/`, `release/`
- Commits: Conventional Commits (`feat:`, `fix:`, `chore:`, `test:`)
- Every use case returns `Result<T>`. UI maps Result to UiState sealed class.
- No `!!`. No raw SQL. No hardcoded strings for keys or secrets.
