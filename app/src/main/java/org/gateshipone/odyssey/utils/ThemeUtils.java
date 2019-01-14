/*
 * Copyright (C) 2019 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.utils;

import android.content.Context;
import android.util.TypedValue;

public class ThemeUtils {

    /**
     * returns the color for the given theme attribute
     *
     * @param context        the current context to resolve the attribute id
     * @param attributeColor the requested theme attribute id
     * @return the requested color as an integer
     */
    public static int getThemeColor(final Context context, final int attributeColor) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attributeColor, value, true);
        return value.data;
    }

    /**
     * returns the resource id for the given theme attribute
     *
     * @param context the current context to resolve the attribute id
     * @param attributeId the requested theme attribute id
     * @return the resolved resource id as an integer
     */
    public static int getThemeResourceId(final Context context, final int attributeId) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, value, true);
        return value.resourceId;
    }
}
