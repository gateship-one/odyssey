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

package org.gateshipone.odyssey.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import org.gateshipone.odyssey.BuildConfig;
import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.dialogs.LicensesDialog;
import org.gateshipone.odyssey.utils.ThemeUtils;

public class OdysseyAboutActivity extends GenericActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_odyssey_about);

        String versionName = " " + BuildConfig.VERSION_NAME;

        ((TextView)findViewById(R.id.activity_about_version)).setText(versionName);
        getWindow().setStatusBarColor(ThemeUtils.getThemeColor(this,R.attr.odyssey_color_primary_dark));

        findViewById(R.id.odyssey_contributors).setOnClickListener(view -> {
            Intent myIntent = new Intent(OdysseyAboutActivity.this, OdysseyContributorsActivity.class);

            startActivity(myIntent);
        });

        findViewById(R.id.odyssey_thirdparty_licenses)
                .setOnClickListener(view -> LicensesDialog.newInstance().show(getSupportFragmentManager(), "LicensesDialog"));

        findViewById(R.id.logo_musicbrainz).setOnClickListener(view -> {
            Intent urlIntent = new Intent(Intent.ACTION_VIEW);
            urlIntent.setData(Uri.parse(getString(R.string.url_musicbrainz)));
            startActivity(urlIntent);
        });

        findViewById(R.id.logo_lastfm).setOnClickListener(view -> {
            Intent urlIntent = new Intent(Intent.ACTION_VIEW);
            urlIntent.setData(Uri.parse(getString(R.string.url_lastfm)));
            startActivity(urlIntent);
        });

        findViewById(R.id.logo_fanarttv).setOnClickListener(view -> {
            Intent urlIntent = new Intent(Intent.ACTION_VIEW);
            urlIntent.setData(Uri.parse(getString(R.string.url_fanarttv)));
            startActivity(urlIntent);
        });
    }

    @Override
    void onServiceConnected() {

    }

    @Override
    void onServiceDisconnected() {

    }
}
