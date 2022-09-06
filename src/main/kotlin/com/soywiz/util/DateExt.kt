package com.soywiz.util

import java.text.*
import java.util.*

private val TYPICAL_DATE_FORMAT_NO_TIME = SimpleDateFormat("yyyy-MM-dd'Z'").also {
    it.timeZone = TimeZone.getTimeZone("UTC")
}

private val TYPICAL_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").also {
    it.timeZone = TimeZone.getTimeZone("UTC")
}

fun Date.toCannonicalString(): String = TYPICAL_DATE_FORMAT.format(this)

val Date.fullYear: Int get() = 1900 + this.year
val Date.month1: Int get() = this.month + 1
val Date.dayInMonth: Int get() = this.date

fun DateParse(str: String): Date {
    val dateFormat = determineDateFormat(str)
    if (dateFormat != null) {
        return SimpleDateFormat(dateFormat).parse(str)
    }

    return try {
        Date(str)
    } catch (e: IllegalArgumentException) {
        val str = if (!str.endsWith("Z")) "${str}Z" else str
        try {
            TYPICAL_DATE_FORMAT.parse(str)
        } catch (e: ParseException) {
            TYPICAL_DATE_FORMAT_NO_TIME.parse(str)
        }
    }
}

private val DATE_FORMAT_REGEXPS = mapOf(
    Regex("^\\d{8}$") to "yyyyMMdd",
    Regex("^\\d{1,2}-\\d{1,2}-\\d{4}$") to "dd-MM-yyyy",
    Regex("^\\d{4}-\\d{1,2}-\\d{1,2}$") to "yyyy-MM-dd",
    Regex("^\\d{1,2}/\\d{1,2}/\\d{4}$") to "MM/dd/yyyy",
    Regex("^\\d{4}/\\d{1,2}/\\d{1,2}$") to "yyyy/MM/dd",
    Regex("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$") to "dd MMM yyyy",
    Regex("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$") to "dd MMMM yyyy",
    Regex("^\\d{12}$") to "yyyyMMddHHmm",
    Regex("^\\d{8}\\s\\d{4}$") to "yyyyMMdd HHmm",
    Regex("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$") to "dd-MM-yyyy HH:mm",
    Regex("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$") to "yyyy-MM-dd HH:mm",
    Regex("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$") to "MM/dd/yyyy HH:mm",
    Regex("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$") to "yyyy/MM/dd HH:mm",
    Regex("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$") to "dd MMM yyyy HH:mm",
    Regex("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$") to "dd MMMM yyyy HH:mm",
    Regex("^\\d{14}$") to "yyyyMMddHHmmss",
    Regex("^\\d{8}\\s\\d{6}$") to "yyyyMMdd HHmmss",
    Regex("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$") to "dd-MM-yyyy HH:mm:ss",
    Regex("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$") to "yyyy-MM-dd HH:mm:ss",
    Regex("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$") to "MM/dd/yyyy HH:mm:ss",
    Regex("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$") to "yyyy/MM/dd HH:mm:ss",
    Regex("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$") to "dd MMM yyyy HH:mm:ss",
    Regex("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$") to "dd MMMM yyyy HH:mm:ss",
)

/**
 * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
 * format is unknown. You can simply extend DateUtil with more formats if needed.
 * @param dateString The date string to determine the SimpleDateFormat pattern for.
 * @return The matching SimpleDateFormat pattern, or null if format is unknown.
 * @see SimpleDateFormat
 */
private fun determineDateFormat(dateString: String): String? {
    for (regexp in DATE_FORMAT_REGEXPS.keys) {
        if (dateString.lowercase(Locale.getDefault()).matches(regexp)) {
            return DATE_FORMAT_REGEXPS[regexp]
        }
    }
    return null // Unknown format.
}
