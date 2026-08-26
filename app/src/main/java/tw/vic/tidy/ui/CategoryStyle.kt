package tw.vic.tidy.ui

import androidx.compose.ui.graphics.Color
import tw.vic.tidy.core.JunkCategory
import tw.vic.tidy.ui.theme.T

/** Warm hues mean reclaimable, cool hues mean "look before you delete". */
fun categoryColor(category: JunkCategory): Color = when (category) {
    JunkCategory.TEMP -> T.Brass
    JunkCategory.EMPTY_DIR -> T.Steel
    JunkCategory.ZERO_BYTE -> T.Violet
    JunkCategory.THUMBS -> T.Jade
    JunkCategory.APK_RESIDUE -> Color(0xFFD4A373)
    JunkCategory.BACKUP_RESIDUE -> Color(0xFF8FA6C4)
    JunkCategory.DUPLICATE -> Color(0xFF9C7BB5)
    JunkCategory.BIG_FILE -> T.Clay
}
