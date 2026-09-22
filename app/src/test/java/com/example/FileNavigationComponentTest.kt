package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CloudBridgeDatabase
import com.example.data.model.CloudFileType
import com.example.data.model.ProviderType
import com.example.data.repository.CloudBridgeRepository
import com.example.ui.CloudBridgeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FileNavigationComponentTest {

    private lateinit var context: Context
    private lateinit var database: CloudBridgeDatabase
    private lateinit var repository: CloudBridgeRepository
    private lateinit var viewModel: CloudBridgeViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = CloudBridgeDatabase.getInMemoryDatabase(context)
        repository = CloudBridgeRepository(database)
        viewModel = CloudBridgeViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testGoogleDriveFileListingAndSelection() = runTest {
        advanceUntilIdle()

        assertEquals(ProviderType.GOOGLE_DRIVE, viewModel.selectedBrowserProvider.value)

        val files = viewModel.browserFiles.value
        assertTrue("Google Drive should have files loaded", files.isNotEmpty())

        val photosFolder = files.find { it.isFolder && it.name == "Photos" }
        assertNotNull("Should have a Photos folder", photosFolder)

        // Select a file
        val fileToSelect = files.first { !it.isFolder }
        viewModel.toggleFileSelection(fileToSelect)
        assertTrue(viewModel.selectedFiles.value.any { it.id == fileToSelect.id })

        // Toggle again to deselect
        viewModel.toggleFileSelection(fileToSelect)
        assertFalse(viewModel.selectedFiles.value.any { it.id == fileToSelect.id })
    }

    @Test
    fun testJioCloudFileListingAndSelection() = runTest {
        advanceUntilIdle()

        // Switch to JioCloud
        viewModel.selectBrowserProvider(ProviderType.JIO_CLOUD)
        advanceUntilIdle()

        assertEquals(ProviderType.JIO_CLOUD, viewModel.selectedBrowserProvider.value)

        val files = viewModel.browserFiles.value
        assertTrue("JioCloud should have files loaded in dev mode", files.isNotEmpty())

        // Verify root files in JioCloud
        val docsFolder = files.find { it.isFolder && it.name == "Jio Documents" }
        assertNotNull("Should find Jio Documents folder", docsFolder)

        val taxFile = files.find { !it.isFolder && it.name.contains("Annual_Tax_Statement") }
        assertNotNull("Should find Annual_Tax_Statement file", taxFile)

        // Select file in JioCloud
        taxFile?.let { viewModel.toggleFileSelection(it) }
        assertTrue(viewModel.selectedFiles.value.any { it.id == taxFile?.id })
    }

    @Test
    fun testBreadcrumbAndUpNavigation() = runTest {
        advanceUntilIdle()

        val initialCrumbs = viewModel.breadcrumbs.value
        assertEquals(1, initialCrumbs.size)

        val files = viewModel.browserFiles.value
        val photosFolder = files.first { it.isFolder }

        // Navigate into Photos
        viewModel.navigateIntoFolder(photosFolder)
        advanceUntilIdle()

        assertEquals(2, viewModel.breadcrumbs.value.size)
        assertEquals(photosFolder.name, viewModel.breadcrumbs.value.last().name)
        assertEquals(photosFolder.id, viewModel.currentFolderId.value)

        // Navigate up one folder
        viewModel.navigateUp()
        advanceUntilIdle()

        assertEquals(1, viewModel.breadcrumbs.value.size)
        assertEquals(null, viewModel.currentFolderId.value)
    }

    @Test
    fun testCategoryFilteringAndSelectAll() = runTest {
        advanceUntilIdle()

        // Filter by PDF
        viewModel.toggleCategoryFilter(CloudFileType.PDF)
        assertEquals(CloudFileType.PDF, viewModel.selectedCategory.value)

        // Select all filtered items
        viewModel.selectAllFiles()
        val selected = viewModel.selectedFiles.value
        assertTrue("Should have selected PDF files", selected.isNotEmpty())
        assertTrue("All selected items should be PDFs", selected.all { it.fileType == CloudFileType.PDF })

        // Clear selection
        viewModel.clearFileSelection()
        assertTrue(viewModel.selectedFiles.value.isEmpty())
    }

    @Test
    fun testSearchQueryFiltering() = runTest {
        advanceUntilIdle()

        viewModel.setSearchQuery("QuickStart")
        advanceUntilIdle()

        val searchResults = viewModel.browserFiles.value
        assertTrue("Search should return matching files", searchResults.all { it.name.contains("QuickStart", ignoreCase = true) })

        viewModel.setSearchQuery("")
        advanceUntilIdle()
        assertTrue("Clearing search restores all files", viewModel.browserFiles.value.size > 1)
    }
}
