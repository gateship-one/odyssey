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

package org.odyssey.models;

public class PlaylistModel implements GenericModel {

    /**
     * The name of the playlist
     */
    private final String mPlaylistName;

    /**
     * Unique id to identify the playlist in the mediastore
     */
    private final long mPlaylistID;

    /**
     * Constructs a PlaylistModel instance with the given parameters.
     */
    public PlaylistModel(String playlistName, long playlistID) {
        mPlaylistName = playlistName;
        mPlaylistID = playlistID;
    }

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
     * Return the section title for the PlaylistModel
     * <p/>
     * The section title is the name of the playlist.
     */
    @Override
    public String getSectionTitle() {
        return mPlaylistName;
    }
}
