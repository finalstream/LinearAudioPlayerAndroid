package net.finalstream.linearaudioplayer.database;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import net.finalstream.linearaudioplayer.LinearConst;
import net.finalstream.linearaudioplayer.bean.AudioItemBean;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;

public class PlayListTable {
	 private static final String CREATE =
		        "create table playlist(_id integer primary key not null, _rating real not null, _playcount int not null, _lastplaydate text)";
		    private static final String DROP = "drop table playlist";
		    private static final String INSERT = "insert into playlist(_id, _rating, _playcount, _lastplaydate) values ( ? , -1, 0, null)";
		    private static final String UPDATE_PLAYSTATE = "update playlist set _playcount = _playcount+1, _lastplaydate = strftime('%Y-%m-%d %H:%M:%S', 'now')  where _id = ?";
		    private static final String UPDATE_RATING = "update playlist set _rating = ? where _id = ?";
		    private static final String DELETE = "delete from playlist where _id = ?";
		    private static final String SELECT = "select * from playlist";
		    private static final String SELECT_FULLRATE = "select * from playlist where _rating = 1";
		    private static final String SELECT_HALFRATE = "select * from playlist where _rating >= 0.5";
		    private static final String SELECT_PLAYFREQHIGH = "select *, _playcount / round(julianday(datetime('now', 'localtime')) - julianday(datetime(_lastplaydate))) _playfreq from playlist";
		    private static final String SELECT_COUNT = "select count(_id) from playlist where _id = ?";
		    private static final String SELECT_NOTRATING = "select * from playlist";
		    private static final String SELECT_GETRATING = "select _rating from playlist where _id = ?";
		    
		    public long mId;     //Column 1
		    public float mRating;
		    public int mPlayCount;
		    public String mLastPlayDate;
		    
		    public String getCreateQuery() { return CREATE; }
		    public String getInsertQuery() { return INSERT; }
		    public String getUpdatePlayStateQuery() { return UPDATE_PLAYSTATE; }
		    public String getUpdateRatingQuery() { return UPDATE_RATING; }
		    public String getDeleteQuery() { return DELETE; }
		    public String getDropQuery() { return DROP; }
		    public Object[] getInsertBindItem() { return new Object[]{mId}; }
		    public String getSelectQuery() { return SELECT; }
		    public String getSelectFullrateQuery() { return SELECT_FULLRATE; }
		    public String getSelectHalfrateQuery() { return SELECT_HALFRATE; }
		    public String getSelectPlayFreqHighQuery() { return SELECT_PLAYFREQHIGH; }
		    public String getSelectCountQuery() { return SELECT_COUNT; }
		    public String getSelectNotRatingQuery() { return SELECT_NOTRATING; }
		    public String getSelectGetRatingQuery() { return SELECT_GETRATING; }
		    
		    public void setData(Cursor c) {
		        // TODO Auto-generated method stub
		        
		        mId = c.getInt(0);
		    }
		    

}
