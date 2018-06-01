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

// IOdysseyPlaybackService.aidl
package org.gateshipone.odyssey.playbackservice;

// Declare any non-default types here with import statements
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.NowPlayingInformation;

interface IOdysseyPlaybackService {

    // Controls the player with predefined actions
    void playURI(String uri);
    void next();
    void previous();
    void togglePause();
    void shufflePlaylist();
    void playAllTracks(String filterString);

    /**
     * position = position in current track ( in seconds)
     */
    void seekTo(int position);

    // save current playlist in mediastore
    void savePlaylist(String name);

    // enqueue a playlist from mediastore
    void enqueuePlaylist(long playlistId);
    void playPlaylist(long playlistId, int position);

    void enqueuePlaylistFile(String path);
    void playPlaylistFile(String path, int position);

    // enqueue all tracks of an album from mediastore
    void enqueueAlbum(String albumKey);
    void playAlbum(String albumKey, int position);

    void enqueueRecentAlbums();
    void playRecentAlbums();

    // enqueue all tracks of an artist from mediastore
    void enqueueArtist(long artistId, String orderKey);
    void playArtist(long artistId, String orderKey);

    /**
     * position = playlist position of jump target
     */
    void jumpTo(int position);

    void toggleRandom();
    void toggleRepeat();

    void enqueueTrack(in TrackModel track, boolean asNext);
    void playTrack(in TrackModel track, boolean clearPlaylist);

    void dequeueTrack(int index);
    void dequeueTracks(int index);
    void clearPlaylist();

    // resume stack methods
    void resumeBookmark(long timestamp);
    void deleteBookmark(long timestamp);
    void createBookmark(String bookmarkTitle);

    // file explorer methods
    void enqueueFile(String filePath, boolean asNext);
    void playFile(String filePath, boolean clearPlaylist);

    void playDirectory(String directoryPath, int position);
    void enqueueDirectoryAndSubDirectories(String directoryPath, String filterString);
    void playDirectoryAndSubDirectories(String directoryPath, String filterString);

    // Information getters

    int getAudioSessionID();
    int getPlaylistSize();
    // return the current index
    int getCurrentIndex();
    // Returns time of current playing title
    int getTrackPosition();
    // return the current nowplayinginformation or null if state is stopped
    NowPlayingInformation getNowPlayingInformation();
    TrackModel getPlaylistSong(int index);
    // If currently playing return this song otherwise null
    TrackModel getCurrentSong();
    // return the working state of the pbs
    boolean isBusy();

    void hideArtworkChanged(boolean enabled);

    void hideMediaOnLockscreenChanged(boolean enabled);

    void startSleepTimer(long durationMS);
}
