package swyp.team.walkit.analytics

/**
 * Firebase Analytics screen_view 이벤트용 화면 타입 정의.
 * 문자열 기반 screen name 분산을 방지하기 위해 enum으로 통합 관리.
 */
enum class AnalyticsScreen(val screenName: String) {
    Splash("Splash"),
    Login("Login"),
    Onboarding("Onboarding"),

    MainHome("Main_Home"),
    MainRecord("Main_Record"),
    MainCharacter("Main_Character"),
    MainMyPage("Main_MyPage"),

    Walking("Walking"),
    PostEmotionSelection("PostEmotionSelection"),
    EmotionRecord("EmotionRecord"),
    WalkingResult("WalkingResult"),

    RouteDetail("RouteDetail"),
    Friends("Friends"),
    FriendSearch("FriendSearch"),
    FriendSearchDetail("FriendSearchDetail"),

    GoalManagement("GoalManagement"),
    Mission("Mission"),
    DressingRoom("DressingRoom"),
    CharacterShop("CharacterShop"),
    UserInfoManagement("UserInfoManagement"),
    NotificationSettings("NotificationSettings"),
    Alarm("Alarm"),
    DailyRecord("DailyRecord"),

    CustomTest("CustomTest"), // 개발 전용, 프로덕션 차단
}
