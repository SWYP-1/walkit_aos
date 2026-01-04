package team.swyp.sdu.ui.components

import org.json.JSONObject
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.LottieCharacterState
import team.swyp.sdu.domain.service.LottieImageProcessor
import timber.log.Timber

/**
 * 캐릭터 표시 관련 유틸리티 함수들
 */
object CharacterDisplayUtils {

    /**
     * 캐릭터 정보를 받아서 LottieCharacterState를 생성하는 범용 함수
     *
     * 이 함수는 캐릭터의 장착된 아이템 정보를 기반으로 Lottie JSON을 수정하여
     * 캐릭터 표시를 위한 상태 객체를 생성합니다.
     *
     * @param character 표시할 캐릭터 정보
     * @param lottieImageProcessor Lottie 이미지 처리기
     * @param baseLottieJson 기본 Lottie JSON 문자열
     * @return 캐릭터 표시를 위한 LottieCharacterState
     */
    suspend fun createLottieCharacterState(
        character: Character?,
        lottieImageProcessor: LottieImageProcessor,
        baseLottieJson: String = "{}"
    ): LottieCharacterState {
        return try {
            if (character == null) {
                Timber.d("캐릭터 정보가 없어 기본 상태 반환")
                return LottieCharacterState(
                    baseJson = baseLottieJson,
                    modifiedJson = null,
                    assets = emptyMap(),
                    isLoading = false
                )
            }

            Timber.d("캐릭터 Lottie 상태 생성 시작: level=${character.level}")

            // 캐릭터의 장착된 아이템 정보를 이용하여 Lottie JSON 수정
            val baseJsonObject = JSONObject(baseLottieJson)
            val modifiedJsonObject = lottieImageProcessor.updateCharacterPartsInLottie(
                baseLottieJson = baseJsonObject,
                character = character
            )
            val modifiedJson = modifiedJsonObject.toString()

            Timber.d("캐릭터 Lottie 상태 생성 완료")

            LottieCharacterState(
                baseJson = baseLottieJson,
                modifiedJson = modifiedJson,
                assets = emptyMap(), // assets 정보는 필요시 추가
                isLoading = false
            )

        } catch (t: Throwable) {
            Timber.e(t, "캐릭터 Lottie 상태 생성 실패")
            LottieCharacterState(
                baseJson = baseLottieJson,
                modifiedJson = null,
                assets = emptyMap(),
                isLoading = false,
                error = t.message ?: "캐릭터 표시 준비 실패"
            )
        }
    }

    /**
     * 캐릭터 표시를 위한 Compose 함수 사용법 예시:
     *
     * ViewModel에서 LottieCharacterState를 생성하여 StateFlow로 제공하고,
     * Composable에서 collect하여 LottieCharacterDisplay에 전달하세요.
     *
     * ViewModel 예시:
     * ```
     * private val _characterState = MutableStateFlow<LottieCharacterState?>(null)
     * val characterState: StateFlow<LottieCharacterState?> = _characterState
     *
     * fun loadCharacterDisplay(character: Character) {
     *     viewModelScope.launch {
     *         val state = CharacterDisplayUtils.createLottieCharacterState(
     *             character = character,
     *             lottieImageProcessor = lottieImageProcessor,
     *             baseLottieJson = "{}"
     *         )
     *         _characterState.value = state
     *     }
     * }
     * ```
     *
     * Composable 예시:
     * ```
     * val characterState by viewModel.characterState.collectAsStateWithLifecycle()
     *
     * LottieCharacterDisplay(
     *     characterLottieState = characterState,
     *     modifier = Modifier.size(200.dp)
     * )
     * ```
     */
}
