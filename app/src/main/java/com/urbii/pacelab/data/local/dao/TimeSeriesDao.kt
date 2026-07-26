package com.urbii.pacelab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.urbii.pacelab.data.local.entity.CadenceSampleEntity
import com.urbii.pacelab.data.local.entity.DistanceSegmentEntity
import com.urbii.pacelab.data.local.entity.ElevationSampleEntity
import com.urbii.pacelab.data.local.entity.HeartRateSampleEntity
import com.urbii.pacelab.data.local.entity.RoutePointEntity
import com.urbii.pacelab.data.local.entity.SpeedSampleEntity
import com.urbii.pacelab.data.local.entity.Vo2MaxSampleEntity

@Dao
interface TimeSeriesDao {
    @Query("DELETE FROM heart_rate_samples WHERE workoutId = :workoutId")
    suspend fun deleteHeartRate(workoutId: String)

    @Query("DELETE FROM speed_samples WHERE workoutId = :workoutId")
    suspend fun deleteSpeed(workoutId: String)

    @Query("DELETE FROM cadence_samples WHERE workoutId = :workoutId")
    suspend fun deleteCadence(workoutId: String)

    @Query("DELETE FROM elevation_samples WHERE workoutId = :workoutId")
    suspend fun deleteElevation(workoutId: String)

    @Query("DELETE FROM distance_segments WHERE workoutId = :workoutId")
    suspend fun deleteDistance(workoutId: String)

    @Query("DELETE FROM vo2max_samples WHERE workoutId = :workoutId")
    suspend fun deleteVo2Max(workoutId: String)

    @Query("DELETE FROM route_points WHERE workoutId = :workoutId")
    suspend fun deleteRoute(workoutId: String)

    suspend fun deleteAll(workoutId: String) {
        deleteHeartRate(workoutId)
        deleteSpeed(workoutId)
        deleteCadence(workoutId)
        deleteElevation(workoutId)
        deleteDistance(workoutId)
        deleteVo2Max(workoutId)
        deleteRoute(workoutId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(samples: List<HeartRateSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeed(samples: List<SpeedSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCadence(samples: List<CadenceSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElevation(samples: List<ElevationSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistance(samples: List<DistanceSegmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVo2Max(samples: List<Vo2MaxSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(samples: List<RoutePointEntity>)

    @Query("SELECT * FROM heart_rate_samples WHERE workoutId = :workoutId ORDER BY timestampUtc")
    suspend fun heartRate(workoutId: String): List<HeartRateSampleEntity>

    @Query("SELECT * FROM speed_samples WHERE workoutId = :workoutId ORDER BY timestampUtc")
    suspend fun speed(workoutId: String): List<SpeedSampleEntity>

    @Query("SELECT * FROM cadence_samples WHERE workoutId = :workoutId ORDER BY timestampUtc")
    suspend fun cadence(workoutId: String): List<CadenceSampleEntity>

    @Query("SELECT * FROM elevation_samples WHERE workoutId = :workoutId ORDER BY timestampUtc")
    suspend fun elevation(workoutId: String): List<ElevationSampleEntity>

    @Query("SELECT * FROM distance_segments WHERE workoutId = :workoutId ORDER BY timestampUtc")
    suspend fun distance(workoutId: String): List<DistanceSegmentEntity>

    @Query("SELECT * FROM vo2max_samples WHERE workoutId = :workoutId ORDER BY timestampUtc")
    suspend fun vo2Max(workoutId: String): List<Vo2MaxSampleEntity>

    @Query("SELECT * FROM route_points WHERE workoutId = :workoutId ORDER BY timestampUtc")
    suspend fun route(workoutId: String): List<RoutePointEntity>
}
