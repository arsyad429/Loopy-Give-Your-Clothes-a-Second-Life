package id.ac.loopy.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.ac.loopy.ui.components.UserBottomBar
import id.ac.loopy.ui.navigateToUserTab
import org.junit.Rule
import org.junit.Test

class UserTabNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun berandaOpensFromDonasikuAndRewards() {
        compose.setContent {
            MaterialTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "s01") {
                    composable("s01") {
                        TextButton(onClick = { nav.navigate("s04") }) { Text("Masuk") }
                    }
                    composable("s04") {
                        Column {
                            Text("Konten Beranda")
                            UserBottomBar("s04") { route -> nav.navigate(route) { launchSingleTop = true } }
                        }
                    }
                    composable("s08") {
                        Column {
                            Text("Konten Donasiku")
                            UserBottomBar("s08", nav::navigateToUserTab)
                        }
                    }
                    composable("s10") {
                        Column {
                            Text("Konten Rewards")
                            UserBottomBar("s10", nav::navigateToUserTab)
                        }
                    }
                }
            }
        }

        compose.onNodeWithText("Masuk").performClick()
        compose.onNodeWithText("Donasiku").performClick()
        compose.onNodeWithText("Konten Donasiku").assertExists()
        compose.onNodeWithText("Beranda").performClick()
        compose.onNodeWithText("Konten Beranda").assertExists()

        compose.onNodeWithText("Rewards").performClick()
        compose.onNodeWithText("Konten Rewards").assertExists()
        compose.onNodeWithText("Beranda").performClick()
        compose.onNodeWithText("Konten Beranda").assertExists()
    }
}
