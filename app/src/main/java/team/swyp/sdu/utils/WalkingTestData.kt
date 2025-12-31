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

    /**
     * 한국 주요 도시 10곳의 무작위 산책 경로 생성
     * 각 경로는 60개 이상의 LocationPoint로 구성
     *
     * @return 10개의 WalkingSession 리스트 (각각 다른 도시의 경로)
     */
    fun generateKoreanWalkingRoutes(): List<WalkingSession> {
        val sessions = mutableListOf<WalkingSession>()

        // 한국 주요 도시 좌표 (위도, 경도)
        val koreanCities = listOf(
            Pair(37.5665, 126.9780) to "서울",     // Seoul
            Pair(35.1796, 129.0756) to "부산",     // Busan
            Pair(35.8714, 128.6014) to "대구",     // Daegu
            Pair(37.4563, 126.7052) to "인천",     // Incheon
            Pair(35.1595, 126.8526) to "광주",     // Gwangju
            Pair(36.3504, 127.3845) to "대전",     // Daejeon
            Pair(35.5384, 129.3114) to "울산",     // Ulsan
            Pair(36.4802, 127.2890) to "세종",     // Sejong
            Pair(37.4138, 127.5183) to "수원",     // Suwon (Gyeonggi)
            Pair(37.8228, 128.1555) to "강릉"      // Gangneung (Gangwon)
        )

        val emotionTypes = listOf(
            EmotionType.HAPPY, EmotionType.JOYFUL, EmotionType.CONTENT,
            EmotionType.DEPRESSED, EmotionType.TIRED, EmotionType.ANXIOUS
        )

        val baseTime = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L) // 10일 전

        koreanCities.forEachIndexed { index, (coords, cityName) ->
            val (baseLat, baseLon) = coords

            // 각 도시마다 다른 시작 시간 (하루 간격)
            val startTime = baseTime + (index * 24 * 60 * 60 * 1000L)

            // 45분 ~ 90분 산책
            val durationMinutes = Random.nextInt(45, 91)
            val endTime = startTime + (durationMinutes * 60 * 1000L)

            // 걸음 수 계산 (분당 100-130걸음)
            val stepCount = durationMinutes * Random.nextInt(100, 131)

            // 거리 계산 (보폭 0.7m)
            val totalDistance = stepCount * 0.7f

            // 60~100개의 위치 포인트 생성
            val pointCount = Random.nextInt(60, 101)
            val locations = generateDenseLocationPoints(
                baseLat = baseLat,
                baseLon = baseLon,
                startTime = startTime,
                duration = durationMinutes,
                pointCount = pointCount,
                cityName = cityName
            )

            // 감정 선택
            val preWalkEmotion = emotionTypes.random()
            val postWalkEmotion = emotionTypes.random()

            val session = WalkingSession(
                startTime = startTime,
                endTime = endTime,
                stepCount = stepCount,
                locations = locations,
                totalDistance = totalDistance,
                preWalkEmotion = preWalkEmotion,
                postWalkEmotion = postWalkEmotion,
                createdDate = DateUtils.millisToIsoUtc(startTime)
            )

            sessions.add(session)
        }

        return sessions
    }

    /**
     * 밀집된 위치 포인트 리스트 생성 (도시 내 산책 경로)
     * 도시의 특성을 고려한 다양한 패턴의 경로 생성
     */
    private fun generateDenseLocationPoints(
        baseLat: Double,
        baseLon: Double,
        startTime: Long,
        duration: Int,
        pointCount: Int,
        cityName: String
    ): List<LocationPoint> {
        val locations = mutableListOf<LocationPoint>()

        // 도시별 특성을 고려한 경로 생성
        val pathPattern = when (cityName) {
            "서울" -> PathPattern.CITY_CENTER // 도심형
            "부산" -> PathPattern.COASTAL     // 해안형
            "대구" -> PathPattern.RIVER       // 강변형
            "인천" -> PathPattern.PORT        // 항만형
            "광주" -> PathPattern.HILLY       // 구릉형
            "대전" -> PathPattern.PARK        // 공원형
            "울산" -> PathPattern.INDUSTRIAL  // 산업형
            "세종" -> PathPattern.PLANNED     // 계획형
            "수원" -> PathPattern.SUBURBAN    // 교외형
            "강릉" -> PathPattern.MOUNTAIN    // 산악형
            else -> PathPattern.CITY_CENTER
        }

        val stepSize = 0.0008 // 약 80m 간격 (더 촘촘하게)
        var currentLat = baseLat
        var currentLon = baseLon

        for (i in 0 until pointCount) {
            val progress = i.toDouble() / pointCount
            val angle = progress * 4 * Math.PI // 2바퀴 회전

            // 도시별 경로 패턴 적용
            val (latOffset, lonOffset) = when (pathPattern) {
                PathPattern.CITY_CENTER -> {
                    // 도심: 그리드 패턴 + 랜덤 이동
                    val gridX = (Math.sin(angle) * 0.8 + Math.random() * 0.4 - 0.2) * stepSize * 15
                    val gridY = (Math.cos(angle * 1.5) * 0.6 + Math.random() * 0.4 - 0.2) * stepSize * 15
                    Pair(gridX, gridY)
                }
                PathPattern.COASTAL -> {
                    // 해안: 곡선 패턴 (파도 모양)
                    val wave = Math.sin(angle * 3) * 0.3
                    val coastalX = Math.cos(angle) * stepSize * 12 + wave * stepSize * 8
                    val coastalY = Math.sin(angle) * stepSize * 8
                    Pair(coastalX, coastalY)
                }
                PathPattern.RIVER -> {
                    // 강변: 직선 + 곡선 혼합
                    val riverX = progress * stepSize * 20 // 직선 진행
                    val riverY = Math.sin(angle * 2) * stepSize * 6 // 강변 곡선
                    Pair(riverX, riverY)
                }
                PathPattern.PORT -> {
                    // 항만: 부두 모양
                    val dockX = Math.cos(angle * 0.5) * stepSize * 18
                    val dockY = Math.sin(angle * 2) * stepSize * 4 + Math.random() * stepSize * 2
                    Pair(dockX, dockY)
                }
                PathPattern.HILLY -> {
                    // 구릉: 언덕 오르내리기
                    val hillX = progress * stepSize * 16
                    val hillY = Math.sin(angle * 4) * stepSize * 5
                    Pair(hillX, hillY)
                }
                PathPattern.PARK -> {
                    // 공원: 자연스러운 곡선
                    val parkX = Math.cos(angle) * stepSize * 10 + Math.sin(angle * 2) * stepSize * 3
                    val parkY = Math.sin(angle) * stepSize * 8 + Math.cos(angle * 3) * stepSize * 2
                    Pair(parkX, parkY)
                }
                PathPattern.INDUSTRIAL -> {
                    // 산업: 직선 + 각진 패턴
                    val industrialX = (Math.floor(angle / (Math.PI/2)) * stepSize * 8) + (Math.random() - 0.5) * stepSize * 2
                    val industrialY = ((angle % (Math.PI/2)) / (Math.PI/2) * stepSize * 12) + (Math.random() - 0.5) * stepSize * 2
                    Pair(industrialX, industrialY)
                }
                PathPattern.PLANNED -> {
                    // 계획: 체계적인 그리드
                    val gridSize = stepSize * 6
                    val gridX = (Math.floor(progress * 8) * gridSize) + (Math.random() - 0.5) * stepSize
                    val gridY = ((progress * 8) % 1 * gridSize) + (Math.random() - 0.5) * stepSize
                    Pair(gridX, gridY)
                }
                PathPattern.SUBURBAN -> {
                    // 교외: 주거지역 패턴
                    val suburbanX = Math.cos(angle) * stepSize * 14 + Math.sin(angle * 3) * stepSize * 2
                    val suburbanY = Math.sin(angle) * stepSize * 10 + Math.random() * stepSize * 3
                    Pair(suburbanX, suburbanY)
                }
                PathPattern.MOUNTAIN -> {
                    // 산악: 가파른 오르내리기
                    val mountainX = progress * stepSize * 18
                    val mountainY = Math.sin(angle * 6) * stepSize * 8 + Math.sin(angle * 12) * stepSize * 2
                    Pair(mountainX, mountainY)
                }
            }

            currentLat += latOffset
            currentLon += lonOffset

            // 도시 경계 내로 제한
            currentLat = currentLat.coerceIn(baseLat - 0.01, baseLat + 0.01)
            currentLon = currentLon.coerceIn(baseLon - 0.01, baseLon + 0.01)

            val timestamp = startTime + (i * duration * 60 * 1000L / pointCount)

            locations.add(
                LocationPoint(
                    latitude = currentLat,
                    longitude = currentLon,
                    timestamp = timestamp,
                    accuracy = Random.nextFloat() * 8f + 3f, // 3f ~ 11f (도시 환경 고려)
                ),
            )
        }

        return locations
    }

    /**
     * 도시별 산책 경로 패턴
     */
    private enum class PathPattern {
        CITY_CENTER,    // 도심: 그리드 + 랜덤
        COASTAL,        // 해안: 파도 모양 곡선
        RIVER,          // 강변: 직선 + 곡선
        PORT,           // 항만: 부두 모양
        HILLY,          // 구릉: 언덕 패턴
        PARK,           // 공원: 자연 곡선
        INDUSTRIAL,     // 산업: 각진 패턴
        PLANNED,        // 계획: 체계적 그리드
        SUBURBAN,       // 교외: 주거지역
        MOUNTAIN        // 산악: 가파른 지형
    }
}

