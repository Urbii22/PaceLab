package com.urbii.pacelab.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneOffset

class Converters {
    @TypeConverter fun instantToString(value: Instant?): String? = value?.toString()
    @TypeConverter fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)
    @TypeConverter fun zoneOffsetToString(value: ZoneOffset?): String? = value?.id
    @TypeConverter fun stringToZoneOffset(value: String?): ZoneOffset? = value?.let(ZoneOffset::of)
}
