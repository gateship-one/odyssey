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

package org.gateshipone.odyssey.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;


import androidx.annotation.NonNull;

import org.gateshipone.odyssey.models.android.AndroidTrackModel;

public class TrackModel implements GenericModel, Parcelable {
    private static final String TAG = TrackModel.class.getSimpleName();
    /**
     * The name of the track
     */
    protected String mTrackName = "";

    /**
     * The name of the artist of the track
     */
    protected String mTrackArtistName = "";

    /**
     * The name of the album of the track
     */
    protected String mTrackAlbumName = "";

    /**
     * The url path to the related media file
     */
    protected String mTrackURL = "";

    /**
     * The duration of the track in ms
     */
    protected long mTrackDuration;

    /**
     * The number of the track (combined cd and tracknumber)
     */
    protected int mTrackNumber;

    /**
     * The date as an integer when this track was added to the device
     */
    protected int mDateAdded;

    protected boolean mNoMetaData;

    public void setTrackName(String mTrackName) {
        this.mTrackName = mTrackName;
    }

    public void setTrackArtistName(String mTrackArtistName) {
        this.mTrackArtistName = mTrackArtistName;
    }

    public void setTrackAlbumName(String mTrackAlbumName) {
        this.mTrackAlbumName = mTrackAlbumName;
    }

    public void setTrackURL(String mTrackURL) {
        this.mTrackURL = mTrackURL;
    }

    public void setTrackNumber(int mTrackNumber) {
        this.mTrackNumber = mTrackNumber;
    }

    public void setDateAdded(int mDateAdded) {
        this.mDateAdded = mDateAdded;
    }

    public void setNoMetaData(boolean mNoMetaData) {
        this.mNoMetaData = mNoMetaData;
    }

    public TrackModel() {

    }


    public TrackModel(@NonNull String name, @NonNull String path) {
        mNoMetaData = true;
        mTrackURL = path;
        mTrackName = name;
    }

    /**
     * Constructs a TrackModel from a Parcel.
     * <p>
     * see {@link Parcelable}
     */
    protected TrackModel(Parcel in) {
        // Class name
        in.readString();
        mTrackName = in.readString();
        mTrackArtistName = in.readString();
        mTrackAlbumName = in.readString();
        mTrackURL = in.readString();
        mTrackDuration = in.readLong();
        mTrackNumber = in.readInt();
        mDateAdded = in.readInt();
        mNoMetaData = in.readInt() == 1;
    }

    /**
     * Provide CREATOR field that generates a TrackModel instance from a Parcel.
     * <p/>
     * see {@link Parcelable}
     */
    public static final Creator<TrackModel> CREATOR = new Creator<TrackModel>() {
        @Override
        public TrackModel createFromParcel(Parcel in) {
            int pos_before = in.dataPosition();
            String className = in.readString();
            // Reset parcel
            in.setDataPosition(pos_before);
            if (className == null || className.isEmpty()) {
                return null;
            } else if (className.equals(AndroidTrackModel.class.getName())) {
                return AndroidTrackModel.CREATOR.createFromParcel(in);
            }
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
            return mTrackURL.substring(mTrackURL.lastIndexOf('/') + 1);
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
        dest.writeString(this.getClass().getName());
        dest.writeString(mTrackName);
        dest.writeString(mTrackArtistName);
        dest.writeString(mTrackAlbumName);
        dest.writeString(mTrackURL);
        dest.writeLong(mTrackDuration);
        dest.writeInt(mTrackNumber);
        dest.writeInt(mDateAdded);
        dest.writeInt(mNoMetaData ? 1 : 0);
    }


    public boolean hasNoMetaData() {
        return mNoMetaData;
    }

    public void metaDataSet(boolean set) {
        mNoMetaData = !set;
    }

    @NonNull
    @Override
    public String toString() {
        return "Track: " + getTrackNumber() + ':' + getTrackName() + '-' + getTrackAlbumName();
    }

    public boolean sameAlbum(AlbumModel albumModel) {
        return mTrackAlbumName.equals(albumModel.mAlbumName);
    }

    public boolean sameAlbum(TrackModel trackModel) {
        return mTrackAlbumName.equals(trackModel.mTrackAlbumName);
    }

    public boolean hasAlbum() {
        return !mTrackAlbumName.isEmpty();
    }

    public void fillMetadata(Context context) {
        mNoMetaData = false;
    }
}
