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

package org.gateshipone.odyssey.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.gateshipone.odyssey.R;

public class MusicDatabaseFactory {
    static MusicDatabase mDatabaseInstance = null;
    static String mLastDBProvider = null;

    synchronized public static MusicDatabase getDatabase(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String provider = sharedPref.getString(context.getString(R.string.pref_library_provider_key), context.getString(R.string.pref_library_provider_default));

        if (!provider.equals(mLastDBProvider) || mDatabaseInstance == null) {
            // Provider changed or was not set before
            // FIXME with real instances
            if (provider.equals(context.getString(R.string.pref_library_provider_android_key))) {

            } else if (provider.equals(context.getString(R.string.pref_library_provider_dummy_key))) {

            } else if (provider.equals(context.getString(R.string.pref_library_provider_internal_key))) {

            }
        }

        return mDatabaseInstance;
    }
}
