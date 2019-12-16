/*
 * Copyright (C) 2019 Team Gateship-One
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

package org.gateshipone.odyssey.models.android;

import android.os.Parcel;

import org.gateshipone.odyssey.models.ArtistModel;


public class AndroidArtistModel extends ArtistModel {

    /**
     * Unique id to identify the artist in the mediastore
     */
    private long mArtistID;

    public AndroidArtistModel(String name, long artistID) {
        super(name);

        mArtistID = artistID;
    }

    public AndroidArtistModel(Parcel in) {
        super(in);

        mArtistID = in.readLong();
    }

    /**
     * Return the unique artist id
     */
    public long getArtistID() {
        return mArtistID;
    }

    public void setArtistID(long artistID) {
        mArtistID = artistID;
    }

    @Override
    public String getArtworkID() {
        return mArtistID == -1 ? mArtistName : String.valueOf(mArtistID);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeLong(mArtistID);
    }

    @Override
    public boolean equals(Object artist) {
        if ( null == artist) {
            return false;
        }
        if ( artist instanceof AndroidArtistModel) {
            return mArtistID == ((AndroidArtistModel) artist).mArtistID && mArtistName.equals(((AndroidArtistModel) artist).mArtistName);
        } else {
            return false;
        }
    }
    public static final Creator<AndroidArtistModel> CREATOR = new Creator<AndroidArtistModel>() {
        @Override
        public AndroidArtistModel createFromParcel(Parcel in) {
            return new AndroidArtistModel(in);
        }

        @Override
        public AndroidArtistModel[] newArray(int size) {
            return new AndroidArtistModel[size];
        }
    };
}
