package team.swyp.sdu.ui.mypage.userInfo

/**
 * 사용자 입력 데이터
 */
data class UserInput(
    val name: String = "",
    val nickname: String = "",
    val birthDate: String = "",
    val email: String? = null,
    val imageName: String? = null,
    val selectedImageUri: String? = null,
    val isNicknameDuplicate: Boolean? = null,  // null=확인전, true=중복, false=사용가능
    val nicknameValidationError: String? = null,
)
