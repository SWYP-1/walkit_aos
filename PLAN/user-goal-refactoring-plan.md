# User & Goal 리팩토링 플랜

## 현재 문제점 분석

### 1. User 클래스의 과도한 책임
현재 `UserProfile`에 다음 정보들이 모두 포함되어 있음:
- 사용자 기본 정보: `uid`, `nickname`, `birthYear`
- 게임/포인트 정보: `clearedCount`, `point`, `goalKmPerWeek`
- **Goal 정보**: `goalInfo`, `goalProgressSessions`, `goalProgressSteps` (책임 분리 필요)

### 2. 서버 API 구조와 불일치
**새로운 서버 API 구조:**
```json
// User
{
  "userId": 1,
  "imageName": "dfdadf.png",
  "nickname": "홍길동",
  "birthDate": "2025-12-07",
  "sex": "MALE"
}

// Goal (별도 엔드포인트)
{
  "targetStepCount": 10000,
  "targetWalkCount": 3
}
```

**현재 구조:**
- UserProfile에 Goal 정보가 포함됨
- Goal 진행도(progress)가 User에 포함됨
- birthDate가 birthYear로만 저장됨
- imageName, sex 필드가 없음

### 3. 데이터 레이어 구조 문제
- `UserEntity`에 Goal 정보가 평탄화되어 저장됨
- Goal 전용 Entity가 없음
- Goal Repository가 없음

### 4.삭제할 필드들 
`clearedCount`, `point`, `goalKmPerWeek` 은 삭제 

---

## 리팩토링 목표

1. **책임 분리**: User와 Goal을 완전히 분리
2. **도메인 모델 정리**: 각 도메인 모델이 단일 책임을 가지도록
3. **서버 API 대응**: 새로운 API 구조에 맞춘 DTO 생성
4. **Repository 분리**: UserRepository와 GoalRepository 분리
5. **마이그레이션**: 기존 데이터 호환성 유지

---

## 리팩토링 단계별 계획

### Phase 1: 도메인 모델 분리 및 확장

#### 1.1 새로운 User 도메인 모델 생성
```kotlin
// domain/model/User.kt
@Serializable
data class User(
    val userId: Long,
    val imageName: String?,
    val nickname: String,
    val birthDate: String, // ISO 8601 형식: "2025-12-07"
    val sex: Sex?,
) {
    companion object {
        val EMPTY = User(
            userId = 0,
            imageName = null,
            nickname = "",
            birthDate = "",
            sex = null,
        )
    }
}

@Serializable
enum class Sex {
    @SerialName("MALE")
    MALE,
    @SerialName("FEMALE")
    FEMALE,
}
```

#### 1.2 새로운 Goal 도메인 모델 생성
```kotlin
// domain/model/Goal.kt
@Serializable
data class Goal(
    val targetStepCount: Int,
    val targetWalkCount: Int,
) {
    companion object {
        val EMPTY = Goal(
            targetStepCount = 0,
            targetWalkCount = 0,
        )
    }
}
```

#### 1.3 기존 UserProfile 유지 (하위 호환성)
- 기존 코드와의 호환성을 위해 `UserProfile` 유지
- 점진적 마이그레이션을 위한 어댑터 메서드 제공

---

### Phase 2: DTO 및 Entity 분리

#### 2.1 User DTO 생성
```kotlin
// data/remote/user/dto/RemoteUserDto.kt
@Serializable
data class RemoteUserDto(
    @SerialName("userId")
    val userId: Long,
    @SerialName("imageName")
    val imageName: String?,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("birthDate")
    val birthDate: String,
    @SerialName("sex")
    val sex: String?,
) {
    fun toDomain(): User = User(
        userId = userId,
        imageName = imageName,
        nickname = nickname,
        birthDate = birthDate,
        sex = sex?.let { Sex.valueOf(it) },
    )
}
```

#### 2.2 Goal DTO 생성
```kotlin
// data/remote/goal/dto/RemoteGoalDto.kt
@Serializable
data class RemoteGoalDto(
    @SerialName("targetStepCount")
    val targetStepCount: Int,
    @SerialName("targetWalkCount")
    val targetWalkCount: Int,
) {
    fun toDomain(): Goal = Goal(
        targetStepCount = targetStepCount,
        targetWalkCount = targetWalkCount,
    )
}
```

#### 2.3 User Entity 수정
```kotlin
// data/local/entity/UserEntity.kt
@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey
    val userId: Long,
    val imageName: String?,
    val nickname: String,
    val birthDate: String,
    val sex: String?,
    val updatedAt: Long = System.currentTimeMillis(),
)
```

#### 2.4 Goal Entity 생성
```kotlin
// data/local/entity/GoalEntity.kt
@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey
    val userId: Long, // User와의 관계
    val targetStepCount: Int,
    val targetWalkCount: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)
```

---

### Phase 3: Repository 분리

#### 3.1 GoalRepository 인터페이스 생성
```kotlin
// domain/repository/GoalRepository.kt
interface GoalRepository {
    val goalFlow: StateFlow<Goal?>
    
    suspend fun getGoal(): Result<Goal>
    suspend fun updateGoal(goal: Goal): Result<Goal>
    suspend fun refreshGoal(): Result<Goal>
}
```

#### 3.2 GoalRepository 구현체 생성
```kotlin
// data/repository/GoalRepositoryImpl.kt
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val goalRemoteDataSource: GoalRemoteDataSource,
) : GoalRepository {
    // 구현...
}
```

#### 3.3 UserRepository 수정
- Goal 관련 메서드 제거
- User만 관리하도록 단순화

---

### Phase 4: Mapper 및 DAO 생성

#### 4.1 GoalMapper 생성
```kotlin
// data/local/mapper/GoalMapper.kt
object GoalMapper {
    fun toEntity(domain: Goal, userId: Long): GoalEntity = ...
    fun toDomain(entity: GoalEntity): Goal = ...
}
```

#### 4.2 GoalDao 생성
```kotlin
// data/local/dao/GoalDao.kt
@Dao
interface GoalDao {
    @Query("SELECT * FROM goal WHERE userId = :userId")
    fun observeGoal(userId: Long): Flow<GoalEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)
    
    @Query("DELETE FROM goal WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Long)
}
```

#### 4.3 UserMapper 수정
- Goal 관련 필드 제거
- 새로운 User 모델로 매핑

---

### Phase 5: Remote DataSource 분리

#### 5.1 GoalRemoteDataSource 생성
```kotlin
// data/remote/goal/GoalRemoteDataSource.kt
@Singleton
class GoalRemoteDataSource @Inject constructor(
    private val goalApi: GoalApi,
    private val tokenProvider: TokenProvider,
) {
    suspend fun fetchGoal(): RemoteGoalDto = ...
    suspend fun updateGoal(goal: Goal): RemoteGoalDto = ...
}
```

#### 5.2 GoalApi 인터페이스 생성
```kotlin
// data/remote/goal/GoalApi.kt
interface GoalApi {
    @GET("goal")
    suspend fun getGoal(): Response<RemoteGoalDto>
    
    @PUT("goal")
    suspend fun updateGoal(@Body goal: RemoteGoalDto): Response<RemoteGoalDto>
}
```

---

### Phase 6: ViewModel 및 UI 업데이트

#### 6.1 UserViewModel 수정
- Goal 관련 로직 제거
- User만 관리

#### 6.2 GoalViewModel 생성
```kotlin
// presentation/viewmodel/GoalViewModel.kt
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
) : ViewModel() {
    val goal: StateFlow<Goal?> = goalRepository.goalFlow.asStateFlow()
    
    fun refreshGoal() { ... }
    fun updateGoal(goal: Goal) { ... }
}
```

#### 6.3 UI 컴포넌트 업데이트
- UserProfile 사용하는 곳을 User로 변경
- Goal 정보가 필요한 곳에 GoalViewModel 주입

---

### Phase 7: 데이터베이스 마이그레이션

#### 7.1 Room Migration 작성
```kotlin
// data/local/database/Migration.kt
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // User 테이블 수정
        database.execSQL("ALTER TABLE user_profile ADD COLUMN imageName TEXT")
        database.execSQL("ALTER TABLE user_profile ADD COLUMN birthDate TEXT")
        database.execSQL("ALTER TABLE user_profile ADD COLUMN sex TEXT")
        
        // Goal 테이블 생성
        database.execSQL("""
            CREATE TABLE goal (
                userId INTEGER PRIMARY KEY,
                targetStepCount INTEGER NOT NULL,
                targetWalkCount INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        
        // 기존 데이터 마이그레이션
        // UserEntity의 goalPeriodType, targetSessions, targetSteps를 Goal 테이블로 이동
    }
}
```

---

## 마이그레이션 전략

### 전략 1: 점진적 마이그레이션 (권장)
1. 새로운 모델과 기존 모델 병행 사용
2. 어댑터 패턴으로 변환
3. 점진적으로 기존 코드 교체
4. 모든 교체 완료 후 기존 모델 제거

### 전략 2: Big Bang 마이그레이션
1. 한 번에 모든 코드 변경
2. 빠르지만 위험도 높음
3. 충분한 테스트 필요

---

## 파일 구조

```
domain/
├── model/
│   ├── User.kt (새로 생성)
│   ├── Goal.kt (새로 생성)
│   └── UserProfile.kt (기존 유지, 점진적 제거)
├── repository/
│   ├── UserRepository.kt (수정)
│   └── GoalRepository.kt (새로 생성)

data/
├── remote/
│   ├── user/
│   │   ├── dto/
│   │   │   └── RemoteUserDto.kt (수정)
│   │   └── UserRemoteDataSource.kt (수정)
│   └── goal/ (새로 생성)
│       ├── dto/
│       │   └── RemoteGoalDto.kt
│       ├── GoalApi.kt
│       └── GoalRemoteDataSource.kt
├── local/
│   ├── entity/
│   │   ├── UserEntity.kt (수정)
│   │   └── GoalEntity.kt (새로 생성)
│   ├── dao/
│   │   ├── UserDao.kt (수정)
│   │   └── GoalDao.kt (새로 생성)
│   └── mapper/
│       ├── UserMapper.kt (수정)
│       └── GoalMapper.kt (새로 생성)
└── repository/
    ├── UserRepositoryImpl.kt (수정)
    └── GoalRepositoryImpl.kt (새로 생성)

presentation/
└── viewmodel/
    ├── UserViewModel.kt (수정)
    └── GoalViewModel.kt (새로 생성)
```

---

## 체크리스트

### Phase 1: 도메인 모델
- [ ] User 도메인 모델 생성
- [ ] Goal 도메인 모델 생성
- [ ] Sex enum 생성
- [ ] 기존 UserProfile 유지 (하위 호환성)

### Phase 2: DTO 및 Entity
- [ ] RemoteUserDto 수정
- [ ] RemoteGoalDto 생성
- [ ] UserEntity 수정
- [ ] GoalEntity 생성

### Phase 3: Repository
- [ ] GoalRepository 인터페이스 생성
- [ ] GoalRepositoryImpl 구현
- [ ] UserRepository에서 Goal 관련 제거

### Phase 4: Mapper 및 DAO
- [ ] GoalMapper 생성
- [ ] GoalDao 생성
- [ ] UserMapper 수정
- [ ] UserDao 수정

### Phase 5: Remote DataSource
- [ ] GoalApi 생성
- [ ] GoalRemoteDataSource 생성
- [ ] UserRemoteDataSource 수정

### Phase 6: ViewModel 및 UI
- [ ] GoalViewModel 생성
- [ ] UserViewModel 수정
- [ ] UI 컴포넌트 업데이트

### Phase 7: 데이터베이스 마이그레이션
- [ ] Room Migration 작성
- [ ] 마이그레이션 테스트
- [ ] AppDatabase에 Migration 추가

---

## 주의사항

1. **하위 호환성**: 기존 UserProfile을 사용하는 코드가 많으므로 점진적 마이그레이션 필요
2. **데이터 손실 방지**: 마이그레이션 시 기존 데이터 보존 필수
3. **테스트**: 각 단계마다 충분한 테스트 필요
4. **API 변경**: 서버 API가 실제로 변경되었는지 확인 필요
5. **의존성**: User와 Goal이 분리되면 일부 ViewModel에서 두 Repository를 모두 주입받아야 함

---

## 예상 소요 시간

- Phase 1: 2시간
- Phase 2: 3시간
- Phase 3: 4시간
- Phase 4: 2시간
- Phase 5: 3시간
- Phase 6: 6시간
- Phase 7: 4시간

**총 예상 시간: 약 24시간**

---

## 다음 단계

1. 서버 API 스펙 최종 확인
2. Phase 1부터 순차적으로 진행
3. 각 Phase 완료 후 테스트 및 검토
4. 문서 업데이트






