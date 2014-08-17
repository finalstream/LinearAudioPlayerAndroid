package net.finalstream.linearaudioplayer.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.finalstream.linearaudioplayer.bean.AudioItemBean;
import net.finalstream.linearaudioplayer.commons.CommonUtils;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;

public class PlayHistoryTable {
	 private static final String CREATE =
		        "create table playhistory(_id integer primary key not null, _sort integer not null)";
		    private static final String DROP = "drop table playhistory";
		    private static final String INSERT = "insert into playhistory(_id, _sort) values ( ? , ?)";
		    //private static final String UPDATE = "update hello set ms = ? where _id = ?";
		    private static final String DELETE = "delete from playhistory where _id = ?";
		    private static final String DELETE_ALL = "delete from playhistory";
		    private static final String SELECT = "select * from playhistory";
		    
		    public long mId;     //Column 1
		    public long mSort;
		    
		    public String getCreateQuery() { return CREATE; }
		    public String getInsertQuery() { return INSERT; }
		    public String getDeleteQuery() { return DELETE; }
		    public String getDeleteAllQuery() { return DELETE_ALL; }
		    public String getDropQuery() { return DROP; }
		    public Object[] getInsertBindItem() { return new Object[]{mId, mSort}; }
		    public String getSelectQuery() { return SELECT; }
		    
		    public void setData(Cursor c) {
		        // TODO Auto-generated method stub
		        
		        mId = c.getInt(0);
		    }
		    
		    public PlayHistoryTable[] makeData(Cursor c) {
		        
		        //最初のデータを参照する
		        c.moveToFirst();
		        int cnt = c.getCount();
		        if (cnt == 0) {
		            
		            c.close();
		            return null;
		        }
		        
		        PlayHistoryTable[] q = new PlayHistoryTable[cnt];
		        for (int i = 0; i < cnt; i++) {
		            q[i] = new PlayHistoryTable();
		            q[i].setData(c);
		            c.moveToNext();
		        }
		        
		        return q;
		    }
		    
		    public LinkedList<AudioItemBean> getPlayHistory(Context context) {
		    	LinkedList<AudioItemBean> result = new LinkedList<AudioItemBean>();
		    	
		    	//セレクト処理
		    	DBHelper mDb = new DBHelper(context);
		    	
				SQLiteDatabase db = mDb.getReadableDatabase();
				try {
		            
		            PlayHistoryTable q = new PlayHistoryTable();
		            
		            //セレクトした結果のカーソルオブジェクトを変換する
		            Cursor cur_a = db.rawQuery(q.getSelectQuery(), null);
		            
		        
		        ContentResolver resolver = context.getContentResolver();
		        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		        Cursor cur_b = resolver.query(uri, 
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
		        		null);
		        
		        
		        
		        /*
			    	result = new MatrixCursor(new String[] {MediaStore.Audio.Media.ARTIST, 
	        				MediaStore.Audio.Media.DATA,
	        				MediaStore.Audio.Media.TITLE,
	        				MediaStore.Audio.Media.ALBUM,
	        				MediaStore.Audio.Media.DURATION,
	        				MediaStore.Audio.Media.YEAR,
	        				MediaStore.Audio.Media._ID,
	        				MediaStore.Audio.Media.DATE_ADDED
	        			});
	        			*/
			    	 CursorJoinerWithIntKey joiner = new CursorJoinerWithIntKey(cur_a, new String[]{MediaStore.Audio.Media._ID}, cur_b, new String[]{MediaStore.Audio.Media._ID});
			    	 for (CursorJoinerWithIntKey.Result joinerResult : joiner) {
			    	     switch (joinerResult) {
			    	         case BOTH:
			    	             result.add(cursor2values(cur_a, cur_b)); // 注：cursor2valuesはCursorをString[]に変換する独自のメソッドです。

			    	             break;
			    	     }
			    	 }
			    	 
			    	 cur_a.close();
			    	 cur_b.close();
			    	 
				}catch(Exception ex) {
					// do nothing
		        } finally {
		        	
		            db.close();
		        }
		    	
		        Collections.sort(result,new Comparator<AudioItemBean>() {
		            @Override
		            public int compare(AudioItemBean o1, AudioItemBean o2) {
		            	//return (int) (o2.getSort() - o1.getSort());
		            	return ((Long)o2.getSort()).compareTo(o1.getSort());
		            	
		            }
		        });
		        
		    	return result;
		    	
		    }
		    
			private AudioItemBean cursor2values(Cursor c1, Cursor c) {
					AudioItemBean audioItemBean = new AudioItemBean();

		        	//Log.d("LINEAR:GETTITLE", new Date(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED))*1000).toLocaleString() +" "+ c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
					audioItemBean.setTitle(c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
		        	audioItemBean.setArtist(c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
		        	audioItemBean.setFilePath(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
		        	audioItemBean.setAlbum(c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
		        	audioItemBean.setYear(c.getInt(c.getColumnIndex(MediaStore.Audio.Media.YEAR)));
		        	audioItemBean.setDuration(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DURATION)));
		        	audioItemBean.setId(c.getLong(c.getColumnIndex(MediaStore.Audio.Media._ID)));
		        	audioItemBean.setSort(c1.getLong(c1.getColumnIndex("_sort")));

		        	
				return audioItemBean;
			}
}
