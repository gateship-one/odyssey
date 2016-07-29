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

public class ArtistModel implements GenericModel {

    private final String mArtistName;
    private final String mArtistURL;
    private final String mArtistKey;
    private final long mArtistID;

    public ArtistModel(String name, String artURL, String artistKey, long artistID) {
        mArtistName = name;
        mArtistURL = artURL;
        mArtistKey = artistKey;
        mArtistID = artistID;
    }

    public String getArtistURL() {
        return mArtistURL;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public String getArtistKey() {
        return mArtistKey;
    }

    public long getArtistID() {
        return mArtistID;
    }

    @Override
    public String toString() {
        return "Artist: " + getArtistName();
    }

    @Override
    public String getSectionTitle() {
        return mArtistName;
    }
}
