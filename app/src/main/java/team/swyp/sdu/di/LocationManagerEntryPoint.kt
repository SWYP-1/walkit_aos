package team.swyp.sdu.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import team.swyp.sdu.domain.service.LocationManager

/**
 * LocationManager를 Composable에서 주입받기 위한 EntryPoint
 *
 * Composable에서는 @Inject를 사용할 수 없으므로,
 * EntryPoint를 통해 LocationManager를 주입받습니다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationManagerEntryPoint {
    fun locationManager(): LocationManager
}









