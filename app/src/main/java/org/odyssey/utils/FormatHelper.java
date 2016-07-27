/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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

package org.odyssey.utils;


public class FormatHelper {
    /**
     * Helper method to uniformly format length strings in Odyssey.
     *
     * @param length Length value in milliseconds
     * @return The formatted string, usable in the ui
     */
    public static String formatTracktimeFromMS(long length) {
        String retVal;
        // calculate duration in minutes and seconds
        String seconds = String.valueOf((length % 60000) / 1000);

        String minutes = String.valueOf(length / 60000);

        if (seconds.length() == 1) {
            retVal = minutes + ":0" + seconds;
        } else {
            retVal = minutes + ":" + seconds;
        }
        return retVal;
    }
}
