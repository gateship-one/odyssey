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

package org.odyssey.models;

public class BookmarkModel implements GenericModel {

    /**
     * Unique id to identify the bookmark
     */
    private long mId;

    /**
     * The name of the bookmark
     */
    private String mTitle;

    /**
     * The number of tracks in the bookmark
     */
    private int mNumberOfTracks;

    /**
     * Constructs a BookmarkModel instance with the given parameters.
     */
    public BookmarkModel(long id, String title, int numberOfTracks) {
        mId = id;
        mTitle = title;
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
