package com.microbeaver.blocknumber

import android.content.Context
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Downloads the spam-number database as a CSV file over HTTPS.
 *
 * Remote layout (hosted on any static host, e.g. GitHub raw):
 *   version.txt  — a single line with the DB version (e.g. "2026-07-04")
 *   spamdb.csv   — lines: number,category,negative,positive,neutral,name
 *
 * The version file is checked first; the CSV is only downloaded when the
 * remote version differs from the local one (cheap incremental behavior).
 */
object SpamDbUpdater {

    const val DEFAULT_BASE_URL =
        "https://raw.githubusercontent.com/tareknahas85-star/block-number-data/main"

    private val executor = Executors.newSingleThreadExecutor()

    sealed class Result {
        data class Updated(val version: String, val count: Long) : Result()
        object UpToDate : Result()
        data class Failed(val message: String) : Result()
    }

    fun updateAsync(context: Context, force: Boolean, callback: (Result) -> Unit) {
        val appContext = context.applicationContext
        executor.execute {
            val result = try {
                update(appContext, force)
            } catch (e: Exception) {
                Result.Failed(e.message ?: e.javaClass.simpleName)
            }
            callback(result)
        }
    }

    /** True when an automatic check is due (once a day). */
    fun autoCheckDue(prefs: Prefs): Boolean {
        if (!prefs.autoUpdateDb) return false
        val last = prefs.lastDbCheck
        return System.currentTimeMillis() - last > 24L * 60 * 60 * 1000
    }

    private fun update(context: Context, force: Boolean): Result {
        val prefs = Prefs(context)
        val store = SpamDbStore(context)
        val base = prefs.spamDbUrl.trimEnd('/')

        val remoteVersion = fetch("$base/version.txt").trim()
        prefs.lastDbCheck = System.currentTimeMillis()
        if (remoteVersion.isEmpty()) return Result.Failed("empty version file")

        val localVersion = store.getMeta("db_version")
        if (!force && remoteVersion == localVersion) return Result.UpToDate

        val csv = fetch("$base/spamdb.csv")
        val entries = parseCsv(csv)
        if (entries.isEmpty()) return Result.Failed("empty database file")

        store.replaceAll(entries, remoteVersion)
        return Result.Updated(remoteVersion, store.count())
    }

    private fun parseCsv(csv: String): List<SpamEntry> {
        val list = mutableListOf<SpamEntry>()
        csv.lineSequence().forEach { line ->
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#") || l.startsWith("number,")) return@forEach
            val parts = l.split(",", limit = 6)
            if (parts.isEmpty()) return@forEach
            val number = NumberUtils.normalize(parts[0])
            if (number.isEmpty()) return@forEach
            list.add(
                SpamEntry(
                    number = number,
                    category = parts.getOrElse(1) { "" }.trim(),
                    negative = parts.getOrElse(2) { "0" }.trim().toIntOrNull() ?: 0,
                    positive = parts.getOrElse(3) { "0" }.trim().toIntOrNull() ?: 0,
                    neutral = parts.getOrElse(4) { "0" }.trim().toIntOrNull() ?: 0,
                    name = parts.getOrElse(5) { "" }.trim()
                )
            )
        }
        return list
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            if (conn.responseCode != 200) {
                throw IllegalStateException("HTTP ${conn.responseCode} for $url")
            }
            conn.inputStream.bufferedReader().use(BufferedReader::readText)
        } finally {
            conn.disconnect()
        }
    }
}
