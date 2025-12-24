package team.swyp.sdu.data.remote.walking.dto

import kotlinx.serialization.Serializable

/**
 * 캐릭터 등급
 */
@Serializable
enum class Grade {
    @kotlinx.serialization.SerialName("SEED")
    SEED,
    
    @kotlinx.serialization.SerialName("SPROUT")
    SPROUT,
    
    @kotlinx.serialization.SerialName("TREE")
    TREE,
}


