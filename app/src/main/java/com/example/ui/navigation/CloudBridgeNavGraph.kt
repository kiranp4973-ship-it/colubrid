package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.screens.AdminConfigScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ConnectionsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransferDetailScreen
import com.example.ui.screens.TransferWizardScreen

/**
 * Route constants for the CloudBridge navigation graph.
 */
object CloudBridgeRoutes {
    const val LANDING = "landing"
    const val DASHBOARD = "dashboard"
    const val TRANSFER = "transfer"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val CONNECTIONS = "connections"
    const val AUTH = "auth"
    const val ADMIN = "admin"
    const val TRANSFER_DETAIL = "transfer_detail/{jobId}"

    fun transferDetail(jobId: String): String = "transfer_detail/$jobId"

    fun fromScreen(screen: Screen): String = when (screen) {
        Screen.LANDING -> LANDING
        Screen.AUTH -> AUTH
        Screen.DASHBOARD -> DASHBOARD
        Screen.TRANSFER_WIZARD -> TRANSFER
        Screen.TRANSFER_DETAIL -> "transfer_detail/active"
        Screen.CONNECTIONS -> CONNECTIONS
        Screen.HISTORY -> HISTORY
        Screen.SETTINGS -> SETTINGS
        Screen.ADMIN -> ADMIN
    }

    fun toScreen(route: String?): Screen = when {
        route == null -> Screen.DASHBOARD
        route.startsWith("transfer_detail") -> Screen.TRANSFER_DETAIL
        route == LANDING -> Screen.LANDING
        route == AUTH -> Screen.AUTH
        route == DASHBOARD -> Screen.DASHBOARD
        route == TRANSFER -> Screen.TRANSFER_WIZARD
        route == CONNECTIONS -> Screen.CONNECTIONS
        route == HISTORY -> Screen.HISTORY
        route == SETTINGS -> Screen.SETTINGS
        route == ADMIN -> Screen.ADMIN
        else -> Screen.DASHBOARD
    }
}

/**
 * Navigation graph managing animated transitions between
 * Landing, Dashboard, Transfer, History, Settings, and secondary screens.
 */
@Composable
fun CloudBridgeNavGraph(
    navController: NavHostController,
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = CloudBridgeRoutes.DASHBOARD
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260)
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260)
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260)
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260)
                )
        }
    ) {
        composable(CloudBridgeRoutes.LANDING) {
            LandingScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(CloudBridgeRoutes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(CloudBridgeRoutes.TRANSFER) {
            TransferWizardScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(CloudBridgeRoutes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(CloudBridgeRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(CloudBridgeRoutes.CONNECTIONS) {
            ConnectionsScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(CloudBridgeRoutes.AUTH) {
            AuthScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(CloudBridgeRoutes.ADMIN) {
            AdminConfigScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = CloudBridgeRoutes.TRANSFER_DETAIL,
            arguments = listOf(
                navArgument("jobId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            LaunchedEffect(jobId) {
                if (!jobId.isNullOrBlank() && jobId != "active") {
                    viewModel.viewJobDetail(jobId)
                }
            }
            TransferDetailScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
