package swyp.team.walkit.data.local.mapper

import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Test
import swyp.team.walkit.data.local.entity.UserEntity
import swyp.team.walkit.domain.model.User

/**
 * UserMapper 단위 테스트
 *
 * 테스트 대상:
 * - toEntity(): User → UserEntity 변환
 * - toDomain(): UserEntity → User 변환 (userId nullable 처리)
 * - 양방향 변환 일관성
 */
class UserMapperTest {

    @Test
    fun `toEntity - User를 UserEntity로 변환`() {
        // Given: User 도메인 객체
        val user = User(
            userId = 123L,
            nickname = "테스트유저",
            imageName = "profile.jpg",
            birthDate = "1990-01-01",
            email = "test@example.com"
        )

        // When: Entity로 변환
        val entity = UserMapper.toEntity(user)

        // Then: 값이 올바르게 매핑됨
        Assert.assertEquals("테스트유저", entity.nickname)
        Assert.assertEquals(123L, entity.userId)
        Assert.assertEquals("profile.jpg", entity.imageName)
        Assert.assertEquals("1990-01-01", entity.birthDate)
        Assert.assertEquals("test@example.com", entity.email)
    }

    @Test
    fun `toDomain - UserEntity를 User로 변환 (userId not null)`() {
        // Given: UserEntity 객체 (userId가 null이 아님)
        val entity = UserEntity(
            nickname = "홍길동",
            userId = 456L,
            imageName = "avatar.png",
            birthDate = "1985-05-15",
            email = "hong@example.com"
        )

        // When: Domain으로 변환
        val user = UserMapper.toDomain(entity)

        // Then: 값이 올바르게 매핑됨
        Assert.assertEquals(456L, user.userId)
        Assert.assertEquals("홍길동", user.nickname)
        Assert.assertEquals("avatar.png", user.imageName)
        Assert.assertEquals("1985-05-15", user.birthDate)
        Assert.assertEquals("hong@example.com", user.email)
    }

    @Test
    fun `toDomain - UserEntity를 User로 변환 (userId null)`() {
        // Given: UserEntity 객체 (userId가 null)
        val entity = UserEntity(
            nickname = "김철수",
            userId = null,  // null 값
            imageName = "default.jpg",
            birthDate = "2000-12-25",
            email = "kim@example.com"
        )

        // When: Domain으로 변환
        val user = UserMapper.toDomain(entity)

        // Then: userId가 0L로 설정됨
        Assert.assertEquals(0L, user.userId)
        Assert.assertEquals("김철수", user.nickname)
        Assert.assertEquals("default.jpg", user.imageName)
        Assert.assertEquals("2000-12-25", user.birthDate)
        Assert.assertEquals("kim@example.com", user.email)
    }

    @Test
    fun `양방향 변환 - User ↔ UserEntity 일관성 보장 (userId not null)`() {
        // Given: 원본 User (userId가 설정된 경우)
        val originalUser = User(
            userId = 789L,
            nickname = "이영희",
            imageName = "photo.jpg",
            birthDate = "1995-08-20",
            email = "lee@example.com"
        )

        // When: User → Entity → User 변환
        val entity = UserMapper.toEntity(originalUser)
        val convertedUser = UserMapper.toDomain(entity)

        // Then: 원본과 변환 결과가 동일함
        Assert.assertEquals(originalUser, convertedUser)
    }

    @Test
    fun `양방향 변환 - UserEntity nullable userId 처리`() {
        // Given: Entity에 null userId가 있는 경우
        val entityWithNullId = UserEntity(
            nickname = "박민수",
            userId = null,
            imageName = "image.png",
            birthDate = "1988-03-10",
            email = "park@example.com"
        )

        // When: Entity → User 변환
        val user = UserMapper.toDomain(entityWithNullId)

        // Then: userId가 0L로 설정되고 다른 값들은 유지됨
        Assert.assertEquals(0L, user.userId)
        Assert.assertEquals("박민수", user.nickname)
        Assert.assertEquals("image.png", user.imageName)
        Assert.assertEquals("1988-03-10", user.birthDate)
        Assert.assertEquals("park@example.com", user.email)

        // When: 다시 Entity로 변환
        val convertedEntity = UserMapper.toEntity(user)

        // Then: userId가 0L로 설정됨 (원래 null이었던 것과 다름)
        Assert.assertEquals(0L, convertedEntity.userId)
        Assert.assertEquals("박민수", convertedEntity.nickname)
    }

    @Test
    fun `다양한 값들로 변환 테스트`() {
        // Given: 다양한 User 값들
        val testUsers = listOf(
            User(1L, "테스트1", "img1.jpg", "1990-01-01", "test1@test.com"),
            User(999L, "테스트2", "null", null, null),  // nullable 필드들
            User(Long.MAX_VALUE, "", "", "", ""),    // 빈 문자열들
            User(0L, "최대길이닉네임최대길이닉네임최대길이", "very_long_image_name.jpg", "2023-12-31", "very_long_email_address@example.com")
        )

        // When & Then: 모든 케이스에서 변환 수행
        testUsers.forEach { originalUser ->
            val entity = UserMapper.toEntity(originalUser)
            val convertedUser = UserMapper.toDomain(entity)

            // userId가 null이 아니었던 경우만 비교 (null이었던 경우는 0L로 변환되므로)
            if (originalUser.userId != 0L) {
                Assert.assertEquals("User 변환 실패: $originalUser", originalUser, convertedUser)
            } else {
                // userId가 0L인 경우는 특별 처리 확인
                Assert.assertEquals(0L, convertedUser.userId)
                Assert.assertEquals(originalUser.nickname, convertedUser.nickname)
                Assert.assertEquals(originalUser.imageName, convertedUser.imageName)
                Assert.assertEquals(originalUser.birthDate, convertedUser.birthDate)
                Assert.assertEquals(originalUser.email, convertedUser.email)
            }
        }
    }

    @Test
    fun `null 값 처리 테스트`() {
        // Given: 모든 nullable 필드가 null인 User
        val userWithNulls = User(
            userId = 100L,
            nickname = "널테스트",
            imageName = null,
            birthDate = null,
            email = null
        )

        // When: 변환 수행
        val entity = UserMapper.toEntity(userWithNulls)
        val convertedUser = UserMapper.toDomain(entity)

        // Then: null 값들이 유지됨
        Assert.assertEquals(userWithNulls, convertedUser)
        assertNull(convertedUser.imageName)
        assertNull(convertedUser.birthDate)
        assertNull(convertedUser.email)
    }
}