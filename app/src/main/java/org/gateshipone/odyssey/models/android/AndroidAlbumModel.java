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
import android.os.Parcelable;
import android.util.Log;

import org.gateshipone.odyssey.models.AlbumModel;

public class AndroidAlbumModel extends AlbumModel {
    private final static String TAG = AndroidAlbumModel.class.getSimpleName();
    /**
     * Unique key to identify the album in the mediastore
     */
    private final String mAlbumKey;

    private long mAlbumID;

    public AndroidAlbumModel(String name, String albumArtURL, String artistName, int dateAdded, String albumKey, long albumID) {
        super(name, albumArtURL, artistName, dateAdded);


        if (albumKey != null) {
            mAlbumKey = albumKey;
        } else {
            mAlbumKey = "";
        }

        mAlbumID = albumID;
    }

    public AndroidAlbumModel(String name, String albumArtURL, String artistName, String albumKey, long albumID) {
        this(name, albumArtURL, artistName, -1, albumKey, albumID);
    }

    protected AndroidAlbumModel(Parcel in) {
        super(in);

        mAlbumKey = in.readString();
        mAlbumID = in.readLong();
    }

    /**
     * Return the unique album key
     */
    public String getAlbumKey() {
        return mAlbumKey;
    }

    public long getAlbumID() {
        return mAlbumID;
    }

    public void setAlbumID(long albumID) {
        mAlbumID = albumID;
    }

    /**
     * Flatten this object in to a Parcel.
     * <p/>
     * see {@link Parcelable}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        Log.v(TAG, "Writing AndroidAlbumModel to parcel");
        dest.writeString(mAlbumKey);
        dest.writeLong(mAlbumID);
    }

    @Override
    public boolean equals(Object album) {
        if (null == album) {
            return false;
        }
        if (album instanceof AndroidAlbumModel) {
            return mAlbumID == ((AndroidAlbumModel) album).mAlbumID && mAlbumName.equals(((AndroidAlbumModel) album).mAlbumName)
                    && mArtistName.equals(((AndroidAlbumModel) album).mArtistName);
        } else {
            return false;
        }
    }

    /**
     * Provide CREATOR field that generates a AlbumModel instance from a Parcel.
     * <p/>
     * see {@link Parcelable}
     */
    public static final Creator<AndroidAlbumModel> CREATOR = new Creator<AndroidAlbumModel>() {
        @Override
        public AndroidAlbumModel createFromParcel(Parcel in) {
            return new AndroidAlbumModel(in);
        }

        @Override
        public AndroidAlbumModel[] newArray(int size) {
            return new AndroidAlbumModel[size];
        }
    };

    @Override
    public String getArtworkID() {
        return mAlbumID == -1 ? mAlbumName : String.valueOf(mAlbumID);
    }

}
