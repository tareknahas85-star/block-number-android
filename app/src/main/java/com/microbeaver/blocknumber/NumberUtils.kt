package com.microbeaver.blocknumber

/**
 * Number normalization and wildcard matching shared by the blacklist
 * and the spam database.
 */
object NumberUtils {

    /** Keeps digits and a single leading '+'. "+963 11-223 344" -> "+96311223344" */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val sb = StringBuilder()
        trimmed.forEachIndexed { i, ch ->
            when {
                ch.isDigit() -> sb.append(ch)
                ch == '+' && i == 0 -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /** Like [normalize] but also keeps the wildcard characters '*' and '#'. */
    fun normalizePattern(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val sb = StringBuilder()
        trimmed.forEachIndexed { i, ch ->
            when {
                ch.isDigit() || ch == '*' || ch == '#' -> sb.append(ch)
                ch == '+' && i == 0 -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Wildcard match: '*' matches any sequence of digits (incl. empty),
     * '#' matches exactly one digit. Leading '+' is treated loosely so
     * "+9665*" also matches "9665xxxxxxx".
     */
    fun wildcardMatches(pattern: String, number: String): Boolean {
        val p = normalizePattern(pattern)
        val n = normalize(number)
        if (p.isEmpty() || n.isEmpty()) return false
        val body = buildString {
            p.forEach { ch ->
                when (ch) {
                    '*' -> append("[0-9]*")
                    '#' -> append("[0-9]")
                    '+' -> {} // handled below
                    else -> append(ch)
                }
            }
        }
        return try {
            val regex = Regex("^\\+?$body$")
            regex.matches(n) || regex.matches(n.removePrefix("+"))
        } catch (e: Exception) {
            false
        }
    }
}
