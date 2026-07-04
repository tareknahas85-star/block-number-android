package com.microbeaver.blocknumber

import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Screens incoming calls before the phone rings.
 *
 * Decision chain:
 *  1. Blocking disabled or SIM excluded      -> allow
 *  2. Hidden number                          -> block if blockHidden
 *  3. Number in contacts                     -> always allow (never blocked)
 *  4. Number matches local blacklist         -> block (reason: blacklist)
 *  5. Negative rating in offline spam DB     -> block if blockByRating (reason: spam)
 *  6. Not in contacts                        -> block if blockUnknown (reason: contacts)
 *  7. Otherwise                              -> allow + optional caller-info notification
 */
class ScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondTo(callDetails, allow = true)
            return
        }

        val prefs = Prefs(this)
        if (!prefs.blockingEnabled) {
            respondTo(callDetails, allow = true)
            return
        }

        // Per-SIM control: if this call arrives on a SIM with blocking disabled, allow it.
        val simId = callDetails.accountHandle?.id
        if (simId != null && !prefs.simBlockEnabled(simId)) {
            respondTo(callDetails, allow = true)
            return
        }

        val number = callDetails.handle?.schemeSpecificPart?.trim().orEmpty()

        // 2. Hidden number
        if (number.isEmpty()) {
            if (prefs.blockHidden) {
                block(callDetails, getString(R.string.hidden_number),
                    BlockedCallStore.REASON_HIDDEN, prefs)
            } else {
                respondTo(callDetails, allow = true)
            }
            return
        }

        // 3. Contacts are never blocked
        if (isInContacts(number)) {
            respondTo(callDetails, allow = true)
            return
        }

        // 4. Local blacklist (wildcards supported)
        if (BlacklistStore(this).findMatch(number) != null) {
            block(callDetails, number, BlockedCallStore.REASON_BLACKLIST, prefs)
            return
        }

        // 5. Offline spam database
        val spam = try {
            SpamDbStore(this).lookup(number)
        } catch (e: Exception) {
            null
        }
        if (prefs.blockByRating && spam != null && spam.isNegative) {
            block(callDetails, number, BlockedCallStore.REASON_SPAM, prefs)
            return
        }

        // 6. Unknown number
        if (prefs.blockUnknown) {
            block(callDetails, number, BlockedCallStore.REASON_NOT_IN_CONTACTS, prefs)
            return
        }

        // 7. Allowed: show caller info while ringing
        if (prefs.notifyCallerInfo) {
            try {
                CallerInfoNotifier.showCallerInfo(this, number, spam)
            } catch (e: Exception) {
                // notifications must never break call handling
            }
        }
        respondTo(callDetails, allow = true)
    }

    private fun block(
        callDetails: Call.Details,
        numberLabel: String,
        reason: String,
        prefs: Prefs
    ) {
        BlockedCallStore(this).insert(
            number = numberLabel,
            timestamp = System.currentTimeMillis(),
            reason = reason
        )
        if (prefs.notifyBlocked) {
            try {
                CallerInfoNotifier.showBlocked(this, numberLabel, reason)
            } catch (e: Exception) {
                // ignore
            }
        }
        respondTo(callDetails, allow = false)
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
            // No contacts permission -> fail open (don't block legitimate calls)
            true
        }
    }
}
