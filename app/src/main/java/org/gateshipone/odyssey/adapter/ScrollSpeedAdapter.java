/*
 * Copyright (C) 2018 Team Gateship-One
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

package org.gateshipone.odyssey.adapter;

/**
 * Abstract adapter used for speed optimizations on asynchronous cover loading.
 * This is necessary to load covers only at certain scroll speed to not put
 * to much load on the CPU during scrolling.
 */
public interface ScrollSpeedAdapter {

    /**
     * Determines how the new time value affects the average (0.0(new value has no effect) - 1.0(average is only the new value, no smoothing)
     */
    float mSmoothingFactor = 0.3f;

    /**
     * Sets the scrollspeed in items per second.
     *
     * @param speed Items per seconds as Integer.
     */
    void setScrollSpeed(int speed);

    /**
     * Returns the smoothed average loading time of images.
     * This value is used by the scrollspeed listener to determine if
     * the scrolling is slow enough to render images (artist, album images)
     *
     * @return Average time to load an image in ms
     */
    long getAverageImageLoadTime();

    /**
     * This method adds new loading times to the smoothed average.
     * Should only be called from the async cover loader.
     *
     * @param time Time in ms to load a image
     */
    void addImageLoadTime(long time);

}
