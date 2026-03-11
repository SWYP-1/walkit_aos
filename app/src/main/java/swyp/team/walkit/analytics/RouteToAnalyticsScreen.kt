package swyp.team.walkit.analytics

import swyp.team.walkit.BuildConfig
import swyp.team.walkit.navigation.Screen

/**
 * Nav destination의 route pattern을 AnalyticsScreen으로 매핑.
 * CustomTest는 프로덕션(릴리즈) 빌드에서 null 반환하여 차단.
 */
fun routeToAnalyticsScreen(routePattern: String): AnalyticsScreen? {
    return when (routePattern) {
        Screen.Splash.route -> AnalyticsScreen.Splash
        Screen.Login.route -> AnalyticsScreen.Login
        Screen.Onboarding.route -> AnalyticsScreen.Onboarding

        Screen.Walking.route -> AnalyticsScreen.Walking
        Screen.PostEmotionSelectionStep.route -> AnalyticsScreen.PostEmotionSelection
        Screen.EmotionRecord.route -> AnalyticsScreen.EmotionRecord
        Screen.WalkingResult.route -> AnalyticsScreen.WalkingResult

        Screen.RouteDetail.route -> AnalyticsScreen.RouteDetail
        Screen.Friends.route -> AnalyticsScreen.Friends
        Screen.FriendSearch.route -> AnalyticsScreen.FriendSearch
        Screen.FriendSearchDetail.route -> AnalyticsScreen.FriendSearchDetail

        Screen.GoalManagement.route -> AnalyticsScreen.GoalManagement
        Screen.Mission.route -> AnalyticsScreen.Mission
        Screen.DressingRoom.route -> AnalyticsScreen.DressingRoom
        Screen.CharacterShop.route -> AnalyticsScreen.CharacterShop
        Screen.UserInfoManagement.route -> AnalyticsScreen.UserInfoManagement
        Screen.NotificationSettings.route -> AnalyticsScreen.NotificationSettings
        Screen.Alarm.route -> AnalyticsScreen.Alarm
        Screen.DailyRecord.route -> AnalyticsScreen.DailyRecord

        Screen.CustomTest.route -> if (BuildConfig.DEBUG) AnalyticsScreen.CustomTest else null

        Screen.Main.route -> null // Main은 탭에서 처리
        else -> null
    }
}
