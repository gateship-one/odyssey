package org.odyssey.models;

public class PlaylistModel implements GenericModel {
    private final String mPlaylistName;
    private final long mPlaylistID;

    public PlaylistModel(String playlistName, long playlistID) {
        mPlaylistName = playlistName;
        mPlaylistID = playlistID;
    }

    public String getPlaylistName() {
        return mPlaylistName;
    }

    public long getPlaylistID() {
        return mPlaylistID;
    }

    @Override
    public String getSectionTitle() {
        return mPlaylistName;
    }
}
