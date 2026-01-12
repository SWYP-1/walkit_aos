package swyp.team.walkit.data.utils

import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.data.local.entity.SyncState
import swyp.team.walkit.domain.model.Grade
import timber.log.Timber

/**
 * Enum 타입 변환 유틸리티
 *
 * 클린 아키텍처 기준으로 공통 변환 로직을 제공합니다.
 * Mapper, UseCase, Network 등 모든 레이어에서 자유롭게 사용 가능합니다.
 */
object EnumConverter {

    /**
     * EmotionType을 String으로 변환 (en 값 사용)
     *
     * @param value 변환할 EmotionType (null이면 기본값 CONTENT의 en 반환)
     * @return EmotionType의 en 문자열
     */
    fun fromEmotionType(value: EmotionType?): String =
        value?.name ?: EmotionType.DELIGHTED.name

    /**
     * String을 EmotionType으로 안전하게 변환 (name 값으로 매칭)
     *
     * 변환이 실패하는 경우:
     * 1. 빈 문자열인 경우
     * 2. 유효하지 않은 enum 값인 경우
     * 3. null인 경우
     *
     * 이 경우 기본값(DELIGHTED)을 반환합니다.
     *
     * @param value 변환할 문자열 (name 값)
     * @return 변환된 EmotionType (실패 시 DELIGHTED)
     */
    fun toEmotionType(value: String?): EmotionType =
        try {
            if (value.isNullOrBlank()) {
                Timber.w("빈 문자열을 EmotionType으로 변환 시도, 기본값(CONTENT) 사용")
                EmotionType.DELIGHTED
            } else {
                EmotionType.valueOf(value.uppercase())
            }
        } catch (e: Throwable) {
            // ExceptionInInitializerError 등 Error 타입도 처리하기 위해 Throwable 사용
            // 에러 타입별로 상세 로그 기록
            when (e) {
                is IllegalArgumentException -> {
                    Timber.w(e, "EmotionType 변환 실패 (IllegalArgumentException): '$value', 기본값(CONTENT) 사용")
                }
                is ExceptionInInitializerError -> {
                    Timber.e(e, "EmotionType 변환 실패 (ExceptionInInitializerError): '$value', 기본값(CONTENT) 사용")
                }
                else -> {
                    Timber.e(e, "EmotionType 변환 실패 (기타 Throwable): '$value', 타입=${e.javaClass.simpleName}, 기본값(CONTENT) 사용")
                }
            }
            EmotionType.DELIGHTED
        }

    /**
     * SyncState를 String으로 변환
     *
     * @param value 변환할 SyncState (null이면 기본값 PENDING의 name 반환)
     * @return SyncState의 name 문자열
     */
    fun fromSyncState(value: SyncState?): String =
        value?.name ?: SyncState.PENDING.name

    /**
     * String을 SyncState로 안전하게 변환
     *
     * 변환이 실패하는 경우 기본값(PENDING)을 반환합니다.
     *
     * @param value 변환할 문자열
     * @return 변환된 SyncState (실패 시 PENDING)
     */
    fun toSyncState(value: String?): SyncState =
        try {
            if (value == null) SyncState.PENDING
            else SyncState.valueOf(value)
        } catch (e: Throwable) {
            // ExceptionInInitializerError 등 Error 타입도 처리하기 위해 Throwable 사용
            Timber.w(e, "SyncState 변환 실패: '$value', 기본값(PENDING) 사용")
            SyncState.PENDING
        }

    /**
     * Grade를 String으로 변환
     *
     * @param value 변환할 Grade (null이면 기본값 SEED의 name 반환)
     * @return Grade의 name 문자열
     */
    fun fromGrade(value: Grade?): String =
        value?.name ?: Grade.SEED.name

    /**
     * String을 Grade로 안전하게 변환
     *
     * 변환이 실패하는 경우 기본값(SEED)을 반환합니다.
     *
     * @param value 변환할 문자열
     * @return 변환된 Grade (실패 시 SEED)
     */
    fun toGrade(value: String?): Grade =
        try {
            if (value == null) Grade.SEED
            else Grade.valueOf(value)
        } catch (e: Throwable) {
            // ExceptionInInitializerError 등 Error 타입도 처리하기 위해 Throwable 사용
            Timber.w(e, "Grade 변환 실패: '$value', 기본값(SEED) 사용")
            Grade.SEED
        }
}






