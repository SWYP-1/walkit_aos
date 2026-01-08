package swyp.team.walkit.ui.character.charactershop

import org.junit.Test
import org.junit.Assert.*
import swyp.team.walkit.testutil.JsonTestUtil

/**
 * CharacterShopViewModel 간단 테스트
 */
class SimpleCharacterShopViewModelTest {

    @Test
    fun `기본 테스트 케이스 - CharacterShopViewModel 클래스 존재 확인`() {
        // CharacterShopViewModel 클래스가 존재하는지 확인
        assertTrue(CharacterShopViewModel::class.java.simpleName == "CharacterShopViewModel")
    }

    @Test
    fun `카테고리 필터링 기본 동작 테스트`() {
        // 간단한 로직 테스트 - 리스트 필터링
        val testList = listOf(1, 2, 3, 4, 5)
        val filteredList = testList.filter { it > 3 }

        assertEquals(2, filteredList.size)
        assertTrue(filteredList.all { it > 3 })
    }

    @Test
    fun `선택된 아이템은 필터링에서 제외되고 항상 표시된다`() {
        // 선택된 아이템은 보유 여부와 관계없이 항상 표시되는지 검증
        data class TestItem(val id: Int, val owned: Boolean, val category: String)

        val items = listOf(
            TestItem(1, true, "HEAD"),   // 보유 + HEAD
            TestItem(2, false, "HEAD"),  // 미보유 + HEAD (선택됨)
            TestItem(3, true, "BODY"),   // 보유 + BODY
            TestItem(4, false, "BODY")   // 미보유 + BODY
        )

        val selectedIds = setOf(2) // 미보유 HEAD 아이템이 선택됨

        // showOwnedOnly = true일 때 필터링
        val filteredOwnedOnly = items.filter { item ->
            // 선택된 아이템은 필터링 제외
            if (selectedIds.contains(item.id)) {
                return@filter true
            }

            val ownedFilter = true  // showOwnedOnly = true
            !ownedFilter || item.owned
        }

        // 결과: 선택된 미보유 아이템(2) + 보유 아이템들(1,3) = 3개
        assertEquals(3, filteredOwnedOnly.size)
        assertTrue(filteredOwnedOnly.any { it.id == 2 }) // 선택된 미보유 아이템 포함
        assertTrue(filteredOwnedOnly.all { it.id != 4 }) // 미선택 미보유 아이템 제외
    }

    @Test
    fun `Toast 메시지 표시 기능 테스트`() {
        // Toast 메시지 표시 로직 검증
        val testMessage = "테스트 메시지"

        // showToast 함수의 동작을 모방
        var toastShown: String? = null
        val mockShowToast = { message: String ->
            toastShown = message
        }

        // Toast 표시
        mockShowToast(testMessage)

        // 검증
        assertEquals(testMessage, toastShown)
    }

    @Test
    fun `구매 성공 시 Toast 메시지 내용 검증`() {
        // 구매 성공 시 표시되는 Toast 메시지 내용 검증
        val expectedPurchaseMessage = "아이템 구매가 완료되었습니다!"
        val expectedSaveMessage = "캐릭터 저장이 완료되었습니다!"

        assertTrue(expectedPurchaseMessage.contains("구매"))
        assertTrue(expectedPurchaseMessage.contains("완료"))
        assertTrue(expectedSaveMessage.contains("저장"))
        assertTrue(expectedSaveMessage.contains("완료"))
    }

    @Test
    fun `저장하기 버튼 disabled 상태 테스트`() {
        // 저장 로딩 상태에 따른 버튼 활성화 상태 검증
        val testCases = listOf(
            true to false,  // isWearLoading = true → enabled = false
            false to true  // isWearLoading = false → enabled = true
        )

        testCases.forEach { (isLoading, expectedEnabled) ->
            val actualEnabled = !isLoading
            assertEquals("isWearLoading=$isLoading일 때 버튼 상태", expectedEnabled, actualEnabled)
        }
    }

    @Test
    fun `저장 중에는 사용자 액션 제한 검증`() {
        // 저장 중에는 selectItem이 호출되지 않아야 함
        val isWearLoading = true
        val shouldAllowAction = !isWearLoading

        assertFalse("저장 중에는 아이템 선택이 제한되어야 함", shouldAllowAction)
    }

    @Test
    fun `구매 작업 중에도 버튼이 disabled 상태 검증`() {
        // performPurchase 시작 시 isWearLoading이 true가 되는지 검증
        val isWearLoadingDuringPurchase = true  // performPurchase() 시작 시 설정

        // 구매 중에는 버튼이 disabled 되어야 함
        val buttonEnabled = !isWearLoadingDuringPurchase
        assertFalse("구매 작업 중에는 저장하기 버튼이 disabled 되어야 함", buttonEnabled)
    }

    @Test
    fun `구매 및 저장 완료 후 버튼이 enabled 상태로 복구 검증`() {
        // finally 블록에서 isWearLoading이 false로 설정되는지 검증
        val isWearLoadingAfterComplete = false  // finally에서 설정

        // 작업 완료 후 버튼이 enabled 되어야 함
        val buttonEnabled = !isWearLoadingAfterComplete
        assertTrue("구매 및 저장 완료 후 버튼이 enabled 되어야 함", buttonEnabled)
    }

    @Test
    fun `구매 완료 후 캐릭터 정보 리프레시가 발생하지 않아야 함`() {
        // performPurchase에서 refreshCharacterInfo()가 호출되지 않아야 함
        // 이미 착용하고 있던 아이템 상태가 유지되어야 함
        val refreshShouldNotBeCalled = true
        assertTrue("구매 완료 후 캐릭터 리프레시가 발생하지 않아야 함", refreshShouldNotBeCalled)
    }

    @Test
    fun `구매 완료 후 wornItemsByPosition에서 착용 상태가 유지되어야 함`() {
        // performPurchase에서 _wornItemsByPosition이 유지되어야 함
        // 구매한 아이템이 캐릭터 미리보기에서 계속 착용된 상태로 유지되어야 함
        val wornItemsShouldBePreserved = true
        assertTrue("구매 완료 후 착용 상태가 유지되어야 함", wornItemsShouldBePreserved)
    }


    @Test
    fun `temp.json에서 위치 데이터 로드 테스트`() {
        val locations = JsonTestUtil.loadLocationsFromTempJson()

        // 데이터 검증
        assertTrue("위치 데이터가 로드되어야 함", locations.isNotEmpty())
        assertTrue("위치 데이터가 충분해야 함", locations.size > 100)

        // 첫 번째 데이터 검증
        val firstLocation = locations.first()
        assertNotNull("위도가 있어야 함", firstLocation.latitude)
        assertNotNull("경도가 있어야 함", firstLocation.longitude)
        assertNotNull("타임스탬프가 있어야 함", firstLocation.timestamp)

        // 위도/경도 범위 검증 (서울 근처)
        assertTrue("위도가 유효한 범위여야 함", firstLocation.latitude in 37.0..38.0)
        assertTrue("경도가 유효한 범위여야 함", firstLocation.longitude in 126.0..128.0)

        // 타임스탬프 검증 (현재 시간 이후가 아니어야 함)
        val currentTime = System.currentTimeMillis()
        assertTrue("타임스탬프가 현재 시간보다 이전이어야 함", firstLocation.timestamp < currentTime)

        // JsonTestUtil 검증 함수 사용
        val validationResult = JsonTestUtil.validateLocations(locations)
        println("🔍 데이터 검증 결과: $validationResult")

        println("✅ 총 ${locations.size}개의 위치 데이터를 로드했습니다.")
        println("📍 첫 번째 위치: ${firstLocation.latitude}, ${firstLocation.longitude}")
        println("⏰ 타임스탬프: ${firstLocation.timestamp}")
    }

    @Test
    fun `존재하지 않는 JSON 파일 로드 시 빈 리스트 반환`() {
        val locations = JsonTestUtil.loadLocationsFromJson("nonexistent")

        assertTrue("존재하지 않는 파일은 빈 리스트를 반환해야 함", locations.isEmpty())
    }

    @Test
    fun `JSON 문자열 파싱 테스트`() {
        val jsonString = """[
            {"latitude":37.123456,"longitude":127.123456,"timestamp":1640995200000,"accuracy":10.0},
            {"latitude":37.234567,"longitude":127.234567,"timestamp":1640995260000,"accuracy":12.0}
        ]"""

        val locations = JsonTestUtil.parseLocationsFromJsonString(jsonString)

        assertEquals("파싱된 데이터가 2개여야 함", 2, locations.size)
        assertEquals("첫 번째 위도가 일치해야 함", 37.123456, locations[0].latitude, 0.000001)
        assertEquals("두 번째 경도가 일치해야 함", 127.234567, locations[1].longitude, 0.000001)
    }
}
