/*
 * Copyright (C) 2023 Team Gateship-One
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

package org.gateshipone.odyssey.models;

public class BookmarkModel implements GenericModel {

    /**
     * Unique id to identify the bookmark
     */
    private final long mId;

    /**
     * The name of the bookmark
     */
    private final String mTitle;

    /**
     * The number of tracks in the bookmark
     */
    private final int mNumberOfTracks;

    /**
     * Constructs a BookmarkModel instance with the given parameters.
     */
    public BookmarkModel(long id, String title, int numberOfTracks) {
        if (title != null) {
            mTitle = title;
        } else {
            mTitle = "";
        }

        mId = id;
        mNumberOfTracks = numberOfTracks;
    }

    /**
     * Return the id of the bookmark
     */
    public long getId() {
        return mId;
    }

    /**
     * Return the name of the bookmark
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Return the number of tracks in the bookmark
     */
    public int getNumberOfTracks() {
        return mNumberOfTracks;
    }

    /**
     * Return the section title for the BookmarkModel
     * <p/>
     * The section title is the name of the bookmark.
     */
    @Override
    public String getSectionTitle() {
        return mTitle;
    }
}
