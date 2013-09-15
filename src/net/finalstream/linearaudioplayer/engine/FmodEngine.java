package net.finalstream.linearaudioplayer.engine;

import net.finalstream.linearaudioplayer.LinearConst;
import net.finalstream.linearaudioplayer.services.LinearAudioPlayerService;

import org.fmod.FMODAudioDevice;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FmodEngine implements IPlayEngine {
	
	LinearAudioPlayerService mService;
	
	
	
	private FMODAudioDevice mFMODAudioDevice = new FMODAudioDevice();
	
	static {
		System.loadLibrary("fmodex");
        System.loadLibrary("main");
	}

	public FmodEngine(LinearAudioPlayerService service){
        mService = service;
		init();
        
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#init()
	 */
	@Override
	public void init() {
		mFMODAudioDevice.start();
    	cBegin();
    	Log.i("LINEAR", "Start FmodAudioDeice.");
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#Dispose()
	 */
	@Override
	public void Dispose(){
		cEnd();
		mFMODAudioDevice.stop();
		Log.i("LINEAR", "End FmodAudioDeice.");
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#open(java.lang.String)
	 */
	@Override
	public void open(String filePath) {
		
		cStop();
		//if (!new File(filePath).exists()) {
		//	mService.playNextSong(null);
		//	return;
		//}
		cOpen(filePath);
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#isOpened()
	 */
	@Override
	public boolean isOpened(){
		return cIsOpened();
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#isPlaying()
	 */
	@Override
	public boolean isPlaying() {
		return cIsPlaying();
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#isPause()
	 */
	@Override
	public boolean isPause() {
		return cIsPause();
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#play()
	 */
	@Override
	public void play() {
		cPlay();
		
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#pause()
	 */
	@Override
	public void pause(){
		cPause();
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#setPosition(int)
	 */
	@Override
	public void setPosition(int msec) {
		cSetPosition(msec);
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#getPosition()
	 */
	@Override
	public int getPosition(){
		return cGetPosition();
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#setVolume(float)
	 */
	@Override
	public void setVolume(float volume) {
		cSetVolume(volume);
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#stop()
	 */
	@Override
	public void stop(){
		cStop();
	}
	
	/* (�� Javadoc)
	 * @see net.finalstream.linearaudioplayer.engine.IPlayEngine#getVersion()
	 */
	@Override
	public int getVersion(){
		return cGetVersion();
	}
	
	public native void cBegin();
	public native void cUpdate();
	public native void cOpen(String filePath);
	public native void cEnd();
	public native boolean cIsPlaying();
	public native boolean cIsPause();
	public native boolean cIsOpened();
	public native void cPlay();
	public native void cPause();
	public native void cSetPosition(int msec);
	public native int cGetPosition();
	public native int cGetVersion();
	public native void cSetVolume(float volume);
	public native void cStop();

	@Override
	public void update() {
		cUpdate();
		
	}

	@Override
	public boolean isEndOfStream() {
		if (!mService.isStopped() && !isPlaying()) {
			return true;
		} else {
			return false;
		}
	}
}
