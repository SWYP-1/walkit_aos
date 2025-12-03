package team.swyp.sdu

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WalkingBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Timber 초기화
        // 개발 중이므로 항상 DebugTree 사용
        // 릴리즈 빌드에서는 Crashlytics 등으로 로그 전송 가능
        Timber.plant(Timber.DebugTree())

        // KakaoMap SDK 초기화
        // AndroidManifest.xml의 meta-data에서 앱 키를 읽어옴
        val appKey = "dd4a75c8aa122d46a52c64492a973b63"
        KakaoMapSdk.init(this, appKey)

        Timber.d("TtApplication onCreate")
    }
}
