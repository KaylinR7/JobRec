package com.example.jobrec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jobrec.ui.screens.AdminCompaniesScreen
import com.example.jobrec.ui.screens.AdminJobsScreen
import com.example.jobrec.ui.screens.AdminUsersScreen
import com.example.jobrec.ui.theme.JobRecTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JobRecTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Companies.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Companies.route) { 
                AdminCompaniesScreen(navController)
            }
            composable(Screen.Jobs.route) { 
                AdminJobsScreen(navController)
            }
            composable(Screen.Users.route) { 
                AdminUsersScreen(navController)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Companies,
        Screen.Jobs,
        Screen.Users
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = screen.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String
) {
    object Companies : Screen(
        route = "companies",
        title = "Companies"
    )
    
    object Jobs : Screen(
        route = "jobs",
        title = "Jobs"
    )
    
    object Users : Screen(
        route = "users",
        title = "Users"
    )
}