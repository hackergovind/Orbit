package com.antigravity.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.antigravity.app.ui.screens.ChatListScreen
import com.antigravity.app.ui.screens.ConversationScreen
import com.antigravity.app.ui.screens.DiagnosticsScreen
import com.antigravity.app.ui.screens.MeshRadarScreen
import com.antigravity.app.ui.viewmodel.ChatViewModel
import com.antigravity.app.ui.viewmodel.MeshViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Radar : Screen("radar", "Mesh Radar", Icons.Filled.Radar)
    object Chats : Screen("chats", "Direct", Icons.Filled.Chat)
    object Groups : Screen("groups", "Groups", Icons.Filled.Group)
    object Diagnostics : Screen("diagnostics", "Diagnostics", Icons.Filled.NetworkCheck)
    object Conversation : Screen("conversation/{targetId}?isGroup={isGroup}", "Chat", Icons.Filled.Chat)
}

val BottomNavigationScreens = listOf(
    Screen.Radar,
    Screen.Chats,
    Screen.Groups,
    Screen.Diagnostics
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val meshViewModel: MeshViewModel = viewModel()
    
    // We'll pass chatViewModel locally when inside the conversation, but ChatList needs state too
    val globalChatViewModel: ChatViewModel = viewModel() 

    Scaffold(
        bottomBar = { AppBottomNavigation(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Radar.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Radar.route) {
                MeshRadarScreen(
                    viewModel = meshViewModel,
                    onNodeSelected = { nodeId ->
                        navController.navigate("conversation/$nodeId?isGroup=false")
                    }
                )
            }
            composable(Screen.Chats.route) {
                ChatListScreen(
                    meshViewModel = meshViewModel,
                    chatViewModel = globalChatViewModel,
                    isGroup = false,
                    onChatSelected = { targetId ->
                        navController.navigate("conversation/$targetId?isGroup=false")
                    }
                )
            }
            composable(Screen.Groups.route) {
                ChatListScreen(
                    meshViewModel = meshViewModel,
                    chatViewModel = globalChatViewModel,
                    isGroup = true,
                    onChatSelected = { groupId ->
                        navController.navigate("conversation/$groupId?isGroup=true")
                    }
                )
            }
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(viewModel = meshViewModel)
            }
            composable(Screen.Conversation.route) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: return@composable
                val isGroup = backStackEntry.arguments?.getString("isGroup")?.toBoolean() ?: false
                
                ConversationScreen(
                    targetId = targetId,
                    isGroup = isGroup,
                    viewModel = globalChatViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on conversation screen
    if (currentRoute?.startsWith("conversation") == true) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BottomNavigationScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(text = screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
