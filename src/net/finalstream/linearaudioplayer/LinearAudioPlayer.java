package net.finalstream.linearaudioplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.umass.util.StringUtilities;

import android.R.drawable;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MenuItem;
import android.view.View.OnTouchListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.finalstream.linearaudioplayer.R.id;
import net.finalstream.linearaudioplayer.adapter.ListPlayListRowAdapter;
import net.finalstream.linearaudioplayer.adapter.ListPlayHistoryRowAdapter;
import net.finalstream.linearaudioplayer.bean.ArtistBean;
import net.finalstream.linearaudioplayer.bean.AudioItemBean;
import net.finalstream.linearaudioplayer.commons.AudioFilenameFilter;
import net.finalstream.linearaudioplayer.commons.CommonUtils;
import net.finalstream.linearaudioplayer.commons.Crypto;
import net.finalstream.linearaudioplayer.commons.CsUncaughtExceptionHandler;
import net.finalstream.linearaudioplayer.commons.ToastMaster;
import net.finalstream.linearaudioplayer.config.PrefActivity;
import net.finalstream.linearaudioplayer.database.CursorJoinerWithIntKey;
import net.finalstream.linearaudioplayer.database.DBHelper;
import net.finalstream.linearaudioplayer.database.PlayHistoryTable;
import net.finalstream.linearaudioplayer.database.PlayListTable;
import net.finalstream.linearaudioplayer.services.LinearAudioPlayerService;

public class LinearAudioPlayer extends Activity {

	//private MediaScannerConnection mConnection;
	
	private class LinearAudioPlayerReceiver extends BroadcastReceiver {
		
		
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (action.equals(LinearAudioPlayerService.RECVACTION_PLAYED)) {
				// 再生直後
				//AudioItemBean bean = (AudioItemBean) intent.getSerializableExtra("AUDIOITEM");
				
				
			} else if (action.equals(LinearAudioPlayerService.RECVACTION_RESTORE)) {
				AudioItemBean bean = (AudioItemBean) intent.getSerializableExtra("AUDIOITEM");
				LinearAudioPlayer.this.updateDisplay(bean);
			}

		}
	}
	
	
	private List<File> audioFiles;
	private final LinearAudioPlayerReceiver receiver = new LinearAudioPlayerReceiver();
	private LinearAudioPlayerService linearAudioPlayerService = null;
	public LinearAudioPlayerService getLinearAudioPlayerService(){
		return linearAudioPlayerService;
	}
	ScheduledExecutorService scheduledEx;
	
	SeekRunner timerTask = null;
	Timer   mTimer   = null;
	Handler mHandler = new Handler();
	
	ListPlayListRowAdapter listadapter;
	public ListPlayListRowAdapter getPlaylistAdapter(){
		return listadapter;
	}
	
	public enum FILTERING_MODE
	{
	 DEFAULT(0), FULL_RATE(1), HALF_RATE(2), PLAYFREQ_HIGH(3), RECENT(4), NOT_RATING(5);
	 
	 private int type;
	 
	 private FILTERING_MODE ( int type )
	 {
	 this.type = type;
	 }
	 
	 public int toValue ( )
	 {
	  return type;
	 }
	 
	 public static FILTERING_MODE fromValue ( int type )
	 {
	  // Initialize.
		 FILTERING_MODE ret_val = DEFAULT;
	  
	  for ( FILTERING_MODE filtermode : values() )
	  {
	   if ( filtermode.toValue() == type )
	   {
	    ret_val = filtermode;
	   }
	  }
	  
	  return ret_val;
	 }
	}
	
	FILTERING_MODE mFilteringMode = FILTERING_MODE.DEFAULT;
	
	ArrayAdapter<ArtistBean> artistSpinnerAdapter;
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			linearAudioPlayerService = ((LinearAudioPlayerService.LinearAudioPlayerBinder)service).getService();
			Log.i("LINEAR","bind complete!");
			//タイマーの初期化処理
            timerTask = new SeekRunner();
            mTimer = new Timer(true);
            mTimer.schedule( timerTask, 100, 100);
            
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(LinearAudioPlayer.this);
        	linearAudioPlayerService.setLastfmUser(pref.getString("lastfmUser", ""));
        	String password = pref.getString("lastfmPassword", "");
        	if (!"".equals(password)) {
        		try {
					password = Crypto.decrypt("net.finalstream.linearaudioplayer", password);
				} catch (Exception e) {
					Log.e("LINEAR","",e);
				}
        	}
        	linearAudioPlayerService.setLastfmPassword(password);
        	
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className) {
			linearAudioPlayerService = null;
			Log.i("LINEAR","unbind.");
			//scheduledEx.shutdown();
		}
		
	};
	
	View[] mView = new View[2];
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	loadSetting();
    	//setTheme(16974123);
    	changeTheme(mTheme);
    	
        //setContentView(R.layout.activity_linear_audio_player);
        setContentView(R.layout.activity_main);
        
        
        
        //アプリケーションで共通に利用するオブジェクトには、メモリリークが発生しないようにthisではなく  
        //Context.getApplicationContext()を使用します。  
        Context context = this.getApplicationContext();   
        //キャッチされない例外により、スレッドが突然終了したときや、  
        //このスレッドに対してほかにハンドラが定義されていないときに  
        //呼び出されるデフォルトのハンドラを設定します。  
        Thread.setDefaultUncaughtExceptionHandler(new CsUncaughtExceptionHandler(context));  
		
        
        ViewPager mViewPager = (ViewPager)findViewById(R.id.viewpager);
        PagerAdapter mPagerAdapter = new MyPagerAdapter();
        mViewPager.setAdapter(mPagerAdapter);
        
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                
            	if (position == 1) {
            		
            		if (linearAudioPlayerService != null) {
	            		anylistadapter.clear();
	            		for (AudioItemBean bean : linearAudioPlayerService.getmPlayHistory()) {
							anylistadapter.add(bean);
						}
	            		
	            		if (rl != null) {
	            		TextView tv = (TextView) rl.findViewById(R.id.textPlayHistoryCount);
	    	            tv.setText(anylistadapter.getCount() + " items");
	            		}
	            	}
            	}
            	
            }
        });
        
        LayoutInflater inflater = getLayoutInflater();
        View mainView = inflater.inflate(R.layout.activity_linear_audio_player, null);
        
        startService(new Intent(LinearAudioPlayerService.ACTION_START));
        
        // サービスから受信するために
        IntentFilter filter = new IntentFilter(LinearAudioPlayerService.ACTION);
        filter.addAction(LinearAudioPlayerService.RECVACTION_PLAYED);
        filter.addAction(LinearAudioPlayerService.RECVACTION_RESTORE);
        registerReceiver(receiver, filter);

        
        //scheduledEx =  Executors.newSingleThreadScheduledExecutor();
        TextView tvTitle = (TextView) mainView.findViewById(R.id.textView1);
        //tvTitle.setSingleLine();
        //tvTitle.setFocusable(true);
        //tvTitle.setFocusableInTouchMode(true);
        //tvTitle.setMarqueeRepeatLimit(-1);
        tvTitle.setText("");
        tvTitle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//            	ListView lv = (ListView) LinearAudioPlayer.this.findViewById(R.id.listView);
//    			Random rnd = new Random();
//    			int ran = rnd.nextInt(lv.getCount());
//    			AudioItemBean  bean =  (AudioItemBean) lv.getItemAtPosition(ran);
//    			LinearAudioPlayer.this.play(LinearAudioPlayer.this, bean);
            	startService(new Intent(LinearAudioPlayerService.ACTION_SKIP));
            }
        });
        
        TextView tvAlbum = (TextView) mainView.findViewById(R.id.TextView01);
        tvAlbum.setSingleLine();
        tvAlbum.setFocusable(true);
        tvAlbum.setFocusableInTouchMode(true);
        tvAlbum.setMarqueeRepeatLimit(-1);
        tvAlbum.setText("");
        
       
        
        
        
        //reloadList();
        listadapter = new ListPlayListRowAdapter(LinearAudioPlayer.this, R.layout.playlist_item, R.id.listView);
    	
        //ListDataLoadTask task = new ListDataLoadTask();
        //task.execute();
        
        Button anybutton = (Button) mainView.findViewById(R.id.button1);
        anybutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	startService(new Intent(LinearAudioPlayerService.ACTION_PAUSE));
            }
        });
        anybutton.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				//unbindService(serviceConnection); // バインド解除
				stop();
				return false;
			}
		});
        
        SeekBar seekbar = (SeekBar) mainView.findViewById(R.id.seekBar1);
		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					linearAudioPlayerService.setPosition(progress);
				}
			}
		});
		
		 // スピナー
		
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // アイテムを追加します
        adapter.add(this.getResources().getString(R.string.filtering_default));
        adapter.add(this.getResources().getString(R.string.filtering_ratingfull));
        adapter.add(this.getResources().getString(R.string.filtering_ratinghalf));
        adapter.add(this.getResources().getString(R.string.filtering_highfreq));
        adapter.add(this.getResources().getString(R.string.filtering_recent));
        adapter.add(this.getResources().getString(R.string.filtering_notrated));
        Spinner spinner = (Spinner) mainView.findViewById(id.spinner1);
        // アダプターを設定します
        spinner.setAdapter(adapter);
       
        spinner.setSelection(mFilteringIndex);
        
        // スピナーのアイテムが選択された時に呼び出されるコールバックリスナーを登録します
 
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            
        	@Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                //Spinner spinner = (Spinner) parent;
                // 選択されたアイテムを取得します
                //String item = (String) spinner.getSelectedItem();
        		
        		mFilteringMode = FILTERING_MODE.fromValue(position);

                ListDataLoadTask task = new ListDataLoadTask();
                task.execute();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        Spinner spinner2 = (Spinner) mainView.findViewById(R.id.Spinner01);
        // アダプターを設定します
        artistSpinnerAdapter = new ArrayAdapter<ArtistBean>(this, android.R.layout.simple_spinner_item);
        artistSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(artistSpinnerAdapter);
        
        
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        
        	@Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
        		Spinner sp = (Spinner) LinearAudioPlayer.this.findViewById(R.id.Spinner01);
        		if(sp.getTag() == null){ 
	                sp.setTag("ok");
        		} else {
        				ArtistBean ab = (ArtistBean)sp.getSelectedItem();
        				if (ab.getArtist() != null && ab.getArtist().equals(artistKey)) {
        					return;
        				}
	        			Log.i("LINEAR", "ArtistSelect");
	        			ListDataLoadTask task = new ListDataLoadTask();
		                task.execute();
		                artistKey = ((ArtistBean)sp.getSelectedItem()).getArtist();
        		}
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        
     // アダプターを設定します
    	ListView listView = (ListView) mainView.findViewById(id.listView);
    	listView.setAdapter(listadapter);
     // ListViewクリック時
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView)parent;
                AudioItemBean bean = (AudioItemBean)listView.getItemAtPosition(position);
                
                
                //再生
                play((Activity) parent.getContext(),bean);
                
            }

            
        });
        
        // RatingBar
        RatingBar rating = (RatingBar) mainView.findViewById(R.id.ratingBarPlaying);
		
        rating.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		        if (event.getAction() == MotionEvent.ACTION_DOWN) {

		        	RatingBar r = (RatingBar) v;
		        	if (r.getRating() == 0.0f) {
		        		r.setRating(0.5f);
		        	} else if (r.getRating() == 0.5f)  {
		        		r.setRating(1.0f);
		        	} else {
		        		r.setRating(0.0f);
		        	}
		        	
		        	//r.setTag(R.string.listitem_fromuser,true);
		        	LinearAudioPlayer.this.updateRating(linearAudioPlayerService.getPlayingItem().getId(),linearAudioPlayerService.getPlayingItem().getFilePath(),  r.getRating());
		        	
		        	// アダプター更新
		            for (int i = 0; i < listadapter.getCount(); i++) {
		    			if(listadapter.getItem(i).getId() == linearAudioPlayerService.getPlayingItem().getId()){
		    				listadapter.getItem(i).setRating(r.getRating());
		    				if (mFilteringMode == FILTERING_MODE.NOT_RATING) {
		    					listadapter.remove(listadapter.getItem(i));
		    					updateItemCount(listadapter.getCount());
		    				}
		    				break;
		    			}
		    		}
		            listadapter.notifyDataSetChanged();
		            
		        	Log.d("LINEAR", "Rating Ontatch.");
		        }
		        return true;
		    }
		});
        
        
        
        mView[0] = mainView;
        
        changeThemeStyle(mTheme);
    }
    
    int mFilteringIndex;
    int mTheme;
    private void loadSetting() {
		
    	SharedPreferences pref =
    			getSharedPreferences(LinearConst.PREF_KEY, MODE_PRIVATE);
    	mFilteringIndex = pref.getInt("FilteringIndex", 0);
    	mTheme = pref.getInt("Theme", 2);
    	
    	
    	
	}

	private String artistKey;
    public String getArtistKey() {
    	return artistKey;
    }
    boolean isLoadComplete = false;
    
    @Override
    protected void onStart() {
    	super.onStart();
    	CsUncaughtExceptionHandler.SendBugReport(this);  
    	if (CommonUtils.isServiceRunning(this, LinearAudioPlayerService.class)) {
			boolean result = bindService(new Intent(LinearAudioPlayerService.ACTION_START), serviceConnection, Context.BIND_AUTO_CREATE);

			// 復元
			if (!isLoadComplete) {
				Log.i("LINEAR_REBIND", String.valueOf(result));
				startService(new Intent(LinearAudioPlayerService.ACTION_GETPLAYINGITEM));
				isLoadComplete = true;
			}
		}
    };
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d(LinearConst.DEBUG_TAG, "Activity OnStop.");
    	saveSetting();
    };
    
    
    private void updateDisplay(AudioItemBean bean) {
		if (bean != null) {
			
	    	TextView tvTitle = (TextView) this.findViewById(R.id.textView1);
	        if (tvTitle != null) {
	        	tvTitle.setText(bean.getTitle());
	        }
	        
	        TextView tvAlbum = (TextView) this.findViewById(R.id.TextView01);
	        if (tvAlbum != null) {
		        String year = "";
		        if (bean.getYear() != 0) {
		        	year = "<" + bean.getYear() + ">";
		        }
		        tvAlbum.setText(bean.getArtist() + " [" + bean.getAlbum() + "] " + year);
		        tvAlbum.setEllipsize(TruncateAt.MARQUEE);
	        }
	        
	        SeekBar seekbar = (SeekBar)findViewById(R.id.seekBar1);
	        if (seekbar != null) {
	        	seekbar.setMax((int) bean.getDuration());
	        }
	        
	        RatingBar rating = (RatingBar) this.findViewById(R.id.ratingBarPlaying);
	        if (rating != null) {
	        	rating.setRating(bean.getRating());
	        }
		}
		
	}
    
    /**
     * 一覧データの取得と表示を行うタスク
     */
    public class ListDataLoadTask extends AsyncTask<Object, Integer, List<AudioItemBean>> {
            // 処理中ダイアログ
            private ProgressDialog progressDialog = null;

            @Override
            protected void onPreExecute() {
                    // バックグラウンドの処理前にUIスレッドでダイアログ表示
                    progressDialog = new ProgressDialog(LinearAudioPlayer.this);
                    progressDialog.setMessage(
                                    "Now Loading...");
                    progressDialog.setIndeterminate(true);
                    progressDialog.show();
            }

            @Override
            protected List<AudioItemBean> doInBackground(Object... params) {
                    // 一覧データの取得をバックグラウンドで実行
            	//AudioItemBean dao = new AudioItemBean(LinearAduioPlayer.this);
                    return reloadList();
            }

            @Override
            protected void onPostExecute(List<AudioItemBean> result) {
                    // 処理中ダイアログをクローズ
                    progressDialog.dismiss();

                    // 表示データのクリア
                    listadapter.clear();

                    // 表示データの設定
                    Map<String, Integer> countMap = new HashMap<String, Integer>();
                    for (AudioItemBean audioitem : result) {
                            listadapter.add(audioitem);
                            if (countMap.containsKey(audioitem.getArtist())) {
                            	countMap.put(audioitem.getArtist(), countMap.get(audioitem.getArtist())+1);
                            } else {
                            	countMap.put(audioitem.getArtist(), 1);
                            }
                    }
                    
                    List<Map.Entry> entries = new ArrayList<Map.Entry>(countMap.entrySet());
                    Collections.sort(entries, new Comparator(){
                        public int compare(Object o1, Object o2){
                            Map.Entry e1 =(Map.Entry)o1;
                            Map.Entry e2 =(Map.Entry)o2;
                            return ((Integer)e2.getValue()).compareTo((Integer)e1.getValue());
                        }
                    });
                    
                    artistSpinnerAdapter.clear();
                    artistSpinnerAdapter.add(new ArtistBean());
                    for (Map.Entry entry : entries) {
                        if (((Integer)entry.getValue()) > 1) {
                        	ArtistBean ab = new ArtistBean();
                        	String artist = "";
                        	if (entry.getKey() != null) {
                        		artist = entry.getKey().toString();
                        	}
                        	
                        	ab.setArtist(artist);
                        	ab.setCount((Integer) entry.getValue());
                        	artistSpinnerAdapter.add(ab);
                        }
                    }

                    updateItemCount(listadapter.getCount());
                    
                    
                    Intent intent = new Intent(LinearAudioPlayerService.ACTION_KEEPLISTDATA);
                    intent.putExtra("LISTDATA", (Serializable) result);
                    startService(intent);
                    
                    Log.i("LINEAR", "List LoadComplete!");
            }
    }
    
    private void updateItemCount(int count) {
    	TextView tvItemCount = (TextView) LinearAudioPlayer.this.findViewById(R.id.textViewItemCount);
        tvItemCount.setText(count + " items");
    }
    
    private List<AudioItemBean> reloadList() {
    	audioFiles = new ArrayList<File>();
    	List<AudioItemBean> list = new ArrayList<AudioItemBean>();
        
    	Log.i("LINEAR", "Reload List.");
    	
        // アイテムを追加します
    	//File sdfile = Environment.getExternalStorageDirectory();	
    	/*
    	searchAudioFiles(sdfile);
    	
    	for (File file : audioFiles) {
			adapter.add(file.getName());
		}*/
    	
    	Spinner sp = (Spinner) this.findViewById(R.id.Spinner01);
    	ArtistBean ab = null;
    	if (sp!=null && sp.getSelectedItem() != null) {
    		ab = (ArtistBean) sp.getSelectedItem();
    	}
    	ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // TODO: IS_MUSIC=1でしぼるか検討する。
        String whereString =" lower("+MediaStore.Audio.Media.DATA + ") not like '%" + "off vocal" + "%' "
    			+ " and lower("+MediaStore.Audio.Media.DATA + ") not like '%" + "instrumental" + "%' "
				 + " and lower("+MediaStore.Audio.Media.DATA + ") not like '%" + "/notifications/" + "%' "
        		+ " and " + MediaStore.Audio.Media.DURATION + " >=  30000 ";
        if (ab != null && ab.getArtist() != null) {
        	whereString += " and " + MediaStore.Audio.Media.ARTIST + " = " + DatabaseUtils.sqlEscapeString(ab.getArtist()) + "";
        }
        
        Cursor c = resolver.query(uri, 
        		new String[] {MediaStore.Audio.Media.ARTIST, 
        				MediaStore.Audio.Media.DATA,
        				MediaStore.Audio.Media.TITLE,
        				MediaStore.Audio.Media.ALBUM,
        				MediaStore.Audio.Media.DURATION,
        				MediaStore.Audio.Media.YEAR,
        				MediaStore.Audio.Media._ID,
        				MediaStore.Audio.Media.DATE_ADDED,
        				MediaStore.Audio.Media.DATE_MODIFIED
        			},
        		whereString,
        		null,
        		null);
        
        // PlayList セレクト処理
    	DBHelper mDb = new DBHelper(this);
    	
    	long recentValue = 0;
		SQLiteDatabase db = mDb.getReadableDatabase();

		try {
            
            PlayListTable q = new PlayListTable();
            
            //セレクトした結果のカーソルオブジェクトを変換する
            Cursor cur = null;
            
            switch(mFilteringMode) {
            case DEFAULT:
            	cur = db.rawQuery(q.getSelectQuery(), null);
            	break;
            case RECENT:
            	cur = db.rawQuery(q.getSelectQuery(), null);
            	// TODO:最近追加の基準値(１週間固定)
            	Calendar cal = Calendar.getInstance();
            	cal.add(Calendar.DATE, -7);
            	recentValue = (cal.getTime().getTime() /1000);
            	break;
            case FULL_RATE:
            	cur = db.rawQuery(q.getSelectFullrateQuery(), null);
            	break;
            case HALF_RATE:
            	cur = db.rawQuery(q.getSelectHalfrateQuery(), null);
            	break;
            case PLAYFREQ_HIGH:
            	cur = db.rawQuery(q.getSelectPlayFreqHighQuery(), null);
            	break;
            case NOT_RATING:
            	cur = db.rawQuery(q.getSelectNotRatingQuery(), null);
            	break;
            }
            
            CursorJoinerWithIntKey joiner;
            switch (mFilteringMode) {
            case DEFAULT:
            case RECENT:
            	joiner = new CursorJoinerWithIntKey(c, new String[]{MediaStore.Audio.Media._ID}, cur, new String[]{MediaStore.Audio.Media._ID});
            	//joiner = new CursorJoinerWithIntKey(c, new String[]{MediaStore.Audio.Media.DATA}, cur, new String[]{MediaStore.Audio.Media.DATA});
	   	    	for (CursorJoinerWithIntKey.Result joinerResult : joiner) {
	   	    	     switch (joinerResult) {
	   	    	     	case LEFT:
	   	    	     		list.add(cursor2values(c));
	   	    	     		break;
	   	    	        case BOTH:
	   	    	             AudioItemBean ai = cursor2values(c); // 注：cursor2valuesはCursorをString[]に変換する独自のメソッドです。
	   	    	             ai.setRating(cur.getFloat(cur.getColumnIndex("_rating")));
	   	    	             ai.setPlaycount(cur.getInt(cur.getColumnIndex("_playcount")));
	   	    	             
	   	    	             if (mFilteringMode == FILTERING_MODE.DEFAULT
	   	    	            		 || (mFilteringMode == FILTERING_MODE.RECENT && ai.getDateadd() >= recentValue) ) {
	   	    	            	 list.add(ai);
	   	    	             }
	   	    	             
	   	    	             break;
	   	    	     }
	   	    	}
            	break;
            case FULL_RATE:
            case HALF_RATE:
            case PLAYFREQ_HIGH:
            	joiner = new CursorJoinerWithIntKey(c, new String[]{MediaStore.Audio.Media._ID}, cur, new String[]{MediaStore.Audio.Media._ID});
            	//joiner = new CursorJoinerWithIntKey(c, new String[]{MediaStore.Audio.Media.DATA}, cur, new String[]{MediaStore.Audio.Media.DATA});
            	for (CursorJoinerWithIntKey.Result joinerResult : joiner) {
	   	    	     switch (joinerResult) {
	   	    	        case BOTH:
	   	    	             AudioItemBean ai = cursor2values(c); // 注：cursor2valuesはCursorをString[]に変換する独自のメソッドです。
	   	    	             ai.setRating(cur.getFloat(cur.getColumnIndex("_rating")));
	   	    	             ai.setPlaycount(cur.getInt(cur.getColumnIndex("_playcount")));
	   	    	             if (mFilteringMode == FILTERING_MODE.PLAYFREQ_HIGH) {
	   	    	            	 ai.setPlayfreq(cur.getFloat(cur.getColumnIndex("_playfreq")));
	   	    	             }
	   	    	             
	   	    	             list.add(ai);
	   	    	             
	   	    	             break;
	   	    	     }
	   	    	}
            	break;
            case NOT_RATING:
            	joiner = new CursorJoinerWithIntKey(c, new String[]{MediaStore.Audio.Media._ID}, cur, new String[]{MediaStore.Audio.Media._ID});
            	//joiner = new CursorJoinerWithIntKey(c, new String[]{MediaStore.Audio.Media.DATA}, cur, new String[]{MediaStore.Audio.Media.DATA});
            	for (CursorJoinerWithIntKey.Result joinerResult : joiner) {
	   	    	     switch (joinerResult) {
	   	    	     	case LEFT:
	   	    	     		list.add(cursor2values(c));
	   	    	     		break;
	   	    	        case BOTH:
	   	    	             AudioItemBean ai = cursor2values(c); // 注：cursor2valuesはCursorをString[]に変換する独自のメソッドです。
	   	    	             ai.setRating(cur.getFloat(cur.getColumnIndex("_rating")));
	   	    	             ai.setPlaycount(cur.getInt(cur.getColumnIndex("_playcount")));
	   	    	             
	   	    	             if (ai.getRating() == -1) {
	   	    	            	 list.add(ai);
	   	    	             }
	   	    	             break;
	   	    	     }
	   	    	}
            	break;
            }
            
	    	 if (c !=null) {
	    		 c.close();
	    	 }
	    	 if (cur != null) {
	    		 cur.close();
	    	 }
	    	 switch(mFilteringMode) {
	    	 case DEFAULT:
	    		 Collections.sort(list,new Comparator<AudioItemBean>() {
			            @Override
			            public int compare(AudioItemBean o1, AudioItemBean o2) {
			            	try {
			            	//return (int) (o2.getDatemodified() - o1.getDatemodified());
			            	return new Long(o2.getDatemodified()).compareTo(o1.getDatemodified());
			            	} catch(Exception ex) {
			            		return 0;
			            	}
			            }
			        });
	    		 break;
	    	 case FULL_RATE:
	    	 case HALF_RATE:
	    		 Collections.sort(list,new Comparator<AudioItemBean>() {
			            @Override
			            public int compare(AudioItemBean o1, AudioItemBean o2) {
			            	return o2.getPlaycount() - o1.getPlaycount();
			            }
			        });
	    		 break;
	    	 case PLAYFREQ_HIGH:
	    		 Collections.sort(list,new Comparator<AudioItemBean>() {
			            @Override
			            public int compare(AudioItemBean o1, AudioItemBean o2) {
			            	return Float.compare(o2.getPlayfreq(), o1.getPlayfreq());
			            }
			        });
	    		 
	    		 if (list.size() > LinearConst.SQL_LIMIT) {
	    			 
	    			 while(list.size() != LinearConst.SQL_LIMIT){
	    				 list.remove(LinearConst.SQL_LIMIT);
	    			 }
	    		 }
	    		 
	    		 break;
	    	 case NOT_RATING:
	    		 /*Collections.sort(list,new Comparator<AudioItemBean>() {
			            @Override
			            public int compare(AudioItemBean o1, AudioItemBean o2) {
			            	return  (int) (o2.getId() - o1.getId());
			            }
			        });*/
	    		 Collections.sort(list,new Comparator<AudioItemBean>() {
			            @Override
			            public int compare(AudioItemBean o1, AudioItemBean o2) {
			            	try {
			            	//return (int) (o2.getDatemodified() - o1.getDatemodified());
			            	return new Long(o2.getDatemodified()).compareTo(o1.getDatemodified());
			            	} catch(Exception ex) {
			            		return 0;
			            	}
			            }
			        });
	    		 break;
	    	 }
	    	 
        /*    
        AudioItemBean audioItemBean;
        c.moveToFirst();
        //while(c.moveToNext()) {
        for (int k = 0; k < c.getCount(); k++) {  
        	audioItemBean = new AudioItemBean();
        	//Log.d("LINEAR:GETTITLE", new Date(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED))*1000).toLocaleString() +" "+ c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        	audioItemBean.setTitle(c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        	audioItemBean.setArtist(c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
        	audioItemBean.setFilePath(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
        	audioItemBean.setAlbum(c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
        	audioItemBean.setYear(c.getInt(c.getColumnIndex(MediaStore.Audio.Media.YEAR)));
        	audioItemBean.setDuration(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DURATION)));
        	audioItemBean.setId(c.getLong(c.getColumnIndex(MediaStore.Audio.Media._ID)));
        	//Log.d("d.linear", String.valueOf(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        	//Log.d( ".linear" , Arrays.toString( c.getColumnNames() ) );  
        	//if (c.getColumnIndex(MediaStore.Audio.Media.TITLE) != -1) {
        	if (new File(audioItemBean.getFilePath()).exists()) {	
        		//String filename = audioItemBean.getFilePath().toLowerCase();
        		//if (filename.indexOf("/notifications/") == -1 
        		//		&& filename.indexOf("instrumental") == -1
        		//		&& filename.indexOf("off vocal") == -1) {
        			//adapter.add(audioItemBean);
        			list.add(audioItemBean);
        		//}
        	}
        	//}
        	c.moveToNext();
        }
        c.close();
        */
    	//for (AudioItemBean audioItemBean : list) {
		//	adapter.add(audioItemBean);
		//}
	    	 
		}finally{
			db.close();
		}
        
		/*
		if (mFilteringMode == FILTERING_MODE.DEFAULT) {
			Collections.sort(list, new AudioItemComparator());
			Collections.reverse(list);
		}*/
        
        
        return list;
	}
    
    /*
    class AudioItemComparator implements java.util.Comparator {
    	public int compare(Object s, Object t) {
    		//               + (x > y)
    		// compare x y = 0 (x = y)
    		//               - (x < y)
    		return (int) (Long.parseLong(((AudioItemBean) s).getDateadd()) - Long.parseLong(((AudioItemBean) t).getDateadd()));
    	}
    }*/
    
    private AudioItemBean cursor2values(Cursor c) {
		AudioItemBean audioItemBean = new AudioItemBean();

    	//Log.d("LINEAR:GETTITLE", new Date(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED))*1000).toLocaleString() +" "+ c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
		audioItemBean.setTitle(c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
    	audioItemBean.setArtist(c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
    	audioItemBean.setFilePath(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
    	audioItemBean.setAlbum(c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
    	audioItemBean.setYear(c.getInt(c.getColumnIndex(MediaStore.Audio.Media.YEAR)));
    	audioItemBean.setDuration(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DURATION)));
    	audioItemBean.setId(c.getLong(c.getColumnIndex(MediaStore.Audio.Media._ID)));
    	audioItemBean.setDateadd(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)));
    	try{
    		File file = new File(audioItemBean.getFilePath());
    		if (file.exists()) {
    		audioItemBean.setDatemodified(file.lastModified());
    		} else {
    			audioItemBean.setDatemodified(0);
    		}
    	} catch(Exception ex) {}
    	
    	// sjis文字化け対策(ボツ)
    	//audioItemBean.setTitle(CommonUtils.utf8toSJIS(c.getBlob(c.getColumnIndex(MediaStore.Audio.Media.TITLE))));
    	//audioItemBean.setArtist(CommonUtils.utf8toSJIS(c.getBlob(c.getColumnIndex(MediaStore.Audio.Media.ARTIST))));
    	//audioItemBean.setAlbum(CommonUtils.utf8toSJIS(c.getBlob(c.getColumnIndex(MediaStore.Audio.Media.ALBUM))));
    	
    	//Log.d("LINEAR", audioItemBean.getId() + " , " +audioItemBean.getTitle());
		
    	//sjis文字化け対策
    	/*
    	if (80804 == audioItemBean.getId()) {
	    	try {
				MP3File mp3file = new MP3File(audioItemBean.getFilePath());
				if (mp3file != null) {
					audioItemBean.setTitle(mp3file.getID3v2Tag().getSongTitle());
				}
	    	} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (org.farng.mp3.TagException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
    	}*/
    	
    	return audioItemBean;
}
	
   
    
    ScheduledFuture<?> future = null;
    private void play(Activity activity, AudioItemBean bean) {
		
    	if (future != null) {
    		future.cancel(true);
    	}

    	
    	//linearAudioPlayerService.play(bean);
        Intent intent = new Intent(LinearAudioPlayerService.ACTION_SKIP);
    	intent.putExtra("AUDIOITEM", bean);
        startService(intent);        
		// いったんアンバインドしてから再度バインド
		//unbindService(serviceConnection);
		if (linearAudioPlayerService == null) {
			bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		}
		
        
    	//String toastMsg = bean.getFilePath();
		
        updateDisplay(bean);
        //TextView tvTitle = (TextView) activity.findViewById(R.id.textView1);
        //tvTitle.setText(bean.getTitle());
        //tvTitle.setEllipsize(TruncateAt.MARQUEE);
        //tvTitle.requestFocus();
        
        //String year = "";
        //if (bean.getYear() != 0) {
        //	year = "<" + bean.getYear() + ">";
        //}
        //TextView tvAlbum = (TextView) activity.findViewById(R.id.TextView01);
        //tvAlbum.setText(bean.getArtist() + " [" + bean.getAlbum() + "] " + year);
        //tvAlbum.setEllipsize(TruncateAt.MARQUEE);
        //tvAlbum.requestFocus();
        
        //SeekBar seekbar = (SeekBar)findViewById(R.id.seekBar1);
        //seekbar.setProgress(0);
        //seekbar.setMax((int) bean.getDuration());
    	
        if(mTimer == null){
        	 
            //タイマーの初期化処理
            timerTask = new SeekRunner();
            mTimer = new Timer(true);
            mTimer.schedule( timerTask, 100, 100);
        }
        //future = scheduledEx.scheduleAtFixedRate(new SeekRunner("seek task"), 0, 500, TimeUnit.MILLISECONDS);
	}
    
  //再帰的にディレクトリ内を調べるメソッド
/*
    private void searchAudioFiles(File f){
    	if(f.isDirectory()){ // ディレクトリならそれ以下のディレクトリ、ファイルを検査
    		File[] files = f.listFiles();
    		for(File file : files){
    			Log.d("d.linear", file.getName());
    			if (!file.getName().startsWith(".")) {
    				searchAudioFiles(file); // 再帰
    			}
    		}
    	}else{ // ファイルの場合は音楽ファイルかどうかの判断
            // Androidで扱えるのはmp3とwavファイルなので、
    		// これらのファイルの場合リストに保存
    		String filename = f.getName().toLowerCase();
    		if(f.getName().endsWith(".mp3")){
    			audioFiles.add(f);
    		}
    	}
    }
    */

    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
       
		getMenuInflater().inflate(R.menu.activity_linear_audio_player, menu);
        
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // メニューアイテムを取得
        MenuItem menuPlayEngine = (MenuItem)menu.findItem(R.id.menuEngine);
        MenuItem menuTheme = (MenuItem)menu.findItem(R.id.menuTheme);
        MenuItem menuConfig = (MenuItem)menu.findItem(R.id.menuConfig);
        MenuItem menuRescan = (MenuItem)menu.findItem(R.id.menu_rescan);
        MenuItem menuMigration = (MenuItem)menu.findItem(R.id.menu_migration);
        MenuItem menuVersion = (MenuItem)menu.findItem(R.id.menu_version);
        MenuItem menuPlayHistoryClear = (MenuItem)menu.findItem(R.id.menu_playhistoryClear);

        ViewPager mViewPager = (ViewPager)findViewById(R.id.viewpager);
        
        if (mViewPager.getCurrentItem() == 0) {
            // main
            menuPlayEngine.setVisible(true);
            menuTheme.setVisible(true);
            menuConfig.setVisible(true);
            menuRescan.setVisible(true);
            menuMigration.setVisible(true);
            menuVersion.setVisible(true);
            menuPlayHistoryClear.setVisible(false);
        } else if (mViewPager.getCurrentItem() == 1) {
            // PlayHisyoty
        	menuPlayEngine.setVisible(false);
        	menuTheme.setVisible(false);
        	menuConfig.setVisible(false);
        	menuRescan.setVisible(false);
        	menuMigration.setVisible(false);
        	menuVersion.setVisible(false);
        	menuPlayHistoryClear.setVisible(true);
        }
        return true;
    }
	
    private int RESULTCODE_PREF = 100;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.menu_version:
	        showVersion();
	        return true;
	        
	    case R.id.menuTheme:
	    	showThemeSelect();
	    	return true;
	        
	    case R.id.menuConfig:
	    	Intent intent = new Intent(this, (Class<?>)PrefActivity.class);
	        startActivityForResult(intent,RESULTCODE_PREF);
	    	return true;
	    	
	    case R.id.menu_migration:
	    	// 1. AlertDialog.Builder クラスのインスタンスを生成
	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        
	        final EditText editView = new EditText(this);
	        
	        // 2. ダイアログタイトル、表示メッセージ、ボタンを設定
	        builder.setView(editView);
	        builder.setMessage("Are you sure?(option input base Dirctory Name)");
	        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {
	                // OK ボタンクリック処理
	            	migration(editView.getText().toString());
	            	
	            }
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {
	                // Cancel ボタンクリック処理
	            	debugWritePlaylist();
	            }
	        });
	        
	        // 3. ダイアログを生成
	        AlertDialog mAlertDlg = builder.create();
	        mAlertDlg.show();
	    	return true;
	    	
	    case R.id.menu_rescan:
	    	//Broadcast the Media Scanner Intent to trigger it
            /*sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                    .parse("file://"
                            + Environment.getExternalStorageDirectory())));*/
	    	String[] rmvDirs = getRemovableStoragePaths(getApplicationContext());
	    	//MediaScannerConnection.scanFile(getApplicationContext(), rmvDirs, null, null);
	    	
	    	List<String> sdfilelist = new ArrayList<String>();
	    	for (String path : rmvDirs) {
				//File dir = new File(path);
	    		if (new File(path).listFiles() == null) continue;
				List<String> allfiles = getFiles(path);
				sdfilelist.addAll(allfiles);
	    		//for (File file : dir.listFiles()) {
				//	if (file.isFile()) sdfilelist.add(file.toString());
				//}
			}
	    	
	    	Collections.sort(sdfilelist, new Comparator<String>(){
	    		public int compare(String o1,String o2){
	    			File f1= new File(o1);
	    			File f2= new File(o2);

	    			try {
	            	//return (int) (o2.getDatemodified() - o1.getDatemodified());
	            	return new Long(f2.lastModified()).compareTo(f1.lastModified());
	            	} catch(Exception ex) {
	            		return 0;
	            	}
	    			//return (int)(f2.lastModified()-f1.lastModified());
	    		}
	    	});
	    	
	    	String[] sdfiles = new String[sdfilelist.size()];
	    	sdfilelist.toArray(sdfiles);
	    	
	    	for (String string : sdfiles) {
				Log.d("LINEAR", "ReScan:" + string);
			}
	    	
	    	MediaScannerConnection.scanFile(
	    			getApplicationContext(), sdfiles, null, 
	    			new OnScanCompletedListener() {
						
						@Override
						public void onScanCompleted(String path, Uri uri) {
							// TODO Auto-generated method stub
							Log.d("LINEAR", "ReScaned(path):" + path);
							Log.d("LINEAR", "ReScaned(uri):" + uri);
						}
					});
	    	
	    	/*
	    	List<String> sdfilelist = new ArrayList<String>();
	    	File sddir = new File("");
	    	for (File file : sddir.listFiles()) {
				if (file.isFile()) sdfilelist.add(file.toString());
			}
	    	String[] sdfiles = new String[sdfilelist.size()];
	    	sdfilelist.toArray(sdfiles);
	    	MediaScannerConnection.scanFile(getApplicationContext(), sdfiles, null, null);
	    	*/
	    	

            //Just a message
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Media Scanner Running...", Toast.LENGTH_SHORT);
            toast.show();
	    	return true;
	    	
	    case R.id.menuEngine:
	    	showEngineSelect();
	    	return true;
	    	
	    case R.id.menu_playhistoryClear:
	    	linearAudioPlayerService.clearPlayHistory();
	    	anylistadapter.clear();
	    	TextView tv = (TextView) rl.findViewById(R.id.textPlayHistoryCount);
	    	tv.setText(anylistadapter.getCount() + " items");
	    	
	    	return true;
	    }
	    return false;
	}
	
	private List<String> getFiles(String path) {
		List<String> pl = new ArrayList<String>();
		File file = new File(path);
		if (!file.isDirectory()) file = file.getParentFile();
		for (File fc : file.listFiles()) {
			if (fc.isDirectory()) pl.addAll(getFiles(fc.getPath()));
			else pl.add(fc.getPath());
		}
		return pl;
	}
	
	public static String[] getRemovableStoragePaths(Context context) {
	    List<String> paths = new ArrayList<String>();
	    try {
	        StorageManager sm = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
	        Method getVolumeList = sm.getClass().getDeclaredMethod("getVolumeList");
	        Object[] volumeList = (Object[])getVolumeList.invoke(sm);
	        for (Object volume : volumeList) {
	            Method getPath = volume.getClass().getDeclaredMethod("getPath");
	            Method isRemovable = volume.getClass().getDeclaredMethod("isRemovable");
	            String path = (String)getPath.invoke(volume);
	            boolean removable = (Boolean)isRemovable.invoke(volume);
	            if (removable) {
	                paths.add(path);
	            }
	        }
	    } catch (ClassCastException e) {
	        e.printStackTrace();
	    } catch (NoSuchMethodException e) {
	        e.printStackTrace();
	    } catch (InvocationTargetException e) {
	        e.printStackTrace();
	    } catch (IllegalAccessException e) {
	        e.printStackTrace();
	    }
	    return paths.toArray(new String[paths.size()]);
	}
	
	private void debugWritePlaylist() {
		
		PlayListTable p = new PlayListTable();
    	DBHelper mDb = new DBHelper(this);
    	
		SQLiteDatabase db = mDb.getReadableDatabase();
		

		Cursor cur2 = db.rawQuery(p.getSelectQuery(), null);
		
		boolean isEof = cur2.moveToFirst();
		while (isEof) {
			Long id = cur2.getLong(cur2.getColumnIndex(MediaStore.Audio.Media._ID));
			String data = cur2.getString(cur2.getColumnIndex("_data"));
			Log.d(LinearConst.DEBUG_TAG, "Playlist Row:" + id.toString() + ", " + data);
			isEof = cur2.moveToNext();
		}
		cur2.close();
		
        db.close();
		
	}
	
	private void migration(String rootdirname) {
		
		// mediastoreから存在するファイルだけ抜き出す。
		Map<Long, String> mediaMap = new HashMap<Long, String>(); 
		ContentResolver resolver = getContentResolver();
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        
		Cursor cur = resolver.query(uri, 
        		new String[] {MediaStore.Audio.Media.DATA,
        				MediaStore.Audio.Media._ID
        			},
        		null,
        		null,
        		null);
		
		boolean isEof = cur.moveToFirst();
		while (isEof) {
			String filePath = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
			
			if (new File(filePath).exists()) {
				mediaMap.put(cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media._ID)), 
					filePath);
			}
			isEof = cur.moveToNext();
		}
		cur.close();
		
		// playlistを取得する
		List<Long> playlistIdList = new ArrayList<Long>();
		Map<String, Object[]> playlistMap = new HashMap<String, Object[]>(); 
		PlayListTable p = new PlayListTable();
    	DBHelper mDb = new DBHelper(this);
    	
		SQLiteDatabase db = mDb.getReadableDatabase();
		
		try {
            
	        db.beginTransaction();
			Cursor cur2 = db.rawQuery(p.getSelectQuery(), null);
			
			isEof = cur2.moveToFirst();
			while (isEof) {
				playlistIdList.add(cur2.getLong(cur2.getColumnIndex(MediaStore.Audio.Media._ID)));
				String filepath = cur2.getString(cur2.getColumnIndex("_data"));
				if (filepath != null && rootdirname != null && rootdirname != "") {
					int idx = filepath.indexOf(rootdirname);
			    	if(idx != -1) filepath = filepath.substring(idx + rootdirname.length());
				}
				playlistMap.put(
						filepath,
						new Object[] {
							cur2.getFloat(cur2.getColumnIndex("_rating")),
							cur2.getInt(cur2.getColumnIndex("_playcount")),
							cur2.getString(cur2.getColumnIndex("_lastplaydate"))
						});
				isEof = cur2.moveToNext();
			}
			cur2.close();
			
			
			for(Map.Entry<Long, String> e : mediaMap.entrySet()) {
			    Long id = e.getKey();
			    String filepath = e.getValue();
			    if (rootdirname != null && rootdirname != "") {
			    	int idx = filepath.indexOf(rootdirname);
			    	if(idx != -1) filepath = filepath.substring(idx + rootdirname.length());
			    }
				if(!playlistIdList.contains(id)) {
					// playlistに存在しないidの場合、追加する。
					Object[] values = playlistMap.get(filepath);
					if (filepath != null && values != null) {
						db.execSQL(p.getInsertQuery(), 
							new Object[]{id, values[0], values[1], values[2], filepath});
					}
				}
			}
			db.setTransactionSuccessful();
        } finally {
            
            db.endTransaction();
            db.close();
        }	
		
		ListDataLoadTask task = new ListDataLoadTask();
        task.execute();
			
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULTCODE_PREF){
            linearAudioPlayerService.setLastfmUser(PrefActivity.getLastfmUser(this));
            linearAudioPlayerService.setLastfmPassword(PrefActivity.getLastfmPassword(this));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	
	String[] theme_items = {"Black","White","Black ICS(Android 4.0 or higher)","White ICS(Android 4.0 or higher)"};
	
	/**
	 * テーマ選択
	 */
	private void showThemeSelect() {
		// set default value
        int def_index = mTheme;
        result_item = theme_items[def_index];
		// Single Choice Dialog
        new AlertDialog.Builder(this)
        .setTitle(R.string.playengineselect)
        .setSingleChoiceItems(theme_items, def_index,
            new DialogInterface.OnClickListener(){
        	public void onClick(DialogInterface dialog, int which) {
        		result_item = theme_items[which];
        		result_index = which;
        	}
        })
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        		/* OKボタンをクリックした時の処理 */
        		
        		
        		mTheme = result_index;
        		saveSetting();
        		finish();
        		startActivity(new Intent(LinearAudioPlayer.this, LinearAudioPlayer.class));
        	}
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        		/* Cancel ボタンをクリックした時の処理 */
        		
        	}
        })
        .show();
	}
	
	private void changeTheme(int theme){
		
		switch(theme){
		case 0:
			setTheme(android.R.style.Theme_Black_NoTitleBar);
			break;
		case 1:
			setTheme(android.R.style.Theme_Light_NoTitleBar);
			break;
		case 2:
			if (Build.VERSION.SDK_INT >= 14) {
				setTheme(16974121);
			} else {
				setTheme(android.R.style.Theme_Black_NoTitleBar);
				mTheme = 0;
			}
			break;
		case 3:
			if (Build.VERSION.SDK_INT >= 14) {
				setTheme(16974124);
			} else {
				setTheme(android.R.style.Theme_Light_NoTitleBar);
				mTheme = 1;
			}
			break;
		}
		
	}
	
int fontColor;
private void changeThemeStyle(int theme){
	SeekBar seekbar;
	Button anybutton;
	TextView time;
		switch(theme){
		case 0:
			seekbar = (SeekBar) mView[0].findViewById(R.id.seekBar1);
			seekbar.setProgressDrawable(this.getResources().getDrawable(net.finalstream.linearaudioplayer.R.drawable.seekbar));
			anybutton = (Button) mView[0].findViewById(R.id.button1);
			anybutton.setBackgroundDrawable(this.getResources().getDrawable(net.finalstream.linearaudioplayer.R.drawable.button));
			time = (TextView) mView[0].findViewById(R.id.textView2);
	        fontColor = 0xffff8c00;
			time.setTextColor(fontColor);
	        break;
		case 1:
			seekbar = (SeekBar) mView[0].findViewById(R.id.seekBar1);
			seekbar.setProgressDrawable(this.getResources().getDrawable(net.finalstream.linearaudioplayer.R.drawable.seekbar_gred));
			anybutton = (Button) mView[0].findViewById(R.id.button1);
			anybutton.setBackgroundDrawable(this.getResources().getDrawable(net.finalstream.linearaudioplayer.R.drawable.button_red));
			time = (TextView) mView[0].findViewById(R.id.textView2);
	        fontColor = Color.rgb(220, 20, 60);
			time.setTextColor(fontColor);
			break;
		case 2:
		case 3:
			//seekbar = (SeekBar) mView[0].findViewById(R.id.seekBar1);
			//seekbar.setProgressDrawable(this.getResources().getDrawable(android.R.drawable.progress_horizontal));
			//anybutton = (Button) mView[0].findViewById(R.id.button1);
			//anybutton.setBackgroundDrawable(this.getResources().getDrawable(android.R.drawable.btn_default));
			time = (TextView) mView[0].findViewById(R.id.textView2);
	        fontColor =Color.rgb(30, 144, 255);
			time.setTextColor(fontColor);
			break;
			
		}

	}
	
	/**
	 * 再生エンジンを選択する。
	 */
	String[] engine_items = {"Android Media Player","FMOD Sound System","BASS Audio Library"};
	String result_item = "";
	int result_index = 0;
	private void showEngineSelect() {
		stop();
		// set default value
        int def_index = linearAudioPlayerService.getPlayEngineMode();
        result_item = engine_items[def_index];
        // Single Choice Dialog
        new AlertDialog.Builder(this)
        .setTitle(R.string.playengineselect)
        .setSingleChoiceItems(engine_items, def_index,
            new DialogInterface.OnClickListener(){
        	public void onClick(DialogInterface dialog, int which) {
        		result_item = engine_items[which];
        		result_index = which;
        	}
        })
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        		/* OKボタンをクリックした時の処理 */
        		
        		linearAudioPlayerService.setPlayEngineMode(result_index);
        		
        		ToastMaster.makeText(LinearAudioPlayer.this, "Change PlayEngine : " + result_item, Toast.LENGTH_SHORT).show();
        	}
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        		/* Cancel ボタンをクリックした時の処理 */
        		
        	}
        })
        .show();
		
	}

	/**
	 * バージョンを表示する。
	 */
	private void showVersion() {
		
		// FmodVersion
		String engineVersion ="";
		String env = "";
		if (linearAudioPlayerService != null) {
			
			String en = Integer.toHexString(linearAudioPlayerService.getPlayEngineVersion());
			
			switch (linearAudioPlayerService.getPlayEngineMode()) {
			case 1:
				env = en.substring(0,1) + "." + en.substring(1,3) + "." + en.substring(3);
				engineVersion = "\n\nPlay Engine: \nPowered by FMOD Sound System ver." + env
						+ "\n Copyright (c) Firelight Technologies Pty, Ltd., 1994-2012." ;
				break;

			case 2:
				env = en.substring(0,1) + "." + Integer.parseInt(en.substring(1, 3)) + "." + Integer.parseInt(en.substring(3,5)) + "." + Integer.parseInt(en.substring(5));
				engineVersion = "\n\nPlay Engine: \nPowered by BASS Audio Library ver." + env
						+ "\n Copyright (c) 1999-2012 Un4seen Developments Ltd." ;
				break;
			}
			
			
			
		}
		
		
		new AlertDialog.Builder(this)
		.setTitle("Version Info")
		.setMessage("Linear Audio Player for Android\n" 
		+ CommonUtils.getVersionNumber("ver.", this) + "\n"
		+ "Copyright (c) 2012-2014 FINALSTREAM." + engineVersion
		+  "\n\nLast.fm API: \nPowered by Last.fm API bindings for java ver.0.1.2"
		+ "\n Copyright (c) 2012 the Last.fm Java Project and Committers."
		).setPositiveButton("OK", null)
		.show();
		
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//stop();
		unbindService(serviceConnection); // バインド解除
		unregisterReceiver(receiver); // 登録解除
		//scheduledEx.shutdown();
		
		
		if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
		Log.i("LINEAR", "Activity onDestory");
		
		/*
		File cachedir = new File(getCacheDir() + "/.last.fm-cache");
		if (cachedir.exists()){
			Log.i("LINEAR", "LASTFM CACHE " + cachedir.list().length + "Files");
		}*/
		//LinearAudioPlayerService.stopSelf(); // サービスは必要ないので終了させる。
	}
	
	private void saveSetting() {
		
			SharedPreferences pref = 
					getSharedPreferences(LinearConst.PREF_KEY, MODE_PRIVATE);
			Editor e = pref.edit();
			
			Spinner spinner = (Spinner) this.findViewById(id.spinner1);
			
			//boolean saveflg = false;
			if (spinner != null) {
				e.putInt("FilteringIndex", spinner.getSelectedItemPosition());
				//saveflg = true;
			}
			if (linearAudioPlayerService != null){
				e.putInt("PlayEngine", linearAudioPlayerService.getPlayEngineMode());
				//saveflg = true;
			}
			e.putInt("Theme", mTheme);
			
			e.commit();
	}


	public void stop() {
		startService(new Intent(LinearAudioPlayerService.ACTION_STOP));
		SeekBar seekbar = (SeekBar)findViewById(R.id.seekBar1);
        seekbar.setProgress(0);
        TextView tvAlbum = (TextView) findViewById(R.id.TextView01);
        tvAlbum.setText("");
        TextView tvTitle = (TextView) findViewById(R.id.textView1);
        tvTitle.setText("");
        TextView tvTime = (TextView) findViewById(R.id.textView2);
        tvTime.setText("00:00");
	}
	
	class SeekRunner extends TimerTask{

		@Override
		public void run() {
			
			 // mHandlerを通じてUI Threadへ処理をキューイング
	         mHandler.post( new Runnable() {
	             public void run() {
	 
	            	 if (linearAudioPlayerService != null && linearAudioPlayerService.isPlaying()) {
	     				int currentPosition = linearAudioPlayerService.getPosition();
	     				SeekBar seekbar = (SeekBar)findViewById(R.id.seekBar1);
	     				seekbar.setProgress(currentPosition);
	     			
	     				TextView time = (TextView)findViewById(R.id.textView2);
	     				SimpleDateFormat smd = new SimpleDateFormat("mm:ss");
	     				Date date = new Date(currentPosition);
	     				time.setText(smd.format(date));
	            	 }
	             }
	         });
		}
	}
	
	ListPlayHistoryRowAdapter anylistadapter;
	RelativeLayout rl;
	private class MyPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
        	if (position == 0) {
        		container.addView(mView[0]);
        		return mView[0];
        	} else  {
	            
	            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            rl = (RelativeLayout)inflater.inflate(R.layout.anylist, null);
	            
	            TextView tvTitle = (TextView) rl.findViewById(R.id.textView1);
	            tvTitle.setTextColor(fontColor);
	            
	            ListView lv = (ListView) rl.findViewById(R.id.anylistView);
	            anylistadapter = new ListPlayHistoryRowAdapter(LinearAudioPlayer.this, R.layout.any_list_item_2, R.id.anylistView);
	            lv.setAdapter(anylistadapter);
	            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	            
	            
	            	
	                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	                    ListView listView = (ListView)parent;
	                    AudioItemBean bean = (AudioItemBean)listView.getItemAtPosition(position);
	                    
	                    // レーティング取得
	                    bean.setRating(linearAudioPlayerService.getRating(bean.getId()));
	                    
	                    //再生
	                    play((Activity) parent.getContext(),bean);
	                    
	                }

	                
	            });
	            
	            
	            
	            container.addView(rl);
	            return rl;
        	}
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager)container).removeView((View)object);
        }
 
        @Override
        public int getCount() {
            return 2;
        }
 
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }
        
        
    }
	
	/**
	 * レーティングを更新
	 * @param f
	 */
	public void updateRating(long id, String data, Float f) {
    	
		
		DBHelper mDb = new DBHelper(this);
        SQLiteDatabase db = mDb.getWritableDatabase();
        try {
            
            db.beginTransaction();
            PlayListTable q = new PlayListTable();
            
            Cursor cur = db.rawQuery(q.getSelectCountQuery(), new String[]{ String.valueOf(id) });
            cur.moveToLast();
            long rowcnt = cur.getInt(0);
            cur.close();
            
            if (rowcnt == 0) {
            	// 同じパスがあれば削除して引き継ぐ
            	Float rating = -1f;
            	int playcount = 0;
            	/*
            	Cursor cur2 = db.rawQuery(q.getSelectDataQuery(), new String[]{ data });
            	cur2.moveToFirst();
            	if (cur2.getCount() > 0) {
            		db.execSQL(q.getDeleteDataQuery(), new String[]{data});
            		rating = cur2.getFloat(cur2.getColumnIndex("_rating"));
            		playcount = cur2.getInt(cur2.getColumnIndex("_playcount"));
            	}
            	cur2.close();*/
            	
            	db.execSQL(q.getInsertQuery(), new Object[]{id, rating, playcount, null, data});
            }
            
            db.execSQL(q.getUpdateRatingQuery(), new Object[]{f ,id});
            
            db.setTransactionSuccessful();
        } finally {
            
            db.endTransaction();
            db.close();
        }
        
    	Log.d("LINEAR", "Update Rating :" + f.toString());
    	
    }
/*
	@Override
	public void onMediaScannerConnected() {
		// TODO Auto-generated method stub
		mConnection.scanFile(Environment.getExternalStorageDirectory().toString(), null);
				/*MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{ Environment.getExternalStorageDirectory().toString() },
                null,
                null);
	}

	@Override
	public void onScanCompleted(String path, Uri uri) {
		// TODO Auto-generated method stub
		//Just a message
        //Toast toast = Toast.makeText(getApplicationContext(),
        //        "Media Scanner Completed.", Toast.LENGTH_SHORT);
        //toast.show();
	}
*/
	
}
