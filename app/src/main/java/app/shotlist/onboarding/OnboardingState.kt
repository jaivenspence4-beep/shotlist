package app.shotlist.onboarding

enum class PermissionStep {
    Intro,
    Personalize,
    Requesting,
    Scanning,
    Ready,
    Denied,
}

data class OnboardingReveal(
    val screenshotsRead: Int = 0,
    val suggestedActions: Int = 0,
) {
    val hasWowMoment: Boolean
        get() = screenshotsRead > 0 || suggestedActions > 0
}
