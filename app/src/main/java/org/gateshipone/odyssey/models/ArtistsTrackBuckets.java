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

public class ArtistsTrackBuckets extends LinkedHashMap<String, List<Pair<Integer, TrackModel>>> {
    private static final String TAG = ArtistsTrackBuckets.class.getSimpleName();

    private final Random mRandomGenerator = new Random();
    public void fillFromList(List <TrackModel> tracks) {
        // Clear all entries
        clear();

        // Iterate over the list and add all tracks to their artist lists
        int trackNo = 0;
        for (TrackModel track: tracks) {
            String artistName = track.getTrackArtistName();
            List<Pair<Integer, TrackModel>> list = get(artistName);
            if (list == null) {
                // If artist is not already in HashMap add a new list for it
                list = new ArrayList<>();
                put(artistName, list);
            }
            // Add pair of position in original playlist and track itself to artists bucket list
            list.add(new Pair<>(trackNo, track));

            trackNo++;
        }

        Log.v(TAG,"Artists found: " + size());
    }

    public int getRandomTrackNumber() {
        int returnValue = 0;

        // First level random, get artist
        int randomArtistNumber = mRandomGenerator.nextInt(size());

        // Get artists bucket list to artist number
        List<Pair<Integer, TrackModel>> artistsTracks;


        // Check if there is a better way to get artist tracks from number (array access?)
        int compareArtistNumber = 0;
        Iterator<List<Pair<Integer, TrackModel>>> listIterator = values().iterator();
        while (listIterator.hasNext() && (compareArtistNumber++ != randomArtistNumber)) {
            listIterator.next();
        }
        artistsTracks = listIterator.next();


        if (artistsTracks == null) {
            return 0;
        }

        // Get random track number
        returnValue = artistsTracks.get(mRandomGenerator.nextInt(artistsTracks.size())).first;
        return returnValue;
    }
}
