package team.swyp.sdu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.data.local.entity.CharacterEntity

/**
 * 캐릭터 정보 DAO
 */
@Dao
interface CharacterDao {
    @Query("SELECT * FROM character_profile WHERE userId = :userId LIMIT 1")
    fun observeCharacter(userId: Long): Flow<CharacterEntity?>

    @Query("SELECT * FROM character_profile WHERE userId = :userId LIMIT 1")
    suspend fun getCharacter(userId: Long): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CharacterEntity)

    @Query("DELETE FROM character_profile WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Long)

    @Query("DELETE FROM character_profile")
    suspend fun clear()
}





