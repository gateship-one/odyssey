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

package org.gateshipone.odyssey.artwork.network;

import android.net.Uri;

import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.GenericModel;
import org.gateshipone.odyssey.utils.FormatHelper;

public class ArtworkRequestModel {

    public enum ArtworkRequestType {
        ALBUM,
        ARTIST
    }

    private GenericModel mModel;

    private ArtworkRequestType mType;

    public ArtworkRequestModel(ArtistModel artistModel) {
        this(artistModel, ArtworkRequestType.ARTIST);
    }

    public ArtworkRequestModel(AlbumModel albumModel) {
        this(albumModel, ArtworkRequestType.ALBUM);
    }

    private ArtworkRequestModel(GenericModel model, ArtworkRequestType type) {
        mModel = model;
        mType = type;
    }

    public ArtworkRequestType getType() {
        return mType;
    }

    public void setMBId(final String mbid) {
        switch (mType) {
            case ALBUM:
                ((AlbumModel) mModel).setMBId(mbid);
                break;
            case ARTIST:
                ((ArtistModel) mModel).setMBId(mbid);
                break;
        }
    }

    public String getAlbumName() {
        String albumName = null;

        switch (mType) {
            case ALBUM:
                albumName = ((AlbumModel) mModel).getAlbumName();
                break;
            case ARTIST:
                break;
        }

        return albumName;
    }

    public String getArtistName() {
        String artistName = null;

        switch (mType) {
            case ALBUM:
                artistName = ((AlbumModel) mModel).getArtistName();
                break;
            case ARTIST:
                artistName = ((ArtistModel) mModel).getArtistName();
                break;
        }

        return artistName;
    }

    public String getEncodedAlbumName() {
        String encodedAlbumName = null;

        switch (mType) {
            case ALBUM:
                encodedAlbumName = Uri.encode(((AlbumModel) mModel).getAlbumName());
                break;
            case ARTIST:
                break;
        }

        return encodedAlbumName;
    }

    public String getLuceneEscapedEncodedAlbumName() {
        String escapedAlbumName = null;

        switch (mType) {
            case ALBUM:
                escapedAlbumName = FormatHelper.escapeSpecialCharsLucene(((AlbumModel) mModel).getAlbumName());
                break;
            case ARTIST:
                break;
        }

        return Uri.encode(escapedAlbumName);
    }

    public String getEncodedArtistName() {
        String encodedArtistName = null;

        switch (mType) {
            case ALBUM:
                encodedArtistName = Uri.encode(((AlbumModel) mModel).getArtistName());
                break;
            case ARTIST:
                encodedArtistName = Uri.encode(((ArtistModel) mModel).getArtistName().replaceAll("/", " "));
                break;
        }

        return encodedArtistName;
    }

    public String getLuceneEscapedEncodedArtistName() {
        String escapedArtistName = null;

        switch (mType) {
            case ALBUM:
                escapedArtistName = FormatHelper.escapeSpecialCharsLucene(((AlbumModel) mModel).getArtistName());
                break;
            case ARTIST:
                escapedArtistName = FormatHelper.escapeSpecialCharsLucene(((ArtistModel) mModel).getArtistName());
                break;
        }

        return Uri.encode(escapedArtistName);
    }

    public GenericModel getGenericModel() {
        return mModel;
    }

    public String getLoggingString() {
        String loggingString = "";

        switch (mType) {
            case ALBUM:
                loggingString = ((AlbumModel) mModel).getAlbumName() + "-" + ((AlbumModel) mModel).getArtistName();
                break;
            case ARTIST:
                loggingString = ((ArtistModel) mModel).getArtistName();
                break;
        }

        return loggingString;
    }
}
