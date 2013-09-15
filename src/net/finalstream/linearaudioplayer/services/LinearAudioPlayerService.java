package net.finalstream.linearaudioplayer.services;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;

import net.finalstream.linearaudioplayer.LinearAudioPlayer;
import net.finalstream.linearaudioplayer.LinearConst;
import net.finalstream.linearaudioplayer.R;
import net.finalstream.linearaudioplayer.adapter.ListPlayListRowAdapter;
import net.finalstream.linearaudioplayer.audio.AudioFocusHelper;
import net.finalstream.linearaudioplayer.audio.MusicFocusable;
import net.finalstream.linearaudioplayer.audio.MusicRetriever;
import net.finalstream.linearaudioplayer.audio.PrepareMusicRetrieverTask;
import net.finalstream.linearaudioplayer.bean.AudioItemBean;
import net.finalstream.linearaudioplayer.commons.CsUncaughtExceptionHandler;
import net.finalstream.linearaudioplayer.commons.StopWatch;
import net.finalstream.linearaudioplayer.commons.ToastMaster;
import net.finalstream.linearaudioplayer.database.DBHelper;
import net.finalstream.linearaudioplayer.database.PlayHistoryTable;
import net.finalstream.linearaudioplayer.database.PlayListTable;
import net.finalstream.linearaudioplayer.engine.AndroidEngine;
import net.finalstream.linearaudioplayer.engine.BassEngine;
import net.finalstream.linearaudioplayer.engine.FmodEngine;
import net.finalstream.linearaudioplayer.engine.IPlayEngine;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class LinearAudioPlayerService extends Service implements 
MusicFocusable,
PrepareMusicRetrieverTask.MusicRetrieverPreparedListener{
	
	private String LASTFM_APIKEY = "252f2eb2d7280e767481d739da89d6b3";
	private String LASTFM_SECRET = "cfd432e5f4e37e1cc4b4532b3b40e3de";
	private Session mLastfmSession = null;
	
	public class LinearAudioPlayerBinder extends Binder {
		
		public LinearAudioPlayerService getService() {
			return LinearAudioPlayerService.this;
		}
		
	}
	
	private class LinearAudioPlayerServiceReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			
			/*
			if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
				//ヘッドホンが抜かれた時の処理
				LinearAudioPlayerService.this.processStopRequest();
			}*/
			
			// plug状態を取得
            boolean isPlugged = false;
            if (intent.getIntExtra("state", 0) > 0) {
                isPlugged = true;
            }

            // plug状態でメッセージを変更。
            if (isPlugged) {
                // ヘッドセットが挿された
            	LinearAudioPlayerService.this.processSkipRequest(new Intent());
            } else {
                // ヘッドセットが抜かれた
            	LinearAudioPlayerService.this.processStopRequest();
            }
			
		}
	}
	
	private static final int NOTIFICATION_ID = 1;
	public static final String ACTION = "Linear Audio Player Service";
	
	private IPlayEngine playEngine;
	//ScheduledExecutorService scheduledEx;
	
	// The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    DateFormat df;
    
	// These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_START = "net.finalstream.linearaudioplayer.action.START";
    public static final String ACTION_PLAY = "net.finalstream.linearaudioplayer.action.PLAY";
    public static final String ACTION_PAUSE = "net.finalstream.linearaudioplayer.action.PAUSE";
    public static final String ACTION_STOP = "net.finalstream.linearaudioplayer.action.STOP";
    public static final String ACTION_SKIP = "net.finalstream.linearaudioplayer.action.SKIP";
    public static final String ACTION_GETPLAYINGITEM = "net.finalstream.linearaudioplayer.action.GETPLAYINGITEM";
    // ActivityからServiceにListをキープするときのアクション
    public static final String ACTION_KEEPLISTDATA = "net.finalstream.linearaudioplayer.action.KEEPLISTDATA";
    
    // 再生後にActivityにAudioItemを渡すアクション
    public static final String RECVACTION_PLAYED = "net.finalstream.linearaudioplayer.recvaction.PLAYED";
    // ServiceからActivityに再生アイテムを渡すアクション
    public static final String RECVACTION_RESTORE = "net.finalstream.linearaudioplayer.recvaction.RESTORE";
    
    // Our instance of our MusicRetriever, which handles scanning for media and
    // providing titles and URIs as we need.
    MusicRetriever mRetriever;
	
 // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;
    
    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    Uri mWhatToPlayAfterRetrieve = null;
    
	// our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;

    // title of the song we are currently playing
    AudioItemBean mPlayingItem = new AudioItemBean();
    
    String mLastfmUser;
    String mLastfmPassword;
    public void setLastfmUser(String user){
    	mLastfmUser = user;
    }
    public void setLastfmPassword(String pass){
    	mLastfmPassword = pass;
    }
    
    public AudioItemBean getPlayingItem() {
    	return mPlayingItem;
    }
    
    NotificationManager mNotificationManager;
    Notification mNotification = null;
    private int mPlayEngineMode = 2;
    
    public void setPlayEngineMode(int mPlayEngineMode) {
		this.mPlayEngineMode = mPlayEngineMode;
		playEngine = null;
		createMediaPlayerIfNeeded();
	}

	public int getPlayEngineMode() {
		return mPlayEngineMode;
	}


	// indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
                    // paused in this state if we don't have audio focus. But we stay in this state
                    // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    State mState = State.Retrieving;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
	
    private long mRelaxTime = System.currentTimeMillis();
	
	@Override
	public IBinder onBind(Intent arg0) {
		
		Log.i("LINEAR", "Service onBind.");
		
		
		
		return new LinearAudioPlayerBinder();
	}
	
	private final LinearAudioPlayerServiceReceiver receiver = new LinearAudioPlayerServiceReceiver();
	
	// 再生履歴リスト
	LinkedList<AudioItemBean> mPlayHistory = new LinkedList<AudioItemBean>();
	// シャッフル再生リスト(RowNoを格納)
	LinkedList<Long> mShufflePlaylist = new LinkedList<Long>();
	
	// 再生カウントストップウオッチ
	StopWatch mPlayCountStopwatch  = new StopWatch();
	
	public StopWatch getStopWatch(){
		return mPlayCountStopwatch;
	}
	
	public LinkedList<AudioItemBean> getmPlayHistory() {
		return mPlayHistory;
	}
	
	public void clearPlayHistory() {
		DBHelper mDb = new DBHelper(this);
        SQLiteDatabase db = mDb.getWritableDatabase();
        try {
            
            db.beginTransaction();
            PlayHistoryTable q = new PlayHistoryTable();
            
            db.execSQL(q.getDeleteAllQuery());
            
            db.setTransactionSuccessful();
        } finally {
            
            db.endTransaction();
            db.close();
        }
        mPlayHistory.clear();
        //mPlayHistoryOnlyId.clear();
	}

	@Override
	public void onCreate() {
			Log.i("LINEAR", "Service onCreate.");
			
			//File cachedir = new File(getCacheDir() + "/.last.fm-cache");
	        //if (cachedir.exists()) {
	        //	deleteFile(cachedir);
	        //}
			
			restore();
			
			createMediaPlayerIfNeeded();
			
		//try {
			//scheduledEx =  Executors.newSingleThreadScheduledExecutor();
			df = new SimpleDateFormat(LinearConst.DATEFORMAT_YYYYMMDDHHMMSSSSS);
			 
			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			
			IntentFilter filter = new IntentFilter(LinearAudioPlayerService.ACTION);
	        filter.addAction(Intent.ACTION_HEADSET_PLUG);
	        
			registerReceiver( receiver , filter );
			
			// Create the retriever and start an asynchronous task that will prepare it.
	        //mRetriever = new MusicRetriever(getContentResolver());
	        //(new PrepareMusicRetrieverTask(mRetriever,this)).execute();

			
			// create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
	        if (android.os.Build.VERSION.SDK_INT >= 8)
	            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
	        else
	            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

	        //processStartRequest();
	        
	        /*
			
			*/
			//mediaPlayer.prepare();
		//} catch (Exception e) {
		//}
	    
	    // 一時停止サービス停止    
		//mSelfStopThread.start();
		
		
		
	}
	
	/**
	 * レストア
	 */
	private void restore() {
		
		SharedPreferences pref =
    			getSharedPreferences(LinearConst.PREF_KEY, MODE_PRIVATE);
    	mPlayEngineMode = pref.getInt("PlayEngine", 2);
		
		//セレクト処理
		PlayHistoryTable q = new PlayHistoryTable();
		mPlayHistory = q.getPlayHistory(this);
		
		//for (AudioItemBean bean : mPlayHistory) {
		//	mPlayHistoryOnlyId.add(bean.getId());
		//}
		
		Log.d("LINEAR","Restore PlayHistoryCount: " + mPlayHistory.size());
		
		
	}

	/** Returns a random Item. If there are no items available, returns null. */
    public AudioItemBean getRandomItem() {
        if (mList.size() <= 0) return null;
        
        if (mShufflePlaylist.size() == 0) {
        	shuffleList(mList);
        }
        
        Long  id = mShufflePlaylist.removeFirst();
        
        int rowNo = findRowNo(id);
        
        while(rowNo == -1) {
        	if (mShufflePlaylist.size() == 0) {
            	shuffleList(mList);
            }
        	id = mShufflePlaylist.removeFirst();
        	rowNo = findRowNo(id);
        }
        
        return mList.get(rowNo);
        
        /*
        int loopcnt = 0;
        
        AudioItemBean bean = mList.get(new Random().nextInt(mList.size()));
    	
        while(mPlayHistoryOnlyId.contains(bean.getId()) && loopcnt < 100) {
			bean = mList.get(new Random().nextInt(mList.size()));
			loopcnt++;
		}
        
        if (loopcnt < 100) {
        	return bean;
        } else {
        	return mList.get(new Random().nextInt(mList.size()));
        }
        */
        /*
        if (mList.size() > LinearConst.PLAYHISTORY_MAX_COUNT) {
        	// 再生履歴MAX数よりプレイリストの件数が多いとき
        	v
			while(mPlayHistoryOnlyId.contains(bean.getId())) {
				bean = mList.get(new Random().nextInt(mList.size()));
			}
			return bean;
        			
        } else {
        	
        	return mList.get(new Random().nextInt(mList.size()));
        
        }
        */
        
    }
	
	private int findRowNo(Long id) {
		int result = -1;
		int i = 0;
		for (AudioItemBean bean : mList) {
			if (bean.getId() == id) {
				result = i;
				break;
			}
			i++;
		}
		return result;
	}

	/**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_PLAY)) processPlayRequest();
        else if (action.equals(ACTION_PAUSE)) processPauseRequest();
        else if (action.equals(ACTION_SKIP)) processSkipRequest(intent);
        else if (action.equals(ACTION_STOP)) processStopRequest();
        else if (action.equals(ACTION_GETPLAYINGITEM)) processGetPlayingItemRequest();
        else if (action.equals(ACTION_KEEPLISTDATA)) processKeepListDataRequest(intent);
        
        return START_NOT_STICKY; // Means we started the service, but don't want it to
                                 // restart in case it's killed.
    }
	
    List<AudioItemBean> mList;
	private void processKeepListDataRequest(Intent intent) {
		mList = (List<AudioItemBean>) intent.getSerializableExtra("LISTDATA");
		shuffleList(mList);
	}

	private void shuffleList(List<AudioItemBean> mList2) {
		List<AudioItemBean> tempList = new ArrayList<AudioItemBean>(mList);
		
		Collections.shuffle(tempList);
		mShufflePlaylist.clear();
		for (AudioItemBean audioItemBean : tempList) {
			mShufflePlaylist.add(audioItemBean.getId());
		}
	}

	private void processGetPlayingItemRequest() {
		
		if (mState == State.Playing || mState == State.Paused) {
			Intent intent = new Intent(RECVACTION_RESTORE);
			intent.putExtra("AUDIOITEM", mPlayingItem);
		
			sendBroadcast(intent);
		}
	}

	@Override
	public void onStart(Intent intent, int startID) {
		
	}
	
	/*
	public void play(AudioItemBean bean) {
		
		
		
		// 音楽を再生
		try {
			
			playEngine.open(bean.getFilePath());
			if (mPlayEngineMode != 0) {
				this.onPrepared();
			}
			
		} catch (Exception e) {
			Log.e("@error", e.getMessage());
		}

		// Notificationの表示
		NotificationManager mNotificationManager =
			(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.ic_notification,
				getNotificationMessage(bean), System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, LinearAudioPlayer.class), 0);
		notification.setLatestEventInfo(getApplicationContext(),
				bean.getTitle(), bean.getArtist(), contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, notification);

	}*/
	
	private String getNotificationMessage(AudioItemBean bean) {
		
		return bean.getTitle() +" - " + bean.getArtist() + " (" + viewRating(bean.getRating()) + ")";
		
	}
	
	private String viewRating(float rating) {
		String result = String.valueOf(rating);
		if (rating == -1.0f) {
			result = this.getResources().getString(R.string.filtering_notrated);
		}
		return result;
	}

	public void pause() {
		playEngine.pause();
		if (playEngine.isPause()) {
			mUpdateHandler.removeMessages(0);
		} else {
			mUpdateHandler.sendMessageDelayed(mUpdateHandler.obtainMessage(0), 1000);
		}
	}
	
	public void setPosition(int msec) {
		if (playEngine == null) {
			return;
		}
		playEngine.setPosition(msec);
	}
	
	public int getPosition() {
		return playEngine.getPosition();
	}
	
	public boolean isPlaying() {
		if (playEngine == null) {
			return false;
		}
		return playEngine.isPlaying();
	}
	
	@Override
	public void onDestroy() {
		
		// Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        Log.d("LINEAR", "State change : Sttoped");
        relaxResources(true);
        giveUpAudioFocus();
        unregisterReceiver( receiver );
        Log.i("LINEAR", "Service onDestory");
		
        
        //ToastMaster.makeText(this, "Stop LinearAudioPlayer", Toast.LENGTH_SHORT).show();
	}
	
	//ファイルやフォルダを削除
	//フォルダの場合、中にあるすべてのファイルやサブフォルダも削除されます
	public static boolean deleteFile(File dirOrFile) {
		if (dirOrFile.isDirectory()) {//ディレクトリの場合
			String[] children = dirOrFile.list();//ディレクトリにあるすべてのファイルを処理する
			for (int i=0; i<children.length; i++) {
				boolean success = deleteFile(new File(dirOrFile, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dirOrFile.delete();
	}
	
    public void onGainedAudioFocus() {
        //Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        //Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
        //    "no duck"), Toast.LENGTH_SHORT).show();
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (playEngine != null && playEngine.isPlaying())
            configAndStartMediaPlayer();
    }
    
    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (playEngine.isPlaying()) pause();
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            playEngine.setVolume(DUCK_VOLUME);  // we'll be relatively quiet
        else
            playEngine.setVolume(1.0f); // we can be loud

        if (!playEngine.isPlaying()) {
        	playEngine.play();
        	mUpdateHandler.sendMessageDelayed(mUpdateHandler.obtainMessage(0), 0);
        //} else if (playEngine.isPause()) {
        } else {
        	pause();
        }
        
        String toastMsg = mPlayingItem.getTitle() + "\n" + mPlayingItem.getArtist() + "\n(" + viewRating(mPlayingItem.getRating()) + ")";
        ToastMaster.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
        
    }
    
    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    
    void createMediaPlayerIfNeeded() {
        if (playEngine == null) {
        	
        	switch (mPlayEngineMode) {
			case 0:
				Log.i(LinearConst.DEBUG_TAG, "PlayEngine : Android Engine.");
				playEngine = new AndroidEngine(this);
				break;

			case 1:
				Log.i(LinearConst.DEBUG_TAG, "PlayEngine : Fmod Engine.");
				playEngine = new FmodEngine(this);
				break;
			case 2:
				Log.i(LinearConst.DEBUG_TAG, "PlayEngine : Bass Engine.");
				playEngine = new BassEngine(this);
				break;
			}
        	
        	//mediaPlayer = new MediaPlayer();
        	//fmodEngine = new FmodEngine(this);
            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
        	//mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
        	//mediaPlayer.setOnPreparedListener(this);
        	//mediaPlayer.setOnCompletionListener(this);
        	//mediaPlayer.setOnErrorListener(this);
        }
    }
    
    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        mState = State.Stopped;
        Log.d("LINEAR", "State change : Sttoped");

        //Intent intent = new Intent(RECVACTION_GETLIST);
        //intent.putExtra("LIST", (Serializable) mRetriever.getItems());
        //sendBroadcast(intent);
        
        // If the flag indicates we should start playing after retrieving, let's do that now.
        //if (mStartPlayingAfterRetrieve) {
            //tryToGetAudioFocus();
            //playNextSong(null);
        //}
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                        && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     * @param filepath 
     */
    public void playNextSong(AudioItemBean audioItem) {
        
    	
    	mState = State.Stopped;
    	Log.d("LINEAR", "State change : Sttoped");
        relaxResources(false); // release everything except MediaPlayer

        try {
            AudioItemBean playingItem = null;

            if (audioItem == null) {
            	playingItem = getRandomItem();
            	
            	// サービスでランダム変更された場合、Activityに返す。
            	Intent intent = new Intent(RECVACTION_RESTORE);
        		intent.putExtra("AUDIOITEM", playingItem);
        		sendBroadcast(intent);
            }else {
            	playingItem = audioItem;
            }
            
            if (playingItem == null) {
                Toast.makeText(this,
                        "No Play Music",
                        Toast.LENGTH_SHORT).show();
                processStopRequest(true); // stop everything!
                return;
            }

            // set the source of the media player a a content URI
            createMediaPlayerIfNeeded();
            //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            playEngine.open(playingItem.getFilePath());
            Log.i("LINEAR", "Open File : " + playingItem.getFilePath());
            
            //mSongTitle = playingItem.getTitle();
            mPlayingItem = playingItem;
            
            mState = State.Preparing;
            Log.d("LINEAR", "State change : Preparing");
            
            setUpAsForeground();

//            // Use the media button APIs (if available) to register ourselves for media button
//            // events
//
//            MediaButtonHelper.registerMediaButtonEventReceiverCompat(
//                    mAudioManager, mMediaButtonReceiverComponent);
//
//            // Use the remote control APIs (if available) to set the playback state
//
//            if (mRemoteControlClientCompat == null) {
//                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
//                intent.setComponent(mMediaButtonReceiverComponent);
//                mRemoteControlClientCompat = new RemoteControlClientCompat(
//                        PendingIntent.getBroadcast(this /*context*/,
//                                0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
//                RemoteControlHelper.registerRemoteControlClient(mAudioManager,
//                        mRemoteControlClientCompat);
//            }
//
//            mRemoteControlClientCompat.setPlaybackState(
//                    RemoteControlClient.PLAYSTATE_PLAYING);
//
//            mRemoteControlClientCompat.setTransportControlFlags(
//                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
//                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
//                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
//                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);
//
//            // Update the remote controls
//            mRemoteControlClientCompat.editMetadata(true)
//                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingItem.getArtist())
//                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingItem.getAlbum())
//                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.getTitle())
//                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
//                            playingItem.getDuration())
//                    // TODO: fetch real item artwork
//                    .putBitmap(
//                            RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
//                            mDummyAlbumArt)
//                    .apply();

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            if (mPlayEngineMode != 0) {
				this.onPrepared();
			}

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            //if (mIsStreaming) mWifiLock.acquire();
            //else if (mWifiLock.isHeld()) mWifiLock.release();
        }
        catch (Exception ex) {
            Log.e("LINEAR", "Exception playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && playEngine != null) {
        	playEngine.stop();
        	mUpdateHandler.removeMessages(0);
        	//fmodEngine = null;
        }
        mRelaxTime = System.currentTimeMillis();
        
        // we can also release the Wifi lock, if we're holding it
        //if (mWifiLock.isHeld()) mWifiLock.release();
    }
    
    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }
    
    /** Called when media player is done playing current song. */
    /*
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
        playNextSong(null);
    	//sendBroadcast(new Intent(RECVACTION_ENDOFSTREAM));
    }*/

    /** Called when media player is done preparing. */
    public void onPrepared() {
        
    	
    	
    	if (!playEngine.isOpened()) {
    		playNextSong(null);
    		return;
    	}
    	
    	mPlayingItem.setSort(Long.parseLong(df.format(new Date())));
    	
    	// The media player is done preparing. That means we can start playing!
        mState = State.Playing;
        Log.d("LINEAR", "State change : Playing");
        updateNotification(mPlayingItem);
        configAndStartMediaPlayer();
        
        afterPlay();
    }
    

    private void afterPlay() {
		
    	mPlayCountStopwatch.reset();
    	mPlayCountStopwatch.start();
    	
    	
    	if (!"".equals(mLastfmUser)
    			&& !"".equals(mLastfmPassword)) {
    		
    		
    	
    			
    		new Thread(new Runnable() {
    		    public void run() {
    		    	try {
	    		    	if (mLastfmSession == null) {
	    		    		
	    		    		mLastfmSession = Authenticator.getMobileSession(
	    		    				mLastfmUser, 
	    		    				mLastfmPassword,
	    		    				LASTFM_APIKEY, 
	    		    				LASTFM_SECRET,
	    		    				getCacheDir().getPath());
	    		    	}
	    		    	if (mLastfmSession != null) {
	    		    	Track.updateNowPlaying(mPlayingItem.getArtist(), mPlayingItem.getTitle(), mLastfmSession);
	    		    	}
    		    	}catch (Exception ex) {
    	    			ex.printStackTrace();
    	    		}
    		    }
    		  }).start();
    		
    		
	    }

    }

	/** Updates the notification. */
    void updateNotification(AudioItemBean bean) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), LinearAudioPlayer.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification.setLatestEventInfo(getApplicationContext(), bean.getTitle(), bean.getArtist() + " (" + viewRating(bean.getRating()) + ")", pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }
    
    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground() {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), LinearAudioPlayer.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = new Notification();
        mNotification.tickerText = getNotificationMessage(mPlayingItem);
        mNotification.icon = R.drawable.ic_notification;
        mNotification.flags = Notification.FLAG_ONGOING_EVENT
        | Notification.FLAG_NO_CLEAR;
        mNotification.setLatestEventInfo(getApplicationContext(), "LinearAudioPlayer",
        		mNotification.tickerText, pi);
        startForeground(NOTIFICATION_ID, mNotification);
    }
    
    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    /*
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
            Toast.LENGTH_SHORT).show();
        Log.e("LINEAR", "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }*/

    void processPlayRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready
            mWhatToPlayAfterRetrieve = null; // play a random song
            mStartPlayingAfterRetrieve = true;
            return;
        }

        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(mPlayingItem);
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            Log.d("LINEAR", "State change : Playing");
            setUpAsForeground();
            configAndStartMediaPlayer();
        }

        // Tell any remote controls that our playback state is 'playing'.
//        if (mRemoteControlClientCompat != null) {
//            mRemoteControlClientCompat
//                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
//        }
    }

    void processPauseRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            Log.d("LINEAR", "State change : Paused");
            mPlayCountStopwatch.suspend();
            pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }  else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            Log.d("LINEAR", "State change : Playing");
            setUpAsForeground();
            mPlayCountStopwatch.resume();
            configAndStartMediaPlayer();
        }

//        // Tell any remote controls that our playback state is 'paused'.
//        if (mRemoteControlClientCompat != null) {
//            mRemoteControlClientCompat
//                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
//        }
    }

    void processSkipRequest(Intent intent) {
        //if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();

            AudioItemBean bean = null;
            if (intent.getSerializableExtra("AUDIOITEM") != null) {
            	bean = (AudioItemBean) intent.getSerializableExtra("AUDIOITEM");
            } 
           
            if (bean == null) {
            	//sendBroadcast(new Intent(RECVACTION_ENDOFSTREAM));
            	playNextSong(null);
            } else {
            	playNextSong(bean);
            }
        //}
    }

    void processStopRequest() {
        processStopRequest(false);
    }
    
    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;
            Log.d("LINEAR", "State change : Sttoped");

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
//            if (mRemoteControlClientCompat != null) {
//                mRemoteControlClientCompat
//                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
//            }
            mPlayCountStopwatch.stop();
            
            if (playEngine != null) {
            	playEngine.stop();
            	mUpdateHandler.removeMessages(0);
            }
            // service is no longer necessary. Will be started again if needed.
            stopSelf();
            
            ToastMaster.makeText(this, "Stop LinearAudioPlayer", Toast.LENGTH_SHORT).show();
        }
    }
    
    void processStartRequest() {
//        Intent intent = new Intent(RECVACTION_GETLIST);
//        intent.putExtra("LIST", (Serializable) mRetriever.getItems());
//        sendBroadcast(intent);
    }
    
    public boolean isStopped() {
    	if (mState == State.Stopped) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * 	再生カウントアップ
     */
    public void updatePlayCount() {
    	
    	updatePlayHistory();
    	
        DBHelper mDb = new DBHelper(this);
        SQLiteDatabase db = mDb.getWritableDatabase();
        try {
            
            db.beginTransaction();
            PlayListTable q = new PlayListTable();
            
            Cursor cur = db.rawQuery(q.getSelectCountQuery(), new String[]{ String.valueOf(mPlayingItem.getId()) });
            cur.moveToLast();
            long rowcnt = cur.getInt(0);
            cur.close();
            
            if (rowcnt == 0) {
            	db.execSQL(q.getInsertQuery(), new Long[]{mPlayingItem.getId()});
            }
            
            db.execSQL(q.getUpdatePlayStateQuery(), new Long[]{mPlayingItem.getId()});
            
            db.setTransactionSuccessful();
        } finally {
            
            db.endTransaction();
            db.close();
        }
    	
    }
    
    private void updatePlayHistory() {
    	
    	// 再生後AudioItemをに再生履歴に追加。
        AudioItemBean double_removeItem = null;
        AudioItemBean limit_removeItem = null;
        int i = 0;
		for (AudioItemBean aib : mPlayHistory) {
			if (mPlayingItem.getId() == aib.getId()) {
				double_removeItem =  mPlayHistory.remove(i);
				break;
			}
			i++;
		}
		mPlayHistory.addFirst(mPlayingItem);
        
		if (mPlayHistory.size() > LinearConst.PLAYHISTORY_MAX_COUNT) {
			// Limit超えている
			limit_removeItem = mPlayHistory.removeLast();
		}
		
		// 再生履歴insert処理
        DBHelper mDb = new DBHelper(this);
        SQLiteDatabase db = mDb.getWritableDatabase();
        try {
            
            db.beginTransaction();
            PlayHistoryTable q = new PlayHistoryTable();
            if (double_removeItem != null) {
            	
            	db.execSQL(q.getDeleteQuery(), new Long[]{double_removeItem.getId()});

            }
            
            if (limit_removeItem != null) {
            	
            	db.execSQL(q.getDeleteQuery(), new Long[]{limit_removeItem.getId()});

            }
            
            q = new PlayHistoryTable();
            q.mId = (int) mPlayingItem.getId();
            q.mSort = Long.parseLong(df.format(new Date()));
            db.execSQL(q.getInsertQuery(), q.getInsertBindItem());
            
            //mPlayHistoryOnlyId.add(mPlayingItem.getId());
            
            Log.d("LINEAR", "PLAYHISTORY INSERT :" + q.mId + " , " + q.mSort);
            
            db.setTransactionSuccessful();
        } finally {
            
            db.endTransaction();
            db.close();
        }
    	
    }
    
    private Thread mSelfStopThread = new Thread() {
		public void run() {
			while (true) {
				// 一時停止後 5 分再生がなかったらサービスを止める
				boolean needSleep = false;
				if (mState == LinearAudioPlayerService.State.Preparing || mState == LinearAudioPlayerService.State.Playing || mState == LinearAudioPlayerService.State.Stopped) {
					needSleep = true;
				} else if (mRelaxTime + 5 * 1000 * 60 > System.currentTimeMillis()) {
					needSleep = true;
				}
				if (!needSleep) {
					break;
				}
				try {
					Thread.sleep(1 * 1000 * 60); // 停止中でない、または 10 分経過してない場合は 1 分休む
				} catch (InterruptedException e) {
				}
			}
			LinearAudioPlayerService.this.stopSelf();
			Log.i("LINEAR", "AutoStopService");
		}
	};
	
	public int getPlayEngineVersion() {

		return playEngine.getVersion();
	}
	
	public float getRating(long id) {
		float result = -1.0f;
		DBHelper mDb = new DBHelper(this);
        SQLiteDatabase db = mDb.getReadableDatabase();
        try {
            
            db.beginTransaction();
            PlayListTable q = new PlayListTable();
            
            Cursor cur = db.rawQuery(q.getSelectGetRatingQuery(), new String[]{ String.valueOf(id) });
            cur.moveToLast();
            result = cur.getFloat(0);
            cur.close();

            db.setTransactionSuccessful();
        } finally {
            
            db.endTransaction();
            db.close();
        }
		return result;
	}
	
	
	private Handler mUpdateHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{		
			playEngine.update();
			
			if (playEngine.isEndOfStream()) {
				Log.d("LINEAR", "Play EndOfStream!");
				LinearAudioPlayerService.this.playNextSong(null);
			}
			
			
			// プレイカウントインクリメント
            if (LinearAudioPlayerService.this.getStopWatch().getTime() > 0 &&
                (LinearAudioPlayerService.this.getStopWatch().getTime()/1000) >
                LinearConst.PLAYCOUNTUP_SECOND)
            {
            	
            	
        			
            		new Thread(new Runnable() {
            		    public void run() {
            		    	try {
            		    		if (mLastfmSession != null) {
	            		    		Track.scrobble(mPlayingItem.getArtist(), mPlayingItem.getTitle(),(int) (System.currentTimeMillis() / 1000), mLastfmSession);
	            		    	
	            		    		if (mPlayingItem.getRating() != -1.0f) {
	            		    			if (mPlayingItem.getRating() == 1.0f) {
	            		    				Track.love(mPlayingItem.getArtist(), mPlayingItem.getTitle(), mLastfmSession);
	            		    			} else if (mPlayingItem.getRating() == 0.0f) {
	            		    				Track.unlove(mPlayingItem.getArtist(), mPlayingItem.getTitle(), mLastfmSession);
	            		    			}
	            		    		}
	            		    	}
	            		    }catch (Exception ex) {
	                			ex.printStackTrace();
	                		}
            		    }
            		    
            		  }).start();
            		
            	
            	
            	LinearAudioPlayerService.this.updatePlayCount();
                Log.d("LINEAR", "PlayCount UP!");
                
                
                
                LinearAudioPlayerService.this.getStopWatch().reset();
            }
			
			removeMessages(0);
		    sendMessageDelayed(obtainMessage(0), 1000);
		}
	};
}
