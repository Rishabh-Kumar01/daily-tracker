package dev.rishabh.dailytracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.rishabh.dailytracker.feature.activities.ActivityScreen
import dev.rishabh.dailytracker.feature.activities.SubMenuScreen
import dev.rishabh.dailytracker.feature.diet.scan.ScanScreen
import dev.rishabh.dailytracker.feature.home.HomeScreen

/**
 * The app's single navigation graph: Home → an activity's sub-menus → a sub-menu's items.
 *
 * IDs are string path args; each destination's ViewModel reads its own id from
 * SavedStateHandle, so no display text is carried through routes.
 */
@Composable
fun DailyTrackerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onActivityClick = { templateId -> navController.navigate(Routes.activity(templateId)) },
            )
        }

        composable(
            route = Routes.ACTIVITY,
            arguments = listOf(navArgument(Routes.ARG_TEMPLATE_ID) { type = NavType.StringType }),
        ) {
            ActivityScreen(
                onBack = { navController.popBackStack() },
                onSubMenuClick = { subMenuId -> navController.navigate(Routes.subMenu(subMenuId)) },
            )
        }

        composable(
            route = Routes.SUB_MENU,
            arguments = listOf(navArgument(Routes.ARG_SUB_MENU_ID) { type = NavType.StringType }),
        ) {
            SubMenuScreen(
                onBack = { navController.popBackStack() },
                onScanClick = { itemId -> navController.navigate(Routes.scan(itemId)) },
            )
        }

        composable(
            route = Routes.SCAN,
            arguments = listOf(navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType }),
        ) {
            ScanScreen(onBack = { navController.popBackStack() })
        }
    }
}
