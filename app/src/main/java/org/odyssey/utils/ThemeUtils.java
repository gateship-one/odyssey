package org.odyssey.utils;

import android.content.Context;
import android.util.TypedValue;

public class ThemeUtils {

    /**
     * returns the color for the given theme attribute
     * @param context the current context to resolve the attribute id
     * @param attributeColor the requested theme attribute id
     * @return the requested color as an integer
     */
    public static int getThemeColor (final Context context, final int attributeColor) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attributeColor, value, true);
        return value.data;
    }
}
