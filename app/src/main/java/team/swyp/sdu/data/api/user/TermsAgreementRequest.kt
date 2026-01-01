package team.swyp.sdu.data.api.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 약관 동의 요청 DTO
 */
@Serializable
data class TermsAgreementRequest(
    @SerialName("termsAgreed")
    val termsAgreed: Boolean,
    
    @SerialName("privacyAgreed")
    val privacyAgreed: Boolean,
    
    @SerialName("locationAgreed")
    val locationAgreed: Boolean,
    
    @SerialName("marketingConsent")
    val marketingConsent: Boolean,
)





