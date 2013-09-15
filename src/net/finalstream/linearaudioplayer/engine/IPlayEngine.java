package net.finalstream.linearaudioplayer.engine;


public interface IPlayEngine {
	
	public abstract void init();

	public abstract void Dispose();

	public abstract void open(String filePath);

	public abstract boolean isOpened();

	public abstract boolean isPlaying();

	public abstract boolean isPause();

	public abstract void play();

	public abstract void pause();

	public abstract void setPosition(int msec);

	public abstract int getPosition();

	public abstract void setVolume(float volume);

	public abstract void stop();

	public abstract int getVersion();

	public abstract void update();

	public abstract boolean isEndOfStream();

}