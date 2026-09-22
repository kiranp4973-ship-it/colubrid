package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuditLogEntity
import com.example.data.local.ProviderConnectionEntity
import com.example.data.local.TransferEventEntity
import com.example.data.local.TransferItemEntity
import com.example.data.local.TransferJobEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.model.CloudFile
import com.example.data.model.CloudFileType
import com.example.data.model.ConflictStrategy
import com.example.data.model.ProviderType
import com.example.data.model.UserRole
import com.example.data.provider.ProviderResult
import com.example.data.repository.CloudBridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    LANDING,
    AUTH,
    DASHBOARD,
    TRANSFER_WIZARD,
    TRANSFER_DETAIL,
    CONNECTIONS,
    HISTORY,
    SETTINGS,
    ADMIN
}

data class Breadcrumb(val id: String?, val name: String)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CloudBridgeViewModel(private val repository: CloudBridgeRepository) : ViewModel() {

    // Navigation State
    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _activeDetailJobId = MutableStateFlow<String?>(null)
    val activeDetailJobId: StateFlow<String?> = _activeDetailJobId.asStateFlow()

    // User session
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
    val isDevMode: StateFlow<Boolean> = repository.isDevMode

    // Live Jobs & Connections
    val transferJobs: StateFlow<List<TransferJobEntity>> = repository.getTransferJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providerConnections: StateFlow<List<ProviderConnectionEntity>> = repository.getConnections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettingsEntity?> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.getAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Metrics for Dashboard & Admin
    val totalJobsCount: StateFlow<Int> = repository.getTotalJobsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val completedJobsCount: StateFlow<Int> = repository.getCompletedJobsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val failedJobsCount: StateFlow<Int> = repository.getFailedJobsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val activeJobsCount: StateFlow<Int> = repository.getActiveJobsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentTransferSpeed: StateFlow<Long> = repository.transferEngine.engineSpeedBytesPerSec

    // Real-Time Progress, Protocols & Diagnostics
    val realtimeWorker = repository.transferEngine.realtimeWorker
    val realtimeProgress = realtimeWorker.liveProgress
    val realtimeProtocol = realtimeWorker.currentProtocol
    val realtimeConnectionStatus = realtimeWorker.connectionStatus
    val realtimeConnectionLogs = realtimeWorker.connectionLogs
    val realtimePollingIntervalMs = realtimeWorker.pollingIntervalMs
    val realtimeIsFallbackActive = realtimeWorker.isFallbackActive
    val simulateRealtimeErrorMode = realtimeWorker.simulateErrorMode

    fun setRealtimeProtocol(protocol: com.example.data.realtime.RealtimeProtocol) {
        realtimeWorker.setProtocol(protocol)
        showToast("Real-time transport: ${protocol.title}")
    }

    fun setPollingInterval(intervalMs: Long) {
        realtimeWorker.setPollingInterval(intervalMs)
        showToast("Polling fallback interval: ${intervalMs}ms")
    }

    fun toggleSimulateRealtimeError(forceError: Boolean) {
        realtimeWorker.toggleSimulateError(forceError)
        if (forceError) {
            showToast("Simulating connection drop: verifying Polling Fallback failover")
        } else {
            showToast("Connection restored: Primary stream reconnected")
        }
    }

    fun clearRealtimeLogs() {
        realtimeWorker.clearLogs()
        showToast("Connection audit logs cleared")
    }

    // Multi-Account File Browser State
    private val _selectedBrowserProvider = MutableStateFlow(ProviderType.GOOGLE_DRIVE)
    val selectedBrowserProvider: StateFlow<ProviderType> = _selectedBrowserProvider.asStateFlow()

    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<Breadcrumb>>(listOf(Breadcrumb(null, "Google Drive")))
    val breadcrumbs: StateFlow<List<Breadcrumb>> = _breadcrumbs.asStateFlow()

    private val _driveFiles = MutableStateFlow<List<CloudFile>>(emptyList())
    val driveFiles: StateFlow<List<CloudFile>> = _driveFiles.asStateFlow()

    private val _browserFiles = MutableStateFlow<List<CloudFile>>(emptyList())
    val browserFiles: StateFlow<List<CloudFile>> = _browserFiles.asStateFlow()

    private val _browserError = MutableStateFlow<String?>(null)
    val browserError: StateFlow<String?> = _browserError.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CloudFileType?>(null)
    val selectedCategory: StateFlow<CloudFileType?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<CloudFile>>(emptySet())
    val selectedFiles: StateFlow<Set<CloudFile>> = _selectedFiles.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _isLoadingDrive = MutableStateFlow(false)
    val isLoadingDrive: StateFlow<Boolean> = _isLoadingDrive.asStateFlow()

    // Notification / Toast message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Active Job Details Flow
    val activeJob: StateFlow<TransferJobEntity?> = _activeDetailJobId
        .flatMapLatest { id ->
            if (id != null) repository.getTransferJob(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeJobItems: StateFlow<List<TransferItemEntity>> = _activeDetailJobId
        .flatMapLatest { id ->
            if (id != null) repository.getTransferItems(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeJobEvents: StateFlow<List<TransferEventEntity>> = _activeDetailJobId
        .flatMapLatest { id ->
            if (id != null) repository.getTransferEvents(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadBrowserFiles()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun viewJobDetail(jobId: String) {
        _activeDetailJobId.value = jobId
        _currentScreen.value = Screen.TRANSFER_DETAIL
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun selectBrowserProvider(provider: ProviderType) {
        if (_selectedBrowserProvider.value == provider) return
        _selectedBrowserProvider.value = provider
        _currentFolderId.value = null
        _searchQuery.value = ""
        _selectedCategory.value = null
        _breadcrumbs.value = listOf(Breadcrumb(null, provider.displayName))
        loadBrowserFiles(provider = provider, folderId = null, query = "")
    }

    fun toggleCategoryFilter(type: CloudFileType?) {
        _selectedCategory.value = if (_selectedCategory.value == type) null else type
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadBrowserFiles(query = query)
    }

    fun navigateUp() {
        val crumbs = _breadcrumbs.value
        if (crumbs.size > 1) {
            val parentCrumb = crumbs[crumbs.size - 2]
            navigateBreadcrumb(parentCrumb)
        }
    }

    fun navigateIntoFolder(folder: CloudFile) {
        if (!folder.isFolder) return
        _currentFolderId.value = folder.id
        val updatedCrumbs = _breadcrumbs.value.toMutableList()
        updatedCrumbs.add(Breadcrumb(folder.id, folder.name))
        _breadcrumbs.value = updatedCrumbs
        loadBrowserFiles(folderId = folder.id)
    }

    fun navigateBreadcrumb(target: Breadcrumb) {
        val currentList = _breadcrumbs.value
        val index = currentList.indexOfFirst { it.id == target.id }
        if (index != -1) {
            _breadcrumbs.value = currentList.subList(0, index + 1)
            _currentFolderId.value = target.id
            loadBrowserFiles(folderId = target.id)
        }
    }

    fun loadBrowserFiles(
        provider: ProviderType = _selectedBrowserProvider.value,
        folderId: String? = _currentFolderId.value,
        query: String? = _searchQuery.value
    ) {
        viewModelScope.launch {
            _isLoadingDrive.value = true
            _browserError.value = null
            when (val res = repository.listProviderFiles(provider, folderId, query)) {
                is ProviderResult.Success -> {
                    _browserFiles.value = res.data.files
                    if (provider == ProviderType.GOOGLE_DRIVE) {
                        _driveFiles.value = res.data.files
                    }
                }
                is ProviderResult.Error -> {
                    _browserFiles.value = emptyList()
                    _browserError.value = res.message
                    showToast(res.message)
                }
            }
            _isLoadingDrive.value = false
        }
    }

    fun loadDriveFiles(folderId: String? = _currentFolderId.value, query: String? = _searchQuery.value) {
        loadBrowserFiles(ProviderType.GOOGLE_DRIVE, folderId, query)
    }

    fun refreshBrowser() {
        loadBrowserFiles()
    }

    fun toggleFileSelection(file: CloudFile) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.any { it.id == file.id }) {
            current.removeAll { it.id == file.id }
        } else {
            current.add(file)
        }
        _selectedFiles.value = current
    }

    fun selectAllFiles() {
        val currentList = _browserFiles.value
        val category = _selectedCategory.value
        val itemsToSelect = if (category != null) {
            currentList.filter { it.fileType == category }
        } else {
            currentList
        }
        _selectedFiles.value = _selectedFiles.value + itemsToSelect
    }

    fun clearFileSelection() {
        _selectedFiles.value = emptySet()
    }

    fun startTransfer(destinationPath: String, conflictStrategy: ConflictStrategy) {
        val selected = _selectedFiles.value.toList()
        if (selected.isEmpty()) {
            showToast("Please select at least one file or folder to transfer.")
            return
        }

        viewModelScope.launch {
            try {
                val jobId = repository.createAndStartTransferJob(
                    selectedFiles = selected,
                    destinationPath = destinationPath,
                    conflictStrategy = conflictStrategy
                )
                clearFileSelection()
                viewJobDetail(jobId)
                showToast("Transfer initiated: ${selected.size} items queued.")
            } catch (e: Exception) {
                showToast("Failed to start transfer: ${e.localizedMessage}")
            }
        }
    }

    fun triggerDemoTransfer() {
        viewModelScope.launch {
            try {
                val jobId = repository.createDemoTransfer()
                viewJobDetail(jobId)
                showToast("Started simulated JioCloud demo transfer (14.8 GB, 120 files).")
            } catch (e: Exception) {
                showToast("Error starting demo: ${e.localizedMessage}")
            }
        }
    }

    fun pauseTransfer(jobId: String) {
        repository.transferEngine.pauseJob(jobId)
        showToast("Transfer paused. You can resume this transfer later.")
    }

    fun resumeTransfer(jobId: String) {
        repository.transferEngine.resumeJob(
            jobId = jobId,
            sourceProvider = repository.googleDriveProvider,
            destinationProvider = repository.getDestinationProvider(),
            maxConcurrency = 3
        )
        showToast("Resuming transfer...")
    }

    fun cancelTransfer(jobId: String) {
        repository.transferEngine.cancelJob(jobId)
        showToast("Transfer cancelled.")
    }

    fun retryFailedItems(jobId: String) {
        repository.transferEngine.retryFailedItems(
            jobId = jobId,
            sourceProvider = repository.googleDriveProvider,
            destinationProvider = repository.getDestinationProvider(),
            maxConcurrency = 3
        )
        showToast("Retrying failed files...")
    }

    fun deleteTransferJob(jobId: String) {
        viewModelScope.launch {
            repository.deleteJob(jobId)
            if (_activeDetailJobId.value == jobId) {
                _activeDetailJobId.value = null
                _currentScreen.value = Screen.HISTORY
            }
            showToast("Transfer history record removed.")
        }
    }

    fun connectGoogleDrive() {
        viewModelScope.launch {
            when (val res = repository.connectGoogleDrive()) {
                is ProviderResult.Success -> {
                    showToast("Connected to Google Drive successfully.")
                    loadDriveFiles()
                }
                is ProviderResult.Error -> showToast(res.message)
            }
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            repository.disconnectGoogleDrive()
            showToast("Google Drive disconnected.")
        }
    }

    fun toggleDevMode(enabled: Boolean) {
        repository.toggleDevMode(enabled)
        showToast(if (enabled) "Development Mode enabled: Mock JioCloud destination active." else "Production Mode enabled: Real JioCloud endpoints required.")
    }

    fun saveJioCloudConfig(baseUrl: String, clientId: String, clientSecret: String) {
        viewModelScope.launch {
            val current = userSettings.value ?: UserSettingsEntity(userId = currentUser.value?.id ?: "usr_prod_demo_01")
            repository.saveSettings(
                current.copy(
                    jioCloudApiBaseUrl = baseUrl,
                    jioCloudClientId = clientId,
                    jioCloudClientSecret = clientSecret
                )
            )
            showToast("JioCloud API credentials updated.")
        }
    }

    fun signIn(email: String, role: UserRole = UserRole.USER) {
        repository.signIn(email, role)
        _currentScreen.value = Screen.DASHBOARD
        showToast("Signed in as $email")
    }

    fun signOut() {
        repository.signOut()
        _currentScreen.value = Screen.AUTH
        showToast("Signed out.")
    }
}

class CloudBridgeViewModelFactory(private val repository: CloudBridgeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CloudBridgeViewModel::class.java)) {
            return CloudBridgeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
