/*
 * Copyright (C) 2018 Team Gateship-One
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
import android.os.RemoteException;

import org.gateshipone.odyssey.models.TrackModel;

import java.lang.ref.WeakReference;

public class OdysseyPlaybackServiceInterface extends IOdysseyPlaybackService.Stub {
    // Holds the actual playback service for handling reasons
    private final WeakReference<PlaybackService> mService;

    public OdysseyPlaybackServiceInterface(PlaybackService service) {
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
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAY, uri);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueTrack(TrackModel track, boolean asNext) {
        // Create enqueuetrack control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACK, track, asNext);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    public void playTrack(TrackModel track, boolean clearPlaylist) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYTRACK, track, clearPlaylist);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void toggleRandom() {
        // Create random control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void toggleRepeat() {
        // Create repeat control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_REPEAT);
        Message msg = mService.get().getHandler().obtainMessage();
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
    public void startSleepTimer(long durationMS) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_START_SLEEPTIMER, durationMS);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
}

    @Override
    public void cancelSleepTimer() {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_CANCEL_SLEEPTIMER);
        Message msg = mService.get().getHandler().obtainMessage();
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
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SEEKTO, position);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void jumpTo(int position) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_JUMPTO, position);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void clearPlaylist() {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_CLEARPLAYLIST);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void next() {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_NEXT);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void previous() {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PREVIOUS);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void togglePause() {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_TOGGLEPAUSE);
        Message msg = mService.get().getHandler().obtainMessage();
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
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACK, index);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void dequeueTracks(int index) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACKS, index);
        Message msg = mService.get().getHandler().obtainMessage();
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
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SHUFFLEPLAYLIST);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playAllTracks(String filterString) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALLTRACKS, filterString);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getCurrentIndex() {
        return mService.get().getCurrentIndex();
    }

    @Override
    public void savePlaylist(String name) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SAVEPLAYLIST, name);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueuePlaylist(long playlistId) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEPLAYLIST, playlistId);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playPlaylist(long playlistId, int position) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYPLAYLIST, playlistId, position);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueuePlaylistFile(String path) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEPLAYLISTFILE, path);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playPlaylistFile(String path, int position) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYPLAYLISTFILE, path, position);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueAlbum(String albumKey) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEALBUM, albumKey);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playAlbum(String albumKey, int position) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALBUM, albumKey, position);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueRecentAlbums() {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUERECENTALBUMS);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playRecentAlbums() {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYRECENTALBUMS);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueArtist(long artistId, String orderKey) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEARTIST, artistId, orderKey);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playArtist(long artistId, String orderKey) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYARTIST, artistId, orderKey);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void resumeBookmark(long timestamp) {
        // create resume bookmark control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RESUMEBOOKMARK, timestamp);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void deleteBookmark(long timestamp) {
        // create delete bookmark control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DELETEBOOKMARK, timestamp);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void createBookmark(String bookmarkTitle) {
        // create create bookmark control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_CREATEBOOKMARK, bookmarkTitle);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueFile(String filePath, boolean asNext) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEFILE, filePath, asNext);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playFile(String filePath, boolean clearPlaylist) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYFILE, filePath, clearPlaylist);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playDirectory(String directoryPath, int position) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYDIRECTORY, directoryPath, position);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueDirectoryAndSubDirectories(String directoryPath, String filterString) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEDIRECTORYANDSUBDIRECTORIES, directoryPath, filterString);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playDirectoryAndSubDirectories(String directoryPath, String filterString) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYDIRECTORYANDSUBDIRECTORIES, directoryPath, filterString);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }
}
