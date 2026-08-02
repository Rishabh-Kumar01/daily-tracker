package dev.rishabh.dailytracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.rishabh.dailytracker.feature.activities.ActivityScreen
import dev.rishabh.dailytracker.feature.activities.SubMenuScreen
import dev.rishabh.dailytracker.feature.diet.scan.ScanScreen
import dev.rishabh.dailytracker.feature.foods.MyFoodsScreen
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
                onMyFoodsClick = { navController.navigate(Routes.MY_FOODS) },
            )
        }

        composable(Routes.MY_FOODS) {
            MyFoodsScreen(onBack = { navController.popBackStack() })
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
        ) { backStackEntry ->
            // "Log it now" from the scanner arrives through this entry's savedStateHandle;
            // the meal screen turns it into an open portion sheet, then it's consumed.
            val scanLogItemId by backStackEntry.savedStateHandle
                .getStateFlow<String?>(Routes.SCAN_LOG_ITEM_ID, null)
                .collectAsStateWithLifecycle()
            val scanLogProductId by backStackEntry.savedStateHandle
                .getStateFlow<String?>(Routes.SCAN_LOG_PRODUCT_ID, null)
                .collectAsStateWithLifecycle()
            SubMenuScreen(
                onBack = { navController.popBackStack() },
                onScanClick = { itemId -> navController.navigate(Routes.scan(itemId)) },
                pendingScanLogItemId = scanLogItemId,
                pendingScanLogProductId = scanLogProductId,
                onScanLogConsumed = {
                    backStackEntry.savedStateHandle.remove<String>(Routes.SCAN_LOG_ITEM_ID)
                    backStackEntry.savedStateHandle.remove<String>(Routes.SCAN_LOG_PRODUCT_ID)
                },
            )
        }

        composable(
            route = Routes.SCAN,
            arguments = listOf(navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString(Routes.ARG_ITEM_ID).orEmpty()
            ScanScreen(
                onBack = { navController.popBackStack() },
                onLogExisting = { productId ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(Routes.SCAN_LOG_ITEM_ID, itemId)
                        set(Routes.SCAN_LOG_PRODUCT_ID, productId)
                    }
                    navController.popBackStack()
                },
            )
        }
    }
}
