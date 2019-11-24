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

import org.gateshipone.odyssey.models.PlaylistModel;

public class AndroidPlaylistModel extends PlaylistModel {
    /**
     * Unique id to identify the playlist in the mediastore
     */
    private final long mPlaylistID;

    public AndroidPlaylistModel(String playlistName, long playlistID) {
        super(playlistName);
        mPlaylistID = playlistID;
    }

    public AndroidPlaylistModel(String playlistName, long playlistID, String path) {
        super(playlistName, path);
        mPlaylistID = playlistID;
    }

    protected AndroidPlaylistModel(Parcel in) {
        super(in);

        mPlaylistID = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mPlaylistID);
    }


    /**
     * Return the id of the playlist
     */
    public long getPlaylistID() {
        return mPlaylistID;
    }

    public static final Creator<AndroidPlaylistModel> CREATOR = new Creator<AndroidPlaylistModel>() {
        @Override
        public AndroidPlaylistModel createFromParcel(Parcel in) {
            return new AndroidPlaylistModel(in);
        }

        @Override
        public AndroidPlaylistModel[] newArray(int size) {
            return new AndroidPlaylistModel[size];
        }
    };
}
