/*
 * Copyright (C) 2019 Team Gateship-One
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

package org.gateshipone.odyssey.models;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/**
 * This class keeps a HashMap of all artists that are part of a track list (e.g. playlist)
 * and their belonging tracks with the original list position as a pair. This can be used to
 * randomize the playback of the playback equally distributed over all artists of the original
 * track list.
 */
public class ArtistsTrackBuckets {
    private static final String TAG = ArtistsTrackBuckets.class.getSimpleName();

    /**
     * Underlying data structure for artist-track buckets
     */
    private LinkedHashMap<String, List<Pair<Integer, TrackModel>>> mData;

    /**
     * Creates an empty data structure
     */
    public ArtistsTrackBuckets() {
        mData = new LinkedHashMap<>();
    }

    private BetterPseudoRandomGenerator mRandomGenerator = new BetterPseudoRandomGenerator();

    /**
     * Creates a list of artists and their tracks with position in the original playlist
     *
     * @param tracks List of tracks
     */
    public synchronized void fillFromList(List<TrackModel> tracks) {
        // Clear all entries
        mData.clear();
        if (tracks == null || tracks.isEmpty()) {
            // Abort for empty data structures
            return;
        }

        // Iterate over the list and add all tracks to their artist lists
        int trackNo = 0;
        for (TrackModel track : tracks) {
            String artistName = track.getTrackArtistName();
            List<Pair<Integer, TrackModel>> list = mData.get(artistName);
            if (list == null) {
                // If artist is not already in HashMap add a new list for it
                list = new ArrayList<>();
                mData.put(artistName, list);
            }
            // Add pair of position in original playlist and track itself to artists bucket list
            list.add(new Pair<>(trackNo, track));

            // Increase the track number (index) of the original playlist
            trackNo++;
        }
        Log.v(TAG,"Recreated buckets with: " + mData.size() + " artists");
    }

    /**
     * Generates a randomized track number within the original track list, that was used for the call
     * of fillFromList. The random track number should be equally distributed over all artists.
     *
     * @return A random number of a track of the original track list
     */
    public synchronized int getRandomTrackNumber() {
        // First level random, get artist
        int randomArtistNumber = mRandomGenerator.getLimitedRandomNumber(mData.size());

        // Get artists bucket list to artist number
        List<Pair<Integer, TrackModel>> artistsTracks;

        // Get the artist at the position of randomArtistNumber. Iterate until the position is reached
        int compareArtistNumber = 0;
        Iterator<List<Pair<Integer, TrackModel>>> listIterator = mData.values().iterator();
        while (listIterator.hasNext() && (compareArtistNumber++ != randomArtistNumber)) {
            listIterator.next();
        }
        // Get the list of tracks belonging to the selected artist
        artistsTracks = listIterator.next();

        // Check if an artist was found
        if (artistsTracks == null) {
            return 0;
        }

        int randomTrackNo = mRandomGenerator.getLimitedRandomNumber(artistsTracks.size());

        Pair<Integer,TrackModel> pair = artistsTracks.get(randomTrackNo);

        Log.v(TAG,"Selected artist no.: " + randomArtistNumber + " with internal track no.: " + randomTrackNo + " and original track no.: " + pair.first );
        // Get random track number
        return pair.first;
    }

    private class BetterPseudoRandomGenerator {
        private static final int POOL_SIZE = 100;
        private Random mJavaGenerator;

        private int[] mRandomPool = new int[POOL_SIZE];

        private int mInternalSeed;

        public BetterPseudoRandomGenerator() {
            mJavaGenerator = new Random();

            // Initialize internal seed
            mInternalSeed = mJavaGenerator.nextInt();

            randomizePool();

            // Do a quick check
            //testDistribution(20,10000);
        }

        private int getInternalRandomNumber() {
            /*
             * Marsaglia, "Xorshift RNGs"
             */
            int newSeed = mInternalSeed;

            newSeed ^= newSeed << 13;
            newSeed ^= newSeed >> 17;
            newSeed ^= newSeed << 5;

            mInternalSeed = newSeed;
            return Math.abs(newSeed);
        }

        int getLimitedRandomNumber(int limit) {
            int position = mJavaGenerator.nextInt(POOL_SIZE);
            int returnValue = mRandomPool[position] % limit;

            mRandomPool[position] = getInternalRandomNumber();

            return returnValue;
        }

        private void randomizePool() {
            for (int i = 0; i < POOL_SIZE; i++) {
                mRandomPool[i] = getInternalRandomNumber();
            }
        }

        private void testDistribution(int numberLimit, int runs) {
            int numberCount[] = new int[numberLimit];

            for (int i = 0; i < runs; i++) {
                numberCount[getLimitedRandomNumber(numberLimit)]++;
            }

            // Print distribution
            for (int i = 0; i < numberLimit; i++) {
                Log.v(TAG,"Number: " + i + " = " + numberCount[i]);
            }
        }
    }
}
