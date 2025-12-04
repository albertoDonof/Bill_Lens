package com.example.billlens.utils

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        // Converte un BigDecimal in una stringa semplice, es. 123.45 -> "123.45"
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        // Converte una stringa di nuovo in un BigDecimal
        return value?.let { BigDecimal(it) }
    }
}