package net.finalstream.linearaudioplayer.engine;

import java.io.IOException;

import net.finalstream.linearaudioplayer.services.LinearAudioPlayerService;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;

public class AndroidEngine implements IPlayEngine {

	MediaPlayer mediaPlayer;
	LinearAudioPlayerService mService;
	boolean mEndOfStream = false;
	
	public AndroidEngine(LinearAudioPlayerService service) {
		
		mService = service;
		init();
		
	}
	
	@Override
	public void init() {
		mediaPlayer = new MediaPlayer();
		
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			public void onCompletion(MediaPlayer mp) {
				mEndOfStream = true;
			}
		});
		
		mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

			public void onPrepared(MediaPlayer mp) {
				mService.onPrepared();
			}
		});
	}

	@Override
	public void Dispose() {
		mediaPlayer.release();
    	mediaPlayer = null;
	}

	@Override
	public void open(String filePath) {

		mediaPlayer.stop();
		mediaPlayer.reset();

		try {
			
			mediaPlayer.setDataSource(filePath);
			mediaPlayer.prepareAsync();
			
		} catch (Exception e) {
			Log.e("@error", e.getMessage());
		}
	}

	@Override
	public boolean isOpened() {

		return true;
	}

	@Override
	public boolean isPlaying() {

		return mediaPlayer.isPlaying();
	}

	@Override
	public boolean isPause() {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	@Override
	public void play() {

		mediaPlayer.start();
		mEndOfStream = false;
	}

	@Override
	public void pause() {
		
		mediaPlayer.pause();

	}

	@Override
	public void setPosition(int msec) {
		if (mediaPlayer == null) {
			return;
		}
		mediaPlayer.seekTo(msec);

	}

	@Override
	public int getPosition() {

		return mediaPlayer.getCurrentPosition();
	}

	@Override
	public void setVolume(float volume) {
		
		mediaPlayer.setVolume(volume, volume);
	}

	@Override
	public void stop() {
		
		mediaPlayer.reset();
		mediaPlayer.stop();
		
	}

	@Override
	public int getVersion() {
		// TODO 自動生成されたメソッド・スタブ
		return 0;
	}

	@Override
	public void update() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public boolean isEndOfStream() {

		return mEndOfStream;
	}

}
