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

package org.gateshipone.odyssey.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.gateshipone.odyssey.R;

public class PreferenceHelper {
    public enum LIBRARY_TRACK_CLICK_ACTION {
        ACTION_ADD_SONG,
        ACTION_PLAY_SONG,
        ACTION_PLAY_SONG_NEXT,
        ACTION_CLEAR_AND_PLAY,
    }

    public static LIBRARY_TRACK_CLICK_ACTION getClickAction(SharedPreferences prefs, Context context) {
        String clickActionPref = prefs.getString(context.getString(R.string.pref_library_click_action_key), context.getString(R.string.pref_library_click_action_default));
        if (clickActionPref.equals(context.getString(R.string.pref_library_click_action_add_key))) {
            return LIBRARY_TRACK_CLICK_ACTION.ACTION_ADD_SONG;
        } else if (clickActionPref.equals(context.getString(R.string.pref_library_click_action_play_key))) {
            return LIBRARY_TRACK_CLICK_ACTION.ACTION_PLAY_SONG;
        } else if (clickActionPref.equals(context.getString(R.string.pref_library_click_action_play_next_key))) {
            return LIBRARY_TRACK_CLICK_ACTION.ACTION_PLAY_SONG_NEXT;
        } else if (clickActionPref.equals(context.getString(R.string.pref_library_click_action_clear_and_play))) {
            return LIBRARY_TRACK_CLICK_ACTION.ACTION_CLEAR_AND_PLAY;
        }
        return LIBRARY_TRACK_CLICK_ACTION.ACTION_ADD_SONG;
    }
}
