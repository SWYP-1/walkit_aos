package team.swyp.sdu.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import team.swyp.sdu.data.local.dao.AppliedItemDao
import team.swyp.sdu.data.local.dao.MissionProgressDao
import team.swyp.sdu.data.local.dao.PurchasedItemDao
import team.swyp.sdu.data.local.dao.UserDao
import team.swyp.sdu.data.local.dao.WalkingSessionDao
import team.swyp.sdu.data.local.entity.AppliedItemEntity
import team.swyp.sdu.data.local.entity.MissionProgressEntity
import team.swyp.sdu.data.local.entity.PurchasedItemEntity
import team.swyp.sdu.data.local.entity.UserEntity
import team.swyp.sdu.data.local.entity.WalkingSessionEntity

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
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walkingSessionDao(): WalkingSessionDao
    abstract fun purchasedItemDao(): PurchasedItemDao
    abstract fun appliedItemDao(): AppliedItemDao
    abstract fun missionProgressDao(): MissionProgressDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "walking_database"
    }
}
