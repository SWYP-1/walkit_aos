package swyp.team.walkit.data.local.mapper

import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Test
import swyp.team.walkit.data.remote.walking.dto.CharacterDto
import swyp.team.walkit.data.remote.walking.dto.ItemImageDto
import swyp.team.walkit.data.remote.walking.mapper.CharacterMapper
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterImage
import swyp.team.walkit.domain.model.Grade

class CharacterMapperTest {

    @Test
    fun `CharacterDto가 Character로 올바르게 변환된다`() {
        // Given
        val characterDto = CharacterDto(
            backgroundImageName = "background_forest.png",
            bodyImage = ItemImageDto(
                imageName = "body_red_jacket.png",
                itemTag = null
            ),
            characterImageName = "character_seed.png",
            feetImage = ItemImageDto(
                imageName = "feet_blue_socks.png",
                itemTag = null
            ),
            grade = "SEED",
            headImage = ItemImageDto(
                imageName = "head_green_hat.png",
                itemTag = "TOP"
            ),
            level = 5,
            nickName = "테스트사용자"
        )

        // When
        val result = CharacterMapper.toDomain(characterDto)

        // Then
        Assert.assertEquals("테스트사용자", result.nickName)
        Assert.assertEquals(5, result.level)
        Assert.assertEquals(Grade.SEED, result.grade)
        Assert.assertEquals("background_forest.png", result.backgroundImageName)
        Assert.assertEquals("character_seed.png", result.characterImageName)

        // Body image (itemTag가 null이므로 null)
        assertNotNull(result.bodyImage)
        Assert.assertEquals("body_red_jacket.png", result.bodyImage?.imageName)
        Assert.assertEquals(null, result.bodyImage?.itemTag)

        // Head image (itemTag가 "TOP")
        assertNotNull(result.headImage)
        Assert.assertEquals("head_green_hat.png", result.headImage?.imageName)
        Assert.assertEquals("TOP", result.headImage?.itemTag)

        // Feet image
        assertNotNull(result.feetImage)
        Assert.assertEquals("feet_blue_socks.png", result.feetImage?.imageName)
        Assert.assertEquals(null, result.feetImage?.itemTag)
    }

    @Test
    fun `null 값들이 안전하게 처리된다`() {
        // Given
        val characterDto = CharacterDto(
            backgroundImageName = null,
            bodyImage = null,
            characterImageName = null,
            feetImage = null,
            grade = "TREE",
            headImage = null,
            level = 1,
            nickName = null
        )

        // When
        val result = CharacterMapper.toDomain(characterDto)

        // Then
        Assert.assertEquals("게스트", result.nickName) // 기본값
        Assert.assertEquals(1, result.level)
        Assert.assertEquals(Grade.TREE, result.grade)
        Assert.assertEquals(null, result.backgroundImageName)
        Assert.assertEquals(null, result.characterImageName)
        Assert.assertEquals(null, result.bodyImage)
        Assert.assertEquals(null, result.headImage)
        Assert.assertEquals(null, result.feetImage)
    }


    @Test
    fun `Character의 백워드 호환성 게터들이 올바르게 작동한다`() {
        // Given
        val character = Character(
            nickName = "호환성테스트",
            level = 3,
            grade = Grade.SPROUT,
            headImage = CharacterImage("head_test.png", "DECOR"),
            bodyImage = CharacterImage("body_test.png", null),
            feetImage = CharacterImage("feet_test.png", null)
        )

        // When & Then - 백워드 호환성 게터들
        Assert.assertEquals("head_test.png", character.headImageName)
        Assert.assertEquals("DECOR", character.headImageTag)
        Assert.assertEquals("body_test.png", character.bodyImageName)
        Assert.assertEquals(null, character.bodyImage?.itemTag) // body는 tag가 없으므로 null
        Assert.assertEquals("feet_test.png", character.feetImageName)
        Assert.assertEquals(null, character.feetImage?.itemTag) // feet는 tag가 없으므로 null
    }

    @Test
    fun `null CharacterImage가 있는 경우 백워드 호환성 게터가 null을 반환한다`() {
        // Given
        val character = Character(
            nickName = "널테스트",
            level = 1,
            grade = Grade.SEED,
            headImage = null,
            bodyImage = null,
            feetImage = null
        )

        // When & Then
        Assert.assertEquals(null, character.headImageName)
        Assert.assertEquals(null, character.headImageTag)
        Assert.assertEquals(null, character.bodyImageName)
        Assert.assertEquals(null, character.bodyImage?.itemTag)
        Assert.assertEquals(null, character.feetImageName)
        Assert.assertEquals(null, character.feetImage?.itemTag)
    }
}
