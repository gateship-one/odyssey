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

import androidx.annotation.NonNull;

import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

/**
 * Message object which get passed between PlaybackServiceInterface ->
 * PlaybackServiceHandler
 */
public class ControlObject {

    public enum PLAYBACK_ACTION {
        PLAY,
        TOGGLE_PAUSE,
        NEXT,
        PREVIOUS,
        SEEK_TO,
        JUMP_TO,
        REPEAT,
        RANDOM,
        ENQUEUE_TRACK,
        PLAY_TRACK,
        DEQUEUE_TRACK,
        DEQUEUE_TRACKS,
        PLAY_ALL_TRACKS,
        RESUME_BOOKMARK,
        DELETE_BOOKMARK,
        CREATE_BOOKMARK,
        SAVE_PLAYLIST,
        CLEAR_PLAYLIST,
        SHUFFLE_PLAYLIST,
        ENQUEUE_PLAYLIST,
        PLAY_PLAYLIST,
        ENQUEUE_FILE,
        PLAY_FILE,
        PLAY_DIRECTORY,
        ENQUEUE_DIRECTORY_AND_SUBDIRECTORIES,
        PLAY_DIRECTORY_AND_SUBDIRECTORIES,
        ENQUEUE_ALBUM,
        PLAY_ALBUM,
        ENQUEUE_RECENT_ALBUMS,
        PLAY_RECENT_ALBUMS,
        ENQUEUE_ARTIST,
        PLAY_ARTIST,
        START_SLEEP_TIMER,
        CANCEL_SLEEP_TIMER,
        SET_SMART_RANDOM
    }

    private final PLAYBACK_ACTION action;
    private final TrackModel track;
    private final PlaylistModel playlist;

    private final Queue<Integer> intValues;
    private final Queue<Long> longValues;
    private final Queue<Boolean> boolValues;
    private final Queue<String> stringValues;

    private ControlObject(PLAYBACK_ACTION action,
                          TrackModel track,
                          PlaylistModel playlist,
                          Queue<Integer> intValues,
                          Queue<Long> longValues,
                          Queue<Boolean> boolValues,
                          Queue<String> stringValues) {
        this.action = action;
        this.track = track;
        this.playlist = playlist;
        this.intValues = intValues;
        this.longValues = longValues;
        this.boolValues = boolValues;
        this.stringValues = stringValues;
    }

    public PLAYBACK_ACTION getAction() {
        return action;
    }

    @NonNull
    public Boolean nextBool() {
        return Objects.requireNonNull(boolValues.poll());
    }

    @NonNull
    public Integer nextInt() {
        return Objects.requireNonNull(intValues.poll());
    }

    @NonNull
    public Long nextLong() {
        return Objects.requireNonNull(longValues.poll());
    }

    @NonNull
    public String nextString() {
        return Objects.requireNonNull(stringValues.poll());
    }

    public TrackModel getTrack() {
        return this.track;
    }

    public PlaylistModel getPlaylist() {
        return this.playlist;
    }

    public static class Builder {

        private final PLAYBACK_ACTION action;

        private final Queue<Integer> intValues;
        private final Queue<Long> longValues;
        private final Queue<Boolean> boolValues;
        private final Queue<String> stringValues;

        private TrackModel track;

        private PlaylistModel playlist;

        public Builder(PLAYBACK_ACTION action) {
            this.action = action;
            intValues = new ArrayDeque<>();
            longValues = new ArrayDeque<>();
            boolValues = new ArrayDeque<>();
            stringValues = new ArrayDeque<>();
        }

        public Builder addInt(int value) {
            intValues.offer(value);
            return this;
        }

        public Builder addLong(long value) {
            longValues.offer(value);
            return this;
        }

        public Builder addBool(boolean value) {
            boolValues.offer(value);
            return this;
        }

        public Builder addString(String value) {
            stringValues.offer(value);
            return this;
        }

        public Builder addTrack(TrackModel track) {
            this.track = track;
            return this;
        }

        public Builder addPlaylist(PlaylistModel playlist) {
            this.playlist = playlist;
            return this;
        }

        public ControlObject build() {
            return new ControlObject(
                    this.action,
                    this.track,
                    this.playlist,
                    this.intValues,
                    this.longValues,
                    this.boolValues,
                    this.stringValues);
        }
    }
}
