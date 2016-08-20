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

    /**
     * The name of the artist
     */
    private final String mArtistName;

    /**
     * The url for the artist cover
     */
    private final String mArtistURL;

    /**
     * Unique id to identify the artist in the mediastore
     */
    private final long mArtistID;

    /**
     * Constructs a ArtistModel instance with the given parameters.
     */
    public ArtistModel(String name, String artURL, long artistID) {
        mArtistName = name;
        mArtistURL = artURL;
        mArtistID = artistID;
    }

    /**
     * Return the url for the artist cover
     */
    public String getArtistURL() {
        return mArtistURL;
    }

    /**
     * Return the name of the artist
     */
    public String getArtistName() {
        return mArtistName;
    }

    /**
     * Return the unique artist id
     */
    public long getArtistID() {
        return mArtistID;
    }

    /**
     * Return the ArtistModel as a String for debugging purposes.
     */
    @Override
    public String toString() {
        return "Artist: " + getArtistName();
    }

    /**
     * Return the section title for the ArtistModel
     * <p/>
     * The section title is the name of the artist.
     */
    @Override
    public String getSectionTitle() {
        return mArtistName;
    }
}
