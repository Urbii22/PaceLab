package com.urbii.pacelab.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.urbii.pacelab.data.local.dao.SyncDao
import com.urbii.pacelab.data.local.dao.WorkoutDao
import com.urbii.pacelab.data.local.entity.CadenceSampleEntity
import com.urbii.pacelab.data.local.entity.DistanceSegmentEntity
import com.urbii.pacelab.data.local.entity.ElevationSampleEntity
import com.urbii.pacelab.data.local.entity.HeartRateSampleEntity
import com.urbii.pacelab.data.local.entity.RoutePointEntity
import com.urbii.pacelab.data.local.entity.SpeedSampleEntity
import com.urbii.pacelab.data.local.entity.SyncStateEntity
import com.urbii.pacelab.data.local.entity.Vo2MaxSampleEntity
import com.urbii.pacelab.data.local.entity.WorkoutAnnotationEntity
import com.urbii.pacelab.data.local.entity.WorkoutEntity

@Database(
    entities = [
        WorkoutEntity::class,
        HeartRateSampleEntity::class,
        SpeedSampleEntity::class,
        CadenceSampleEntity::class,
        ElevationSampleEntity::class,
        DistanceSegmentEntity::class,
        Vo2MaxSampleEntity::class,
        RoutePointEntity::class,
        WorkoutAnnotationEntity::class,
        SyncStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PaceLabDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun syncDao(): SyncDao
}
