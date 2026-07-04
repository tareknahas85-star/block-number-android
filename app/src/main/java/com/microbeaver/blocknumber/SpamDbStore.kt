package com.microbeaver.blocknumber

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class SpamEntry(
    val number: String,
    val name: String,
    val category: String,
    val negative: Int,
    val positive: Int,
    val neutral: Int
) {
    /** Negative rating = clearly more negative than positive reviews. */
    val isNegative: Boolean get() = negative > 0 && negative >= positive * 2
}

/**
 * Offline spam-number database. Filled by [SpamDbUpdater] from a remote
 * CSV file; queried during call screening. Works fully offline.
 */
class SpamDbStore(context: Context) :
    SQLiteOpenHelper(context, "spam_db.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE spam_numbers (
                number TEXT PRIMARY KEY,
                name TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL DEFAULT '',
                negative INTEGER NOT NULL DEFAULT 0,
                positive INTEGER NOT NULL DEFAULT 0,
                neutral INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun lookup(number: String): SpamEntry? {
        val n = NumberUtils.normalize(number)
        if (n.isEmpty()) return null
        val candidates = linkedSetOf(n, n.removePrefix("+"), "+" + n.removePrefix("+"))
        val placeholders = candidates.joinToString(",") { "?" }
        readableDatabase.query(
            "spam_numbers",
            arrayOf("number", "name", "category", "negative", "positive", "neutral"),
            "number IN ($placeholders)", candidates.toTypedArray(),
            null, null, null, "1"
        ).use { c ->
            if (c.moveToFirst()) {
                return SpamEntry(
                    c.getString(0), c.getString(1), c.getString(2),
                    c.getInt(3), c.getInt(4), c.getInt(5)
                )
            }
        }
        return null
    }

    fun count(): Long {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM spam_numbers", null).use { c ->
            return if (c.moveToFirst()) c.getLong(0) else 0L
        }
    }

    /** Replaces the whole DB content in one transaction. */
    fun replaceAll(entries: List<SpamEntry>, version: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("spam_numbers", null, null)
            for (e in entries) {
                db.insertWithOnConflict("spam_numbers", null, ContentValues().apply {
                    put("number", NumberUtils.normalize(e.number))
                    put("name", e.name)
                    put("category", e.category)
                    put("negative", e.negative)
                    put("positive", e.positive)
                    put("neutral", e.neutral)
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            setMeta("db_version", version)
            setMeta("updated_at", System.currentTimeMillis().toString())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getMeta(key: String): String? {
        readableDatabase.query(
            "meta", arrayOf("value"), "key = ?", arrayOf(key), null, null, null
        ).use { c -> return if (c.moveToFirst()) c.getString(0) else null }
    }

    private fun setMeta(key: String, value: String) {
        writableDatabase.insertWithOnConflict("meta", null, ContentValues().apply {
            put("key", key)
            put("value", value)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
