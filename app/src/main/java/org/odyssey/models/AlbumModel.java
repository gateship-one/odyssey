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

public class AlbumModel implements GenericModel {

    private final String mAlbumName;
    private final String mAlbumArtURL;
    private final String mArtistName;
    private final String mAlbumKey;

    public AlbumModel(String name, String albumArtURL, String artistName, String albumKey ) {
        mAlbumName = name;
        mAlbumArtURL = albumArtURL;
        mArtistName = artistName;
        mAlbumKey = albumKey;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public String getAlbumArtURL() {
        return mAlbumArtURL;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public String getAlbumKey() {
        return mAlbumKey;
    }

    @Override
    public String toString() {
        return "Album: " + getAlbumName() + " from: " + getArtistName();
    }

    @Override
    public String getSectionTitle() {
        return mAlbumName;
    }
}
