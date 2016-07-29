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

    private long mId;
    private String mTitle;
    private int mNumberOfTracks;

    public BookmarkModel(long id, String title, int numberOfTracks) {
        mId = id;
        mTitle = title;
        mNumberOfTracks = numberOfTracks;
    }

    public long getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getNumberOfTracks() {
        return mNumberOfTracks;
    }

    @Override
    public String getSectionTitle() {
        return mTitle;
    }
}
