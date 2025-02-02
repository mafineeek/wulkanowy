package io.github.wulkanowy.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset
import java.util.Date

class Converters {

    @TypeConverter
    fun timestampToDate(value: Long?): LocalDate? = value?.run {
        Date(value).toInstant().atZone(ZoneOffset.UTC).toLocalDate()
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    }

    @TypeConverter
    fun timestampToTime(value: Long?): LocalDateTime? = value?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC)
    }

    @TypeConverter
    fun timeToTimestamp(date: LocalDateTime?): Long? {
        return date?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun monthToInt(month: Month?) = month?.value

    @TypeConverter
    fun intToMonth(value: Int?) = value?.let { Month.of(it) }

    @TypeConverter
    fun intListToGson(list: List<Int>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun gsonToIntList(value: String): List<Int> {
        return Gson().fromJson(value, object : TypeToken<List<Int>>() {}.type)
    }

    @TypeConverter
    fun stringPairListToGson(list: List<Pair<String, String>>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun gsonToStringPairList(value: String): List<Pair<String, String>> {
        return Gson().fromJson(value, object : TypeToken<List<Pair<String, String>>>() {}.type)
    }
}
