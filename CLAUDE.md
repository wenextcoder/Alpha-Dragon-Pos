# CLAUDE.md — Alpha Dragon POS

## What this project is
Offline-first Android POS app (Kotlin). Single device, single shop. No cloud, no sync, no backend. Admin-only login. Full brief: `docs/project-brief-v3.md`.

## Stack
- Kotlin + Jetpack Compose + MVVM + Clean Architecture
- Hilt (DI), Room + SQLCipher (local DB), Android Keystore (secrets)
- bcrypt (PIN hashing), Retrofit + OkHttp (merchant APIs only)
- Each merchant and terminal = separate Gradle module

## Architecture rules
- `ui/` → ViewModel only. No business logic in Composables.
- `domain/` → Pure Kotlin use cases. Zero Android imports.
- `data/` → Room + Repository implementations. No UI imports.
- `security/` → Keystore, device fingerprint, PIN hashing, root detection.
- Merchant/terminal modules implement shared interfaces in `:core:payment-interface`. Core app never imports a merchant SDK directly.

## Key constraints
- **No internet required** for any POS operation. Card payments check connectivity first and gracefully fall back.
- **One Admin account** only. Created at first launch. No other login roles.
- **Device-bound.** Device fingerprint checked on every launch.
- **All sensitive data in Keystore.** API keys and DB encryption key never in SQLite or source.
- **Customer data** = optional name/phone/email on a transaction for receipt only. No customer table.

## Code rules
- All fallible ops return `Result<T>`. No unhandled exceptions to UI.
- No `!!`. No `Log.*` in release (use `Logger` wrapper).
- No hardcoded secrets anywhere — not in source, not in `strings.xml`, not in `BuildConfig`.
- All DB queries parameterized. No raw SQL string concatenation.
- `FLAG_SECURE` on login, PIN, settings, and merchant config screens.

## Database
SQLCipher-encrypted SQLite. Key tables: `app_config`, `admin`, `products`, `categories`, `transactions`, `transaction_items`, `refunds`, `merchant_configs`, `terminal_configs`, `tax_rules`, `audit_log`.

## Build variants
| Variant | Logging | Obfuscation | Credentials |
|---------|---------|-------------|-------------|
| debug | on | off | sandbox |
| staging | off | off | sandbox |
| release | off | R8 on | production via Keystore |

## First launch flow
No admin record → Setup screen (shop name, username, 6-digit PIN) → Login screen → Main POS.

## Phases
1. Offline POS core (cash sales, products, history, Admin auth)
2. Merchant APIs + terminal drivers (card payments, refunds)
3. Backup/restore, full reports, settings
4. QA on all 5 terminals, hardening, production APK
