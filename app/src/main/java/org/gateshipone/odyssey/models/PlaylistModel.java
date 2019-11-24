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

import android.os.Parcel;
import android.os.Parcelable;

import org.gateshipone.odyssey.models.android.AndroidPlaylistModel;

public class PlaylistModel implements GenericModel, Parcelable {
    /**
     * The name of the playlist
     */
    private final String mPlaylistName;

    private final String mPlaylistPath;

    /**
     * Constructs a PlaylistModel instance with the given parameters.
     */
    public PlaylistModel(String playlistName) {
        if (playlistName != null) {
            mPlaylistName = playlistName;
        } else {
            mPlaylistName = "";
        }
        mPlaylistPath = "";
    }

    public PlaylistModel(String playlistName, String path) {
        if (playlistName != null) {
            mPlaylistName = playlistName;
        } else {
            mPlaylistName = "";
        }
        mPlaylistPath = path;
    }


    protected PlaylistModel(Parcel in) {
        // Class name
        in.readString();
        mPlaylistName = in.readString();
        mPlaylistPath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.getClass().getName());
        dest.writeString(mPlaylistName);
        dest.writeString(mPlaylistPath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlaylistModel> CREATOR = new Creator<PlaylistModel>() {
        @Override
        public PlaylistModel createFromParcel(Parcel in) {
            int pos_before = in.dataPosition();
            String className = in.readString();
            // Reset parcel
            in.setDataPosition(pos_before);
            if (className == null || className.isEmpty()) {
                return null;
            } else if (className.equals(AndroidPlaylistModel.class.getName())) {
                return AndroidPlaylistModel.CREATOR.createFromParcel(in);
            }
            return new PlaylistModel(in);
        }

        @Override
        public PlaylistModel[] newArray(int size) {
            return new PlaylistModel[size];
        }
    };

    /**
     * Return the name of the playlist
     */
    public String getPlaylistName() {
        return mPlaylistName;
    }


    public String getPlaylistPath() {
        return mPlaylistPath;
    }

    /**
     * Return the section title for the PlaylistModel
     * <p/>
     * The section title is the name of the playlist.
     */
    @Override
    public String getSectionTitle() {
        return mPlaylistName;
    }

}
