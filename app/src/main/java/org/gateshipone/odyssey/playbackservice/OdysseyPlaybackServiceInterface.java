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

package org.gateshipone.odyssey.playbackservice;

import android.os.Message;

import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.lang.ref.WeakReference;

public class OdysseyPlaybackServiceInterface extends IOdysseyPlaybackService.Stub {
    // Holds the actual playback service for handling reasons
    private final WeakReference<PlaybackService> mService;

    OdysseyPlaybackServiceInterface(PlaybackService service) {
        mService = new WeakReference<>(service);
    }

    /**
     * Following are methods which call the handler thread (which runs at
     * audio priority) so that handling of playback is done in a seperate
     * thread for performance reasons.
     */
    @Override
    public void playURI(String uri) {
        // Create play control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY)
                .addString(uri)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueTrack(TrackModel track, boolean asNext) {
        // Create enqueuetrack control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ENQUEUE_TRACK)
                .addTrack(track)
                .addBool(asNext)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    public void playTrack(TrackModel track, boolean clearPlaylist) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_TRACK)
                .addTrack(track)
                .addBool(clearPlaylist)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void toggleRandom() {
        // Create random control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.RANDOM)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void toggleRepeat() {
        // Create repeat control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.REPEAT)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getAudioSessionID() {
        return mService.get().getAudioSessionID();
    }

    @Override
    public void hideArtworkChanged(boolean enabled) {
        mService.get().hideArtwork(enabled);
    }

    @Override
    public void hideMediaOnLockscreenChanged(boolean enabled) {
        mService.get().hideMediaOnLockscreen(enabled);
    }

    @Override
    public void changeAutoBackwardsSeekAmount(int amount) {
        mService.get().setAutoBackwardsSeekAmount(amount);
    }

    @Override
    public void setSmartRandom(int intelligenceFactor) {
        // Create repeat control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.SET_SMART_RANDOM)
                .addInt(intelligenceFactor)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void startSleepTimer(long durationMS, boolean stopAfterCurrent) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.START_SLEEP_TIMER)
                .addLong(durationMS)
                .addBool(stopAfterCurrent)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void cancelSleepTimer() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.CANCEL_SLEEP_TIMER)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public boolean hasActiveSleepTimer() {
        return mService.get().hasActiveSleepTimer();
    }

    @Override
    public boolean isBusy() {
        return mService.get().isBusy();
    }

    @Override
    public void seekTo(int position) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.SEEK_TO)
                .addInt(position)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void jumpTo(int position) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.JUMP_TO)
                .addInt(position)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void clearPlaylist() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.CLEAR_PLAYLIST)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void next() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.NEXT)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void previous() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PREVIOUS)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void togglePause() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.TOGGLE_PAUSE)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getTrackPosition() {
        return mService.get().getTrackPosition();
    }

    @Override
    public TrackModel getCurrentSong() {
        return mService.get().getCurrentTrack();
    }

    public NowPlayingInformation getNowPlayingInformation() {
        return mService.get().getNowPlayingInformation();
    }

    @Override
    public void dequeueTrack(int index) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.DEQUEUE_TRACK)
                .addInt(index)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void dequeueTracks(int index) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.DEQUEUE_TRACKS)
                .addInt(index)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public TrackModel getPlaylistSong(int index) {
        return mService.get().getPlaylistTrack(index);
    }

    @Override
    public int getPlaylistSize() {
        return mService.get().getPlaylistSize();
    }

    @Override
    public void shufflePlaylist() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.SHUFFLE_PLAYLIST)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playAllTracks(String filterString) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_ALL_TRACKS)
                .addString(filterString)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getCurrentIndex() {
        return mService.get().getCurrentIndex();
    }

    @Override
    public void savePlaylist(String name) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.SAVE_PLAYLIST)
                .addString(name)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueuePlaylist(PlaylistModel playlist) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ENQUEUE_PLAYLIST)
                .addPlaylist(playlist)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playPlaylist(PlaylistModel playlist, int position) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_PLAYLIST)
                .addPlaylist(playlist)
                .addInt(position)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueAlbum(long albumId, String orderKey) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ENQUEUE_ALBUM)
                .addLong(albumId)
                .addString(orderKey)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playAlbum(long albumId, String orderKey, int position) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_ALBUM)
                .addLong(albumId)
                .addString(orderKey)
                .addInt(position)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueRecentAlbums() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ENQUEUE_RECENT_ALBUMS)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playRecentAlbums() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_RECENT_ALBUMS)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueArtist(long artistId, String albumOrderKey, String trackOrderKey) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ENQUEUE_ARTIST)
                .addLong(artistId)
                .addString(albumOrderKey)
                .addString(trackOrderKey)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playArtist(long artistId, String albumOrderKey, String trackOrderKey) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_ARTIST)
                .addLong(artistId)
                .addString(albumOrderKey)
                .addString(trackOrderKey)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void resumeBookmark(long timestamp) {
        // create resume bookmark control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.RESUME_BOOKMARK)
                .addLong(timestamp)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void deleteBookmark(long timestamp) {
        // create delete bookmark control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.DELETE_BOOKMARK)
                .addLong(timestamp)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void createBookmark(String bookmarkTitle) {
        // create create bookmark control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.CREATE_BOOKMARK)
                .addString(bookmarkTitle)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueFile(String filePath, boolean asNext) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ENQUEUE_FILE)
                .addString(filePath)
                .addBool(asNext)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playFile(String filePath, boolean clearPlaylist) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_FILE)
                .addString(filePath)
                .addBool(clearPlaylist)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playDirectory(String directoryPath, int position) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_DIRECTORY)
                .addString(directoryPath)
                .addInt(position)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueDirectoryAndSubDirectories(String directoryPath, String filterString) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ENQUEUE_DIRECTORY_AND_SUBDIRECTORIES)
                .addString(directoryPath)
                .addString(filterString)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playDirectoryAndSubDirectories(String directoryPath, String filterString) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.PLAY_DIRECTORY_AND_SUBDIRECTORIES)
                .addString(directoryPath)
                .addString(filterString)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }
}
