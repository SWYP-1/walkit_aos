package swyp.team.walkit.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import swyp.team.walkit.data.local.dao.AppliedItemDao
import swyp.team.walkit.data.local.dao.CharacterDao
import swyp.team.walkit.data.local.dao.GoalDao
import swyp.team.walkit.data.local.dao.MissionProgressDao
import swyp.team.walkit.data.local.dao.NotificationSettingsDao
import swyp.team.walkit.data.local.dao.PurchasedItemDao
import swyp.team.walkit.data.local.dao.UserDao
import swyp.team.walkit.data.local.dao.WalkingSessionDao
import swyp.team.walkit.data.local.entity.AppliedItemEntity
import swyp.team.walkit.data.local.entity.CharacterEntity
import swyp.team.walkit.data.local.entity.GoalEntity
import swyp.team.walkit.data.local.entity.MissionProgressEntity
import swyp.team.walkit.data.local.entity.NotificationSettingsEntity
import swyp.team.walkit.data.local.entity.PurchasedItemEntity
import swyp.team.walkit.data.local.entity.UserEntity
import swyp.team.walkit.data.local.entity.WalkingSessionEntity

/**
 * Room Database
 *
 * 앱의 로컬 데이터베이스입니다.
 * Hilt를 통해 싱글톤으로 제공됩니다.
 */
@Database(
    entities = [
        WalkingSessionEntity::class,
        PurchasedItemEntity::class,
        AppliedItemEntity::class,
        MissionProgressEntity::class,
        UserEntity::class,
        CharacterEntity::class,
        GoalEntity::class,
        NotificationSettingsEntity::class,
    ],
    version = 13,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walkingSessionDao(): WalkingSessionDao
    abstract fun purchasedItemDao(): PurchasedItemDao
    abstract fun appliedItemDao(): AppliedItemDao
    abstract fun missionProgressDao(): MissionProgressDao
    abstract fun userDao(): UserDao
    abstract fun characterDao(): CharacterDao
    abstract fun goalDao(): GoalDao
    abstract fun notificationSettingsDao(): NotificationSettingsDao

    companion object {
        /**
         * Migration from version 12 to 13: Change UserEntity PrimaryKey from nickname to userId
         */
        const val DATABASE_NAME = "walking_database"
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create new table with userId as PrimaryKey
                database.execSQL("""
                    CREATE TABLE user_profile_new (
                        userId INTEGER PRIMARY KEY NOT NULL,
                        nickname TEXT NOT NULL,
                        imageName TEXT,
                        birthDate TEXT,
                        email TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                // 2. Copy data from old table (only rows where userId is not null, pick one record per userId)
                database.execSQL("""
                    INSERT OR REPLACE INTO user_profile_new (userId, nickname, imageName, birthDate, email, updatedAt)
                    SELECT userId, nickname, imageName, birthDate, email, updatedAt
                    FROM user_profile
                    WHERE userId IS NOT NULL
                    ORDER BY updatedAt DESC
                """)

                // 3. Drop old table
                database.execSQL("DROP TABLE user_profile")

                // 4. Rename new table to original name
                database.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")
            }
        }
    }

}
