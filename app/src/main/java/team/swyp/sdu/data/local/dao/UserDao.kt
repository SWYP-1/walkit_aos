package team.swyp.sdu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.data.local.entity.UserEntity

/**
 * 사용자 캐시 DAO
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}
