package org.odyssey.playbackservice;

import android.os.Message;
import android.os.RemoteException;

import org.odyssey.models.TrackModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
    public void play(TrackModel track) throws RemoteException {
        // Create play control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAY, track);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void pause() throws RemoteException {
        // Create pause control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PAUSE);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void stop() throws RemoteException {
        // Create stop control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_STOP);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void setNextTrack(String uri) throws RemoteException {
        // Create nexttrack control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SETNEXTRACK, uri);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueTracks(List<TrackModel> tracks) throws RemoteException {
        // Create enqueuetracks control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACKS, (ArrayList<TrackModel>) tracks);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueTrack(TrackModel track, boolean asNext) throws RemoteException {
        // Create enqueuetrack control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACK, track, asNext);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void dequeueTrack(TrackModel track) throws RemoteException {
        // Create dequeuetrack control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACK, track);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void dequeueTracks(List<TrackModel> tracks) throws RemoteException {
        // Create dequeuetracks control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACKS, (ArrayList<TrackModel>) tracks);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void getCurrentList(List<TrackModel> list) throws RemoteException {
        for (TrackModel track : mService.get().getCurrentList()) {
            list.add(track);
        }
    }

    @Override
    public void setRandom(int random) throws RemoteException {
        // Create random control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM, random);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void setRepeat(int repeat) throws RemoteException {
        // Create repeat control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_REPEAT, repeat);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public String getArtist() throws RemoteException {
        TrackModel track = mService.get().getCurrentTrack();
        if (track != null) {
            return track.getTrackArtistName();
        }
        return "";
    }

    @Override
    public String getAlbum() throws RemoteException {
        TrackModel track = mService.get().getCurrentTrack();
        if (track != null) {
            return track.getTrackAlbumName();
        }
        return "";
    }

    @Override
    public String getTrackname() throws RemoteException {
        TrackModel track = mService.get().getCurrentTrack();
        if (track != null) {
            return track.getTrackName();
        }
        return "";
    }

    @Override
    public int getTrackNo() throws RemoteException {
        TrackModel track = mService.get().getCurrentTrack();
        if (track != null) {
            return track.getTrackNumber();
        }
        return 0;
    }

    @Override
    public void seekTo(int position) throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SEEKTO, position);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void jumpTo(int position) throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_JUMPTO, position);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void clearPlaylist() throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_CLEARPLAYLIST);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void resume() throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RESUME);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void next() throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_NEXT);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void previous() throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PREVIOUS);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void togglePause() throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_TOGGLEPAUSE);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getTrackPosition() throws RemoteException {
        return mService.get().getTrackPosition();
    }

    @Override
    public int getTrackDuration() throws RemoteException {
        return mService.get().getTrackDuration();
    }

    @Override
    public int getRandom() throws RemoteException {
        return mService.get().getRandom();
    }

    @Override
    public int getRepeat() throws RemoteException {
        return mService.get().getRepeat();
    }

    @Override
    public TrackModel getCurrentSong() throws RemoteException {
        return mService.get().getCurrentTrack();
    }

    @Override
    public void dequeueTrackIndex(int index) throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUEINDEX, index);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public TrackModel getPlaylistSong(int index) throws RemoteException {
        return mService.get().getPlaylistTrack(index);
    }

    @Override
    public int getPlaylistSize() throws RemoteException {
        return mService.get().getPlaylistSize();
    }

    @Override
    public void shufflePlaylist() throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SHUFFLEPLAYLIST);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playAllTracksShuffled() throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALLTRACKSSHUFFLED);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playAllTracks() throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALLTRACKS);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getCurrentIndex() throws RemoteException {
        return mService.get().getCurrentIndex();
    }

    @Override
    public int getPlaying() throws RemoteException {
        return mService.get().isPlaying() ? 1 : 0;
    }

    @Override
    public void savePlaylist(String name) throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SAVEPLAYLIST, name);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueuePlaylist(long playlistId) throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEPLAYLIST, playlistId);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueAlbum(String albumKey) throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEALBUM, albumKey);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueArtist(long artistId) throws RemoteException {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEARTIST, artistId);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void resumeBookmark(long timestamp) throws RemoteException {
        // create resume bookmark control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RESUMEBOOKMARK, timestamp);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void deleteBookmark(long timestamp) throws RemoteException {
        // create delete bookmark control object
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DELETEBOOKMARK, timestamp);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void createBookmark(String bookmarkTitle) throws RemoteException {
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
    public void enqueueDirectory(String directoryPath) {
        ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEDIRECTORY, directoryPath);
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }
}
