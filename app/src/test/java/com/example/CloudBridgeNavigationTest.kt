package com.example

import com.example.ui.Screen
import com.example.ui.navigation.CloudBridgeRoutes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudBridgeNavigationTest {

    @Test
    fun verifyNavigationRoutesMapping() {
        // Landing route
        assertEquals(CloudBridgeRoutes.LANDING, CloudBridgeRoutes.fromScreen(Screen.LANDING))
        assertEquals(Screen.LANDING, CloudBridgeRoutes.toScreen(CloudBridgeRoutes.LANDING))

        // Dashboard route
        assertEquals(CloudBridgeRoutes.DASHBOARD, CloudBridgeRoutes.fromScreen(Screen.DASHBOARD))
        assertEquals(Screen.DASHBOARD, CloudBridgeRoutes.toScreen(CloudBridgeRoutes.DASHBOARD))

        // Transfer route
        assertEquals(CloudBridgeRoutes.TRANSFER, CloudBridgeRoutes.fromScreen(Screen.TRANSFER_WIZARD))
        assertEquals(Screen.TRANSFER_WIZARD, CloudBridgeRoutes.toScreen(CloudBridgeRoutes.TRANSFER))

        // History route
        assertEquals(CloudBridgeRoutes.HISTORY, CloudBridgeRoutes.fromScreen(Screen.HISTORY))
        assertEquals(Screen.HISTORY, CloudBridgeRoutes.toScreen(CloudBridgeRoutes.HISTORY))

        // Settings route
        assertEquals(CloudBridgeRoutes.SETTINGS, CloudBridgeRoutes.fromScreen(Screen.SETTINGS))
        assertEquals(Screen.SETTINGS, CloudBridgeRoutes.toScreen(CloudBridgeRoutes.SETTINGS))

        // Detail route with parameter
        val detailRoute = CloudBridgeRoutes.transferDetail("job-101")
        assertEquals("transfer_detail/job-101", detailRoute)
        assertEquals(Screen.TRANSFER_DETAIL, CloudBridgeRoutes.toScreen(detailRoute))
    }
}
