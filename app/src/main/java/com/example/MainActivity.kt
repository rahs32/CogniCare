package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.MainViewModel
import com.example.ui.AacTab
import com.example.ui.MonitorTab
import com.example.ui.SosTab
import com.example.ui.OnboardingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        if (!isOnboardingCompleted) {
            OnboardingScreen(onComplete = { sensitivity, family, friends, strangers, phone ->
                viewModel.completeOnboarding(sensitivity, family, friends, strangers, phone)
            })
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Text("CogniCare Menu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                        Divider()
                        NavigationDrawerItem(label = { Text("Reports") }, selected = false, onClick = { 
                            scope.launch { drawerState.close() }
                            navController.navigate("reports") { popUpTo(navController.graph.startDestinationId) { saveState = true } ; launchSingleTop = true ; restoreState = true }
                        })
                        NavigationDrawerItem(label = { Text("Edit Habits & Tasks") }, selected = false, onClick = { /* TODO */ })
                        NavigationDrawerItem(label = { Text("Caretaker Details") }, selected = false, onClick = { /* TODO */ })
                        Divider()
                        NavigationDrawerItem(label = { Text("Profile") }, selected = false, onClick = { /* TODO */ })
                        NavigationDrawerItem(label = { Text("Settings") }, selected = false, onClick = { /* TODO */ })
                        NavigationDrawerItem(label = { Text("Reset Demo Onboarding") }, selected = false, onClick = {
                            scope.launch { 
                                viewModel.userPreferences.clearOnboarding() 
                                drawerState.close()
                            }
                        })
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("CogniCare") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        
                        NavigationBar(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.ChatBubble, contentDescription = "AAC") },
                                label = { Text("AAC") },
                                selected = currentRoute == "aac",
                                onClick = {
                                    navController.navigate("aac") { popUpTo(navController.graph.startDestinationId) { saveState = true } ; launchSingleTop = true ; restoreState = true }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Monitor") },
                                label = { Text("Monitor") },
                                selected = currentRoute == "monitor",
                                onClick = {
                                    navController.navigate("monitor") { popUpTo(navController.graph.startDestinationId) { saveState = true } ; launchSingleTop = true ; restoreState = true }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Warning, contentDescription = "SOS") },
                                label = { Text("SOS") },
                                selected = currentRoute == "sos",
                                onClick = {
                                    navController.navigate("sos") { popUpTo(navController.graph.startDestinationId) { saveState = true } ; launchSingleTop = true ; restoreState = true }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "aac",
                        modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
                    ) {
                        composable("aac") { AacTab(viewModel) }
                        composable("monitor") { MonitorTab(viewModel) }
                        composable("sos") { SosTab(viewModel) }
                        composable("reports") { com.example.ui.ReportsTab(viewModel) }
                    }
                }
            }
        }
      }
    }
  }
}
