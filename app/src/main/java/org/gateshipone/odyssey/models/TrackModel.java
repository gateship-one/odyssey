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

import android.os.Parcel;
import android.os.Parcelable;

public class TrackModel implements GenericModel, Parcelable {

    /**
     * The name of the track
     */
    private final String mTrackName;

    /**
     * The name of the artist of the track
     */
    private final String mTrackArtistName;

    /**
     * The name of the album of the track
     */
    private final String mTrackAlbumName;

    /**
     * The unique key of the album of the track
     */
    private final String mTrackAlbumKey;

    /**
     * The url path to the related media file
     */
    private final String mTrackURL;

    /**
     * The duration of the track in ms
     */
    private final long mTrackDuration;

    /**
     * The number of the track (combined cd and tracknumber)
     */
    private final int mTrackNumber;

    /**
     * The unique id of the track in the mediastore
     */
    private final long mTrackId;

    /**
     * Constructs a TrackModel instance with the given parameters.
     */
    public TrackModel(String name, String artistName, String albumName, String albumKey, long duration, int trackNumber, String url, long trackId) {
        if (name != null) {
            mTrackName = name;
        } else {
            mTrackName = "";
        }

        if (artistName != null) {
            mTrackArtistName = artistName;
        } else {
            mTrackArtistName = "";
        }

        if (albumName != null) {
            mTrackAlbumName = albumName;
        } else {
            mTrackAlbumName = "";
        }

        if (albumKey != null) {
            mTrackAlbumKey = albumKey;
        } else {
            mTrackAlbumKey = "";
        }
        mTrackDuration = duration;
        mTrackNumber = trackNumber;
        if (url != null) {
            mTrackURL = url;
        } else {
            mTrackURL = "";
        }

        mTrackId = trackId;
    }

    /**
     * Constructs a TrackModel with default values
     */
    public TrackModel() {
        mTrackName = "";
        mTrackArtistName = "";
        mTrackAlbumName = "";
        mTrackAlbumKey = "";
        mTrackDuration = 0;
        mTrackNumber = 0;
        mTrackURL = "";
        mTrackId = -1;
    }

    /**
     * Return the name of the track
     */
    public String getTrackName() {
        return mTrackName;
    }

    /**
     * Return the name of the artist
     */
    public String getTrackArtistName() {
        return mTrackArtistName;
    }

    /**
     * Return the name of the album
     */
    public String getTrackAlbumName() {
        return mTrackAlbumName;
    }

    /**
     * Return the unique album key
     */
    public String getTrackAlbumKey() {
        return mTrackAlbumKey;
    }

    /**
     * Return the duration of the track
     */
    public long getTrackDuration() {
        return mTrackDuration;
    }

    /**
     * Return the number of the track
     */
    public int getTrackNumber() {
        return mTrackNumber;
    }

    /**
     * Return the url of the track
     */
    public String getTrackURL() {
        return mTrackURL;
    }

    /**
     * Return the unique id of the track
     */
    public long getTrackId() {
        return mTrackId;
    }

    /**
     * Return the section title for the TrackModel
     * <p/>
     * The section title is the name of the track.
     */
    @Override
    public String getSectionTitle() {
        return mTrackName;
    }

    /**
     * Equals method for the TrackModel
     * <p/>
     * TrackModel instances are equal if they have the same id
     */
    @Override
    public boolean equals(Object model) {
        if (model == null) {
            return false;
        }
        if (model == this) {
            return true;
        }
        if (!(model instanceof TrackModel)) {
            return false;
        }
        TrackModel track = (TrackModel) model;

        return (this.mTrackId == track.mTrackId);
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation.
     * <p/>
     * see {@link Parcelable}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     * <p/>
     * see {@link Parcelable}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTrackName);
        dest.writeString(mTrackArtistName);
        dest.writeString(mTrackAlbumName);
        dest.writeString(mTrackURL);
        dest.writeInt(mTrackNumber);
        dest.writeLong(mTrackDuration);
        dest.writeString(mTrackAlbumKey);
        dest.writeLong(mTrackId);
    }

    /**
     * Provide CREATOR field that generates a TrackModel instance from a Parcel.
     * <p/>
     * see {@link Parcelable}
     */
    public static Parcelable.Creator<TrackModel> CREATOR = new Creator<TrackModel>() {

        @Override
        public TrackModel[] newArray(int size) {
            return new TrackModel[size];
        }

        @Override
        public TrackModel createFromParcel(Parcel source) {
            String trackName = source.readString();
            String artistName = source.readString();
            String albumName = source.readString();
            String url = source.readString();
            int number = source.readInt();
            long duration = source.readLong();
            String albumKey = source.readString();
            long trackId = source.readLong();

            return new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, trackId);
        }
    };
}
