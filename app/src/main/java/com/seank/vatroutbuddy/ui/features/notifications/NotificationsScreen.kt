@file:OptIn(ExperimentalFoundationApi::class)

package com.seank.vatroutbuddy.ui.features.notifications

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.seank.vatroutbuddy.R
import com.seank.vatroutbuddy.ui.navigation.NavigationRoutes

@Composable
fun NotificationsScreen(
    onEditNotificationsClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel(),
    collapsibleToolbar: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsState()
    NotificationsScreenContent(
        uiState = uiState,
        editNotifications = onEditNotificationsClicked,
        collapsibleToolbar = collapsibleToolbar,
        refreshPermissions = viewModel::refreshPermissions,
        requestNotificationPermissions = viewModel::requestNotificationPermission,
        toggleWaterbodySubscription = viewModel::toggleWaterbodySubscription,
        toggleCountySubscription = viewModel::toggleCountySubscription,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsScreenContent(
    uiState: NotificationsUiState,
    collapsibleToolbar: Boolean,
    editNotifications: () -> Unit,
    refreshPermissions: () -> Unit,
    requestNotificationPermissions: (permissionLauncher: ActivityResultLauncher<String>) -> Unit,
    toggleWaterbodySubscription: (String, Boolean) -> Unit,
    toggleCountySubscription: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    var hasRequestedPermission by remember { mutableStateOf(false) }

    // We want the top app bar to scroll away ONLY if there is content to scroll,
    // but we also want to make sure that if the app bar is collapsed we can still expand it
    val listState = rememberLazyListState()
    val topAppBarState = rememberTopAppBarState()
    val isCollapsed by remember { derivedStateOf { topAppBarState.collapsedFraction > 0f } }
    val canScroll by remember { derivedStateOf { isCollapsed || listState.canScrollForward || listState.canScrollBackward } }
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        state = topAppBarState,
        canScroll = { collapsibleToolbar && canScroll }
    )

    val topAppBarColors = TopAppBarDefaults.topAppBarColors(
        scrolledContainerColor = MaterialTheme.colorScheme.surface
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            refreshPermissions()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedPermission && uiState is NotificationsUiState.NoPermissions) {
            hasRequestedPermission = true
            requestNotificationPermissions(permissionLauncher)
        }
    }

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                refreshPermissions()
            }

            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_notifications)) },
                colors = topAppBarColors,
                windowInsets = WindowInsets(0.dp),
                scrollBehavior = topBarScrollBehavior,
                actions = {
                    if (uiState is NotificationsUiState.Success) {
                        IconButton(onClick = editNotifications) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_notifications_title)
                            )
                        }
                    }
                }
            )
        },
        // We have consumed the bottom padding for the nav bar in the parent scaffold
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                NotificationsUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // TODO: Loading/skeleton state?
                    }
                }

                NotificationsUiState.NoPermissions -> {
                    NotificationsPermissionPrompt(
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                    )
                }

                is NotificationsUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp)
                    ) {
                        stickyHeader {
                            SectionHeader(
                                title = stringResource(R.string.notifications_county_section_title),
                                onEditClick = editNotifications
                            )
                        }

                        if (uiState.subscribedCounties.isEmpty()) {
                            item {
                                EmptySubscriptionSection(
                                    text = "No notifications"
                                )
                            }
                        } else {
                            items(
                                items = uiState.subscribedCounties,
                                key = { it }
                            ) { county ->
                                SubscriptionItem(
                                    text = county,
                                    onRemove = { toggleCountySubscription(county, false) }
                                )
                            }
                        }

                        stickyHeader {
                            SectionHeader(
                                title = stringResource(R.string.notifications_waterbody_section_title),
                                onEditClick = editNotifications
                            )
                        }

                        if (uiState.subscribedWaterbodies.isEmpty()) {
                            item {
                                EmptySubscriptionSection(
                                    text = "No notifications"
                                )
                            }
                        } else {
                            items(
                                items = uiState.subscribedWaterbodies,
                                key = { it }
                            ) { waterbody ->
                                SubscriptionItem(
                                    text = waterbody,
                                    onRemove = {
                                        toggleWaterbodySubscription(
                                            waterbody,
                                            false
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onEditClick) {
                Text(stringResource(R.string.notifications_edit_button).uppercase())
            }
        }
    }
}

@Composable
private fun EmptySubscriptionSection(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubscriptionItem(
    text: String,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.notifications_remove_button)
                )
            }
        }
    }
}

@Composable
private fun NotificationsPermissionPrompt(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.notifications_permission_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.notifications_permission_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOpenSettings
            ) {
                Text(stringResource(R.string.notifications_permission_button))
            }
        }
    }
} 