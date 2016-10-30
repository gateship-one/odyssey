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

package org.gateshipone.odyssey.utils;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatHelper {
    /**
     * Helper method to uniformly format length strings in Odyssey.
     *
     * @param length Length value in milliseconds
     * @return the formatted string, usable in the ui
     */
    public static String formatTracktimeFromMS(long length) {

        String retVal;

        int seconds = (int) (length / 1000);

        int hours = seconds / 3600;

        int minutes = (seconds - (hours * 3600)) / 60;

        seconds = seconds - (hours * 3600) - (minutes * 60);

        if (hours == 0) {
            retVal = String.format(Locale.getDefault(), "%02d" + ":" + "%02d", minutes, seconds);
        } else {
            retVal = String.format(Locale.getDefault(), "%02d" + ":" + "%02d" + ":" + "%02d", hours, minutes, seconds);
        }

        return retVal;
    }

    /**
     * Helper method to format the mediastore track number to a track number string
     *
     * @param trackNumber the tracknumber from the mediastore
     * @return the formatted string, usable in the ui
     */
    public static String formatTrackNumber(int trackNumber) {

        if (trackNumber == -1) {
            return "";
        }

        String trackNumberString = String.valueOf(trackNumber);

        // mediastore combines track and cd number in one string so cut off the first two literals
        if (trackNumberString.length() >= 4) {
            trackNumberString = trackNumberString.substring(2);
        }

        return trackNumberString;
    }

    /**
     * Helper method to format a timestamp to a uniformly format date string in Odyssey.
     *
     * @param timestamp The timestamp in milliseconds
     * @return the formatted string, usable in the ui
     */
    public static String formatTimeStampToString(long timestamp) {
        Date date = new Date(timestamp);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);
    }
}
