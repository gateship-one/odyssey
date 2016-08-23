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
	void play(in TrackModel track);
	void pause();
	void resume();
	void stop();
	void next();
	void previous();
	void togglePause();
	void shufflePlaylist();
	void playAllTracks();
	void playAllTracksShuffled();

	/**
	 * position = position in current track ( in seconds)
	 */
	void seekTo(int position);
	// Returns time of current playing title
	int getTrackPosition();
	int getTrackDuration();

	// If currently playing return this song otherwise null
	TrackModel getCurrentSong();

	// return the current nowplayinginformation or null if state is stopped
	NowPlayingInformation getNowPlayingInformation();

	// save current playlist in mediastore
	void savePlaylist(String name);

    // enqueue a playlist from mediastore
    void enqueuePlaylist(long playlistId);

    // enqueue all tracks of an album from mediastore
    void enqueueAlbum(String albumKey);

    // enqueue all tracks of an artist from mediastore
    void enqueueArtist(long artistId);

	// return the current index
	int getCurrentIndex();

	TrackModel getPlaylistSong(int index);
	int getPlaylistSize();

	/**
	 * position = playlist position of jump target
	 */
	void jumpTo(int position);

	void toggleRandom();
	void toggleRepeat();

	// track is the full uri with "file://" !
	void setNextTrack(String track);

	void enqueueTrack(in TrackModel track, boolean asNext);
	void enqueueTracks(in List<TrackModel> tracks);
	void dequeueTrack(in TrackModel track);
	void dequeueTracks(in List<TrackModel> tracks);
	void dequeueTrackIndex(int index);
	void clearPlaylist();

	void getCurrentList(out List<TrackModel> tracks);

	// resume stack methods
	void resumeBookmark(long timestamp);
	void deleteBookmark(long timestamp);
	void createBookmark(String bookmarkTitle);

	// file explorer methods
	void enqueueFile(String filePath, boolean asNext);
	void enqueueDirectory(String directoryPath);

	// Information getters
	String getArtist();
	String getAlbum();
	String getTrackname();
	int getTrackNo();
}