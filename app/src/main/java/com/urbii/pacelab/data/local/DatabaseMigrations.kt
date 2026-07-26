package com.urbii.pacelab.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds referential integrity and idempotent sample indexes to databases from the first MVP build. */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        migrateMetricTable(database, "heart_rate_samples", "distanceMetersFromStart REAL")
        migrateMetricTable(database, "speed_samples", "distanceMetersFromStart REAL")
        migrateMetricTable(database, "cadence_samples", "distanceMetersFromStart REAL")
        migrateMetricTable(database, "elevation_samples", "distanceMetersFromStart REAL")
        migrateMetricTable(database, "vo2max_samples", "distanceMetersFromStart REAL")
        migrateMetricTable(database, "distance_segments", "distanceMetersFromStart REAL NOT NULL")
        migrateRouteTable(database)
    }
}

private fun migrateMetricTable(database: SupportSQLiteDatabase, table: String, distanceColumn: String) {
    val temporary = "${table}_migration"
    database.execSQL(
        """
        CREATE TABLE `$temporary` (
            `id` TEXT NOT NULL,
            `workoutId` TEXT NOT NULL,
            `timestampUtc` TEXT NOT NULL,
            `elapsedSeconds` REAL NOT NULL,
            $distanceColumn,
            `value` REAL NOT NULL,
            `sampleIndex` INTEGER NOT NULL,
            PRIMARY KEY(`id`),
            FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    database.execSQL("INSERT INTO `$temporary` SELECT * FROM `$table`")
    database.execSQL("DROP TABLE `$table`")
    database.execSQL("ALTER TABLE `$temporary` RENAME TO `$table`")
    database.execSQL("CREATE INDEX `index_${table}_workoutId_timestampUtc` ON `$table` (`workoutId`, `timestampUtc`)")
    database.execSQL("CREATE UNIQUE INDEX `index_${table}_workoutId_sampleIndex` ON `$table` (`workoutId`, `sampleIndex`)")
}

private fun migrateRouteTable(database: SupportSQLiteDatabase) {
    val table = "route_points"
    val temporary = "${table}_migration"
    database.execSQL(
        """
        CREATE TABLE `$temporary` (
            `id` TEXT NOT NULL,
            `workoutId` TEXT NOT NULL,
            `timestampUtc` TEXT NOT NULL,
            `elapsedSeconds` REAL NOT NULL,
            `latitude` REAL NOT NULL,
            `longitude` REAL NOT NULL,
            `altitudeMeters` REAL,
            `horizontalAccuracyMeters` REAL,
            `bearingDegrees` REAL,
            `sampleIndex` INTEGER NOT NULL,
            PRIMARY KEY(`id`),
            FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    database.execSQL("INSERT INTO `$temporary` SELECT * FROM `$table`")
    database.execSQL("DROP TABLE `$table`")
    database.execSQL("ALTER TABLE `$temporary` RENAME TO `$table`")
    database.execSQL("CREATE INDEX `index_${table}_workoutId_timestampUtc` ON `$table` (`workoutId`, `timestampUtc`)")
    database.execSQL("CREATE UNIQUE INDEX `index_${table}_workoutId_sampleIndex` ON `$table` (`workoutId`, `sampleIndex`)")
}
