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

public class PlaylistModel implements GenericModel, Parcelable {

    /**
     * Public enum to identify the type of the playlist.
     */
    public enum PLAYLIST_TYPES {
        // placeholder for dummy playlist objects
        UNKNOWN,
        // represents a playlist that should be created
        CREATE_NEW,
        // playlist is stored in the android mediastore
        MEDIASTORE,
        // playlist is stored in the odyssey database
        ODYSSEY_LOCAL,
        // playlist is a playlist file like m3u
        FILE
    }

    /**
     * The name of the playlist.
     */
    private final String mPlaylistName;

    /**
     * Unique id to identify the playlist in the mediastore or the odyssey db.
     */
    private final long mPlaylistID;

    /**
     * The number of tracks of the playlist.
     */
    private final int mPlaylistTracks;

    /**
     * File path to playlist.
     * This property should only be set if the mPlaylistType is FILE.
     */
    private final String mPlaylistPath;

    /**
     * Identifier for the playlist.
     */
    private final PLAYLIST_TYPES mPlaylistType;

    private PlaylistModel(String playlistName, long playlistId, int playlistTracks, String playlistPath, PLAYLIST_TYPES playlistType) {
        if (playlistName != null) {
            mPlaylistName = playlistName;
        } else {
            mPlaylistName = "";
        }

        if (playlistPath != null) {
            mPlaylistPath = playlistPath;
        } else {
            mPlaylistPath = "";
        }

        mPlaylistID = playlistId;
        mPlaylistTracks = playlistTracks;
        mPlaylistType = playlistType;
    }

    /**
     * Constructs a PlaylistModel of type FILE.
     */
    public PlaylistModel(String playlistName, String playlistPath) {
        this(playlistName, -1, -1, playlistPath, PLAYLIST_TYPES.FILE);
    }

    /**
     * Constructs a PlaylistModel instance with the given parameters.
     */
    public PlaylistModel(String playlistName, long playlistID, PLAYLIST_TYPES playlistType) {
        this(playlistName, playlistID, -1, null, playlistType);
    }

    /**
     * Constructs a PlaylistModel instance with the given parameters.
     */
    public PlaylistModel(String playlistName, long playlistID, int mPlaylistTracks, PLAYLIST_TYPES playlistType) {
        this(playlistName, playlistID, mPlaylistTracks, null, playlistType);
    }

    /**
     * Constructs a PlaylistModel from a Parcel.
     * <p>
     * see {@link Parcelable}
     */
    protected PlaylistModel(Parcel in) {
        mPlaylistID = in.readLong();
        mPlaylistTracks = in.readInt();
        mPlaylistType = PLAYLIST_TYPES.values()[in.readInt()];
        mPlaylistName = in.readString();
        mPlaylistPath = in.readString();
    }

    /**
     * Provide CREATOR field that generates a TrackModel instance from a Parcel.
     * <p/>
     * see {@link Parcelable}
     */
    public static final Creator<PlaylistModel> CREATOR = new Creator<PlaylistModel>() {
        @Override
        public PlaylistModel createFromParcel(Parcel in) {
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

    /**
     * Return the id of the playlist
     */
    public long getPlaylistID() {
        return mPlaylistID;
    }

    /**
     * Return the type of the playlist
     */
    public PLAYLIST_TYPES getPlaylistType() {
        return mPlaylistType;
    }

    /**
     * Return the number of tracks of the playlist
     */
    public int getPlaylistTracks() {
        return mPlaylistTracks;
    }

    /**
     * Return the path to the playlist file.
     * Only valid if mPlaylistType is FILE.
     */
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
        dest.writeLong(mPlaylistID);
        dest.writeInt(mPlaylistTracks);
        dest.writeInt(mPlaylistType.ordinal());
        dest.writeString(mPlaylistName);
        dest.writeString(mPlaylistPath);
    }

}
