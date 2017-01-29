/*
 * Copyright (C) 2017 Team Gateship-One
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

public class ArtistModel implements GenericModel, Parcelable {

    /**
     * The name of the artist
     */
    private final String mArtistName;

    /**
     * Unique id to identify the artist in the mediastore
     */
    private final long mArtistID;

    private String mMBID;

    private boolean mImageFetching;

    /**
     * Constructs a ArtistModel instance with the given parameters.
     */
    public ArtistModel(String name, long artistID) {
        if (name != null) {
            mArtistName = name;
        } else {
            mArtistName = "";
        }

        mArtistID = artistID;
    }

    public ArtistModel(ArtistModel artist) {
        mArtistName  = artist.mArtistName;
        mArtistID = artist.mArtistID;

    }

    protected ArtistModel(Parcel in) {
        mArtistName = in.readString();
        mArtistID = in.readLong();
        mMBID = in.readString();
        mImageFetching = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mArtistName);
        dest.writeLong(mArtistID);
        dest.writeString(mMBID);
        dest.writeByte((byte) (mImageFetching ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ArtistModel> CREATOR = new Creator<ArtistModel>() {
        @Override
        public ArtistModel createFromParcel(Parcel in) {
            return new ArtistModel(in);
        }

        @Override
        public ArtistModel[] newArray(int size) {
            return new ArtistModel[size];
        }
    };

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

    public void setMBID(String mbid) {
        mMBID = mbid;
    }

    public String getMBID() {
        return mMBID;
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

    public synchronized void setFetching(boolean fetching) {
        mImageFetching = fetching;
    }

    public synchronized boolean getFetching() {
        return mImageFetching;
    }

    @Override
    public boolean equals(Object artist) {
        if ( null == artist) {
            return false;
        }
        if ( artist instanceof ArtistModel) {
            return mArtistID == ((ArtistModel) artist).mArtistID && mArtistName.equals(((ArtistModel) artist).mArtistName);
        } else {
            return false;
        }
    }
}
