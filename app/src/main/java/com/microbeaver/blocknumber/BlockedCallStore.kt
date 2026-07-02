package com.microbeaver.blocknumber

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class BlockedCall(val id: Long, val number: String, val timestamp: Long)

class BlockedCallStore(context: Context) :
    SQLiteOpenHelper(context, "blocked_calls.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE blocked_calls (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT NOT NULL,
                ts INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(number: String, timestamp: Long) {
        writableDatabase.insert("blocked_calls", null, ContentValues().apply {
            put("number", number)
            put("ts", timestamp)
        })
    }

    fun getAll(): List<BlockedCall> {
        val list = mutableListOf<BlockedCall>()
        readableDatabase.query(
            "blocked_calls", arrayOf("id", "number", "ts"),
            null, null, null, null, "ts DESC", "200"
        ).use { c ->
            while (c.moveToNext()) {
                list.add(BlockedCall(c.getLong(0), c.getString(1), c.getLong(2)))
            }
        }
        return list
    }

    fun clear() {
        writableDatabase.delete("blocked_calls", null, null)
    }
}
