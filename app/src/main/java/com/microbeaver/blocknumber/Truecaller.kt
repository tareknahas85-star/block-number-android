package com.microbeaver.blocknumber

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager

/**
 * One-tap Truecaller lookup. Truecaller has no public caller-ID API,
 * so we open its search page (app if installed, web otherwise).
 */
object Truecaller {

    private fun countryIso(context: Context): String = try {
        val tm = context.getSystemService(TelephonyManager::class.java)
        (tm?.networkCountryIso.orEmpty().ifEmpty { tm?.simCountryIso.orEmpty() })
    } catch (e: Exception) {
        ""
    }.ifEmpty { "sy" }.lowercase()

    fun searchIntent(context: Context, number: String): Intent {
        val digits = number.filter { it.isDigit() || it == '+' }
        val uri = Uri.parse(
            "https://www.truecaller.com/search/" +
                countryIso(context) + "/" + Uri.encode(digits)
        )
        return Intent(Intent.ACTION_VIEW, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun open(context: Context, number: String) {
        try {
            context.startActivity(searchIntent(context, number))
        } catch (e: Exception) {
            // no browser / activity found: ignore
        }
    }
}
