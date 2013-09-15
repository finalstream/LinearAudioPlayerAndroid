/*===============================================================================================
 PlaySound Example
 Copyright (c), Firelight Technologies Pty, Ltd 2004-2011.

 This example shows how to simply load and play multiple sounds.  This is about the simplest
 use of FMOD.
 This makes FMOD decode the into memory when it loads.  If the sounds are big and possibly take
 up a lot of ram, then it would be better to use the FMOD_CREATESTREAM flag so that it is
 streamed in realtime as it plays.
===============================================================================================*/

#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include "fmod.h"
#include "fmod_errors.h"

#define NUM_SOUNDS 3

FMOD_SYSTEM  *gSystem  = 0;
FMOD_CHANNEL *gChannel = 0;
FMOD_SOUND	 *gSound;

#define CHECK_RESULT(x) \
{ \
	FMOD_RESULT _result = x; \
	if (_result != FMOD_OK) \
	{ \
		__android_log_print(ANDROID_LOG_ERROR, "fmod", "FMOD error! (%d) %s\n%s:%d", _result, FMOD_ErrorString(_result), __FILE__, __LINE__); \
		exit(-1); \
	} \
}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cBegin(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;

	result = FMOD_System_Create(&gSystem);
	CHECK_RESULT(result);

	result = FMOD_System_Init(gSystem, 32, FMOD_INIT_NORMAL, 0);
	CHECK_RESULT(result);

	/*
	result = FMOD_System_CreateSound(gSystem, "/sdcard/fmod/drumloop.wav", FMOD_DEFAULT | FMOD_LOOP_OFF, 0, &gSound[0]);
	CHECK_RESULT(result);

	result = FMOD_System_CreateSound(gSystem, "/sdcard/fmod/jaguar.wav", FMOD_DEFAULT, 0, &gSound[1]);
	CHECK_RESULT(result);

	result = FMOD_System_CreateSound(gSystem, "/sdcard/fmod/swish.wav", FMOD_DEFAULT, 0, &gSound[2]);
	CHECK_RESULT(result);
	*/
}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cOpen(JNIEnv *env, jobject thiz, jstring filepathstring)
{
	FMOD_RESULT result = FMOD_OK;

	char *filepath = (char *)(*env)->GetStringUTFChars(env, filepathstring, 0);

	result = FMOD_System_CreateStream(gSystem, filepath, FMOD_DEFAULT, 0, &gSound);
	//CHECK_RESULT(result);

}

jboolean Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cIsOpened(JNIEnv *env, jobject thiz)
{
	if (gSound != 0) {
		return 1;
	} else {
		return 0;
	}

}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cUpdate(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT	result = FMOD_OK;

	result = FMOD_System_Update(gSystem);
	CHECK_RESULT(result);
}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cEnd(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	unsigned int i = 0;

	//for (i = 0; i < NUM_SOUNDS; i++)
	//{
	if (gSound != 0)
	{
		result = FMOD_Sound_Release(gSound);
		CHECK_RESULT(result);
	}
	//}

	result = FMOD_System_Release(gSystem);
	CHECK_RESULT(result);
}

jboolean Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cIsPlaying(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_BOOL playing = 0;

	if (gChannel != 0)
	{
		result = FMOD_Channel_IsPlaying(gChannel, &playing);
		if (result != FMOD_ERR_INVALID_HANDLE && result != FMOD_ERR_CHANNEL_STOLEN)
		{
			CHECK_RESULT(result);
		}
	}

	return playing;
}

jboolean Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cIsPause(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_BOOL pause = 0;

	if (gChannel != 0)
	{
		result = FMOD_Channel_GetPaused(gChannel, &pause);
		//CHECK_RESULT(result);
	}

	return pause;
}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cPlay(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;

	result = FMOD_System_PlaySound(gSystem, FMOD_CHANNEL_FREE, gSound, 0, &gChannel);
	CHECK_RESULT(result);
}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cStop(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;

	if (gSound != 0)
	{
		if (gChannel != 0) {
			FMOD_Channel_Stop(gChannel);
		}
		FMOD_Sound_Release(gSound);
		gSound = 0;
	}

}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cPause(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_BOOL paused = 0;

	if (gChannel != 0) {
		result = FMOD_Channel_GetPaused(gChannel, &paused);
		CHECK_RESULT(result);

		result = FMOD_Channel_SetPaused(gChannel, !paused);
		CHECK_RESULT(result);
	}
}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cSetPosition(JNIEnv *env, jobject thiz, jint position)
{
	FMOD_RESULT result = FMOD_OK;

    result = FMOD_Channel_SetPosition(gChannel, position, FMOD_TIMEUNIT_MS);
    //CHECK_RESULT(result);
}


jint Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cGetPosition(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	int position = 0;

	if (gChannel)
	{
		result = FMOD_Channel_GetPosition(gChannel, &position, FMOD_TIMEUNIT_MS);
		if (result != FMOD_ERR_INVALID_HANDLE && result != FMOD_ERR_CHANNEL_STOLEN)
		{
			CHECK_RESULT(result);
		}
	}

	return position;
}

void Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cSetVolume(JNIEnv *env, jobject thiz, jfloat volume)
{
	FMOD_Channel_SetVolume(gChannel, volume);
}

jint Java_net_finalstream_linearaudioplayer_engine_FmodEngine_cGetVersion(JNIEnv *env, jobject thiz, jfloat volume)
{
	//int version = 0;
	//FMOD_System_GetVersion(gSystem, &version);
	return FMOD_VERSION;
}
