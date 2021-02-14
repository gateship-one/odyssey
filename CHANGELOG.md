# Changelog Odyssey

## Version: 1.1.21 Tag: release-33 (2021-02-13)

* Add Indonesian translation
* Update French translation
* Fix creating a new playlists if mutliple already exists ([Issue #213](https://github.com/gateship-one/odyssey/issues/213))

## Version: 1.1.20 Tag: release-32 (2020-10-11)

* Add dutch translation (thanks to Harry van der Wolf)
* Update french translation
* Preserve search query between views ([Issue #199](https://github.com/gateship-one/odyssey/issues/199))
* Exclude artworks from backups
* Upgrade to Android 10
  * If your device is running Android 10 and you detect any bugs compared to the prior version please let us know via GitHub or by Email
* Changes related to Android 10
  * Playlists will now be stored internal (due to issues with Android 10)
  * Add option to sort by filename (due to occasional broken sorting with Android 10 [#192](https://github.com/gateship-one/odyssey/issues/192))

## Version: 1.1.19 Tag: release-31 (2020-03-02)

* further improvements for the artwork retrival
* add czech translation
* update french translation
* improve widget behaviour

## Version: 1.1.18 Tag: release-30 (2019-06-05)

* replace random with smart random feature
* improve artwork retrival
* remove Last.fm as artist image provider
* fix several backnavigation issues
* add support for local album images ([Issue #99](https://github.com/gateship-one/odyssey/issues/99))
* fix several AndroidX issues

## Version: 1.1.17 Tag: release-29 (2019-02-07)

* updated french translation
* optional experimental smart random feature
* migrate to AndroidX
* fix possible image scaling error
* add automatic backwards seek on resume
* add sleep timer option to stop after current track

## Version: 1.1.16 Tag: release-28 (2018-11-10)

* optional sleep timer ([Issue #149](https://github.com/gateship-one/odyssey/issues/149))
* fix possible deadlock in the search function
* updated french translation
* fix ripple effect in the settings
* fix crash for files containing colons in the filename ([Issue #167](https://github.com/gateship-one/odyssey/issues/167))
* fix files with colons in the filename couldn't be played

## Version: 1.1.15 Tag: release-27 (2018-07-04)

* improved scrolling behaviour for AlbumTracks and ArtistAlbums
* further stability fixes for the playback connection
* option to show the playlist when open by widget or notification and playback is active
* improved playlist parsing (.m3u, .pls)
* remove trailing whitespaces in search ([Issue #153](https://github.com/gateship-one/odyssey/issues/153))
* optional private notification (thanks to Dennis Guse)

## Version: 1.1.14 Tag: release-26 (2018-04-09)

* updated french and italian translations
* support .m4b files in the filebrowser (playback may not be supported by all devices)
* further stability fixes for the playback connection
* improved behaviour when odyssey is opened by notification

## Version: 1.1.13 Tag: release-25 (2018-02-14)

* add replace and play from index as additional default action for library tracks
* fix not playing tracks from the filebrowser properly
* fix moving to the last song of a playlist file (.m3u, .pls)
* fix bitmap scaling in gridview
* clear bitmap cache properly after remove artwork image
* prevent crashes after device rotation

## Version: 1.1.12 Tag: release-24 (2018-02-03)

* Default action for library tracks selectable (play song, add song (default), add as next song)
* State saving for android 8 fixed
* Updated french and italian translations
* Playbackservice fixes (disappearing playlist)
* linked FAQ

## Version: 1.1.11 Tag: release-23 (2018-01-10)

* Fix crash when choosing 'Play after current song' with an empty playlist
* Fix possible problem in cover loading process

## Version: 1.1.10 Tag: release-22 (2017-12-31)

* Minor bug fixes
* Restructure cover art storage to support larger images (previous downloaded cover art will be lost)
* Improve image processing (scaling and caching)
* Happy new year!

## Version: 1.1.9 Tag: release-21 (2017-12-16)

* Add polish translation (thanks to Daria Szatan)
* Update of french translation (thanks to Kévin Minions)
* Change music volume by hardware controls if Odyssey is in foreground
* minor UI and performance tweaks

## Version: 1.1.8 Tag: release-20 (2017-11-13)

* Hotfix for: https://issuetracker.google.com/issues/64434571

## Version: 1.1.7 Tag: release-19 (2017-11-05)

* Add ability to open music files from a file browser ([Issue #41](https://github.com/gateship-one/odyssey/issues/41), [Issue #58](https://github.com/gateship-one/odyssey/issues/58))
* Improve playback speed if started from file browser ([Issue #53](https://github.com/gateship-one/odyssey/issues/53))
* Only play search results ([Issue #94](https://github.com/gateship-one/odyssey/issues/94))
* New sdk version (26)
* Adaptive icon
* Fix sorting of recent albums

## Version: 1.1.6 Tag: release-18 (2017-07-25)

* Allow horizontal resizing of homescreen widget
* Experimental support for .m3u and .pls files
* Fix equalizer issues ([Issue #69](https://github.com/gateship-one/odyssey/issues/69))

## Version: 1.1.5 Tag: release-17 (2017-05-17)

* Add recent albums view
* Fix equalizer issues ([Issue #61](https://github.com/gateship-one/odyssey/issues/61))
* Improve placeholder for cover (thanks to mray)
* Remove sections from playlist
* Filtering fixes

## Version: 1.1.4 Tag: release-16 (2017-02-24)

* Optionally show artist image alternating to cover image in NowPlayingView
* Option to reload album / artist image in the corresponing fragments
* Fixes in FileBrowser when filtering is active
* Non music filtering
* Song sharing in NowPlayingView
* Add option to remove an entire section from the current playlist
* Show disc number if album contains more than one disc
* Add french translation (thanks to Kévin Minions)
* Fix rare GaplessPlayer crash ([Issue #48](https://github.com/gateship-one/odyssey/issues/48))

## Version: 1.1.3 Tag: release-15 (2017-01-23)

* Add option to trigger a mediascan in the file browser for the current folder.
* Quicklinks to open Wikipedia page for Album & Artists in the NowPlayingView menu.

## Version: 1.1.2 Tag: release-14 (2017-01-10)

* Save search string on device rotation.
* Add option to set a default directory for the file browser.
* Don't show directories that contain .nomedia files in the file browser.
* Fix multiple UI issues.
* Add categories for the settings.
* Add option to hide artworks.
* Swipe NowPlayingView up on first play to notify user about the view.

## Version: 1.1.1 Tag: release-13 (2016-12-30)

* Hotfix for crash if not connected to any network.

## Version: 1.1.0 Tag: release-12 (2016-12-22)

* Artwork support with MusicBrainz, Last.fm, Fanart.tv as artwork provider (album, artist images)
* Album images in playlist view as sections
* Tablet optimized nowplaying screen
* Listviews for artists / albums (optional)
* Fix multiple UI issues
* Unify settings (your personal settings will be deleted)
* Add italian translation

## Version: 1.0.11 Tag: release-11 (2016-11-05)

* add new themes (including light and oled themes)
* support .opus files in the filebrowser (playback may not be supported by all devices)
* extract ID tags if the track is not in the android media database

## Version: 1.0.10 Tag: release-10 (2016-10-27)

* Bugfix playing files containing special characters like #,% etc.

## Version: 1.0.9 Tag: release-9 (2016-10-24)

* Files with ":" in filename can now be played.
* Track duration for tracks not part of the system database show the correct track duration during playback.

## Version: 1.0.8 Tag: release-8 (2016-10-22)

* crash fixes in Album/Artistmodels
* change version scheme to x.y.z

## Version: 1.0 Tag: release-7 (2016-10-20)

* crash fix in storage selection

## Version: 1.0 Tag: release-6 (2016-10-07)

* fix crash reasons on devices with Android 5.0

## Version: 1.0 Tag: release-5 (2016-10-03)

* fix crash during playlist shuffling
* change behaviour of alltracks playbutton to play all tracks alphabetically

## Version: 1.0 Tag: release-4 (2016-10-02)

## Version: 1.0 Tag: release-3 (2016-09-24)

* optimize memory consumption
* UI fixes

## Version: 1.0 (2016-09-11)

* add error dialog for missing equalizer
* file browser: play file on click
* save dialog: clear field on first click

## Version: 1.0 (2016-09-09)

* initial release
