/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.finalstream.linearaudioplayer.audio;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import net.finalstream.linearaudioplayer.bean.AudioItemBean;

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a random song, with its title and URI, upon
 * request.
 */
public class MusicRetriever {
    final String TAG = "LINEARMusicRetriever";

    ContentResolver mContentResolver;

    // the items (songs) we have queried
    List<AudioItemBean> mItems = new ArrayList<AudioItemBean>();

    Random mRandom = new Random();

    public List<AudioItemBean> getItems() {
    	return mItems;
    }
    
    public MusicRetriever(ContentResolver cr) {
        mContentResolver = cr;
    }

    /**
     * Loads music data. This method may take long, so be sure to call it asynchronously without
     * blocking the main thread.
     */
    public void prepare() {
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.i(TAG, "Querying media...");
        Log.i(TAG, "URI: " + uri.toString());

        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        // TODO: IS_MUSIC=1Ç≈ÇµÇ⁄ÇÈÇ©åüì¢Ç∑ÇÈÅB
        Cursor cur = mContentResolver.query(uri, 
        		new String[] {MediaStore.Audio.Media.ARTIST, 
        				MediaStore.Audio.Media.DATA,
        				MediaStore.Audio.Media.TITLE,
        				MediaStore.Audio.Media.ALBUM,
        				MediaStore.Audio.Media.DURATION,
        				MediaStore.Audio.Media.YEAR,
        				MediaStore.Audio.Media._ID,
        				MediaStore.Audio.Media.DATE_ADDED
        			},
        		"lower("+MediaStore.Audio.Media.DATA + ") not like '%" + "off vocal" + "%'"
        			+ "and lower("+MediaStore.Audio.Media.DATA + ") not like '%" + "instrumental" + "%'"
        				 + "and lower("+MediaStore.Audio.Media.DATA + ") not like '%" + "/notifications/" + "%'",
        		null,
        		MediaStore.Audio.Media.DATE_ADDED + " DESC" );
        Log.i(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
            return;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            Log.e(TAG, "Failed to move cursor to first row (no query results).");
            return;
        }

        Log.i(TAG, "Listing...");

        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int yearColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);
        
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

        Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
        Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));

        // add each song to mItems
        AudioItemBean audioItemBean;
        do {
            Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
            audioItemBean = new AudioItemBean();
        	//Log.d("LINEAR:GETTITLE", new Date(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED))*1000).toLocaleString() +" "+ c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        	audioItemBean.setTitle(cur.getString(titleColumn));
        	audioItemBean.setArtist(cur.getString(artistColumn));
        	audioItemBean.setFilePath(cur.getString(dataColumn));
        	audioItemBean.setAlbum(cur.getString(albumColumn));
        	audioItemBean.setYear(cur.getInt(yearColumn));
        	audioItemBean.setDuration(cur.getLong(durationColumn));
        	audioItemBean.setId(cur.getLong(idColumn));
        	//Log.d("d.linear", String.valueOf(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        	//Log.d( ".linear" , Arrays.toString( c.getColumnNames() ) );  
        	//if (c.getColumnIndex(MediaStore.Audio.Media.TITLE) != -1) {
        	if (new File(audioItemBean.getFilePath()).exists()) {	
        		//String filename = audioItemBean.getFilePath().toLowerCase();
        		//if (filename.indexOf("/notifications/") == -1 
        		//		&& filename.indexOf("instrumental") == -1
        		//		&& filename.indexOf("off vocal") == -1) {
        			mItems.add(audioItemBean);
        		//}
        	}

        } while (cur.moveToNext());

        cur.close();
        
        Log.i(TAG, "Done querying media. MusicRetriever is ready.");
        
        
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    /** Returns a random Item. If there are no items available, returns null. */
    public AudioItemBean getRandomItem() {
        if (mItems.size() <= 0) return null;
        return mItems.get(mRandom.nextInt(mItems.size()));
    }

    public static class Item {
        long id;
        String artist;
        String title;
        String album;
        long duration;

        public Item(long id, String artist, String title, String album, long duration) {
            this.id = id;
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.duration = duration;
        }

        public long getId() {
            return id;
        }

        public String getArtist() {
            return artist;
        }

        public String getTitle() {
            return title;
        }

        public String getAlbum() {
            return album;
        }

        public long getDuration() {
            return duration;
        }

        public Uri getURI() {
            return ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
    }
}
