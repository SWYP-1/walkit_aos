package swyp.team.walkit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import swyp.team.walkit.data.local.entity.UserEntity

/**
 * 사용자 캐시 DAO - UID 기반 단일 유저 관리
 */
@Dao
interface UserDao {
    /**
     * UID로 사용자 관찰 (단일 유저 캐시)
     *
     * @param uid 사용자 UID
     * @return 해당 UID의 사용자 Flow, 없으면 null
     */
    @Query("SELECT * FROM user_profile WHERE userId = :uid LIMIT 1")
    fun observeUserByUid(uid: Long): Flow<UserEntity?>

    /**
     * UID로 사용자 조회 (단일 유저 캐시)
     *
     * @param uid 사용자 UID
     * @return 해당 UID의 사용자, 없으면 null
     */
    @Query("SELECT * FROM user_profile WHERE userId = :uid LIMIT 1")
    suspend fun getUserByUid(uid: Long): UserEntity?

    /**
     * 현재 저장된 사용자 관찰 (단일 유저 캐시)
     *
     * @return 저장된 사용자 Flow, 없으면 null
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    /**
     * 현재 저장된 사용자 조회 (단일 유저 캐시)
     *
     * @return 저장된 사용자, 없으면 null
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserEntity)

    /**
     * UID로 프로필 이미지 업데이트
     *
     * @param uid 사용자 UID
     * @param imageName 업데이트할 이미지 이름
     * @param updatedAt 업데이트 시간
     */
    @Query("UPDATE user_profile SET imageName = :imageName, updatedAt = :updatedAt WHERE userId = :uid")
    suspend fun updateImageNameByUid(
        uid: Long,
        imageName: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}
