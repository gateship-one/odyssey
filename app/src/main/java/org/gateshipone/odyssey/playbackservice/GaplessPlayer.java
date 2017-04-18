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

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import org.gateshipone.odyssey.utils.FormatHelper;

/**
 * This class provides an easy to use interface for the android provided
 * mediaplayer class. It handles the transition between songs so the playback
 * appears to be gapless.
 * <p>
 * This does not work on all devices as some devices have a gap during song transition
 * because of hardware decoders that don't handle transitions gapless.
 */
public class GaplessPlayer {
    private final static String TAG = "OdysseyGaplessPlayer";

    public enum REASON {
        IOError, SecurityError, StateError, ArgumentError
    }

    /**
     * MediaPlayer of the currently playing track (if any)
     */
    private MediaPlayer mCurrentMediaPlayer = null;

    /**
     * MediaPlayer of the next track to play. This can be null if no next player is set.
     */
    private MediaPlayer mNextMediaPlayer = null;

    /**
     * Saves if the player for the current track is prepared or not.
     */
    private boolean mCurrentPrepared = false;

    /**
     * Saves if the second player is finished with preparing.
     */
    private boolean mSecondPrepared = false;

    /**
     * Saves if the second player is currently preparing to play (buffering,opening codec, opening hw decoder,...)
     */
    private boolean mSecondPreparing = false;

    /**
     * URL of the currently playing song (if any)
     */
    private String mPrimarySource = null;

    /**
     * URL of the next song to play.
     */
    private String mSecondarySource = null;

    /**
     * Time value to seek to after preparing the current song. This is used for resuming playback
     * at a specific position.
     */
    private int mPrepareTime = 0;

    /**
     * PlaybackService using this class. This required as a context for wakelocks, callbacks,...
     */
    private PlaybackService mPlaybackService;

    /**
     * Lock to synchronize access to the boolean variable mSecondPreparing
     */
    private Semaphore mSecondPreparingLock;

    /**
     * Registered listeners for track finish callbacks
     */
    private ArrayList<OnTrackFinishedListener> mTrackFinishedListeners;

    /**
     * Registered listeners for track start callbacks
     */
    private ArrayList<OnTrackStartedListener> mTrackStartListeners;

    /**
     * Public constructor.
     *
     * @param service PlaybackService to use as context and for callbacks.
     */
    public GaplessPlayer(PlaybackService service) {
        this.mTrackFinishedListeners = new ArrayList<>();
        this.mTrackStartListeners = new ArrayList<>();
        mPlaybackService = service;
        mSecondPreparingLock = new Semaphore(1);
        Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());
    }

    /**
     * This method will prepare the song with the URL to play and after preparing starts playing it.
     *
     * @param uri URL of the ressource to play.
     * @throws PlaybackException In case of error the Exception is thrown
     */
    public void play(String uri) throws PlaybackException {
        play(uri, 0);
    }

    /**
     * Initializes the first mediaplayers with uri and prepares it so it can get
     * started
     *
     * @param uri - Path to media file
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IllegalStateException
     * @throws IOException
     */
    public synchronized void play(String uri, int jumpTime) throws PlaybackException {
        // Another player currently exists, remove it.
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.reset();
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = null;
        }

        // Create new MediaPlayer object.
        mCurrentMediaPlayer = new MediaPlayer();
        mCurrentPrepared = false;

        // Set the type of the stream to music.
        mCurrentMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the datasource of the player to the provider URI
            uri = FormatHelper.encodeFileURI(uri);
            mCurrentMediaPlayer.setDataSource(uri);
        } catch (IllegalArgumentException e) {
            throw new PlaybackException(REASON.ArgumentError);
        } catch (SecurityException e) {
            throw new PlaybackException(REASON.SecurityError);
        } catch (IllegalStateException e) {
            throw new PlaybackException(REASON.StateError);
        } catch (IOException e) {
            throw new PlaybackException(REASON.IOError);
        }

        // Save parameters for later usage
        mPrimarySource = uri;

        // Set a listener for completion of playback
        mCurrentMediaPlayer.setOnCompletionListener(new TrackCompletionListener());

        // Set a listener for the prepare complete event. This can then start the playback.
        mCurrentMediaPlayer.setOnPreparedListener(mPrimaryPreparedListener);

        // Save the requested seek time
        mPrepareTime = jumpTime;

        // Start the prepare procedure of the MediaPlayer. This happens asynchronously so a the callback
        // above is required.
        mCurrentMediaPlayer.prepareAsync();
    }

    /**
     * Pauses the currently running mediaplayer If already paused it continues
     * the playback
     */
    public synchronized void togglePause() {
        // Check if a Mediaplayer exits and if it is actual playing
        if (mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying()) {
            // In this case pause the playback
            mCurrentMediaPlayer.pause();
        } else if (mCurrentMediaPlayer != null && mCurrentPrepared) {
            // If a MediaPlayer exists and is also prepared the toggle command should start playback.
            mCurrentMediaPlayer.start();

            // Enable wakelock during playback.
            mCurrentMediaPlayer.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }

    }

    /**
     * Just pauses currently running player
     */
    public synchronized void pause() {
        // Check if a MediaPlayer exits and if it is actual playing
        if (mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying()) {
            mCurrentMediaPlayer.pause();
        }
    }

    /**
     * Resumes playback
     */
    public synchronized void resume() {
        // If a MediaPlayer exists and is also prepared this command should start playback.
        if (mCurrentMediaPlayer != null && mCurrentPrepared) {
            mCurrentMediaPlayer.start();
        }
    }

    /**
     * Stops media playback
     */
    public synchronized void stop() {
        // Check if a player exists otherwise there is nothing to do.
        if (mCurrentMediaPlayer != null) {
            // Check if the player for the next song exists already
            if (mNextMediaPlayer != null) {
                // Remove the next player from the currently playing one.
                mCurrentMediaPlayer.setNextMediaPlayer(null);
                // Release the MediaPlayer, not usable after this command
                mNextMediaPlayer.release();

                // Reset variables to clean internal state
                mNextMediaPlayer = null;
                mSecondPrepared = false;
                mSecondPreparing = false;
            }

            // Check if the currently active player is ready
            if (mCurrentPrepared) {
                /*
                 * Signal android desire to close audio effect session
                 */
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mCurrentMediaPlayer.getAudioSessionId());
                audioEffectIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mPlaybackService.getPackageName());
                mPlaybackService.sendBroadcast(audioEffectIntent);
                Log.v(TAG,"Closing effect for session: " + mCurrentMediaPlayer.getAudioSessionId());
            }
            // Release the current player
            mCurrentMediaPlayer.release();

            // Reset variables to clean internal state
            mCurrentMediaPlayer = null;
            mCurrentPrepared = false;
        }
    }

    /**
     * Seeks the currently playing track to the requested position. Bounds/state check are done.
     *
     * @param position Position in milliseconds to seek to.
     */
    public synchronized void seekTo(int position) {
        try {
            // Check if the MediaPlayer is in a valid state to seek and the requested position is within bounds
            if (mCurrentMediaPlayer != null && mCurrentPrepared && position < mCurrentMediaPlayer.getDuration()) {
                mCurrentMediaPlayer.seekTo(position);
            }
        } catch (IllegalStateException exception) {
            Log.e(TAG, "Illegal state during seekTo");
        }
    }

    /**
     * Returns the position of the currently playing track in milliseconds.
     *
     * @return Position of the currently playing track in milliseconds. 0 if not playing.
     */
    public synchronized int getPosition() {
        try {
            // State checks for the MediaPlayer, only request time if object exists and the player is prepared.
            if (mCurrentMediaPlayer != null && mCurrentPrepared) {
                return mCurrentMediaPlayer.getCurrentPosition();
            }
        } catch (IllegalStateException exception) {
            Log.e(TAG, "Illegal state during CurrentPositon");
            return 0;
        }
        return 0;
    }

    /**
     * Returns the duration of the current track
     *
     * @return Duration of the currently playing track in milliseconds. 0 if not playing.
     */
    public synchronized int getDuration() {
        try {
            // State checks for the MediaPlayer, only request time if object exists and the player is prepared.
            if (mCurrentMediaPlayer != null && mCurrentPrepared) {
                return mCurrentMediaPlayer.getDuration();
            }
        } catch (IllegalStateException exception) {
            Log.e(TAG, "Illegal state during CurrentPositon");
            return 0;
        }
        return 0;
    }

    /**
     * Checks if this player is currently running
     *
     * @return True if the player actually plays a track, false otherwise.
     */
    public synchronized boolean isRunning() {
        if (mCurrentMediaPlayer != null) {
            return mCurrentMediaPlayer.isPlaying();
        }
        return false;
    }

    /**
     * Checks if the first player is prepared.
     *
     * @return True if prepared and ready to play, false otherwise.
     */
    public synchronized boolean isPrepared() {
        return mCurrentMediaPlayer != null && mCurrentPrepared;
    }

    /**
     * Sets the volume of the currently playing MediaPlayer
     *
     * @param leftChannel  Volume from 0.0 - 1.0 for left playback channel
     * @param rightChannel Volume from 0.0 - 1.0 for right playback channel
     */
    public synchronized void setVolume(float leftChannel, float rightChannel) {
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.setVolume(leftChannel, rightChannel);
        }
    }

    /**
     * Sets next MediaPlayer to uri and start preparing it. If next MediaPlayer
     * was already initialized it gets reset
     *
     * @param uri URI of the next song to play.
     */
    public synchronized void setNextTrack(String uri) throws PlaybackException {
        // Reset the prepared state of the second mediaplayer
        mSecondPrepared = false;

        // If the current MediaPlayer is not already set, this should not be called. Wait for
        // prepare finish then.
        if (mCurrentMediaPlayer == null) {
            // This call makes absolutely no sense at this point so abort
            throw new PlaybackException(REASON.StateError);
        }
        // Next mediaplayer already set, clear it first.
        if (mNextMediaPlayer != null) {
            // Remove this player from the currently active one as a next one
            mCurrentMediaPlayer.setNextMediaPlayer(null);
            // Release the player that is not needed any longer
            mNextMediaPlayer.release();

            // Reset internal state variables
            mNextMediaPlayer = null;
            mSecondPrepared = false;
            mSecondPreparing = false;
        }

        // Check if the uri contains something
        if (uri != null && !uri.isEmpty()) {
            // Create a new MediaPlayer to prepare as next song playback
            mNextMediaPlayer = new MediaPlayer();

            // Set the prepare finished listener
            mNextMediaPlayer.setOnPreparedListener(mSecondaryPreparedListener);

            // Set the playback type to music again
            mNextMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                // Try setting the data source
                uri = FormatHelper.encodeFileURI(uri);
                mNextMediaPlayer.setDataSource(uri);
            } catch (IllegalArgumentException e) {
                throw new PlaybackException(REASON.ArgumentError);
            } catch (SecurityException e) {
                throw new PlaybackException(REASON.SecurityError);
            } catch (IllegalStateException e) {
                throw new PlaybackException(REASON.StateError);
            } catch (IOException e) {
                throw new PlaybackException(REASON.IOError);
            }

            // Save the uri for latter usage
            mSecondarySource = uri;

            // Check if primary is prepared before preparing the second one.
            try {
                mSecondPreparingLock.acquire();
            } catch (InterruptedException e) {
                throw new PlaybackException(REASON.StateError);
            }

            // If the first MediaPlayer is prepared already just start the second prepare here.
            if (mCurrentPrepared) {
                mSecondPreparing = true;
                mNextMediaPlayer.prepareAsync();
            }
            mSecondPreparingLock.release();
        }
    }

    private OnPreparedListener mPrimaryPreparedListener = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            // Sequentially execute all critical operations on the MP objects
            synchronized (GaplessPlayer.this) {
                // Check if the callback happened from the current media player
                if (!mp.equals(mCurrentMediaPlayer)) {
                    return;
                }
                // If mp equals currentMediaPlayback it should start playing
                mCurrentPrepared = true;

                // only start playing if its desired

                // Check if an immediate jump is requested
                if (mPrepareTime > 0) {
                    mp.seekTo(mPrepareTime);
                    mPrepareTime = 0;
                }
                mp.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mp.start();

                /*
                 * Signal audio effect desire to android
                 */
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mp.getAudioSessionId());
                audioEffectIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mPlaybackService.getPackageName());
                audioEffectIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                mPlaybackService.sendBroadcast(audioEffectIntent);
                mp.setAuxEffectSendLevel(1.0f);

                // Notify connected listeners
                for (OnTrackStartedListener listener : mTrackStartListeners) {
                    listener.onTrackStarted(mPrimarySource);
                }

                try {
                    mSecondPreparingLock.acquire();
                } catch (InterruptedException e) {
                    // FIXME some handling? Not sure if necessary
                }

                // If second MediaPlayer exists and is not already prepared and not already preparing
                // Start preparing the second MP here.
                if (!mSecondPrepared && mNextMediaPlayer != null && !mSecondPreparing) {
                    mSecondPreparing = true;
                    // Delayed initialization second mediaplayer
                    mNextMediaPlayer.prepareAsync();
                }
                mSecondPreparingLock.release();
            }
        }
    };

    private OnPreparedListener mSecondaryPreparedListener = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            // Sequentially execute all critical operations on the MP objects
            synchronized (GaplessPlayer.this) {
                // Check if the callback happened from the current second media player, it can
                // happen that callbacks are called when the MP is no longer relevant, abort then.
                if (!mp.equals(mNextMediaPlayer) && !mp.equals(mCurrentMediaPlayer)) {
                    return;
                }

                if (mp == mCurrentMediaPlayer) {
                    // MediaPlayer got primary MP before finishing preparing, start playback
                    // Workaround for issue #48

                    // Enable wakelock
                    mp.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

                    // Playback start
                    mCurrentMediaPlayer.start();

                    /*
                     * Signal audio effect desire to android
                     */
                    Intent audioEffectOpenIntent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                    audioEffectOpenIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mp.getAudioSessionId());
                    audioEffectOpenIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mPlaybackService.getPackageName());
                    audioEffectOpenIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                    Log.v(TAG,"Opening effect for session: " + mp.getAudioSessionId());
                    mPlaybackService.sendBroadcast(audioEffectOpenIntent);
                    mCurrentMediaPlayer.setAuxEffectSendLevel(1.0f);


                    // Notify connected listeners that playback has started
                    for (OnTrackStartedListener listener : mTrackStartListeners) {
                        listener.onTrackStarted(mPrimarySource);
                    }
                } else {
                    // Normal case. Second is prepared while primary MP is playing.
                    // Set as next player


                    // Second MediaPlayer is now ready to be used and can be set as a next MediaPlayer to the current one
                    mSecondPreparing = false;

                    // If it is nextMediaPlayer it should be set for currentMP
                    mp.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

                    // Set the internal state
                    mSecondPrepared = true;

                    // Set this now prepared MediaPlayer as the next one
                    mCurrentMediaPlayer.setNextMediaPlayer(mp);
                }
            }
        }
    };

    // Notification for Services using GaplessPlayer
    public interface OnTrackFinishedListener {
        void onTrackFinished();
    }

    public interface OnTrackStartedListener {
        void onTrackStarted(String URI);
    }


    /**
     * Registers a listener to this class to be notified when a track finishes playback
     *
     * @param listener Listener to register
     */
    public void setOnTrackFinishedListener(OnTrackFinishedListener listener) {
        mTrackFinishedListeners.add(listener);
    }

    /**
     * Removes a track finish listener from this class.
     *
     * @param listener Listener to remove from list
     */
    public void removeOnTrackFinishedListener(OnTrackFinishedListener listener) {
        mTrackFinishedListeners.remove(listener);
    }


    /**
     * Registers a track start listener to this class (Called when a MediaPlayer is prepared and started)
     *
     * @param listener Listener to register
     */
    public void setOnTrackStartListener(OnTrackStartedListener listener) {
        mTrackStartListeners.add(listener);
    }

    /**
     * Removes a track start listener from this class.
     *
     * @param listener Listener to remove from list
     */
    public void removeOnTrackStartListener(OnTrackStartedListener listener) {
        mTrackStartListeners.remove(listener);
    }


    /**
     * This listener will handle callbacks when a track finishes playback
     */
    private class TrackCompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            // Sequentially execute all critical operations on the MP objects
            synchronized (GaplessPlayer.this) {
                // Reset the current MediaPlayer variable
                mCurrentMediaPlayer = null;

                // Notify connected listeners that the last track is now finished
                for (OnTrackFinishedListener listener : mTrackFinishedListeners) {
                    listener.onTrackFinished();
                }

                int audioSessionID = mp.getAudioSessionId();

                mp.release();


                // Set current MP to next MP if one is ready
                if (mNextMediaPlayer != null && (mSecondPrepared || mSecondPreparing)) {
                    // Next media player should now be playing already, so make this the current one.
                    mCurrentMediaPlayer = mNextMediaPlayer;
                    // Register this listener to the now playing MediaPlayer also
                    mCurrentMediaPlayer.setOnCompletionListener(new TrackCompletionListener());

                    // Move the second to primary source (URI)
                    mPrimarySource = mSecondarySource;

                    // Reset the now obsolete second MediaPlayer state variables
                    mSecondarySource = null;
                    mNextMediaPlayer = null;

                    if (mSecondPrepared) {
                        // Notify connected listeners that playback has started
                        for (OnTrackStartedListener listener : mTrackStartListeners) {
                            listener.onTrackStarted(mPrimarySource);
                        }
                    }
                } else {
                    /*
                    * Signal android desire to close audio effect session
                    */
                    Intent audioEffectIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                    audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionID);
                    audioEffectIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mPlaybackService.getPackageName());
                    Log.v(TAG,"Closing effect for session: " + audioSessionID);
                    mPlaybackService.sendBroadcast(audioEffectIntent);
                }
            }
        }
    }


    /**
     * Exception class used to signal playback errors
     */
    public class PlaybackException extends Exception {

        REASON mReason;

        public PlaybackException(REASON reason) {
            mReason = reason;
        }

        public REASON getReason() {
            return mReason;
        }
    }

    /**
     * Returns whether {@link GaplessPlayer} is active or inactive so it can receive commands
     *
     * @return True if this class is busy and false if it is not doing important work.
     */
    public synchronized boolean getActive() {
        if (mSecondPreparing) {
            return true;
        } else if (!mCurrentPrepared && (mCurrentMediaPlayer != null)) {
            return true;
        }

        return false;
    }

}
