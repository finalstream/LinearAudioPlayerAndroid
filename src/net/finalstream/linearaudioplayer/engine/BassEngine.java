package net.finalstream.linearaudioplayer.engine;

import android.util.Log;

import com.un4seen.bass.BASS;

import net.finalstream.linearaudioplayer.LinearConst;
import net.finalstream.linearaudioplayer.services.LinearAudioPlayerService;

public class BassEngine implements IPlayEngine {

	int channel;				// channel handle
	LinearAudioPlayerService mService;
	
	public BassEngine(LinearAudioPlayerService service) {
		mService = service;
		init();
	}

	@Override
	public void init() {
		// initialize default output device
		if (!BASS.BASS_Init(-1, 44100, 0)) {
			Log.e(LinearConst.DEBUG_TAG, "Can't initialize device");
			return;
		}
	}

	@Override
	public void Dispose() {
		BASS.BASS_Stop();
		BASS.BASS_Free();
	}

	@Override
	public void open(String filePath) {
		stop();
		channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, 0);
	}

	@Override
	public boolean isOpened() {
		return true;
	}

	@Override
	public boolean isPlaying() {
		boolean playing = false;

        if (channel != 0)
        {
            int status = BASS.BASS_ChannelIsActive(channel);

            if (status == BASS.BASS_ACTIVE_PLAYING)
            {
                playing = true;
            }
        }

        return playing;
	}

	@Override
	public boolean isPause() {
		boolean pause = false;

        if (channel != 0)
        {
            int status = BASS.BASS_ChannelIsActive(channel);

            if (status == BASS.BASS_ACTIVE_PAUSED)
            {
                pause = true;
            }
        }

        return pause;
	}

	@Override
	public void play() {
		
		if (channel != 0)
        {
            
            BASS.BASS_ChannelPlay(channel, false);

        }

	}

	@Override
	public void pause() {
		
		BASS.BASS_ChannelPause(channel);
	
	}

	@Override
	public void setPosition(int msec) {
		if (channel != 0)
        {
            long position = BASS.BASS_ChannelSeconds2Bytes(channel, (double)((double)msec / 1000));
            BASS.BASS_ChannelSetPosition(channel, position, BASS.BASS_POS_BYTE);
        }
	}

	@Override
	public int getPosition() {
		long position = BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE);
        double seconds = BASS.BASS_ChannelBytes2Seconds(channel, position);
        return (int) (seconds * 1000);
	}

	@Override
	public void setVolume(float volume) {
		 if (channel != 0)
         {
             BASS.BASS_ChannelSetAttribute(
                 channel, 
                 BASS.BASS_ATTRIB_VOL, 
                 volume);
         }

	}

	@Override
	public void stop() {
		if (channel != 0)
        {
            BASS.BASS_ChannelStop(channel);
            BASS.BASS_ChannelSetPosition(channel, 0, BASS.BASS_POS_BYTE);
            BASS.BASS_StreamFree(channel);
        }
	}

	@Override
	public int getVersion() {

		return BASS.BASS_GetVersion();
	}

	@Override
	public void update() {
		// TODO 自動生成されたメソッド・スタブ

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
