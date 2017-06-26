/*
 * Copyright (C) 2017 Team Gateship-One
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

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

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

import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
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

    /**
     * PlaybackState enum for the PlaybackService
     */
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

    /**
     * Idle state of this service. Used for notifying the user in the GUI about long running activites
     */
    public enum PLAYBACKSERVICESTATE {
        // If the service is performing an operation
        WORKING,
        // If the service is finished with the operation
        IDLE
    }

    /**
     * Tag used for debugging
     */
    private static final String TAG = "OdysseyPlaybackService";

    private static final String HANDLER_THREAD_NAME = "OdysseyPBSHandler";

    /**
     * Constants for Intent actions
     */
    public static final String ACTION_PLAY = "org.gateshipone.odyssey.play";
    public static final String ACTION_PAUSE = "org.gateshipone.odyssey.pause";
    public static final String ACTION_NEXT = "org.gateshipone.odyssey.next";
    public static final String ACTION_PREVIOUS = "org.gateshipone.odyssey.previous";
    public static final String ACTION_SEEKTO = "org.gateshipone.odyssey.seekto";
    public static final String ACTION_STOP = "org.gateshipone.odyssey.stop";
    public static final String ACTION_QUIT = "org.gateshipone.odyssey.quit";
    public static final String ACTION_TOGGLEPAUSE = "org.gateshipone.odyssey.togglepause";


    /**
     * Request code for the timeout intent when the PlaybackService is waiting to quit
     */
    private static final int TIMEOUT_INTENT_QUIT_REQUEST_CODE = 5;

    /**
     * Timeout time that the PlaybackService waits until it stops itself in milliseconds. (5 Minutes)
     */
    private final static int SERVICE_CANCEL_TIME = 5 * 60 * 1000;

    /**
     * Defines how often the random generator should retry to get a random number if it always
     * hits the currently running track
     */
    private final static int RANDOM_RETRIES = 20;

    /**
     * Thread in which messages are handled
     */
    private HandlerThread mHandlerThread;

    /**
     * Handler that executes action requested by a message
     */
    private PlaybackServiceHandler mHandler;

    /**
     * Saves if the audiofocus was lost for some reason. If it is set the playback will resume,
     * when the audiofocus gets back to this class.
     */
    private boolean mLostAudioFocus = false;

    /**
     * GaplessPlayer object that handles all the MediaPlayer objects and encapsulates the gapless
     * functions of android.
     */
    private GaplessPlayer mPlayer;

    /**
     * Currently active playlist.
     */
    private List<TrackModel> mCurrentList;

    /**
     * Index of the currently active track.
     */
    private int mCurrentPlayingIndex;

    /**
     * Index of the track that is played next. Does not necessarily be the mCurrentPlayingIndex + 1
     * because random could be activated.
     */
    private int mNextPlayingIndex;

    /**
     * Saves the index of the track that was played before the current one.
     */
    private int mLastPlayingIndex;

    /**
     * Saves if the volume is temporarily reduced because of a notification (for example)
     */
    private boolean mIsDucked = false;

    /**
     * Saves the time (in milliseconds) of the last playback.
     */
    private int mLastPosition = 0;

    /**
     * Random generator used to find a random position when random playback is active.
     */
    private Random mRandomGenerator;

    /**
     * Saves if random playback is active
     */
    private RANDOMSTATE mRandom = RANDOMSTATE.RANDOM_OFF;

    /**
     * Saves if repeat is active
     */
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
    private WakeLock mSongTransitionWakelock = null;

    /**
     * Databasemanager for saving and restoring states including their playlist
     */
    private OdysseyDatabaseManager mDatabaseManager = null;

    /**
     * BroadcastReceiver that handles all control intents
     */
    private BroadcastControlReceiver mBroadcastControlReceiver = null;

    private boolean mBusy = false;


    /**
     * Called when the PlaybackService is bound by an activity.
     *
     * @param intent Intent used for binding.
     * @return The Interface used for the service connection.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return new OdysseyPlaybackServiceInterface(this);
    }

    /**
     * Called when an activity unbounds from this service.
     *
     * @param intent Intent used for unbinding
     * @return True if unbound successfully
     */
    @Override
    public boolean onUnbind(final Intent intent) {
        super.onUnbind(intent);
        return true;
    }

    /**
     * Called when the service is created because it is requested by an activity
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Start Handlerthread which is used for the asynchronous handler.
        mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME, Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new PlaybackServiceHandler(mHandlerThread.getLooper(), this);

        // Create MediaPlayer object used throughout the complete runtime of this service
        mPlayer = new GaplessPlayer(this);

        // Register listeners with the GaplessPlayer
        mPlayer.setOnTrackStartListener(new PlaybackStartListener());
        mPlayer.setOnTrackFinishedListener(new PlaybackFinishListener());
        // Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // set up the OdysseyDatabaseManager
        mDatabaseManager = new OdysseyDatabaseManager(getApplicationContext());

        // read a possible saved playlist from the database
        mCurrentList = mDatabaseManager.readPlaylist();

        // read a possible saved state from database
        OdysseyServiceState state = mDatabaseManager.getState();

        // Resume the loaded state to internal variables
        mCurrentPlayingIndex = state.mTrackNumber;
        mLastPosition = state.mTrackPosition;
        mRandom = state.mRandomState;
        mRepeat = state.mRepeatState;

        // Check if saved state is within bounds of resumed playlist
        if (mCurrentPlayingIndex < 0 || mCurrentPlayingIndex > mCurrentList.size()) {
            mCurrentPlayingIndex = -1;
        }

        // Internal state initialization
        mLastPlayingIndex = -1;
        mNextPlayingIndex = -1;

        // Create a new BroadcastControlReceiver that handles all control broadcasts sent to the PlaybackService
        if (mBroadcastControlReceiver == null) {
            // Create a new instance if not already done, FIXME is this actual possible?
            mBroadcastControlReceiver = new BroadcastControlReceiver();

            // Create a filter to only handle certain actions
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            intentFilter.addAction(ACTION_PREVIOUS);
            intentFilter.addAction(ACTION_PAUSE);
            intentFilter.addAction(ACTION_PLAY);
            intentFilter.addAction(ACTION_TOGGLEPAUSE);
            intentFilter.addAction(ACTION_NEXT);
            intentFilter.addAction(ACTION_STOP);
            intentFilter.addAction(ACTION_QUIT);

            intentFilter.addAction(ArtworkManager.ACTION_NEW_ARTWORK_READY);

            // Register the receiver within the system
            registerReceiver(mBroadcastControlReceiver, intentFilter);
        }

        // Request the powermanager to initialize the transition wakelock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // Initialize the transition wake lock (see above for the reason)
        mSongTransitionWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // set up random generator used for random playback
        mRandomGenerator = new Random();


        // Initialize the mediacontrol manager for lockscreen pictures and remote control
        mPlaybackServiceStatusHelper = new PlaybackServiceStatusHelper(this);
    }

    /**
     * Called when an intent is used to start the service (e.g. from the widget)
     *
     * @param intent  Intent used for starting the PlaybackService
     * @param flags   Some flags (not used)
     * @param startId Id (not used)
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (null != intent && intent.getExtras() != null) {
            String action = intent.getExtras().getString("action");

            if (action != null) {
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
        } else {
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    /**
     * Called when the system wants to get rid of Odyssey :(. Clean up and unregister BroadcastReceivers
     */
    @Override
    public void onDestroy() {
        // Cancel any pending quit alerts
        cancelQuitAlert();

        // Unregister a existing broadcastreceiver
        if (mBroadcastControlReceiver != null) {
            unregisterReceiver(mBroadcastControlReceiver);
            mBroadcastControlReceiver = null;
        }

        // Stop myself
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

    /**
     * Cancels possible outstanding alerts registered within the AlarmManager to quit this service.
     */
    public synchronized void cancelQuitAlert() {
        // Request the alarm manager and quit the alert with the given TIMEOUT_INTENT_QUIT_REQUEST_CODE
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent quitIntent = new Intent(ACTION_QUIT);
        PendingIntent quitPI = PendingIntent.getBroadcast(this, TIMEOUT_INTENT_QUIT_REQUEST_CODE, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(quitPI);
    }

    /**
     * Stops all playback and the service afterwards, because it usually is not required afterwards
     */
    public void stop() {
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
            // Notify simple last.fm scrobbler about playback stop
            mPlaybackServiceStatusHelper.notifyLastFM(mCurrentList.get(mCurrentPlayingIndex), PlaybackServiceStatusHelper.SLS_STATES.SLS_COMPLETE);
        }

        // Request the GaplessPlayer to stop its playback.
        mPlayer.stop();

        // Reset the interal state variables
        mCurrentPlayingIndex = 0;
        mLastPosition = 0;

        mNextPlayingIndex = -1;
        mLastPlayingIndex = -1;

        // Request to stop the service
        stopService();
    }

    /**
     * Pauses playback (if one is running) otherwise is doing nothing.
     */
    public void pause() {
        // Check if GaplessPlayer is playing something
        if (mPlayer.isRunning()) {
            // Pause the playback before saving the position
            mPlayer.pause();

            // Save the position because it is later used to save the state in the database
            mLastPosition = mPlayer.getPosition();

            // Start an alert within the AlarmManager to quit this service after a timeout (defined in SERVICE_CANCEL_TIME)
            AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            Intent quitIntent = new Intent(ACTION_QUIT);
            PendingIntent quitPI = PendingIntent.getBroadcast(this, TIMEOUT_INTENT_QUIT_REQUEST_CODE, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + SERVICE_CANCEL_TIME, quitPI);

            // Broadcast simple.last.fm.scrobble broadcast to inform about pause state
            if (mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
                TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
            }
        }

        // Distribute the new status to everything (Notification, widget, ...)
        mPlaybackServiceStatusHelper.updateStatus();
    }

    /**
     * Resumes playback of a previously paused playback. If called first after starting the service,
     * this will resume the last state (loaded from the database)
     */
    public void resume() {
        cancelQuitAlert();

        // Check if mediaplayer needs preparing because we are resuming an state from the database or stopped state
        if (!mPlayer.isPrepared() && (mCurrentPlayingIndex != -1) && (mCurrentPlayingIndex < mCurrentList.size())) {
            jumpToIndex(mCurrentPlayingIndex, mLastPosition);
            return;
        }

        // Check if no mCurrentPlayingIndex is available which means that we should start playing position 0 (if available).
        if (mCurrentPlayingIndex < 0 && mCurrentList.size() > 0) {
            // Songs exist, so start playback of playlist begin
            jumpToIndex(0);
        } else if (mCurrentPlayingIndex < 0 && mCurrentList.size() == 0) {
            // If no songs are enqueued to the playlist just do nothing here. FIXME is this update necessary? (no change in state)
            // mPlaybackServiceStatusHelper.updateStatus();
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
                // Abort command if we don't acquired the audio focus
                return;
            }

            // Notify the helper class to start a media session
            mPlaybackServiceStatusHelper.startMediaSession();

            // This will instruct the GaplessPlayer to actually start playing
            mPlayer.resume();

            // Notify simple last.fm scrobbler about the playback resume
            mPlaybackServiceStatusHelper.notifyLastFM(mCurrentList.get(mCurrentPlayingIndex), PlaybackServiceStatusHelper.SLS_STATES.SLS_RESUME);

            // Reset the time position because it is invalid now
            mLastPosition = 0;

            // Notify the helper class, that the state has changed
            mPlaybackServiceStatusHelper.updateStatus();
        }
    }

    /**
     * Toggles between playing & paused
     */
    public void togglePause() {
        // Toggles playback state
        if (mPlayer.isRunning()) {
            pause();
        } else {
            resume();
        }
    }

    /**
     * Add all tracks to the playlist and start playing them
     */
    public void playAllTracks() {
        // Notify the user about the possible long running operation
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        // clear the playlist before adding all tracks
        clearPlaylist();


        // Get a list of all available tracks from the MusicLibraryHelper
        List<TrackModel> allTracks = MusicLibraryHelper.getAllTracks(getApplicationContext());

        mCurrentList.addAll(allTracks);

        // Start playing the first item in the list
        jumpToIndex(0);

        // Notify the user that the operation is now finished
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * Adds all tracks to playlist, play and then shuffle
     */
    public void playAllTracksShuffled() {
        playAllTracks();
        shufflePlaylist();
    }

    /**
     * Shuffles the current playlist
     */
    public void shufflePlaylist() {
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
            // get the current TrackModel and remove it from playlist
            TrackModel currentItem = mCurrentList.get(mCurrentPlayingIndex);
            mCurrentList.remove(mCurrentPlayingIndex);

            // shuffle playlist and set currentitem as first element
            Collections.shuffle(mCurrentList);
            mCurrentList.add(0, currentItem);

            // reset index
            mCurrentPlayingIndex = 0;

            mPlaybackServiceStatusHelper.updateStatus();

            // set next track for the GaplessPlayer which has now changed
            try {
                if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                    mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
                } else {
                    mPlayer.setNextTrack(null);
                }
            } catch (GaplessPlayer.PlaybackException e) {
                handlePlaybackException(e);
            }
        } else if (mCurrentList.size() > 0 && mCurrentPlayingIndex < 0) {
            // service stopped just shuffle playlist
            Collections.shuffle(mCurrentList);

            // sent broadcast
            mPlaybackServiceStatusHelper.updateStatus();
        }
    }

    /**
     * Jumps to the next song that is set in mNextPlayingIndex
     */
    public void setNextTrack() {
        // Keep device at least for 5 seconds turned on
        mSongTransitionWakelock.acquire(5000);

        // Save the last playing index, to allow the user to jump back
        mLastPlayingIndex = mCurrentPlayingIndex;

        // Jump to the mNextPlayingIndex
        jumpToIndex(mNextPlayingIndex);
    }

    /**
     * Sets nextplayback track to preceding on in playlist
     */
    public void setPreviousTrack() {
        // Keep device at least for 5 seconds turned on
        mSongTransitionWakelock.acquire(5000);

        // Logic to restart the song if playback is not progressed beyond 2000ms.
        // This enables the behavior of CD-players which a user is used to.
        if (getTrackPosition() > 2000) {
            // Check if current song should be restarted
            jumpToIndex(mCurrentPlayingIndex);
        } else if (mRandom == RANDOMSTATE.RANDOM_ON) {
            // handle random mode
            if (mLastPlayingIndex == -1) {
                // if no mLastPlayingIndex reuse mCurrentPlayingIndex and restart the song
                jumpToIndex(mCurrentPlayingIndex);
            } else if (mLastPlayingIndex >= 0 && mLastPlayingIndex < mCurrentList.size()) {
                // If a song was played before this one, jump back to it
                jumpToIndex(mLastPlayingIndex);
            }
        } else {
            // Check if the repeat track mode is activated which means that the user is stuck to the current song
            if (mRepeat == REPEATSTATE.REPEAT_TRACK) {
                // repeat the current track again
                jumpToIndex(mCurrentPlayingIndex);
            } else {
                // Check if the first playlist element is reached
                if ((mCurrentPlayingIndex - 1 >= 0) && mCurrentPlayingIndex < mCurrentList.size() && mCurrentPlayingIndex >= 0) {
                    // Jump to the previous song (sequential back jump)
                    jumpToIndex(mCurrentPlayingIndex - 1);
                } else if (mRepeat == REPEATSTATE.REPEAT_ALL) {
                    // In repeat mode next track is last track of playlist
                    jumpToIndex(mCurrentList.size() - 1);
                } else {
                    // If repeat all is not activated just stop playback if we try to move to position -1
                    stop();
                }
            }
        }
    }


    /**
     * Getter for the handler used by the service interface
     *
     * @return The handler of this service
     */
    protected PlaybackServiceHandler getHandler() {
        return mHandler;
    }

    /**
     * Returns a reference to the playlist. DO NOT MODIFY THE LIST!!!
     *
     * @return Reference to mCurrentList
     */
    public List<TrackModel> getCurrentList() {
        return mCurrentList;
    }

    /**
     * @return Size of the playlist
     */
    public int getPlaylistSize() {
        return mCurrentList.size();
    }

    /**
     * Getter to retrieve TrackModel items from the playlist
     *
     * @param index Position of the track to return
     * @return Valid track if position within bounds, empty track otherwise
     */
    public TrackModel getPlaylistTrack(int index) {
        if ((index >= 0) && (index < mCurrentList.size())) {
            return mCurrentList.get(index);
        }
        return new TrackModel();
    }

    /**
     * Clears the current playlist and stops playback afterwards. Also resets repeat, random state
     */
    public void clearPlaylist() {
        // Clear the list
        mCurrentList.clear();
        // reset random and repeat state
        mRandom = RANDOMSTATE.RANDOM_OFF;
        mRepeat = REPEATSTATE.REPEAT_OFF;
        // Stop the playback
        stop();
    }

    /**
     * Jumps playback to the given index
     *
     * @param index Position of song to play
     */
    public void jumpToIndex(int index) {
        jumpToIndex(index, 0);
    }

    /**
     * Jumps playback to the given index and position inside the song (in milliseconds)
     *
     * @param index    Position of the song to play
     * @param jumpTime Position inside the song (milliseconds)
     */
    public void jumpToIndex(int index, int jumpTime) {

        // Prevent that the user freaks out and taps one song after another. This waits for finishing, to prevent race conditions.
        if (mPlayer.getActive()) {
            // Abort here if the player is still active
            return;
        }

        // Cancel possible alerts registered within the AlarmManager
        cancelQuitAlert();

        // Stop playback before starting a new song. This ensures state safety
        mPlayer.stop();

        // Set mCurrentPlayingIndex to new song after checking the bounds
        if (index < mCurrentList.size() && index >= 0) {
            mCurrentPlayingIndex = index;

            /*
             * Make sure service is "started" so android doesn't handle it as a
             * "bound service"
             */
            Intent serviceStartIntent = new Intent(this, PlaybackService.class);
            serviceStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            startService(serviceStartIntent);

            // Get the item that is requested to be played.
            TrackModel item = mCurrentList.get(mCurrentPlayingIndex);

            // Request audio focus before doing anything
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Abort command if audio focus was not granted
                return;
            }

            // Notify the PlaybackServiceStatusHelper that a new media session is started
            mPlaybackServiceStatusHelper.startMediaSession();

            // Try to start playback of the track url.
            try {
                mPlayer.play(item.getTrackURL(), jumpTime);
            } catch (GaplessPlayer.PlaybackException e) {
                // Handle an error of the play commant
                handlePlaybackException(e);
            }

            // Sets the mNextPlayingIndex to the index just startet, because the PlaybackStartListener will
            // set the mCurrentPlayingIndex to the mNextPlayingIndex. This ensures that no additional code
            // is necessary to handle playback start
            mNextPlayingIndex = index;
        } else if (index < 0) {
            // If index < 0 is requested for whatever reason stop the playback
            stop();
        }

    }

    /**
     * Seeks the GaplessPlayer to the given position, but only if it is playing.
     *
     * @param position Position in milliseconds to seek to
     */
    public void seekTo(int position) {
        switch (getPlaybackState()) {
            case PLAYING:
            case PAUSE:
                mLastPosition = position;
                mPlayer.seekTo(position);
                break;
            case RESUMED:
                mLastPosition = position;
                break;
            case STOPPED:
                return;
        }
    }

    /**
     * Returns the playback position of the GaplessPlayer
     *
     * @return Playback position if playing/paused or 0 if stopped
     */
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

    /**
     * @return Returns the duration of the active track or 0 if no active track
     */
    public int getTrackDuration() {
        return mPlayer.getDuration();
    }

    /**
     * Enqueue all given tracks.
     * Prepare the next track for playback if needed.
     */
    public void enqueueTracks(List<TrackModel> tracklist) {
        // Saved to check if we played the last song of the list
        int oldSize = mCurrentList.size();

        // Add the tracks to the actual list
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

        // Inform the helper that the state has changed
        mPlaybackServiceStatusHelper.updateStatus();
    }

    /**
     * Enqueue all tracks of an album identified by the albumKey.
     *
     * @param albumKey The key of the album
     */
    public void enqueueAlbum(String albumKey) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        // get all tracks for the current albumkey from mediastore
        List<TrackModel> tracks = MusicLibraryHelper.getTracksForAlbum(albumKey, getApplicationContext());

        // add tracks to current playlist
        enqueueTracks(tracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    public void enqueueRecentAlbums() {
        // TODO check if this is working properly

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        List<TrackModel> tracks = MusicLibraryHelper.getRecentTracks(getApplicationContext());

        enqueueTracks(tracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * Enqueue all tracks of an artist identified by the artistId.
     *
     * @param artistId The id of the artist
     * @param orderKey String to specify the order of the tracks
     */
    public void enqueueArtist(long artistId, String orderKey) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        // get all tracks for the current artistId from mediastore
        List<TrackModel> tracks = MusicLibraryHelper.getTracksForArtist(artistId, orderKey, getApplicationContext());

        // add tracks to current playlist
        enqueueTracks(tracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
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
        // Check if current song is old last one, if so set next song to MP for
        // gapless playback
        int oldSize = mCurrentList.size();
        mCurrentList.add(track);
        /*
         * If currently playing and playing is the last one in old playlist set
         * enqueued one to next one for gapless mediaplayback
         */
        if (mCurrentPlayingIndex == (oldSize - 1) && (oldSize != 0)) {
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

        // Check if currently playing index is set to a valid value
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

    /**
     * Dequeues a track from the playlist
     *
     * @param index Position of the track to remove
     */
    public void dequeueTrack(int index) {
        // Check if track is currently playing, if so stop it
        if (mCurrentPlayingIndex == index) {
            // Stop playback of currentsong
            stop();
            // Delete song at index
            mCurrentList.remove(index);
            // Jump to next song which should be at index now
            // Jump is secured to check playlist bounds
            jumpToIndex(index);
        } else if ((mCurrentPlayingIndex + 1) == index) {
            // Deletion of next song which requires extra handling
            // because of gapless playback, set next song to next one
            mCurrentList.remove(index);
            setNextTrackForMP();
        } else if (index >= 0 && index < mCurrentList.size()) {
            mCurrentList.remove(index);
            // mCurrentIndex and mNextPlayingIndex is now moved one position up so update variables
            if (index < mCurrentPlayingIndex) {
                mCurrentPlayingIndex--;
                mNextPlayingIndex--;
            }
        }
        // Send new NowPlaying because playlist changed
        mPlaybackServiceStatusHelper.updateStatus();
    }

    /**
     * Dequeues a section from a playlist
     * A section is defined as a bunch of tracks from the same album.
     *
     * @param index start position of the section to remove
     */
    public void dequeueTracks(int index) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        int endIndex = index + 1;

        String albumKey = mCurrentList.get(index).getTrackAlbumKey();

        // get endindex for section
        while (endIndex < mCurrentList.size()) {
            if (albumKey.equals(mCurrentList.get(endIndex).getTrackAlbumKey())) {
                endIndex++;
            } else {
                break;
            }
        }

        if (mCurrentPlayingIndex >= index && mCurrentPlayingIndex < endIndex) {
            // current song is in section so stop playing
            stop();

            // remove section and update endindex accordingly
            ListIterator<TrackModel> iterator = mCurrentList.listIterator(index);

            while (iterator.hasNext()) {
                TrackModel track = iterator.next();

                if (albumKey.equals(track.getTrackAlbumKey())) {
                    iterator.remove();
                    endIndex--;
                } else {
                    break;
                }
            }

            // Jump to next song which should be at endIndex now
            // Jump is secured to check playlist bounds
            jumpToIndex(endIndex);
        } else if ((mCurrentPlayingIndex + 1) == index) {
            // Deletion of next song which requires extra handling
            // because of gapless playback, set next song to next one

            // remove section
            ListIterator<TrackModel> iterator = mCurrentList.listIterator(index);

            while (iterator.hasNext()) {
                TrackModel track = iterator.next();

                if (albumKey.equals(track.getTrackAlbumKey())) {
                    iterator.remove();
                } else {
                    break;
                }
            }

            setNextTrackForMP();
        } else if (index >= 0 && index < mCurrentList.size()) {
            // check if section is before current song
            boolean beforeCurrentTrack = endIndex <= mCurrentPlayingIndex;

            ListIterator<TrackModel> iterator = mCurrentList.listIterator(index);

            while (iterator.hasNext()) {
                TrackModel track = iterator.next();

                if (albumKey.equals(track.getTrackAlbumKey())) {
                    iterator.remove();
                    if (beforeCurrentTrack) {
                        // if section is before current song update mCurrentPlayingIndex and mNextPlayingIndex
                        mCurrentPlayingIndex--;
                        mNextPlayingIndex--;
                    }
                } else {
                    break;
                }
            }
        }

        // Send new NowPlaying because playlist changed
        mPlaybackServiceStatusHelper.updateStatus();

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * Stops the gapless mediaplayer and cancels the foreground service. Removes
     * any ongoing notification.
     */
    public void stopService() {
        // Cancel possible cancel timers
        cancelQuitAlert();

        // Save the current playback position
        mLastPosition = getTrackPosition();

        // If it is still running stop playback.
        PLAYSTATE state = getPlaybackState();
        if (state == PLAYSTATE.PLAYING || state == PLAYSTATE.PAUSE) {
            mPlayer.stop();
        }


        // Save the state of the PBS at once
        if (mCurrentList.size() > 0) {
            OdysseyServiceState serviceState = new OdysseyServiceState();

            serviceState.mTrackNumber = mCurrentPlayingIndex;
            serviceState.mTrackPosition = mLastPosition;
            serviceState.mRandomState = mRandom;
            serviceState.mRepeatState = mRepeat;
            mDatabaseManager.saveState(mCurrentList, serviceState, "auto", true);
        }

        // Final status update
        mPlaybackServiceStatusHelper.updateStatus();

        // Stops the service itself.
        stopSelf();
    }

    /**
     * @return The current random state of this service
     */
    public RANDOMSTATE getRandom() {
        return mRandom;
    }

    /**
     * @return The current repeat state of this service
     */
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
        mBusy = true;

        MusicLibraryHelper.savePlaylist(name, mCurrentList, getApplicationContext());

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * enqueue a selected playlist from mediastore
     *
     * @param playlistId the id of the selected playlist
     */
    public void enqueuePlaylist(long playlistId) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        // get playlist from mediastore
        List<TrackModel> playlistTracks = MusicLibraryHelper.getTracksForPlaylist(playlistId, getApplicationContext());

        // add tracks to current playlist
        enqueueTracks(playlistTracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * Resume the bookmark with the given timestamp
     */
    public void resumeBookmark(long timestamp) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        // clear current playlist
        clearPlaylist();

        // get playlist from database
        mCurrentList = mDatabaseManager.readPlaylist(timestamp);

        // get state from database
        OdysseyServiceState state = mDatabaseManager.getState(timestamp);

        // Copy the loaded state to internal state
        mCurrentPlayingIndex = state.mTrackNumber;
        mLastPosition = state.mTrackPosition;
        mRandom = state.mRandomState;
        mRepeat = state.mRepeatState;

        // Check if playlist bounds match loaded indices
        if (mCurrentPlayingIndex < 0 || mCurrentPlayingIndex > mCurrentList.size()) {
            mCurrentPlayingIndex = -1;
        }

        mLastPlayingIndex = -1;
        mNextPlayingIndex = -1;

        // call resume and start playback
        resume();

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * Delete the bookmark with the given timestamp from the database.
     */
    public void deleteBookmark(long timestamp) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        // delete wont affect current playback
        // so just delete the state and the playlist from the database
        mDatabaseManager.removeState(timestamp);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * Create a bookmark with the given title and save it in the database.
     */
    public void createBookmark(String bookmarkTitle) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        // grab the current state and playlist and save this as a new bookmark with the given title
        OdysseyServiceState serviceState = new OdysseyServiceState();

        // Move internal state to the new created state object
        serviceState.mTrackNumber = mCurrentPlayingIndex;
        serviceState.mTrackPosition = getTrackPosition();
        serviceState.mRandomState = mRandom;
        serviceState.mRepeatState = mRepeat;

        mDatabaseManager.saveState(mCurrentList, serviceState, bookmarkTitle, false);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * creates a trackmodel for a given filepath and add the track to the playlist
     *
     * @param filePath the path to the selected file
     * @param asNext   flag if the file should be enqueued as next
     */
    public void enqueueFile(String filePath, boolean asNext) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        FileModel currentFile = new FileModel(filePath);
        TrackModel track = FileExplorerHelper.getInstance().getTrackModelForFile(getApplicationContext(), currentFile);

        enqueueTrack(track, asNext);

        // Send new NowPlaying because playlist changed
        mPlaybackServiceStatusHelper.updateStatus();

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * creates trackmodels for a given directorypath and adds the tracks to the playlist
     *
     * @param directoryPath the path to the selected directory
     */
    public void enqueueDirectory(String directoryPath) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        FileModel currentDirectory = new FileModel(directoryPath);

        List<TrackModel> tracks = FileExplorerHelper.getInstance().getTrackModelsForFolder(getApplicationContext(), currentDirectory);

        // add tracks to current playlist
        enqueueTracks(tracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
    }

    /**
     * creates trackmodels for a given directorypath (inclusive all subdirectories) and adds the tracks to the playlist
     *
     * @param directoryPath the path to the selected directory
     */
    public void enqueueDirectoryAndSubDirectories(String directoryPath) {
        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.WORKING);
        mBusy = true;

        FileModel currentDirectory = new FileModel(directoryPath);

        List<TrackModel> tracks = FileExplorerHelper.getInstance().getTrackModelsForFolderAndSubFolders(getApplicationContext(), currentDirectory);

        // add tracks to current playlist
        enqueueTracks(tracks);

        mPlaybackServiceStatusHelper.broadcastPlaybackServiceState(PLAYBACKSERVICESTATE.IDLE);
        mBusy = false;
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
     * Returns the working state of the service
     *
     * @return true if the service is busy else false
     */
    public boolean isBusy() {
        return mBusy;
    }

    public void hideArtwork(boolean enable) {
        mPlaybackServiceStatusHelper.hideArtwork(enable);
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
     * Sets the index, of the track to play next,to a random generated one.
     */
    private void randomizeNextTrack() {
        // Set next index to random one
        if (mCurrentList.size() > 0) {
            mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());

            // if next index equal to current index create a new random
            // index but just trying RANDOM_RETRIES times
            int counter = 0;
            while (mNextPlayingIndex == mCurrentPlayingIndex && counter < RANDOM_RETRIES) {
                mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
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

    public int getAudioSessionID() {
        return mPlayer.getAudioSessionID();
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
            // Move the index to the next one
            mCurrentPlayingIndex = mNextPlayingIndex;

            if (mCurrentPlayingIndex >= 0 && mCurrentPlayingIndex < mCurrentList.size()) {
                // Broadcast simple.last.fm.scrobble broadcast about the started track
                TrackModel newTrackModel = mCurrentList.get(mCurrentPlayingIndex);
                mPlaybackServiceStatusHelper.notifyLastFM(newTrackModel, PlaybackServiceStatusHelper.SLS_STATES.SLS_START);
            }
            // Notify all the things
            mPlaybackServiceStatusHelper.updateStatus();
            if (mRandom == RANDOMSTATE.RANDOM_OFF) {
                // Check the repeat state

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
                        } else if (mCurrentList.size() > 0 && mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                            // If the end of the playlist was not reached move to the next track
                            mNextPlayingIndex = mCurrentPlayingIndex + 1;
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

            // Check if temporary wakelock is held to prevent device from shutting down during
            // the short transition
            if (mSongTransitionWakelock.isHeld()) {
                // we could release wakelock here again, because the GaplessPlayer is now wakelocking again
                mSongTransitionWakelock.release();
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
            // Remember the last track index for moving backwards in the queue.
            mLastPlayingIndex = mCurrentPlayingIndex;
            if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
                // Broadcast simple.last.fm.scrobble broadcast about the track finish
                TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
                mPlaybackServiceStatusHelper.notifyLastFM(item, PlaybackServiceStatusHelper.SLS_STATES.SLS_COMPLETE);
            }

            // No more tracks
            if (mNextPlayingIndex == -1) {
                stop();
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
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // If we are ducked at the moment, return volume to full output
                if (mIsDucked) {
                    mPlayer.setVolume(1.0f, 1.0f);
                    mIsDucked = false;
                } else if (mLostAudioFocus) {
                    // If we temporarily lost the audio focus we can resume playback here
                    resume();
                    mLostAudioFocus = false;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Pause playback here, because we lost audio focus (not temporarily)
                if (mPlayer.isRunning()) {
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Pause audio for the moment of focus loss, will be resumed.
                if (mPlayer.isRunning()) {
                    pause();
                    mLostAudioFocus = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // We only need to duck our volume, so set it to 10%? and save that we ducked for resuming
                if (mPlayer.isRunning()) {
                    mPlayer.setVolume(0.1f, 0.1f);
                    mIsDucked = true;
                }
                break;
            default:
                break;
        }

    }

    /**
     * Receiver class for all the different broadcasts which are able to control
     * the PlaybackService. Also the receiver for the noisy event (e.x.
     * headphone unplugging)
     */
    private class BroadcastControlReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                /* Check if audio focus is currently lost. For example an incoming call gets picked up
                and now the user disconnects the headphone. The music should not resume when the call
                is finished and the audio focus is regained.
                 */
                if (mLostAudioFocus) {
                    mLostAudioFocus = false;
                }
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
            } else if (intent.getAction().equals(ArtworkManager.ACTION_NEW_ARTWORK_READY)) {
                // Check if artwork is for currently playing album
                String albumKey = intent.getStringExtra(ArtworkManager.INTENT_EXTRA_KEY_ALBUM_KEY);
                mPlaybackServiceStatusHelper.newAlbumArtworkReady(albumKey);
            }
        }

    }

}
