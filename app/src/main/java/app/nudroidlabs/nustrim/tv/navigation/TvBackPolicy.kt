package app.nudroidlabs.nustrim.tv.navigation

enum class TvBackAction {
    POP_ROUTE,
    OPEN_SIDEBAR,
    EXIT_APP,
}

fun resolveTvBackAction(
    currentRoute: TvRoute,
    canPop: Boolean,
    sidebarOpen: Boolean,
): TvBackAction = when {
    canPop -> TvBackAction.POP_ROUTE
    currentRoute is TvRoute.Root && !sidebarOpen -> TvBackAction.OPEN_SIDEBAR
    else -> TvBackAction.EXIT_APP
}
