package team.swyp.sdu.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 산책 저장 API 응답
 * 
 * 서버에 산책 데이터를 저장한 후 반환되는 응답입니다.
 */
@Serializable
data class WalkSaveResponse(
    /**
     * 저장된 산책 세션의 고유 ID
     */
    val id: String,
    
    /**
     * 서버에 업로드된 이미지 URL
     * 이미지가 없거나 업로드 실패 시 null
     */
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    
    /**
     * 생성 시간 (ISO 8601 형식)
     */
    @SerialName("createdAt")
    val createdAt: String? = null
)








