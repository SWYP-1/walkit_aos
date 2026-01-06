package swyp.team.walkit.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 11,
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
        const val DATABASE_NAME = "walking_database"
    }
}
