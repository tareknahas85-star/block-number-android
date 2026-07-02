package com.microbeaver.blocknumber

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("block_number_prefs", Context.MODE_PRIVATE)

    var blockingEnabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    var blockHidden: Boolean
        get() = sp.getBoolean(KEY_BLOCK_HIDDEN, true)
        set(value) = sp.edit().putBoolean(KEY_BLOCK_HIDDEN, value).apply()

    /** Per-SIM blocking: keyed by PhoneAccountHandle.id, enabled by default. */
    fun simBlockEnabled(simId: String): Boolean =
        sp.getBoolean(KEY_SIM_PREFIX + simId, true)

    fun setSimBlockEnabled(simId: String, value: Boolean) =
        sp.edit().putBoolean(KEY_SIM_PREFIX + simId, value).apply()

    companion object {
        private const val KEY_ENABLED = "blocking_enabled"
        private const val KEY_BLOCK_HIDDEN = "block_hidden"
        private const val KEY_SIM_PREFIX = "sim_block_"
    }
}
