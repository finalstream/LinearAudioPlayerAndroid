package net.finalstream.linearaudioplayer.database;

import net.finalstream.linearaudioplayer.LinearConst;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DBHelper extends SQLiteOpenHelper {
	
	private static final String DB_NAME = "LINEAR_DATABASE";
    private static final int DB_VERSION = 3;
    
    public DBHelper(Context context) {
        
        //�f�[�^�x�[�X���ƁA�f�[�^�x�[�X�o�[�W������ݒ�
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        
        // ����DB�쐬���́A������̃��\�b�h�����s�����
        
        db.beginTransaction();
        try {
            
            PlayHistoryTable q = new PlayHistoryTable();
            db.execSQL(q.getCreateQuery());
            
            PlayListTable q2 = new PlayListTable();
            db.execSQL(q2.getCreateQuery());
            
            db.setTransactionSuccessful();
            
        } finally {
            
            //�g�����U�N�V�����̏I��
            db.endTransaction();
        }
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("LINEAR", String.format(
				                "DBUpdgrade onUpgrade: oldVersion=%d,newVersion=%d", oldVersion,
				                newVersion));
		
		if (oldVersion == 1 && newVersion == 2) {
			// PlayList�e�[�u���ǉ�
			db.beginTransaction();
	        try {
	            
	            PlayListTable q = new PlayListTable();
	            db.execSQL(q.getCreateQuery());
	            db.setTransactionSuccessful();
	            
	            Log.d(LinearConst.DEBUG_TAG, "DBUpdgrade! v2");
	            
	        } finally {
	            
	            //�g�����U�N�V�����̏I��
	            db.endTransaction();
	        }
			
		} else if (oldVersion == 2 && newVersion == 3) {
			
			// PlayList�e�[�u���ǉ�
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
	            
	            //�g�����U�N�V�����̏I��
	            db.endTransaction();
	        }
		}
		
	}
	
}
