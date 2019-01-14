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

package org.gateshipone.odyssey.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

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
    private long mTrackDuration;

    /**
     * The number of the track (combined cd and tracknumber)
     */
    private final int mTrackNumber;

    /**
     * The unique id of the track in the mediastore
     */
    private final long mTrackId;

    /**
     * The date as an integer when this track was added to the device
     */
    private final int mDateAdded;

    public TrackModel(String name, String artistName, String albumName, String albumKey, long duration, int trackNumber, String url, long trackId, int dateAdded) {
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

        mDateAdded = dateAdded;
    }

    /**
     * Constructs a TrackModel instance with the given parameters.
     */
    public TrackModel(String name, String artistName, String albumName, String albumKey, long duration, int trackNumber, String url, long trackId) {
        this(name, artistName, albumName, albumKey, duration, trackNumber, url, trackId, -1);
    }

    /**
     * Constructs a TrackModel with default values
     */
    public TrackModel() {
        this(null, null, null, null, 0, 0, null, -1, -1);
    }

    /**
     * Constructs a TrackModel from a Parcel.
     * <p>
     * see {@link Parcelable}
     */
    protected TrackModel(Parcel in) {
        mTrackName = in.readString();
        mTrackArtistName = in.readString();
        mTrackAlbumName = in.readString();
        mTrackAlbumKey = in.readString();
        mTrackURL = in.readString();
        mTrackDuration = in.readLong();
        mTrackNumber = in.readInt();
        mTrackId = in.readLong();
        mDateAdded = in.readInt();
    }

    /**
     * Provide CREATOR field that generates a TrackModel instance from a Parcel.
     * <p/>
     * see {@link Parcelable}
     */
    public static final Creator<TrackModel> CREATOR = new Creator<TrackModel>() {
        @Override
        public TrackModel createFromParcel(Parcel in) {
            return new TrackModel(in);
        }

        @Override
        public TrackModel[] newArray(int size) {
            return new TrackModel[size];
        }
    };

    /**
     * Return the name of the track
     */
    public String getTrackName() {
        return mTrackName;
    }

    /**
     * Return the name of the track, or the file basename if empty
     */
    public String getTrackDisplayedName() {
        if (mTrackName.isEmpty()) {
            return mTrackURL.substring(mTrackURL.lastIndexOf('/') + 1, mTrackURL.length());
        }

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
     * Set the duration of the track
     *
     * @param trackDuration the new duration in ms
     */
    public void setTrackDuration(long trackDuration) {
        mTrackDuration = trackDuration;
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

    public int getDateAdded() {
        return mDateAdded;
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
        dest.writeString(mTrackAlbumKey);
        dest.writeString(mTrackURL);
        dest.writeLong(mTrackDuration);
        dest.writeInt(mTrackNumber);
        dest.writeLong(mTrackId);
        dest.writeInt(mDateAdded);
    }

    @NonNull
    @Override
    public String toString() {
        return "Track: " + getTrackNumber() + ':' + getTrackName() + '-' + getTrackAlbumName();
    }
}
