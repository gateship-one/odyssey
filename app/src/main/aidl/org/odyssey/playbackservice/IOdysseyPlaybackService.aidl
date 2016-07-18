// IOdysseyPlaybackService.aidl
package org.odyssey.playbackservice;

// Declare any non-default types here with import statements
import org.odyssey.models.TrackModel;

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

	// save current playlist in mediastore
	void savePlaylist(String name);

	// return the current index
	int getCurrentIndex();

	TrackModel getPlaylistSong(int index);
	int getPlaylistSize();

	/**
	 * position = playlist position of jump target
	 */
	void jumpTo(int position);

	void setRandom(int random);
	void setRepeat(int repeat);

	int getRandom();
	int getRepeat();
	int getPlaying();

	// track is the full uri with "file://" !
	void setNextTrack(String track);

	void enqueueTrackAsNext(in TrackModel track);

	void enqueueTrack(in TrackModel track);
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

	// Information getters
	String getArtist();
	String getAlbum();
	String getTrackname();
	int getTrackNo();
}