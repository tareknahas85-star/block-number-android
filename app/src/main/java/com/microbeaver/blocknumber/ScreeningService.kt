package com.microbeaver.blocknumber

import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Screens incoming calls: any number NOT found in the user's contacts is rejected.
 * Requires the app to hold the CALL_SCREENING role (requested in MainActivity).
 */
class ScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // Only screen incoming calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondTo(callDetails, allow = true)
            return
        }

        val prefs = Prefs(this)
        if (!prefs.blockingEnabled) {
            respondTo(callDetails, allow = true)
            return
        }

        val number = callDetails.handle?.schemeSpecificPart?.trim().orEmpty()

        val allow = when {
            // Hidden / private number → treat as unknown
            number.isEmpty() -> !prefs.blockHidden
            else -> isInContacts(number)
        }

        if (!allow) {
            BlockedCallStore(this).insert(
                number = if (number.isEmpty()) getString(R.string.hidden_number) else number,
                timestamp = System.currentTimeMillis()
            )
        }
        respondTo(callDetails, allow)
    }

    private fun respondTo(callDetails: Call.Details, allow: Boolean) {
        val response = if (allow) {
            CallResponse.Builder().build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .build()
        }
        respondToCall(callDetails, response)
    }

    private fun isInContacts(number: String): Boolean {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null
            )?.use { it.count > 0 } ?: false
        } catch (e: SecurityException) {
            // No contacts permission → fail open (don't block legitimate calls)
            true
        }
    }
}
