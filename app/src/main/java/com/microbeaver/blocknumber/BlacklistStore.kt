package com.microbeaver.blocknumber

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class BlacklistEntry(
    val id: Long,
    val pattern: String,
    val label: String,
    val createdAt: Long
)

/**
 * Local blacklist with wildcard support:
 *  '*' = any number of digits, '#' = exactly one digit.
 */
class BlacklistStore(context: Context) :
    SQLiteOpenHelper(context, "blacklist.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE blacklist (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pattern TEXT NOT NULL UNIQUE,
                label TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun add(pattern: String, label: String): Boolean {
        val normalized = pattern.trim()
        if (normalized.isEmpty()) return false
        val values = ContentValues().apply {
            put("pattern", normalized)
            put("label", label.trim())
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertWithOnConflict(
            "blacklist", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    fun remove(id: Long) {
        writableDatabase.delete("blacklist", "id = ?", arrayOf(id.toString()))
    }

    fun getAll(): List<BlacklistEntry> {
        val list = mutableListOf<BlacklistEntry>()
        readableDatabase.query(
            "blacklist", arrayOf("id", "pattern", "label", "created_at"),
            null, null, null, null, "created_at DESC"
        ).use { c ->
            while (c.moveToNext()) {
                list.add(BlacklistEntry(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
            }
        }
        return list
    }

    /** Returns the matching entry, or null if the number is not blacklisted. */
    fun findMatch(number: String): BlacklistEntry? =
        getAll().firstOrNull { NumberUtils.wildcardMatches(it.pattern, number) }
}
