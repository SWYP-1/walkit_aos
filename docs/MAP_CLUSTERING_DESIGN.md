# 지도 클러스터링 설계 문서

## 개요

장소 핀(SPOT)이 밀집된 지역에서 가독성을 높이기 위해 Greedy 방식의 클러스터링을 직접 구현했다.  
카카오맵 SDK에는 자체 클러스터링 API가 없어 거리 기반 수동 클러스터링을 선택했다.

---

## 구성 파일

| 파일 | 역할 |
|---|---|
| `data/model/MapPin.kt` | 단일 핀 / 클러스터 핀 sealed class |
| `data/utils/MapClusteringUtil.kt` | 클러스터링 로직 (거리 계산 + Greedy 그룹핑) |
| `ui/interactivemap/InteractiveMapViewModel.kt` | 줌 레벨 상태 관리 + `MapClusteringUtil.cluster()` 호출 |
| `ui/components/KakaoMapView.kt` | 클러스터 Bitmap 생성 + 지도에 렌더링 |

---

## 500m 기준을 어떻게 표현했는가

클러스터링 묶음 기준 거리는 **실제 지구 표면 거리 500m**로 정의했다.

```kotlin
// MapClusteringUtil.kt
private const val CLUSTER_THRESHOLD_METERS = 500.0
```

두 마커 사이의 거리를 위·경도 좌표로부터 계산하기 위해 **Haversine 공식**을 사용했다.  
Haversine은 지구를 구로 가정하고 두 점의 대원 거리를 계산하는 공식으로,  
평면 좌표 단순 비교와 달리 위도·경도 왜곡을 보정한다.

```kotlin
private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0          // 지구 반지름 (미터)
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
```

즉, **500.0m는 픽셀이나 dp가 아닌 WGS-84 좌표계 기준의 실제 거리**다.  
줌 레벨과 무관하게 항상 동일한 지리적 거리를 기준으로 그룹이 형성된다.

---

## 줌 레벨과의 관계

클러스터링은 **줌 레벨에 따라 활성화/비활성화**된다.

```kotlin
const val UNCLUSTER_ZOOM = 16
```

| 줌 레벨 | 상태 | 대략적인 화면 표시 범위 |
|---|---|---|
| 1 ~ 12 | 클러스터링 활성 | 시/도 ~ 구/군 단위 |
| 13 ~ 15 | 클러스터링 활성 | 약 1~3km 반경 |
| **15** | 클러스터링 활성 | **약 500m ~ 1km 반경** (기본 줌) |
| **16** | **클러스터링 해제** | **약 200~500m 반경** |
| 17 이상 | 클러스터링 해제 | 거리/골목 단위 |

카카오맵 SDK의 줌 레벨 15는 화면 기준 약 500m~1km 범위를 표시하는 축척에 해당한다.  
이 레벨에서는 500m 이내 마커들이 화면 상 근접하게 보이기 때문에 클러스터링이 유효하다.

줌 레벨 16부터는 500m가 화면에서 충분히 넓게 펼쳐지므로 개별 핀으로 풀어 표시한다.  
따라서 **클러스터 해제 기준인 줌 16은 500m 기준 거리와 시각적으로 대응**된다.

> 기본 카메라 줌은 `_zoomLevel = MutableStateFlow(15)`로 설정되어 있다.

---

## 알고리즘: Greedy 클러스터링

O(n²) Greedy 방식으로 구현했다. 마커 수가 수백 개 이하인 앱 특성상 성능 문제가 없다.

```
1. 마커 목록을 순서대로 순회
2. 아직 할당되지 않은 마커 i를 새 그룹의 시드로 지정
3. i 이후 모든 미할당 마커 j에 대해 haversine(i, j) <= 500m 이면 같은 그룹으로 묶음
4. 그룹 크기 1 → MapPin.Single, 2 이상 → MapPin.Cluster (중심 = 포함 마커 평균 좌표)
```

---

## 마커 타입별 처리

| 마커 타입 | 클러스터링 여부 | 이유 |
|---|---|---|
| `FRIEND` | 항상 개별 표시 | 친구 위치는 정확히 표시해야 함 |
| `SPOT` | 줌 15 이하에서 클러스터링 | 장소 밀집 시 가독성 향상 |

---

## 클러스터 클릭 동작

클러스터 핀을 클릭하면 해당 클러스터의 중심 좌표로 줌인한다.

```kotlin
val targetZoom = (UNCLUSTER_ZOOM).coerceAtLeast(
    (map.cameraPosition?.zoomLevel ?: 15) + 2
)
```

- 최소 줌 16(UNCLUSTER_ZOOM)으로 이동해 클러스터가 자동으로 해제되도록 보장한다.
- 현재 줌 + 2가 16보다 크면 더 크게 줌인한다.

---

## 클러스터 핀 외형

클러스터 핀은 Canvas로 직접 Bitmap을 생성한다.

- **크기**: 48dp (밀도 독립적)
- **배경**: 파란 원 (`#5B93FF`)
- **테두리**: 흰색 2dp 원
- **텍스트**: 포함 마커 수, 99 초과 시 `99+` 표시

---

## 데이터 흐름

```
InteractiveMapViewModel
  ├── _uiState (allMarkers)
  └── _zoomLevel
        ↓ combine
  mapPins: StateFlow<List<MapPin>>
        = MapClusteringUtil.cluster(allMarkers, zoomLevel)
        ↓
KakaoMapView
  └── drawPins(pins) → LabelLayer에 마커 렌더링
```

줌 레벨이 변경될 때마다 `onZoomChanged(zoom)` → `_zoomLevel.value = zoom` → `mapPins` 재계산 → 지도 다시 그림.
