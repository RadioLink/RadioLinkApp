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

	static
	{
		try
		{
			System.loadLibrary("gnustl_shared");
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
		this.nativeInit(samplingRate, numberOfChannels, frameSize, outputBitrateBps);
	}

	/**
	 * Reads bytes from the InputStream, decodes them an writes them into the given buffer.
	 * 
	 * @param in The buffer to write the decoded data to.
	 * @param out The buffer to write the decoded data to.
	 * @return Amount of bytes read.
	 * @throws IOException
	 */
	public int decode(byte[] in, short[] out) throws IOException
	{
		int bytesEncoded = nativeDecodeBytes(in,out);
		return bytesEncoded;
	}

	/**
	 * Encodes a buffered input signal and writes it to the output stream.
	 *
	 * @param in Input signal (interleaved if 2 channels). Length needs to be frame_size*channels*sizeof(opus_int16)
	 * @param out
	 * @throws IOException
	 */
	public int encode(short[] in,byte[] out) throws IOException
	{
		return this.nativeEncodeBytes(in, out);
	}

	public void close() throws IOException
	{
		this.nativeReleaseDecoder();
		this.nativeReleaseEncoder();
	}
}
