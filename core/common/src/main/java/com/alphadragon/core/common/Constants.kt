package com.alphadragon.core.common

object AppConfig {
    const val DB_NAME = "alpha_dragon.db"
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    const val DB_KEY_ALIAS = "alpha_dragon_db_key"
    const val BACKUP_KEY_ALIAS = "alpha_dragon_backup_key"
    const val BACKUP_FILE_EXTENSION = ".adb"
    const val PIN_LENGTH = 6
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOGIN_LOCKOUT_SECONDS = 30L
    const val DEFAULT_SESSION_TIMEOUT_MINUTES = 480   // 8 hours
    const val DEFAULT_BG_LOCK_TIMEOUT_MINUTES = 5
    const val DEFAULT_BACKUP_REMINDER_DAYS = 7
    const val BCRYPT_COST = 12
}

object ConfigKeys {
    const val SETUP_COMPLETE = "setup_complete"
    const val SHOP_NAME = "shop_name"
    const val SHOP_CURRENCY = "shop_currency"
    const val SHOP_TIMEZONE = "shop_timezone"
    const val SHOP_LOGO_PATH = "shop_logo_path"
    const val SHOP_ADDRESS = "shop_address"
    const val SESSION_TIMEOUT_MINUTES = "session_timeout_minutes"
    const val BG_LOCK_TIMEOUT_MINUTES = "bg_lock_timeout_minutes"
    const val PIN_REQUIRED_FOR_REFUNDS = "pin_required_for_refunds"
    const val LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
    const val BACKUP_REMINDER_DAYS = "backup_reminder_days"
    const val RECEIPT_FOOTER = "receipt_footer"
    const val LAST_UPDATE_CHECK = "last_update_check"
    const val DISCOUNTS_ENABLED = "discounts_enabled"
    const val LOW_STOCK_THRESHOLD = "low_stock_threshold"
}
