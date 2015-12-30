/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Manuel Beuttler
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <stdio.h>

#include <opus.h>

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, "jniopus", __VA_ARGS__))
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, "jniopus", __VA_ARGS__))

OpusDecoder *dec;
OpusEncoder *enc;

//Config
opus_int32 SAMPLING_RATE;
int CHANNELS;
int APPLICATION_TYPE = OPUS_APPLICATION_VOIP;
int FRAME_SIZE;

// Output 320 kb/s bitrate.
int OUTPUT_BITRATE_BPS = 320*1024;

const int MAX_PAYLOAD_BYTES = 4000;
//--

/*
 * Class:     jp_tf_1web_radiolink_audio_opus_OpusManager
 * Method:    nativeInit
 * Signature: (I;I;I;I)Z
 */
JNIEXPORT jboolean JNICALL Java_jp_tf_1web_radiolink_audio_opus_OpusManager_nativeInit (JNIEnv *env, jobject obj, jint samplingRate, jint numberOfChannels, jint frameSize,jint outputBitrateBps)
{
	FRAME_SIZE = frameSize;
	SAMPLING_RATE = samplingRate;
	CHANNELS = numberOfChannels;
	OUTPUT_BITRATE_BPS = outputBitrateBps;

	int size;
	int error;

	//size = opus_decoder_get_size(CHANNELS);
	//dec = malloc(size);
	//error = opus_decoder_init(dec, SAMPLING_RATE, CHANNELS);

	dec = opus_decoder_create(SAMPLING_RATE,CHANNELS,&error);
	LOGD("Initialized Decoder with ErrorCode: %d", error);

	//size = opus_encoder_get_size(CHANNELS);
	//enc = malloc(size);
	//error = opus_encoder_init(enc, SAMPLING_RATE, CHANNELS, APPLICATION_TYPE);

	//_encoder = opus_encoder_create(_format.mSampleRate, _format.mChannelsPerFrame, OPUS_APPLICATION_VOIP, &err);
	enc = opus_encoder_create(SAMPLING_RATE, CHANNELS, APPLICATION_TYPE, &error);
	LOGD("Initialized Encoder ErrorCode: %d", error);

	error = opus_encoder_ctl(enc, OPUS_SET_BITRATE(OUTPUT_BITRATE_BPS));
	LOGD("OPUS_SET_BITRATE ErrorCode: %d", error);
	error = opus_encoder_ctl(enc, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
	LOGD("OPUS_SET_SIGNAL ErrorCode: %d", error);
	error = opus_encoder_ctl(enc, OPUS_SET_MAX_BANDWIDTH(OPUS_BANDWIDTH_NARROWBAND));
	LOGD("OPUS_SET_MAX_BANDWIDTH ErrorCode: %d", error);
	error = opus_encoder_ctl(enc, OPUS_SET_DTX(1));
	LOGD("OPUS_SET_DTX ErrorCode: %d", error);

	return error;
}

/*
 * Class:     jp_tf_1web_radiolink_audio_opus_OpusManager
 * Method:    nativeDecodeBytes
 * Signature: ([B;[S)I
 */
JNIEXPORT jint JNICALL Java_jp_tf_1web_radiolink_audio_opus_OpusManager_nativeDecodeBytes (JNIEnv *env, jobject obj, jbyteArray in, jshortArray out)
{
	//LOGD("Opus decoding");
	//LOGD("FrameSize: %d - SamplingRate: %d - Channels: %d", FRAME_SIZE, SAMPLING_RATE, CHANNELS);

	jint inputArraySize = (*env)->GetArrayLength(env, in);
	jint outputArraySize = (*env)->GetArrayLength(env, out);

	//LOGD("Length of Input Array: %d", inputArraySize);
	//LOGD("Length of Output Array: %d", outputArraySize);

	jbyte* encodedData = (*env)->GetByteArrayElements(env, in, 0);
	opus_int16 *data = (opus_int16*)calloc(outputArraySize,sizeof(opus_int16));
	int decodedDataArraySize = opus_decode(dec, encodedData, inputArraySize, data, FRAME_SIZE, 0);

	//LOGD( "Length of Decoded Data: %d", decodedDataArraySize);

	if (decodedDataArraySize >=0)
	{
		if (decodedDataArraySize <= outputArraySize)
		{
			(*env)->SetShortArrayRegion(env,out,0, decodedDataArraySize, data);
		}
		else
		{
			LOGD("Output array of size: %d to small for storing encoded data.", outputArraySize);

			return -1;
		}
	}

	(*env)->ReleaseByteArrayElements(env,in,encodedData,JNI_ABORT);

	return decodedDataArraySize;
}

/*
 * Class:     jp_tf_1web_radiolink_audio_opus_OpusManager
 * Method:    nativeReleaseDecoder
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jp_tf_1web_radiolink_audio_opus_OpusManager_nativeReleaseDecoder (JNIEnv *env, jobject obj)
{
	/*
	 * 	opus_encoder_destroy(enc);
	 *
	 *	free(enc);
	 *
	 * If the encoder was created with opus_encoder_init() rather than opus_encoder_create(),
	 * then no action is required aside from potentially freeing the memory that was manually allocated for it
	 * (calling free(enc) for the example above)
	 */

	return 1;
}

/*
 * Class:     jp_tf_1web_radiolink_audio_opus_OpusManager
 * Method:    nativeEncodeBytes
 * Signature: ([S;[B)I
 */
JNIEXPORT jint JNICALL Java_jp_tf_1web_radiolink_audio_opus_OpusManager_nativeEncodeBytes (JNIEnv *env, jobject obj, jshortArray in, jbyteArray out)
{
	//LOGD("Opus Encoding");
	//LOGD("FrameSize: %d - SamplingRate: %d - Channels: %d", FRAME_SIZE, SAMPLING_RATE, CHANNELS);

	jint inputArraySize = (*env)->GetArrayLength(env, in);
	jint outputArraySize = (*env)->GetArrayLength(env, out);

	jshort *audioSignal = (*env)->GetShortArrayElements(env, in, 0);

	//LOGD("Length of Input Data: %d", inputArraySize);

	unsigned char *data = (unsigned char*)calloc(MAX_PAYLOAD_BYTES,sizeof(unsigned char));
	int dataArraySize = opus_encode(enc, audioSignal, FRAME_SIZE, data, MAX_PAYLOAD_BYTES);

	//LOGD("Length of Encoded Data: %d", dataArraySize);

	if (dataArraySize >=0)
	{
		if (dataArraySize <= outputArraySize)
		{
			(*env)->SetByteArrayRegion(env,out,0,dataArraySize,data);
		}
		else
		{
			LOGD("Output array of size: %d to small for storing encoded data.", outputArraySize);

			return -1;
		}
	}

	(*env)->ReleaseShortArrayElements(env,in,audioSignal,JNI_ABORT);

	return dataArraySize;
}

/*
 * Class:     jp_tf_1web_radiolink_audio_opus_OpusManager
 * Method:    nativeReleaseEncoder
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jp_tf_1web_radiolink_audio_opus_OpusManager_nativeReleaseEncoder (JNIEnv *env, jobject obj)
{
	/*
	 * 	opus_encoder_destroy(enc);
	 *
	 *	free(enc);
	 *
	 * If the encoder was created with opus_encoder_init() rather than opus_encoder_create(),
	 * then no action is required aside from potentially freeing the memory that was manually allocated for it
	 * (calling free(enc) for the example above)
	 */


	return 1;
}
