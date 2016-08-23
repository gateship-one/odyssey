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

package org.gateshipone.odyssey.playbackservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.managers.PlaybackServiceStatusHelper;
import org.gateshipone.odyssey.playbackservice.statemanager.OdysseyDatabaseManager;
import org.gateshipone.odyssey.utils.FileExplorerHelper;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

public class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {

    /**
     * enums for random, repeat state
     */
    public enum RANDOMSTATE {
        // If random mode is off
        RANDOM_OFF,
        // If random mode is on
        RANDOM_ON
    }

    public enum REPEATSTATE {
        // If repeat mode is off
        REPEAT_OFF,
        // If the playlist should be repeated
        REPEAT_ALL,
        // If the current track should be repeated
        REPEAT_TRACK
    }

    public enum PLAYSTATE {
        // If a song is actual playing
        PLAYING,
        // If a song was playing before but is now paused (but the GaplessPlayer is prepared and ready to resume).
        PAUSE,
        // If the PBS was loaded and is ready to resume playing. This is a state
        // where the user never actually played a song in this session (GaplessPlayer is not yet prepared).
        RESUMED,
        // If no track is in the playlist the state is stopped. (Does not mean the PBS is not running)
        STOPPED
    }

    public enum PLAYBACKSERVICESTATE {
        // If the serivce is performing an operation
        WORKING,
        // If the service is finished with the operation
        IDLE
    }

    public static final String TAG = "OdysseyPlaybackService";

    public static final String ACTION_PLAY = "org.gateshipone.odyssey.play";
    public static final String ACTION_PAUSE = "org.gateshipone.odyssey.pause";
    public static final String ACTION_NEXT = "org.gateshipone.odyssey.next";
    public static final String ACTION_PREVIOUS = "org.gateshipone.odyssey.previous";
    public static final String ACTION_SEEKTO = "org.gateshipone.odyssey.seekto";
    public static final String ACTION_STOP = "org.gateshipone.odyssey.stop";
    public static final String ACTION_QUIT = "org.gateshipone.odyssey.quit";
    public static final String ACTION_TOGGLEPAUSE = "org.gateshipone.odyssey.togglepause";


    private static final int TIMEOUT_INTENT_QUIT = 5;

    private final static int SERVICE_CANCEL_TIME = 60 * 5 * 1000;

    private HandlerThread mHandlerThread;
    private PlaybackServiceHandler mHandler;

    private boolean mLostAudioFocus = false;

    /**
     * Mediaplayback stuff
     */
    private GaplessPlayer mPlayer;
    private List<TrackModel> mCurrentList;
    private int mCurrentPlayingIndex;
    private int mNextPlayingIndex;
    private int mLastPlayingIndex;
    private boolean mIsDucked = false;
    private boolean mIsPaused = false;
    private int mLastPosition = 0;
    private Random mRandomGenerator;

    private RANDOMSTATE mRandom = RANDOMSTATE.RANDOM_OFF;
    private REPEATSTATE mRepeat = REPEATSTATE.REPEAT_OFF;

    /**
     * MediaControls manager
     */
    private PlaybackServiceStatusHelper mPlaybackServiceStatusHelper;

    /**
     * Temporary wakelock for transition to next song.
     * Without it, some android devices go to sleep and don't start
     * the next song.
     */
    private WakeLock mTempWakelock = null;

    /**
     * Databasemanager for saving and restoring states including their playlist
     */
    private OdysseyDatabaseManager mDatabaseManager = null;


    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Bind:" + intent.getType());
        return new OdysseyPlaybackServiceInterface(this);
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        super.onUnbind(intent);
        Log.v(TAG, "Unbind");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Odyssey PlaybackService onCreate");
        Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());

        // Start Handlerthread
        mHandlerThread = new HandlerThread("OdysseyHandlerThread", Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new PlaybackServiceHandler(mHandlerThread.getLooper(), this);

        // Create MediaPlayer
        mPlayer = new GaplessPlayer(this);
        Log.v(TAG, "Service created");

        // Set listeners
        mPlayer.setOnTrackStartListener(new PlaybackStartListener());
        mPlayer.setOnTrackFinishedListener(new PlaybackFinishListener());
        // Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // set up playlistmanager
        mDatabaseManager = new OdysseyDatabaseManager(getApplicationContext());

        // read playlist from database
        mCurrentList = mDatabaseManager.readPlaylist();

        // read state from database
        OdysseyServiceState state = mDatabaseManager.getState();
        mCurrentPlayingIndex = state.mTrackNumber;
        mLastPosition = state.mTrackPosition;
        mRandom = state.mRandomState;
        mRepeat = state.mRepeatState;

        if (mCurrentPlayingIndex < 0 || mCurrentPlayingIndex > mCurrentList.size()) {
            mCurrentPlayingIndex = -1;
        }

        mLastPlayingIndex = -1;
        mNextPlayingIndex = -1;


        if (mNoisyReceiver == null) {
            mNoisyReceiver = new BroadcastControlReceiver();

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            intentFilter.addAction(ACTION_PREVIOUS);
            intentFilter.addAction(ACTION_PAUSE);
            intentFilter.addAction(ACTION_PLAY);
            intentFilter.addAction(ACTION_TOGGLEPAUSE);
            intentFilter.addAction(ACTION_NEXT);
            intentFilter.addAction(ACTION_STOP);
            intentFilter.addAction(ACTION_QUIT);

            registerReceiver(mNoisyReceiver, intentFilter);
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mTempWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // set up random generator
        mRandomGenerator = new Random();


        // Initialize the mediacontrol manager for lockscreen pictures and remote control
        mPlaybackServiceStatusHelper = new PlaybackServiceStatusHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "PBS onStartCommand");
        if (intent.getExtras() != null) {
            String action = intent.getExtras().getString("action");

            if (action != null) {
                Log.v(TAG, "Action requested: " + action);
                switch (action) {
                    case ACTION_TOGGLEPAUSE:
                        togglePause();
                        break;
                    case ACTION_NEXT:
                        setNextTrack();
                        break;
                    case ACTION_PREVIOUS:
                        setPreviousTrack();
                        break;
                    case ACTION_STOP:
                        stop();
                        break;
                    case ACTION_PLAY:
                        resume();
                        break;
                    case ACTION_QUIT:
                        stopService();
                        break;
                }
            }
        }
        Log.v(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service destroyed");

        cancelQuitAlert();

        if (mNoisyReceiver != null) {
            unregisterReceiver(mNoisyReceiver);
            mNoisyReceiver = null;
        }
        stopSelf();

    }

    /**
     * Directly plays uri
     */
    public void playURI(TrackModel track) {
        // Clear playlist, enqueue uri, jumpto 0
        clearPlaylist();
        enqueueTrack(track);
        jumpToIndex(0);
    }

    public synchronized void cancelQuitAlert() {
        Log.v(TAG, "Cancelling quit alert in alertmanager");
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent quitIntent = new Intent(ACTION_QUIT);
        PendingIntent quitPI = PendingIntent.getBroadcast(this, TIMEOUT_INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(quitPI);
    }

    /**
     * Stops all playback
     */
    public void stop() {
        Log.v(TAG, "PBS stop()");
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
            // Notify simple last.fm scrobbler
            mPlaybackServiceStatusHelper.notifyLastFM(mCurrentList.get(mCurrentPlayingIndex), PlaybackServiceStatusHelper.SLS_STATES.SLS_COMPLETE);
        }

        mPlayer.stop();
        mCurrentPlayingIndex = 0;
        mLastPosition = 0;

        mNextPlayingIndex = -1;
        mLastPlayingIndex = -1;

        stopService();
    }

    public void pause() {
        Log.v(TAG, "PBS pause");

        if (mPlayer.isRunning()) {
            mLastPosition = mPlayer.getPosition();
            mPlayer.pause();

            AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            Intent quitIntent = new Intent(ACTION_QUIT);
            PendingIntent quitPI = PendingIntent.getBroadcast(this, TIMEOUT_INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + SERVICE_CANCEL_TIME, quitPI);

            // Broadcast simple.last.fm.scrobble broadcast
            if (mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
                TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
                Log.v(TAG, "Send to SLS: " + item);

            }

            mIsPaused = true;
        }

        mPlaybackServiceStatusHelper.updateStatus();

    }

    public void resume() {
        cancelQuitAlert();

//        // Check if mediaplayer needs preparing
        if (!mPlayer.isPrepared() && (mCurrentPlayingIndex != -1) && (mCurrentPlayingIndex < mCurrentList.size())) {
            jumpToIndex(mCurrentPlayingIndex, mLastPosition);
            Log.v(TAG, "Resuming position before playback to: " + mLastPosition);
            return;
        }

        if (mCurrentPlayingIndex < 0 && mCurrentList.size() > 0) {
            // Songs existing so start playback of playlist begin
            jumpToIndex(0);
        } else if (mCurrentPlayingIndex < 0 && mCurrentList.size() == 0) {
            mPlaybackServiceStatusHelper.updateStatus();
        } else if (mCurrentPlayingIndex < mCurrentList.size()) {

            /*
             * Make sure service is "started" so android doesn't handle it as a
             * "bound service"
             */
            Intent serviceStartIntent = new Intent(this, PlaybackService.class);
            serviceStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            startService(serviceStartIntent);

            // Request audio focus before doing anything
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Abort command
                return;
            }

            mPlaybackServiceStatusHelper.startMediaSession();

            mPlayer.resume();

            // Notify simple last.fm scrobbler
            mPlaybackServiceStatusHelper.notifyLastFM(mCurrentList.get(mCurrentPlayingIndex), PlaybackServiceStatusHelper.SLS_STATES.SLS_RESUME);

            mIsPaused = false;
            mLastPosition = 0;

            mPlaybackServiceStatusHelper.updateStatus();
        }
    }

    public void togglePause() {
        // Toggles playback state
        if (mPlayer.isRunning()) {
            pause();
        } else {
            resume();
        }
    }

    /**
     * add all tracks to playlist and play
     */
    public void playAllTracks() {

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        // clear playlist
        clearPlaylist();

        // stop service
        stop();

        // get all tracks
        List<TrackModel> allTracks = MusicLibraryHelper.getAllTracks(getApplicationContext());

        mCurrentList.addAll(allTracks);

        // start playing
        jumpToIndex(0);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * add all tracks to playlist, shuffle and play
     */
    public void playAllTracksShuffled() {
        playAllTracks();
        shufflePlaylist();
    }

    /**
     * shuffle the current playlist
     */
    public void shufflePlaylist() {

        // save currentindex
        int index = mCurrentPlayingIndex;

        if (mCurrentList.size() > 0 && index >= 0 && (index < mCurrentList.size())) {
            // get the current TrackModel and remove it from playlist
            TrackModel currentItem = mCurrentList.get(index);
            mCurrentList.remove(index);

            // shuffle playlist and set currentitem as first element
            Collections.shuffle(mCurrentList);
            mCurrentList.add(0, currentItem);

            // reset index
            mCurrentPlayingIndex = 0;

            mPlaybackServiceStatusHelper.updateStatus();

            // set next track for gapless

            try {
                mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
            } catch (GaplessPlayer.PlaybackException e) {
                handlePlaybackException(e);
            }
        } else if (mCurrentList.size() > 0 && index < 0) {
            // service stopped just shuffle playlist
            Collections.shuffle(mCurrentList);

            // sent broadcast
            mPlaybackServiceStatusHelper.updateStatus();
        }
    }

    /**
     * Sets nextplayback track to following on in playlist
     */
    public void setNextTrack() {
        // Needs to set gaplessplayer next object and reorganize playlist
        // Keep device at least for 5 seconds turned on
        mTempWakelock.acquire(5000);
        mLastPlayingIndex = mCurrentPlayingIndex;
        jumpToIndex(mNextPlayingIndex);
    }

    /**
     * Sets nextplayback track to preceding on in playlist
     */
    public void setPreviousTrack() {
        // Needs to set gaplessplayer next object and reorganize playlist
        // Get wakelock otherwise device could go to deepsleep until new song
        // starts playing

        // Keep device at least for 5 seconds turned on
        mTempWakelock.acquire(5000);

        if (getTrackPosition() > 2000) {
            // Check if current song should be restarted
            jumpToIndex(mCurrentPlayingIndex);
        } else if (mRandom == RANDOMSTATE.RANDOM_ON) {
            // handle random mode
            if (mLastPlayingIndex == -1) {
                // if no lastindex reuse currentindex
                jumpToIndex(mCurrentPlayingIndex);
            } else if (mLastPlayingIndex >= 0 && mLastPlayingIndex < mCurrentList.size()) {
                Log.v(TAG, "Found old track index");
                jumpToIndex(mLastPlayingIndex);
            }
        } else {
            if (mRepeat == REPEATSTATE.REPEAT_TRACK) {
                // repeat the current track again
                jumpToIndex(mCurrentPlayingIndex);
            } else {
                // Check if start is reached
                if ((mCurrentPlayingIndex - 1 >= 0) && mCurrentPlayingIndex < mCurrentList.size() && mCurrentPlayingIndex >= 0) {
                    jumpToIndex(mCurrentPlayingIndex - 1);
                } else if (mRepeat == REPEATSTATE.REPEAT_ALL) {
                    // In repeat mode next track is last track of playlist
                    jumpToIndex(mCurrentList.size() - 1);
                } else {
                    stop();
                }
            }
        }
    }

    protected PlaybackServiceHandler getHandler() {
        return mHandler;
    }

    public List<TrackModel> getCurrentList() {
        return mCurrentList;
    }

    public int getPlaylistSize() {
        return mCurrentList.size();
    }

    public TrackModel getPlaylistTrack(int index) {
        if ((index >= 0) && (index < mCurrentList.size())) {
            return mCurrentList.get(index);
        }
        return new TrackModel();
    }

    public void clearPlaylist() {
        Log.v(TAG, "Clearing Playlist");
        // Clear the list
        mCurrentList.clear();
        // reset random and repeat state
        mRandom = RANDOMSTATE.RANDOM_OFF;
        mRepeat = REPEATSTATE.REPEAT_OFF;
        // Stop the playback
        stop();
    }

    public void jumpToIndex(int index) {
        jumpToIndex(index, 0);
    }

    public void jumpToIndex(int index, int jumpTime) {
        Log.v(TAG, "Playback of index: " + index + " requested");
        Log.v(TAG, "Playlist size: " + mCurrentList.size());

        if (mPlayer.getActive()) {
            Log.v(TAG, "Ignoring command because gapless player is active");
            return;
        }

        cancelQuitAlert();

        // Stop playback
        mPlayer.stop();
        // Set currentindex to new song
        if (index < mCurrentList.size() && index >= 0) {
            mCurrentPlayingIndex = index;
            Log.v(TAG, "Start playback of: " + mCurrentList.get(mCurrentPlayingIndex));

            /*
             * Make sure service is "started" so android doesn't handle it as a
             * "bound service"
             */
            Intent serviceStartIntent = new Intent(this, PlaybackService.class);
            serviceStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            startService(serviceStartIntent);

            TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
            // Request audio focus before doing anything
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Abort command
                return;
            }

            // Start media session
            mPlaybackServiceStatusHelper.startMediaSession();

            mIsPaused = false;

            try {
                mPlayer.play(item.getTrackURL(), jumpTime);
            } catch (GaplessPlayer.PlaybackException e) {
                handlePlaybackException(e);
            }

            // Check if another song follows current one for gapless
            // playback
            mNextPlayingIndex = index;
        } else if (index == -1) {
            stop();
        }

    }

    public void seekTo(int position) {
        if (mPlayer.isRunning()) {
            mPlayer.seekTo(position);
        }
    }

    public int getTrackPosition() {
        switch (getPlaybackState()) {
            case PLAYING:
                return mPlayer.getPosition();
            case PAUSE:
            case RESUMED:
                return mLastPosition;
            case STOPPED:
                return 0;
        }
        return 0;
    }

    public int getTrackDuration() {
        if (mPlayer.isPrepared() || mPlayer.isRunning()) {
            return mPlayer.getDuration();
        } else {
            return 0;
        }
    }

    /**
     * Enqueue all given tracks.
     * Prepare the next track for playback if needed.
     */
    public void enqueueTracks(List<TrackModel> tracklist) {

        Log.v(TAG, "Enqueing " + tracklist.size() + "tracks");

        int oldSize = mCurrentList.size();

        mCurrentList.addAll(tracklist);

        /*
         * If currently playing and playing is the last one in old playlist set
         * enqueued one to next one for gapless mediaplayback
         */
        if (mCurrentPlayingIndex == (oldSize - 1) && (mCurrentPlayingIndex >= 0)) {
            // Next song for MP has to be set for gapless mediaplayback
            mNextPlayingIndex = mCurrentPlayingIndex + 1;
            setNextTrackForMP();
        }

        // Send new NowPlaying because playlist changed
        mPlaybackServiceStatusHelper.updateStatus();
    }

    /**
     * Enqueue all tracks of an album identified by the albumKey.
     *
     * @param albumKey The key of the album
     */
    public void enqueueAlbum(String albumKey) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        // get all tracks for the current albumkey from mediastore
        List<TrackModel> tracks = MusicLibraryHelper.getTracksForAlbum(albumKey, getApplicationContext());

        // add tracks to current playlist
        enqueueTracks(tracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * Enqueue all tracks of an artist identified by the artistId.
     *
     * @param artistId The id of the artist
     */
    public void enqueueArtist(long artistId) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        // get all tracks for the current artistId from mediastore
        List<TrackModel> tracks = MusicLibraryHelper.getTracksForArtist(artistId, getApplicationContext());

        // add tracks to current playlist
        enqueueTracks(tracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * Enqueue the given track.
     *
     * @param track  the current trackmodel
     * @param asNext flag if the track should be enqueued as next
     */
    public void enqueueTrack(TrackModel track, boolean asNext) {
        if (asNext) {
            enqueueAsNextTrack(track);
        } else {
            enqueueTrack(track);
        }
    }

    /**
     * Enqueue the given track.
     *
     * @param track the current trackmodel
     */
    private void enqueueTrack(TrackModel track) {

        Log.v(TAG, "Enqueing track: " + track.getTrackName());
        // Check if current song is old last one, if so set next song to MP for
        // gapless playback
        int oldSize = mCurrentList.size();
        mCurrentList.add(track);
        /*
         * If currently playing and playing is the last one in old playlist set
         * enqueued one to next one for gapless mediaplayback
         */
        if (mCurrentPlayingIndex == (oldSize - 1) && (mCurrentPlayingIndex >= 0)) {
            // Next song for MP has to be set for gapless mediaplayback
            mNextPlayingIndex = mCurrentPlayingIndex + 1;
            setNextTrackForMP();
        }
        // Send new NowPlaying because playlist changed
        mPlaybackServiceStatusHelper.updateStatus();
    }

    /**
     * Enqueue the given track as next.
     *
     * @param track the current trackmodel
     */
    private void enqueueAsNextTrack(TrackModel track) {

        // Check if currently playing, than enqueue after current song
        if (mCurrentPlayingIndex >= 0) {
            // Enqueue in list structure
            mCurrentList.add(mCurrentPlayingIndex + 1, track);
            mNextPlayingIndex = mCurrentPlayingIndex + 1;
            // Set next track to new one
            setNextTrackForMP();
        } else {
            // If not playing just add it to the beginning of the playlist
            mCurrentList.add(0, track);
            // Start playback which is probably intended
            jumpToIndex(0);
        }

        // Send new NowPlaying because playlist changed
        mPlaybackServiceStatusHelper.updateStatus();
    }

    public void dequeueTrack(int index) {
        // Check if track is currently playing, if so stop it
        if (mCurrentPlayingIndex == index) {
            // Stop playback of currentsong
            stop();
            // Delete song at index
            mCurrentList.remove(index);
            // Jump to next song which should be at index now
            // Jump is safe about playlist length so no need for extra safety
            jumpToIndex(index);
        } else if ((mCurrentPlayingIndex + 1) == index) {
            // Deletion of next song which requires extra handling
            // because of gapless playback, set next song to next on
            mCurrentList.remove(index);
            setNextTrackForMP();
        } else if (index >= 0 && index < mCurrentList.size()) {
            mCurrentList.remove(index);
            // mCurrentIndex is now moved one position up so set variable
            if (index < mCurrentPlayingIndex) {
                mCurrentPlayingIndex--;
            }
        }
        // Send new NowPlaying because playlist changed
        mPlaybackServiceStatusHelper.updateStatus();
    }

    /**
     * Stops the gapless mediaplayer and cancels the foreground service. Removes
     * any ongoing notification.
     */
    public void stopService() {
        Log.v(TAG, "Stopping service");

        // Cancel possible cancel timers ( yeah, very funny )
        cancelQuitAlert();

        mLastPosition = getTrackPosition();

        // If it is still running stop playback.
        PLAYSTATE state = getPlaybackState();
        if (state == PLAYSTATE.PLAYING || state == PLAYSTATE.PAUSE) {
            mPlayer.stop();
        }

        Log.v(TAG, "Stopping service and saving playlist with size: " + mCurrentList.size() + " and currentplaying: " + mCurrentPlayingIndex + " at position: " + mLastPosition);

        // Save the state of the PBS at once
        if (mCurrentList.size() > 0) {
            OdysseyServiceState serviceState = new OdysseyServiceState();

            serviceState.mTrackNumber = mCurrentPlayingIndex;
            serviceState.mTrackPosition = mLastPosition;
            serviceState.mRandomState = mRandom;
            serviceState.mRepeatState = mRepeat;
            mDatabaseManager.saveState(mCurrentList, serviceState, "auto", true);
        }

        mPlaybackServiceStatusHelper.updateStatus();

        // Stops the service itself.
        stopSelf();
    }

    public RANDOMSTATE getRandom() {
        return mRandom;
    }

    public REPEATSTATE getRepeat() {
        return mRepeat;
    }

    /**
     * Enables/disables repeat function. If enabling check if end of playlist is
     * already reached and then set next track to track0.
     * <p/>
     * If disabling check if last track plays.
     */
    public void toggleRepeat() {

        // get all repeat states
        REPEATSTATE[] repeatstates = REPEATSTATE.values();

        // toggle the repeat state
        mRepeat = repeatstates[(mRepeat.ordinal() + 1) % repeatstates.length];

        // update the status
        mPlaybackServiceStatusHelper.updateStatus();

        switch (mRepeat) {
            case REPEAT_OFF:
                // If playing last track, next track must be invalid
                if (mCurrentPlayingIndex == mCurrentList.size() - 1) {
                    mNextPlayingIndex = -1;
                } else {
                    mNextPlayingIndex = mCurrentPlayingIndex + 1;
                }
                setNextTrackForMP();
                break;
            case REPEAT_ALL:
                // If playing last track, next must be first in playlist
                if (mCurrentPlayingIndex == mCurrentList.size() - 1) {
                    mNextPlayingIndex = 0;
                } else {
                    mNextPlayingIndex = mCurrentPlayingIndex + 1;
                }
                setNextTrackForMP();
                break;
            case REPEAT_TRACK:
                // Next track must be same track again
                mNextPlayingIndex = mCurrentPlayingIndex;
                setNextTrackForMP();
                break;
        }
    }

    /**
     * Enables/disables the random function. If enabling randomize next song and
     * notify the gaplessPlayer about the new track. If deactivating set check
     * if new track exists and set it to this.
     */
    public void toggleRandom() {

        // get all random states
        RANDOMSTATE[] randomstates = RANDOMSTATE.values();

        // toggle the random state
        mRandom = randomstates[(mRandom.ordinal() + 1) % randomstates.length];

        // update the status
        mPlaybackServiceStatusHelper.updateStatus();
        if (mRandom == RANDOMSTATE.RANDOM_ON) {
            randomizeNextTrack();
        } else {
            // Set nextTrack to next in list
            if ((mCurrentPlayingIndex + 1 < mCurrentList.size()) && mCurrentPlayingIndex >= 0) {
                mNextPlayingIndex = mCurrentPlayingIndex + 1;
            }
        }
        // Notify GaplessPlayer
        setNextTrackForMP();
    }

    /**
     * Returns the index of the currently playing/paused track
     */
    public int getCurrentIndex() {
        return mCurrentPlayingIndex;
    }

    /**
     * Returns current track if any is playing/paused at the moment.
     */
    public TrackModel getCurrentTrack() {
        if (mCurrentPlayingIndex >= 0 && mCurrentList.size() > mCurrentPlayingIndex) {
            return mCurrentList.get(mCurrentPlayingIndex);
        }
        return null;
    }

    /**
     * Return the current nowplaying information including the current track.
     */
    public NowPlayingInformation getNowPlayingInformation() {

        PLAYSTATE state = getPlaybackState();

        if (state == PLAYSTATE.STOPPED) {
            return new NowPlayingInformation();
        } else {
            TrackModel currentTrack = mCurrentList.get(mCurrentPlayingIndex);

            return new NowPlayingInformation(state, mCurrentPlayingIndex, mRepeat, mRandom, mCurrentList.size(), currentTrack);
        }
    }

    /**
     * Save the current playlist in mediastore
     */
    public void savePlaylist(String name) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        MusicLibraryHelper.savePlaylist(name, mCurrentList, getApplicationContext());

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * enqueue a selected playlist from mediastore
     *
     * @param playlistId the id of the selected playlist
     */
    public void enqueuePlaylist(long playlistId) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        // get playlist from mediastore
        List<TrackModel> playlistTracks = MusicLibraryHelper.getTracksForPlaylist(playlistId, getApplicationContext());

        // add tracks to current playlist
        enqueueTracks(playlistTracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * Resume the bookmark with the given timestamp
     */
    public void resumeBookmark(long timestamp) {

        Log.v(TAG, "resume bookmark: " + timestamp);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        // clear current playlist
        clearPlaylist();

        // get playlist from database
        mCurrentList = mDatabaseManager.readPlaylist(timestamp);

        // get state from database
        OdysseyServiceState state = mDatabaseManager.getState(timestamp);

        mCurrentPlayingIndex = state.mTrackNumber;
        mLastPosition = state.mTrackPosition;
        mRandom = state.mRandomState;
        mRepeat = state.mRepeatState;

        if (mCurrentPlayingIndex < 0 || mCurrentPlayingIndex > mCurrentList.size()) {
            mCurrentPlayingIndex = -1;
        }

        mLastPlayingIndex = -1;
        mNextPlayingIndex = -1;

        // call resume and start playback
        resume();

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * Delete the bookmark with the given timestamp from the database.
     */
    public void deleteBookmark(long timestamp) {

        Log.v(TAG, "delete bookmark: " + timestamp);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        // delete wont affect current playback
        // so just delete the state and the playlist from the database
        mDatabaseManager.removeState(timestamp);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * Create a bookmark with the given title and save it in the database.
     */
    public void createBookmark(String bookmarkTitle) {

        Log.v(TAG, "create bookmark: " + bookmarkTitle);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        // grab the current state and playlist and save this as a new bookmark with the given title
        OdysseyServiceState serviceState = new OdysseyServiceState();

        serviceState.mTrackNumber = mCurrentPlayingIndex;
        serviceState.mTrackPosition = getTrackPosition();
        serviceState.mRandomState = mRandom;
        serviceState.mRepeatState = mRepeat;

        mDatabaseManager.saveState(mCurrentList, serviceState, bookmarkTitle, false);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * creates a trackmodel for a given filepath and add the track to the playlist
     *
     * @param filePath the path to the selected file
     * @param asNext   flag if the file should be enqueued as next
     */
    public void enqueueFile(String filePath, boolean asNext) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        FileModel currentFile = new FileModel(filePath);

        TrackModel track = FileExplorerHelper.getInstance(getApplicationContext()).getTrackModelForFile(currentFile);

        enqueueTrack(track, asNext);

        // Send new NowPlaying because playlist changed
        mPlaybackServiceStatusHelper.updateStatus();

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * creates trackmodels for a given directorypath and adds the tracks to the playlist
     *
     * @param directoryPath the path to the selected directory
     */
    public void enqueueDirectory(String directoryPath) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);

        FileModel currentDirectory = new FileModel(directoryPath);

        List<TrackModel> tracks = FileExplorerHelper.getInstance(getApplicationContext()).getTrackModelsForFolder(currentDirectory);

        // add tracks to current playlist
        enqueueTracks(tracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
    }

    /**
     * Returns the playback state of the service
     */
    public PLAYSTATE getPlaybackState() {
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0) {
            // Check current playback state. If playing inform all listeners and
            // check if notification is set, and set if not.
            if (mPlayer.isRunning() && (mCurrentPlayingIndex < mCurrentList.size())) {
                // Player is running and current index seems to be valid
                return PLAYSTATE.PLAYING;
            } else if (!mPlayer.isPrepared()) {
                // If a position is set but the player is not prepared yet it is clear, that the user has not played the song yet
                return PLAYSTATE.RESUMED;
            } else {
                // Only case left is that the player is paused, because a track is set AND PREPARED so it was played already
                return PLAYSTATE.PAUSE;
            }
        } else {
            // No playback because list is empty
            return PLAYSTATE.STOPPED;
        }
    }

    /**
     * Handles all the exceptions from the GaplessPlayer. For now it justs stops
     * itself and outs an Toast message to the user. Thats the best we could
     * think of now :P.
     */
    private void handlePlaybackException(GaplessPlayer.PlaybackException exception) {
        Log.v(TAG, "Exception occured: " + exception.getReason().toString());
        Toast.makeText(getBaseContext(), TAG + ":" + exception.getReason().toString(), Toast.LENGTH_LONG).show();
        // TODO better handling?
        // Stop service on exception for now
        stopService();
    }

    /**
     * Sets the index of the track to play next to a random generated one.
     */
    private void randomizeNextTrack() {
        // Set next index to random one
        if (mCurrentList.size() > 0) {
            mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());

            // if next index equal to current index create a new random
            // index but just trying 20 times
            int counter = 0;
            while (mNextPlayingIndex == mCurrentPlayingIndex && counter < 20) {
                mCurrentPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                counter++;
            }
        }
    }

    /**
     * Sets the next track of the GaplessPlayer to the nextTrack in the queue so
     * there can be a smooth transition from one track to the next one.
     */
    private void setNextTrackForMP() {
        // If player is not running or at least prepared, this makes no sense
        if (mPlayer.isPrepared() || mPlayer.isRunning()) {
            // Sets the next track for gapless playing
            if (mNextPlayingIndex >= 0 && mNextPlayingIndex < mCurrentList.size()) {
                try {
                    mPlayer.setNextTrack(mCurrentList.get(mNextPlayingIndex).getTrackURL());
                } catch (GaplessPlayer.PlaybackException e) {
                    handlePlaybackException(e);
                }
            } else {
                try {
                    /*
                     * No tracks remains. So set it to null. GaplessPlayer knows
                     * how to handle this :)
                     */
                    mPlayer.setNextTrack(null);
                } catch (GaplessPlayer.PlaybackException e) {
                    handlePlaybackException(e);
                }
            }
        }
    }

    /**
     * Listener class for playback begin of the GaplessPlayer. Handles the
     * different scenarios: If no random playback is active, check if new track
     * is ready and set index and GaplessPlayer to it. If no track remains in
     * queue check if repeat is activated and if reset queue to track 0. If
     * not generate a random index and set GaplessPlayer to that random track.
     */
    private class PlaybackStartListener implements GaplessPlayer.OnTrackStartedListener {
        @Override
        public void onTrackStarted(String URI) {
            mCurrentPlayingIndex = mNextPlayingIndex;
            Log.v(TAG, "track started: " + URI + " PL index: " + mCurrentPlayingIndex);

            if (mCurrentPlayingIndex >= 0 && mCurrentPlayingIndex < mCurrentList.size()) {
                // Broadcast simple.last.fm.scrobble broadcast
                TrackModel newTrackModel = mCurrentList.get(mCurrentPlayingIndex);
                mPlaybackServiceStatusHelper.notifyLastFM(newTrackModel, PlaybackServiceStatusHelper.SLS_STATES.SLS_START);
            }
            // Notify all the things
            mPlaybackServiceStatusHelper.updateStatus();
            if (mRandom == RANDOMSTATE.RANDOM_OFF) {
                // Random off

                switch (mRepeat) {
                    case REPEAT_OFF:
                        // Repeat off so next track is the next track in the playlist if available
                        if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                            mNextPlayingIndex = mCurrentPlayingIndex + 1;
                        } else {
                            mNextPlayingIndex = -1;
                        }
                        break;
                    case REPEAT_ALL:
                        // Repeat playlist so set to first PL song if last song is
                        // reached
                        if (mCurrentList.size() > 0 && mCurrentPlayingIndex + 1 == mCurrentList.size()) {
                            mNextPlayingIndex = 0;
                        }
                        break;
                    case REPEAT_TRACK:
                        // Repeat track so next track is the current track
                        mNextPlayingIndex = mCurrentPlayingIndex;
                        break;
                }
            } else {
                // Random on
                randomizeNextTrack();
            }

            // Sets the next track for gapless playing
            setNextTrackForMP();

            if (mTempWakelock.isHeld()) {
                // we could release wakelock here already
                mTempWakelock.release();
            }
        }
    }

    /**
     * Listener class for the GaplessPlayer. If a track finishes playback send
     * it to the simple last.fm scrobbler. Check if this was the last track of
     * the queue and if so send an update to all the things like
     * notification,broadcastreceivers and lockscreen. Stop the service.
     */
    private class PlaybackFinishListener implements GaplessPlayer.OnTrackFinishedListener {

        @Override
        public void onTrackFinished() {
            Log.v(TAG, "Playback of index: " + mCurrentPlayingIndex + " finished ");
            // Remember the last track index for moving backwards in the queue.
            mLastPlayingIndex = mCurrentPlayingIndex;
            if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
                // Broadcast simple.last.fm.scrobble broadcast
                TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
                mPlaybackServiceStatusHelper.notifyLastFM(item, PlaybackServiceStatusHelper.SLS_STATES.SLS_COMPLETE);
            }

            // No more tracks
            if (mNextPlayingIndex == -1) {
                stop();
                mPlaybackServiceStatusHelper.updateStatus();
            }
        }
    }

    /**
     * Callback method for AudioFocus changes. If audio focus change a call got
     * in or a notification for example. React to the different kinds of
     * changes. Resumes on regaining.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.v(TAG, "Audiofocus changed");
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.v(TAG, "Gained audiofocus");
                if (mIsDucked) {
                    mPlayer.setVolume(1.0f, 1.0f);
                    mIsDucked = false;
                } else if (mLostAudioFocus) {
                    resume();
                    mLostAudioFocus = false;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.v(TAG, "Lost audiofocus");
                // Stop playback service
                if (mPlayer.isRunning()) {
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.v(TAG, "Lost audiofocus temporarily");
                // Pause audio for the moment of focus loss
                if (mPlayer.isRunning()) {
                    pause();
                    mLostAudioFocus = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.v(TAG, "Lost audiofocus temporarily duckable");
                if (mPlayer.isRunning()) {
                    mPlayer.setVolume(0.1f, 0.1f);
                    mIsDucked = true;
                }
                break;
            default:
                break;
        }

    }

    private BroadcastControlReceiver mNoisyReceiver = null;

    /**
     * Receiver class for all the different broadcasts which are able to control
     * the PlaybackService. Also the receiver for the noisy event (e.x.
     * headphone unplugging)
     */
    private class BroadcastControlReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Broadcast received: " + intent.getAction());
            if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                /* Check if audio focus is currently lost. For example an incoming call
                and now the user disconnects it headphone. The music should not resume when the call
                is finished and the audio focus is regained.
                 */
                if (mLostAudioFocus) {
                    mLostAudioFocus = false;
                }
                Log.v(TAG, "NOISY AUDIO! CANCEL MUSIC");
                pause();
            } else if (intent.getAction().equals(ACTION_PLAY)) {
                resume();
            } else if (intent.getAction().equals(ACTION_PAUSE)) {
                pause();
            } else if (intent.getAction().equals(ACTION_NEXT)) {
                setNextTrack();
            } else if (intent.getAction().equals(ACTION_PREVIOUS)) {
                setPreviousTrack();
            } else if (intent.getAction().equals(ACTION_STOP)) {
                stop();
            } else if (intent.getAction().equals(ACTION_TOGGLEPAUSE)) {
                togglePause();
            } else if (intent.getAction().equals(ACTION_QUIT)) {
                stopService();
            }
        }

    }

}
