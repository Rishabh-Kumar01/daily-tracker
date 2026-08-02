package dev.rishabh.dailytracker.navigation

/**
 * The navigation graph: Home → one activity's sub-menus → one sub-menu's items.
 *
 * IDs travel in the path. Screens load their own header (activity/sub-menu name) from the
 * id rather than passing display text through the route, so a rename shows up everywhere.
 */
object Routes {
    const val HOME = "home"

    /** The My Foods library — browse, edit, and archive products. */
    const val MY_FOODS = "myfoods"

    const val ARG_TEMPLATE_ID = "templateId"
    const val ARG_SUB_MENU_ID = "subMenuId"
    const val ARG_ITEM_ID = "itemId"

    const val ACTIVITY = "activity/{$ARG_TEMPLATE_ID}"
    const val SUB_MENU = "submenu/{$ARG_SUB_MENU_ID}"

    /** The barcode scanner is per-food: the item decides the product's grouping key. */
    const val SCAN = "scan/{$ARG_ITEM_ID}"

    /**
     * Scan → meal hand-off keys, written to the sub-menu entry's savedStateHandle when a
     * re-scanned product should go straight to its portion sheet ("Log it now").
     */
    const val SCAN_LOG_ITEM_ID = "scanLogItemId"
    const val SCAN_LOG_PRODUCT_ID = "scanLogProductId"

    fun activity(templateId: String) = "activity/$templateId"
    fun subMenu(subMenuId: String) = "submenu/$subMenuId"
    fun scan(itemId: String) = "scan/$itemId"
}
