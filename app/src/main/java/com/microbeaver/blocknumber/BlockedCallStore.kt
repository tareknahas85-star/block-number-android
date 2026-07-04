package com.microbeaver.blocknumber

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class BlockedCall(
    val id: Long,
    val number: String,
    val timestamp: Long,
    val reason: String
)

class BlockedCallStore(context: Context) :
    SQLiteOpenHelper(context, "blocked_calls.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE blocked_calls (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT NOT NULL,
                ts INTEGER NOT NULL,
                reason TEXT NOT NULL DEFAULT 'contacts'
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE blocked_calls ADD COLUMN reason TEXT NOT NULL DEFAULT 'contacts'"
            )
        }
    }

    fun insert(number: String, timestamp: Long, reason: String) {
        writableDatabase.insert("blocked_calls", null, ContentValues().apply {
            put("number", number)
            put("ts", timestamp)
            put("reason", reason)
        })
    }

    fun getAll(): List<BlockedCall> {
        val list = mutableListOf<BlockedCall>()
        readableDatabase.query(
            "blocked_calls", arrayOf("id", "number", "ts", "reason"),
            null, null, null, null, "ts DESC", "200"
        ).use { c ->
            while (c.moveToNext()) {
                list.add(BlockedCall(c.getLong(0), c.getString(1), c.getLong(2), c.getString(3)))
            }
        }
        return list
    }

    fun clear() {
        writableDatabase.delete("blocked_calls", null, null)
    }

    companion object {
        const val REASON_NOT_IN_CONTACTS = "contacts"
        const val REASON_BLACKLIST = "blacklist"
        const val REASON_HIDDEN = "hidden"
        const val REASON_SPAM = "spam"
    }
}
