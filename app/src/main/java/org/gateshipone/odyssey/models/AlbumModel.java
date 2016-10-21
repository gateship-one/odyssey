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

package org.gateshipone.odyssey.models;

public class AlbumModel implements GenericModel {

    /**
     * The name of the album
     */
    private final String mAlbumName;

    /**
     * The url for the album cover
     */
    private final String mAlbumArtURL;

    /**
     * The name of the artist for the current album
     */
    private final String mArtistName;

    /**
     * Unique key to identify the album in the mediastore
     */
    private final String mAlbumKey;

    /**
     * Constructs a AlbumModel instance with the given parameters.
     */
    public AlbumModel(String name, String albumArtURL, String artistName, String albumKey) {
        if (name != null) {
            mAlbumName = name;
        } else {
            mAlbumName = "";
        }

        if (albumArtURL != null) {
            mAlbumArtURL = albumArtURL;
        } else {
            mAlbumArtURL = "";
        }

        if (artistName != null) {
            mArtistName = artistName;
        } else {
            mArtistName = "";
        }

        if (albumKey != null) {
            mAlbumKey = albumKey;
        } else {
            mAlbumKey = "";
        }
    }

    /**
     * Return the name of the album
     */
    public String getAlbumName() {
        return mAlbumName;
    }

    /**
     * Return the url for the album cover
     */
    public String getAlbumArtURL() {
        return mAlbumArtURL;
    }

    /**
     * Return the name of the related artist
     */
    public String getArtistName() {
        return mArtistName;
    }

    /**
     * Return the unique album key
     */
    public String getAlbumKey() {
        return mAlbumKey;
    }

    /**
     * Return the AlbumModel as a String for debugging purposes.
     */
    @Override
    public String toString() {
        return "Album: " + getAlbumName() + " from: " + getArtistName();
    }

    /**
     * Return the section title for the AlbumModel
     * <p/>
     * The section title is the name of the album.
     */
    @Override
    public String getSectionTitle() {
        return mAlbumName;
    }
}
