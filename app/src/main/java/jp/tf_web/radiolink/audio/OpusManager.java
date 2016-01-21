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


package jp.tf_web.radiolink.audio;

import java.io.IOException;
import android.util.Log;

import jp.tf_web.radiolink.util.ByteArrayUtil;

/**
 * @author Manuel Beuttler
 *
 *
 *	OpusManager class extending FilterOutputStream
 *
 *	Uses native c code via JNI to read and decode bytes from an input stream and write them into an array.
 *
 *	Native resource: de_stuttgart_hdm_opuswalkietalkie_OpusDecoder.h, e_stuttgart_hdm_opuswalkietalkie_OpusDecoder.c
 */
public class OpusManager
{
	private static String	TAG	= "OpusManager";

	//Native methods
	private native boolean nativeInit( int samplingRate, int numberOfChannels, int frameSize, int outputBitrateBps);
	private native int nativeDecodeBytes( byte[] in , short[] out);
	private native boolean nativeReleaseDecoder();

	private native int nativeEncodeBytes( short[] in, byte[] out );
	private native boolean nativeReleaseEncoder();

	private int samplingRate;

	static
	{
		try
		{
			System.loadLibrary( "jniopus" );
		}
		catch ( Exception e )
		{
			Log.e( TAG, "Could not load Systemlibrary 'jniopus'" );
		}
	}

	/**
	 * The passed frame_size must an opus frame size for the encoder's sampling rate.
	 * For example, at 48kHz the permitted values are 120, 240, 480, 960, 1920, and 2880.
	 * 
	 * @param samplingRate Configured sampling rate or frequency
	 * @param numberOfChannels Number of channels in the audio signal ( 1 = mono)
	 * @param frameSize Number of samples per frame of input signal
	 */
	public OpusManager(int samplingRate, int numberOfChannels, int frameSize,int outputBitrateBps) {
		this.samplingRate = samplingRate;
		this.nativeInit(samplingRate, numberOfChannels, frameSize, outputBitrateBps);
	}


	/** 設定されているサンプリングレートを取得
	 *
	 * @return
	 */
	public int getSamplingRate(){
		return this.samplingRate;
	}

	/**
	 * Reads bytes from the InputStream, decodes them an writes them into the given buffer.
	 * 
	 * @param in The buffer to write the decoded data to.
	 * @param out The buffer to write the decoded data to.
	 * @return Amount of bytes read.
	 * @throws IOException
	 */
	private int decode(byte[] in, short[] out) throws IOException
	{
		int bytesEncoded = nativeDecodeBytes(in,out);
		return bytesEncoded;
	}

	/** Opus デコード
	 *
	 * @param opus
	 * @param frameSize
	 * @return
	 */
	public byte[] decode(final byte[] opus,final int frameSize){
		//Opus デコードする
		byte[] result = null;
		try {
			short[] outBuf = new short[frameSize];
			int dec_size = decode(opus, outBuf);
			if(dec_size > 0){
				result = ByteArrayUtil.shortArr2byteArr(outBuf, dec_size);
			}
			else{
				//Opusデコードに失敗
				Log.d(TAG, "opus decode result:"+result);
			}
		}catch (IOException e){
			//Opus デコードに失敗
			e.printStackTrace();
			Log.d(TAG, "error "+e.toString());
		}
		return result;
	}

	/**
	 * Encodes a buffered input signal and writes it to the output stream.
	 *
	 * @param in Input signal (interleaved if 2 channels). Length needs to be frame_size*channels*sizeof(opus_int16)
	 * @param out
	 * @throws IOException
	 */
	private int encode(short[] in,byte[] out) throws IOException
	{
		return this.nativeEncodeBytes(in, out);
	}

	/** Opus エンコード
	 *
	 * @param pcm PCMの入ったバイト配列
	 * @return Encode後のバイト配列
	 */
	public byte[] encode(final byte[] pcm){
		byte[] result = null;
		try {
			//byte[] を short[] に変換
			short[] src = ByteArrayUtil.byteArr2shortArr(pcm, pcm.length);
			//Opus エンコードする
			byte[] outByeBuf = new byte[src.length];
			int enc_size = encode(src, outByeBuf);
			if(enc_size > 1) {
				result = new byte[enc_size];
				System.arraycopy(outByeBuf, 0, result, 0, enc_size);
			}
		}
		catch (IOException e){
			//Opus エンコードに失敗
			e.printStackTrace();
			Log.d(TAG, "error "+e.toString());
		}
		return result;

	}

	public void close() throws IOException
	{
		this.nativeReleaseDecoder();
		this.nativeReleaseEncoder();
	}
}
