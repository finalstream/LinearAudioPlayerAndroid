package net.finalstream.linearaudioplayer.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;


public class LinearContentProvider extends ContentProvider {
	
	private DBHelper mDb;
    public static final String AUTHORITY = "net.finalstream.linearaudioplayer.lineardatabase";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");
    private static final String TAG = "LinearContentProvider";
    
    //ディレクトリのMIMEタイプ
    private static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + AUTHORITY;
    
    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        // TODO Auto-generated method stub
        
        //ディレクトリのみアクセス
        return CONTENT_TYPE;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onCreate() {
        
        Log.d(TAG, "onCreate");
        mDb = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        
        Log.d(TAG, "query");
        Log.d(TAG, uri.toString());
        
        PlayHistoryTable hello = new PlayHistoryTable();
        final SQLiteDatabase db = mDb.getReadableDatabase();
        Cursor c = null;
        c = db.rawQuery(hello.getSelectQuery(), null);
        if (c == null) {
            Log.d(TAG, "Cursor is null");
        }
        
        c.setNotificationUri(getContext().getContentResolver(), uri);
        
        return c;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        // TODO Auto-generated method stub
        return 0;
    }
	
}
