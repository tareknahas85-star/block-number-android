package com.microbeaver.blocknumber

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("block_number_prefs", Context.MODE_PRIVATE)

    var blockingEnabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Block every number that is not in the contacts (original behavior). */
    var blockUnknown: Boolean
        get() = sp.getBoolean(KEY_BLOCK_UNKNOWN, true)
        set(value) = sp.edit().putBoolean(KEY_BLOCK_UNKNOWN, value).apply()

    var blockHidden: Boolean
        get() = sp.getBoolean(KEY_BLOCK_HIDDEN, true)
        set(value) = sp.edit().putBoolean(KEY_BLOCK_HIDDEN, value).apply()

    /** Block numbers rated negative in the offline spam database. */
    var blockByRating: Boolean
        get() = sp.getBoolean(KEY_BLOCK_BY_RATING, true)
        set(value) = sp.edit().putBoolean(KEY_BLOCK_BY_RATING, value).apply()

    /** Show caller-info notification while the phone rings. */
    var notifyCallerInfo: Boolean
        get() = sp.getBoolean(KEY_NOTIFY_CALLER, true)
        set(value) = sp.edit().putBoolean(KEY_NOTIFY_CALLER, value).apply()

    /** Show a notification when a call is blocked. */
    var notifyBlocked: Boolean
        get() = sp.getBoolean(KEY_NOTIFY_BLOCKED, false)
        set(value) = sp.edit().putBoolean(KEY_NOTIFY_BLOCKED, value).apply()

    var autoUpdateDb: Boolean
        get() = sp.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

    var lastDbCheck: Long
        get() = sp.getLong(KEY_LAST_DB_CHECK, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_DB_CHECK, value).apply()

    var spamDbUrl: String
        get() = sp.getString(KEY_SPAM_DB_URL, SpamDbUpdater.DEFAULT_BASE_URL)!!
        set(value) = sp.edit().putString(KEY_SPAM_DB_URL, value).apply()

    /** Per-SIM blocking: keyed by PhoneAccountHandle.id, enabled by default. */
    fun simBlockEnabled(simId: String): Boolean =
        sp.getBoolean(KEY_SIM_PREFIX + simId, true)

    fun setSimBlockEnabled(simId: String, value: Boolean) =
        sp.edit().putBoolean(KEY_SIM_PREFIX + simId, value).apply()

    companion object {
        private const val KEY_ENABLED = "blocking_enabled"
        private const val KEY_BLOCK_UNKNOWN = "block_unknown"
        private const val KEY_BLOCK_HIDDEN = "block_hidden"
        private const val KEY_BLOCK_BY_RATING = "block_by_rating"
        private const val KEY_NOTIFY_CALLER = "notify_caller_info"
        private const val KEY_NOTIFY_BLOCKED = "notify_blocked"
        private const val KEY_AUTO_UPDATE = "auto_update_db"
        private const val KEY_LAST_DB_CHECK = "last_db_check"
        private const val KEY_SPAM_DB_URL = "spam_db_url"
        private const val KEY_SIM_PREFIX = "sim_block_"
    }
}
