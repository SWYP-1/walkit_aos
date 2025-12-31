package team.swyp.sdu.utils

import kotlin.random.Random
import team.swyp.sdu.data.model.Emotion
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.domain.service.ActivityType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 산책 기록 테스트 데이터 생성 유틸리티
 *
 * 10일치의 산책 기록을 생성합니다.
 * 각 기록에는 감정(Emotion) 리스트가 포함됩니다.
 */
object WalkingTestData {
/**
 * 40일치 산책 기록 테스트 데이터 생성
 *
 * @return 40개의 WalkingSession 리스트
 */
    fun generateTestSessions(): List<WalkingSession> {
        val sessions = mutableListOf<WalkingSession>()
        val today = LocalDate.now()
        val baseLat = 37.2411 // 용인시청 위도
        val baseLon = 127.1776 // 용인시청 경도

        // 감정 타입 리스트 (다양한 감정 조합)
        val emotionTypes = listOf(
            listOf(EmotionType.HAPPY, EmotionType.JOYFUL),
            listOf(EmotionType.CONTENT, EmotionType.JOYFUL),
            listOf(EmotionType.HAPPY, EmotionType.CONTENT),
            listOf(EmotionType.JOYFUL, EmotionType.CONTENT),
            listOf(EmotionType.DEPRESSED, EmotionType.TIRED),
            listOf(EmotionType.ANXIOUS, EmotionType.TIRED),
            listOf(EmotionType.DEPRESSED, EmotionType.ANXIOUS),
            listOf(EmotionType.HAPPY, EmotionType.DEPRESSED),
            listOf(EmotionType.JOYFUL, EmotionType.ANXIOUS),
            listOf(EmotionType.CONTENT, EmotionType.TIRED),
        )

        // 감정 메모 리스트
        val emotionNotes = listOf(
            "오늘 날씨가 좋아서 기분이 좋았어요",
            "평화로운 산책이었습니다",
            "목표를 달성해서 뿌듯해요",
            "편안하게 걷는 시간이었어요",
            "친구와 함께 걸어서 즐거웠어요",
            "운동 후 기분이 상쾌해요",
            "스트레스 해소에 좋았어요",
            "새로운 경로를 발견했어요",
            "조용한 시간을 가질 수 있었어요",
            "활기찬 하루였어요",
        )

        for (i in 0 until 40) {
            val date = today.minusDays((9 - i).toLong())
            val startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val durationMinutes = Random.nextInt(20, 61) // 20분 ~ 60분
            val endTime = startTime + (durationMinutes * 60 * 1000L)

            // 걸음 수 (분당 약 100~120걸음 기준)
            val stepCount = durationMinutes * Random.nextInt(100, 121)

            // 거리 (걸음 수 기반, 보폭 약 0.7m)
            val totalDistance = stepCount * 0.7f

            // 위치 포인트 생성 (산책 경로)
            val locations = generateLocationPoints(
                baseLat = baseLat + (Math.random() - 0.5) * 0.01, // 약 500m 범위 내
                baseLon = baseLon + (Math.random() - 0.5) * 0.01,
                startTime = startTime,
                duration = durationMinutes,
                pointCount = Random.nextInt(15, 31), // 15~30개 포인트
            )

            // 활동 통계 생성
            val activityStats = generateActivityStats(
                duration = durationMinutes * 60 * 1000L,
                totalDistance = totalDistance,
            )

            // 감정 리스트 생성 (1~3개)
            val emotionCount = Random.nextInt(1, 4)
            val sessionEmotions = mutableListOf<Emotion>()
            val selectedEmotionTypes = emotionTypes[i % emotionTypes.size].shuffled().take(emotionCount)

            for (j in 0 until emotionCount) {
                val emotionTime = startTime + (j * durationMinutes * 60 * 1000L / emotionCount)
                sessionEmotions.add(
                    Emotion(
                        type = selectedEmotionTypes[j % selectedEmotionTypes.size],
                        timestamp = emotionTime,
                        note = if (j == 0) emotionNotes[i % emotionNotes.size] else null,
                    ),
                )
            }

            // 산책 전/후 감정 랜덤 선택
            val allEmotionTypes = listOf(
                EmotionType.HAPPY,
                EmotionType.JOYFUL,
                EmotionType.CONTENT,
                EmotionType.DEPRESSED,
                EmotionType.TIRED,
                EmotionType.ANXIOUS,
            )
            val preWalkEmotion = allEmotionTypes.random()
            val postWalkEmotion = allEmotionTypes.random()

            sessions.add(
                WalkingSession(
                    startTime = startTime,
                    endTime = endTime,
                    stepCount = stepCount,
                    locations = locations,
                    totalDistance = totalDistance,
                    preWalkEmotion = preWalkEmotion,
                    postWalkEmotion = postWalkEmotion,
                    createdDate = DateUtils.formatToIsoDateTime(startTime)
                ),
            )
        }

        return sessions
    }

    /**
     * 위치 포인트 리스트 생성
     */
    private fun generateLocationPoints(
        baseLat: Double,
        baseLon: Double,
        startTime: Long,
        duration: Int,
        pointCount: Int,
    ): List<LocationPoint> {
        val locations = mutableListOf<LocationPoint>()
        val stepSize = 0.0015 // 약 150m 간격
        var currentLat = baseLat
        var currentLon = baseLon

        for (i in 0 until pointCount) {
            // 경로 패턴 (원형이 아닌 자연스러운 패스)
            val angle = (i.toDouble() / pointCount) * 2 * Math.PI
            val radius = stepSize * (0.5 + Math.random() * 0.5) // 0.5 ~ 1.0 배

            currentLat += Math.cos(angle) * radius * (Math.random() - 0.5) * 2
            currentLon += Math.sin(angle) * radius * (Math.random() - 0.5) * 2

            val timestamp = startTime + (i * duration * 60 * 1000L / pointCount)

            locations.add(
                LocationPoint(
                    latitude = currentLat,
                    longitude = currentLon,
                    timestamp = timestamp,
                    accuracy = Random.nextFloat() * 10f + 5f, // 5f ~ 15f
                ),
            )
        }

        return locations
    }

    /**
     * 활동 통계 생성
     */
    private fun generateActivityStats(
        duration: Long,
        totalDistance: Float,
    ): List<team.swyp.sdu.data.model.ActivityStats> {
        val stats = mutableListOf<team.swyp.sdu.data.model.ActivityStats>()

        // WALKING이 대부분 (80%)
        val walkingDuration = (duration * 0.8).toLong()
        val walkingDistance = totalDistance * 0.8f
        stats.add(
            team.swyp.sdu.data.model.ActivityStats(
                type = ActivityType.WALKING,
                duration = walkingDuration,
                distance = walkingDistance,
            ),
        )

        // RUNNING이 일부 (20%)
        val runningDuration = duration - walkingDuration
        val runningDistance = totalDistance - walkingDistance
        if (runningDuration > 0) {
            stats.add(
                team.swyp.sdu.data.model.ActivityStats(
                    type = ActivityType.RUNNING,
                    duration = runningDuration,
                    distance = runningDistance,
                ),
            )
        }

        return stats
    }

    /**
     * 11월 한 달치 산책 기록 테스트 데이터 생성
     *
     * 11월 1일부터 11월 30일까지 각 날짜마다 산책 기록을 생성합니다.
     * 약 70% 확률로 각 날짜에 기록이 생성됩니다 (약 21일 정도).
     * 각 기록에는 다양한 감정과 활동 통계가 포함됩니다.
     *
     * @param year 연도 (기본값: 현재 연도)
     * @return 11월의 WalkingSession 리스트
     */
    fun generateNovemberSessions(year: Int = LocalDate.now().year): List<WalkingSession> {
        val sessions = mutableListOf<WalkingSession>()
        val november = YearMonth.of(year, 11)
        val baseLat = 37.2411 // 용인시청 위도
        val baseLon = 127.1776 // 용인시청 경도

        // 감정 타입 리스트 (다양한 감정 조합)
        val allEmotionTypes = listOf(
            EmotionType.HAPPY,
            EmotionType.JOYFUL,
            EmotionType.CONTENT,
            EmotionType.DEPRESSED,
            EmotionType.TIRED,
            EmotionType.ANXIOUS,
        )

        // 감정 조합 패턴
        val emotionCombinations = listOf(
            listOf(EmotionType.HAPPY, EmotionType.JOYFUL),
            listOf(EmotionType.CONTENT, EmotionType.JOYFUL),
            listOf(EmotionType.HAPPY, EmotionType.CONTENT),
            listOf(EmotionType.JOYFUL, EmotionType.CONTENT),
            listOf(EmotionType.DEPRESSED, EmotionType.TIRED),
            listOf(EmotionType.ANXIOUS, EmotionType.TIRED),
            listOf(EmotionType.DEPRESSED, EmotionType.ANXIOUS),
            listOf(EmotionType.HAPPY, EmotionType.DEPRESSED),
            listOf(EmotionType.JOYFUL, EmotionType.ANXIOUS),
            listOf(EmotionType.CONTENT, EmotionType.TIRED),
            listOf(EmotionType.DEPRESSED, EmotionType.CONTENT),
            listOf(EmotionType.ANXIOUS, EmotionType.HAPPY),
            listOf(EmotionType.TIRED, EmotionType.JOYFUL),
            listOf(EmotionType.DEPRESSED, EmotionType.JOYFUL),
        )

        // 감정 메모 리스트
        val emotionNotes = listOf(
            "오늘 날씨가 좋아서 기분이 좋았어요",
            "평화로운 산책이었습니다",
            "목표를 달성해서 뿌듯해요",
            "편안하게 걷는 시간이었어요",
            "친구와 함께 걸어서 즐거웠어요",
            "운동 후 기분이 상쾌해요",
            "스트레스 해소에 좋았어요",
            "새로운 경로를 발견했어요",
            "조용한 시간을 가질 수 있었어요",
            "활기찬 하루였어요",
            "가을 단풍이 아름다웠어요",
            "바람이 시원해서 좋았어요",
            "혼자 걷는 시간이 필요했어요",
            "일상의 소소한 행복을 느꼈어요",
            "건강한 하루였어요",
        )

        // 11월의 모든 날짜 순회
        val daysInNovember = november.lengthOfMonth()
        for (day in 1..daysInNovember) {
            // 70% 확률로 기록 생성 (약 21일 정도)
            if (Random.nextFloat() > 0.3f) {
                val date = november.atDay(day)
                
                // 하루 중 다양한 시간대에 산책 (오전, 오후, 저녁)
                val hourOfDay = when (Random.nextInt(0, 3)) {
                    0 -> Random.nextInt(7, 11) // 오전 7-10시
                    1 -> Random.nextInt(14, 18) // 오후 2-5시
                    else -> Random.nextInt(18, 21) // 저녁 6-8시
                }
                val minuteOfDay = Random.nextInt(0, 60)
                
                val startTime = date
                    .atTime(hourOfDay, minuteOfDay)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                // 산책 시간 (15분 ~ 90분)
                val durationMinutes = Random.nextInt(15, 91)
                val endTime = startTime + (durationMinutes * 60 * 1000L)

                // 걸음 수 (분당 약 100~130걸음 기준)
                val stepCount = durationMinutes * Random.nextInt(100, 131)

                // 거리 (걸음 수 기반, 보폭 약 0.7m)
                val totalDistance = stepCount * 0.7f

                // 위치 포인트 생성 (산책 경로)
                val locations = generateLocationPoints(
                    baseLat = baseLat + (Math.random() - 0.5) * 0.02, // 약 1km 범위 내
                    baseLon = baseLon + (Math.random() - 0.5) * 0.02,
                    startTime = startTime,
                    duration = durationMinutes,
                    pointCount = Random.nextInt(20, 41), // 20~40개 포인트
                )

                // 활동 통계 생성
                val activityStats = generateActivityStats(
                    duration = durationMinutes * 60 * 1000L,
                    totalDistance = totalDistance,
                )

                // 감정 리스트 생성 (1~3개, 날짜에 따라 다양한 감정)
                val emotionCount = Random.nextInt(1, 4)
                val sessionEmotions = mutableListOf<Emotion>()
                
                // 날짜에 따라 감정 패턴 선택 (주기적으로 변화)
                val emotionPatternIndex = (day - 1) % emotionCombinations.size
                val selectedEmotionTypes = emotionCombinations[emotionPatternIndex].shuffled()
                
                // 때때로 다른 감정도 추가 (10% 확률)
                val finalEmotionTypes = if (Random.nextFloat() < 0.1f) {
                    (selectedEmotionTypes + allEmotionTypes.random()).distinct().take(emotionCount)
                } else {
                    selectedEmotionTypes.take(emotionCount)
                }

                for (j in 0 until emotionCount) {
                    val emotionTime = startTime + (j * durationMinutes * 60 * 1000L / emotionCount)
                    val emotionType = finalEmotionTypes.getOrElse(j) { allEmotionTypes.random() }
                    
                    sessionEmotions.add(
                        Emotion(
                            type = emotionType,
                            timestamp = emotionTime,
                            note = if (j == 0) emotionNotes[(day - 1) % emotionNotes.size] else null,
                        ),
                    )
                }

                // 산책 전/후 감정 랜덤 선택
                val preWalkEmotion = allEmotionTypes.random()
                val postWalkEmotion = allEmotionTypes.random()

                sessions.add(
                    WalkingSession(
                        startTime = startTime,
                        endTime = endTime,
                        stepCount = stepCount,
                        locations = locations,
                        totalDistance = totalDistance,
                        preWalkEmotion = preWalkEmotion,
                        postWalkEmotion = postWalkEmotion,
                        createdDate = DateUtils.millisToIsoUtc(startTime)
                    ),
                )
            }
        }

        return sessions.sortedBy { it.startTime } // 시간순 정렬
    }

    /**
     * 12월 초(1~5일) 산책 기록 테스트 데이터 생성
     */
    fun generateEarlyDecemberSessions(year: Int = LocalDate.now().year): List<WalkingSession> {
        val sessions = mutableListOf<WalkingSession>()
        val december = YearMonth.of(year, 12)
        val baseLat = 37.2411
        val baseLon = 127.1776
        val days = (1..5)

        days.forEach { day ->
            // 60% 확률로 생성
            if (Random.nextFloat() > 0.4f) {
                val date = december.atDay(day)
                val hourOfDay = Random.nextInt(7, 21)
                val minuteOfDay = Random.nextInt(0, 60)

                val startTime = date
                    .atTime(hourOfDay, minuteOfDay)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val durationMinutes = Random.nextInt(20, 75)
                val durationMillis = durationMinutes * 60 * 1000L
                val endTime = startTime + durationMillis
                val stepCount = durationMinutes * Random.nextInt(100, 131)
                val totalDistance = stepCount * 0.7f

                val locations =
                    generateLocationPoints(
                        baseLat = baseLat + Random.nextDouble(-0.01, 0.01),
                        baseLon = baseLon + Random.nextDouble(-0.01, 0.01),
                        startTime = startTime,
                        duration = durationMinutes,
                        pointCount = Random.nextInt(10, 18),
                    )

                val stats = generateActivityStats(durationMillis, totalDistance)
                val emotionTypes = listOf(EmotionType.HAPPY, EmotionType.JOYFUL, EmotionType.CONTENT, EmotionType.DEPRESSED, EmotionType.TIRED, EmotionType.ANXIOUS)
                val sessionEmotions =
                    listOf(
                        Emotion(
                            type = emotionTypes.random(),
                            timestamp = startTime + durationMillis / 2,
                            note = "12월 초 기분 좋은 산책",
                        ),
                    )

                // 산책 전/후 감정 랜덤 선택
                val preWalkEmotion = emotionTypes.random()
                val postWalkEmotion = emotionTypes.random()

                sessions.add(
                    WalkingSession(
                        startTime = startTime,
                        endTime = endTime,
                        stepCount = stepCount,
                        locations = locations,
                        totalDistance = totalDistance,
                        preWalkEmotion = preWalkEmotion,
                        postWalkEmotion = postWalkEmotion,
                        createdDate = DateUtils.millisToIsoUtc(startTime)
                    ),
                )
            }
        }

        return sessions
    }

    /**
     * 12월 1일 ~ 11일 산책 기록 생성 (확률적 생성)
     */
    fun generateDecemberRangeSessions(
        year: Int = LocalDate.now().year,
        startDay: Int = 1,
        endDay: Int = 16,
    ): List<WalkingSession> {
        val sessions = mutableListOf<WalkingSession>()
        val december = YearMonth.of(year, 12)
        val baseLat = 37.2411
        val baseLon = 127.1776

        val emotionTypes = listOf(EmotionType.HAPPY, EmotionType.JOYFUL, EmotionType.CONTENT, EmotionType.DEPRESSED, EmotionType.TIRED, EmotionType.ANXIOUS)


        for (day in startDay..endDay) {
            if (Random.nextFloat() > 0.35f) {
                val date = december.atDay(day)
                sessions.add(generateSessionForDate(date, baseLat, baseLon))
            }
        }
        return sessions.sortedBy { it.startTime }
    }

    /**
     * 특정 날짜에 단일 세션 생성 (더미)
     */
    fun generateSessionForDate(
        date: LocalDate,
        baseLat: Double = 37.2411,
        baseLon: Double = 127.1776,
    ): WalkingSession {

        val hourOfDay = Random.nextInt(7, 21)
        val minuteOfDay = Random.nextInt(0, 60)

        val startTime = date
            .atTime(hourOfDay, minuteOfDay)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val durationMinutes = Random.nextInt(25, 80)
        val durationMillis = durationMinutes * 60 * 1000L
        val endTime = startTime + durationMillis
        val stepCount = durationMinutes * Random.nextInt(100, 131)
        val totalDistance = stepCount * 0.7f

        val locations =
            generateLocationPoints(
                baseLat = baseLat + Random.nextDouble(-0.01, 0.01),
                baseLon = baseLon + Random.nextDouble(-0.01, 0.01),
                startTime = startTime,
                duration = durationMinutes,
                pointCount = Random.nextInt(12, 20),
            )

        val stats = generateActivityStats(durationMillis, totalDistance)
//        val emotionTypes = listOf(EmotionType.HAPPY, EmotionType.JOYFUL, EmotionType.CONTENT, EmotionType.DEPRESSED)
//        val sessionEmotions =
//            listOf(
//                Emotion(
//                    type = emotionTypes.random(),
//                    timestamp = startTime + durationMillis / 3,
//                    note = "샘플 산책",
//                ),
//            )

        // 산책 전/후 감정 랜덤 선택
        val emotionTypes = listOf(EmotionType.HAPPY, EmotionType.JOYFUL, EmotionType.CONTENT, EmotionType.DEPRESSED, EmotionType.TIRED, EmotionType.ANXIOUS)
        val preWalkEmotion = emotionTypes.random()
        val postWalkEmotion = emotionTypes.random()

        return WalkingSession(
            startTime = startTime,
            endTime = endTime,
            stepCount = stepCount,
            locations = locations,
            totalDistance = totalDistance,
            preWalkEmotion = preWalkEmotion,
            postWalkEmotion = postWalkEmotion,
            createdDate = DateUtils.millisToIsoUtc(startTime)
        )
    }
}

