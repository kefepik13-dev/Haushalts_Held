package com.example.haushaltsheld.util

import android.content.Context
import com.example.haushaltsheld.R

/**
 * Assigns a consistent color to each user ID for calendar and task list display.
 */
object UserColorHelper {

    private val colorResIds = intArrayOf(
        R.color.user_color_1,
        R.color.user_color_2,
        R.color.user_color_3,
        R.color.user_color_4,
        R.color.user_color_5,
        R.color.user_color_6,
        R.color.user_color_7,
        R.color.user_color_8
    )

    /**
     * Returns a color (resource id) for the given user ID. Same ID always gets the same color.
     */
    fun getColorResId(userId: String): Int {
        if (userId.isEmpty()) return colorResIds[0]
        val index = userId.hashCode().and(0x7FFF_FFFF) % colorResIds.size
        return colorResIds[index]
    }

    /**
     * Returns the actual color int for the given user ID using the context.
     */
    fun getColor(context: Context, userId: String): Int {
        return context.getColor(getColorResId(userId))
    }

    /**
     * Builds a map from user ID to color int for use in adapters.
     */
    fun buildUserIdToColorMap(context: Context, userIds: Set<String>): Map<String, Int> {
        return userIds.associateWith { getColor(context, it) }
    }
}
