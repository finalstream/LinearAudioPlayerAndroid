package net.finalstream.linearaudioplayer.database;

import net.finalstream.linearaudioplayer.LinearConst;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;


public class DBHelper extends SQLiteOpenHelper {
	
	private static final String DB_NAME = "LINEAR_DATABASE";
    private static final int DB_VERSION = 10;
    private final Context myContext;
    
    public DBHelper(Context context) {
        
        //データベース名と、データベースバージョンを設定
        super(context, DB_NAME, null, DB_VERSION);
        myContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        
        // 初回DB作成時は、こちらのメソッドが実行される
        
        db.beginTransaction();
        try {
            
            PlayHistoryTable q = new PlayHistoryTable();
            db.execSQL(q.getCreateQuery());
            
            PlayListTable q2 = new PlayListTable();
            db.execSQL(q2.getCreateQuery());
            
            db.setTransactionSuccessful();
            
        } finally {
            
            //トランザクションの終了
            db.endTransaction();
        }
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("LINEAR", String.format(
				                "DBUpdgrade onUpgrade: oldVersion=%d,newVersion=%d", oldVersion,
				                newVersion));
		
		if (oldVersion == 1 && newVersion == 2) {
			// PlayListテーブル追加
			db.beginTransaction();
	        try {
	            
	            PlayListTable q = new PlayListTable();
	            db.execSQL(q.getCreateQuery());
	            db.setTransactionSuccessful();
	            
	            Log.d(LinearConst.DEBUG_TAG, "DBUpdgrade! v2");
	            
	        } finally {
	            
	            //トランザクションの終了
	            db.endTransaction();
	        }
			
		} else if (oldVersion == 2 && newVersion == 3) {
			
			// PlayListテーブル追加
			db.beginTransaction();
			try {
				String sqlstr =   
			            "select count(*) " +  
			            "from sqlite_master " +  
			            "where type = 'table' and " +  
			            "name = 'playlist'";  
		        Cursor cursor = db.rawQuery(sqlstr, null);  
		          
		        cursor.moveToFirst();  
		          
		        if(cursor.getInt(0) == 0) {  
			            
			            PlayListTable q = new PlayListTable();
			            db.execSQL(q.getCreateQuery());
			            db.setTransactionSuccessful();
			            
			            Log.d(LinearConst.DEBUG_TAG, "DBUpdgrade! v3");
			            
			        
		        }  
			} finally {
	            
	            //トランザクションの終了
	            db.endTransaction();
	        }
		} else if (newVersion == 10) {
			
			db.beginTransaction();
			try {
				
				// カラム追加
				try {
				db.execSQL("ALTER TABLE playlist add column _data;");
				}catch(Exception ex) {
					
				}
				
				ContentResolver resolver = this.myContext.getContentResolver();
				Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		        
				
		        Cursor cur = resolver.query(uri, 
		        		new String[] {MediaStore.Audio.Media.ARTIST, 
		        				MediaStore.Audio.Media.DATA,
		        				MediaStore.Audio.Media.TITLE,
		        				MediaStore.Audio.Media.ALBUM,
		        				MediaStore.Audio.Media.DURATION,
		        				MediaStore.Audio.Media.YEAR,
		        				MediaStore.Audio.Media._ID,
		        				MediaStore.Audio.Media.DATE_ADDED
		        			},
		        		null,
		        		null,
		        		null);
				
				boolean isEof = cur.moveToFirst();
				while (isEof) {
					db.execSQL("update playlist set _data =? where _id = ?", new Object[]{ cur.getString(cur.getColumnIndex("_data")),  cur.getString(cur.getColumnIndex("_id")) } );
				    isEof = cur.moveToNext();
				}
				cur.close();
		        db.setTransactionSuccessful();
	            
	            Log.d(LinearConst.DEBUG_TAG, "DBUpdgrade! v10");
			} finally {
	            
	            //トランザクションの終了
	            db.endTransaction();
	        }
		}
		
	}
	
}
