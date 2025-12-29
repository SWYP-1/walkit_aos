package team.swyp.sdu.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 캐릭터 등급
 * 
 * 각 등급은 고정된 레벨을 가집니다:
 * - SEED: Lv.1
 * - SPROUT: Lv.2
 * - TREE: Lv.3
 */
@Serializable
enum class Grade(val level: Int) {
    @SerialName("SEED")
    SEED(1),
    
    @SerialName("SPROUT")
    SPROUT(2),
    
    @SerialName("TREE")
    TREE(3),
}


