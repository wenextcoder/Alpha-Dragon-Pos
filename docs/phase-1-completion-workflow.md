# Phase 1 Completion Workflow (Short)

1. **Fresh install + setup**
   - Launch app, complete Admin setup (shop, username, 6-digit PIN).
   - Admin is created and session starts.

2. **Security checks on launch**
   - Root detection and device fingerprint verification run.
   - Local encrypted DB/keys are loaded from app-secure storage.

3. **Configure catalog**
   - Create categories (tax/display order).
   - Create/edit products with price, tax, stock, and optional image.

4. **Run a cash sale**
   - Search/select products from grid, adjust qty/discounts, review totals.
   - Optionally add customer info for receipt context.
   - Complete payment with **Cash** (Card/Split are Phase 2 paths).

5. **Validate records**
   - Confirm transaction appears in history with filters/search.
   - Check daily summary report reflects totals and payment breakdown.

6. **Phase 1 done**
   - Offline flow works end-to-end: setup → sale → stored transaction → reporting, with security controls enabled.

