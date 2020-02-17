/*
 * Copyright (C) 2020 Team Gateship-One
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

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

/**
 * Utils class which holds several static methods for string comparison tasks.
 */
public class StringCompareUtils {

    /**
     * Global threshold for string comparison.
     */
    private static final double COMPARE_THRESHOLD = 0.20;

    /**
     * Method to compare to strings using normalized levenshtein distance.
     * <p>
     * If the COMPARE_THRESHOLD is violated a simple contains check will be applied.
     *
     * @param expected The expected string.
     * @param actual   The actual string.
     * @return True if the comparison succeed otherwise false.
     */
    public static boolean compareStrings(final String expected, final String actual) {
        final NormalizedLevenshtein comparator = new NormalizedLevenshtein();

        double distance = comparator.distance(expected, actual);

        if (distance < COMPARE_THRESHOLD) {
            return true;
        } else {
            return actual.toLowerCase().contains(expected.toLowerCase()) || expected.toLowerCase().contains(actual.toLowerCase());
        }
    }
}
