package com.soywiz.util

import com.fasterxml.jackson.databind.JsonMappingException
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