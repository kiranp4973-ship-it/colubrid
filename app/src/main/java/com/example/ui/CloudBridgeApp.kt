package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserRole
import com.example.ui.components.CloudBridgeLogo
import com.example.ui.navigation.CloudBridgeNavGraph
import com.example.ui.navigation.CloudBridgeRoutes
import com.example.ui.theme.CloudBridgePrimary

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBridgeApp(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: CloudBridgeRoutes.DASHBOARD
    val currentScreen = CloudBridgeRoutes.toScreen(currentRoute)

    val requestedScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeJobsCount by viewModel.activeJobsCount.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Synchronize ViewModel navigation requests with NavController
    LaunchedEffect(requestedScreen) {
        val targetRoute = CloudBridgeRoutes.fromScreen(requestedScreen)
        val activeRoute = navController.currentDestination?.route
        if (activeRoute != targetRoute && !(targetRoute.startsWith("transfer_detail") && activeRoute?.startsWith("transfer_detail") == true)) {
            navController.navigate(targetRoute) {
                if (targetRoute == CloudBridgeRoutes.DASHBOARD) {
                    popUpTo(CloudBridgeRoutes.DASHBOARD) { inclusive = false }
                }
                launchSingleTop = true
            }
        }
    }

    // Keep ViewModel screen in sync with NavController backstack pop
    LaunchedEffect(currentScreen) {
        if (viewModel.currentScreen.value != currentScreen) {
            viewModel.navigateTo(currentScreen)
        }
    }

    // Determine bottom and top navigation visibility
    val showBottomBar = currentScreen !in listOf(Screen.LANDING, Screen.AUTH)

    // Navigation items
    val navItems = listOf(
        NavItem(Screen.DASHBOARD, "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
        NavItem(Screen.TRANSFER_WIZARD, "Transfer", Icons.Default.SwapHoriz, "nav_transfer"),
        NavItem(Screen.CONNECTIONS, "Clouds", Icons.Default.CloudQueue, "nav_connections"),
        NavItem(Screen.HISTORY, "History", Icons.Default.History, "nav_history"),
        NavItem(Screen.SETTINGS, "Settings", Icons.Default.Settings, "nav_settings")
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("cloudbridge_root_scaffold"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showBottomBar && currentScreen != Screen.TRANSFER_DETAIL) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CloudBridgeLogo(size = 28)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CloudBridge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        if (currentScreen != Screen.DASHBOARD) {
                            IconButton(
                                onClick = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate(CloudBridgeRoutes.DASHBOARD) {
                                            popUpTo(CloudBridgeRoutes.DASHBOARD) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("app_bar_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Dashboard"
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentUser?.role == UserRole.ADMIN && currentScreen != Screen.ADMIN) {
                            IconButton(
                                onClick = {
                                    navController.navigate(CloudBridgeRoutes.ADMIN) {
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.testTag("app_bar_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Console",
                                    tint = CloudBridgePrimary
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen ||
                            (item.screen == Screen.TRANSFER_WIZARD && currentScreen == Screen.TRANSFER_DETAIL)

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                val targetRoute = CloudBridgeRoutes.fromScreen(item.screen)
                                navController.navigate(targetRoute) {
                                    popUpTo(CloudBridgeRoutes.DASHBOARD) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (item.screen == Screen.TRANSFER_WIZARD && activeJobsCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(activeJobsCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CloudBridgePrimary,
                                selectedTextColor = CloudBridgePrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = CloudBridgePrimary.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            CloudBridgeNavGraph(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
