/*
 * Cons.java
 * Copyright (c) 2017
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.core;

/**
 * Created by Johnny on 05.03.2015.
 */
public class Cons
{
    public final static String LOGTAG = "SSJ";
    public final static float DFLT_SYNC_INTERVAL = 5.0f; //in seconds
    public final static float DFLT_WATCH_INTERVAL = 1.0f; //in seconds
    public final static long SLEEP_IN_LOOP = 100; //in ms
    public final static long TIMER_SYNC_ACCURACY = 100; //in ms
    public final static long SLEEP_ON_COMPONENT_IDLE = 5000; //in ms
    public final static long WAIT_BL_CONNECT = 1000; //in ms
    public static final long WAIT_BL_DISCONNECT = 1000; //in ms
    public final static long WAIT_THREAD_TERMINATION = 60000; //in ms
    public final static int MAX_EVENT_SIZE = 4096; //in bytes
    public final static int MAX_NUM_EVENTS_PER_CHANNEL = 128; //in bytes

    public final static int THREAD_PRIORIIY_LOW = 10; //nice scale, -20 highest priority, 19 lowest priority
    public final static int THREAD_PRIORITY_NORMAL = -2; //nice scale, -20 highest priority, 19 lowest priority
    public final static int THREAD_PRIORIIY_HIGH = -19; //nice scale, -20 highest priority, 19 lowest priority

    public final static String DEFAULT_BL_SERIAL_UUID = "00001101-0000-1000-8000-00805f9b34fb";

	public enum Type {
        UNDEF,
        BYTE,
        CHAR,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        BOOL,
        STRING,
		IMAGE,
        EMPTY //only used for events
    }

    public enum FileType
    {
        ASCII,
        BINARY
    }

    public enum AudioFormat {
        // These values must be kept in sync with core/jni/android_media_AudioFormat.h
        // Also sync av/services/audiopolicy/managerdefault/ConfigParsingUtils.h
        /** Invalid audio data format */
        ENCODING_DEFAULT(android.media.AudioFormat.ENCODING_DEFAULT),
        /** Audio data format: PCM 16 bit per sample. Guaranteed to be supported by devices. */
        ENCODING_PCM_16BIT(android.media.AudioFormat.ENCODING_PCM_16BIT),
        /** Audio data format: PCM 8 bit per sample. Not guaranteed to be supported by devices. */
        ENCODING_PCM_8BIT(android.media.AudioFormat.ENCODING_PCM_8BIT),
        /** Audio data format: single-precision floating-point per sample */
        ENCODING_PCM_FLOAT(android.media.AudioFormat.ENCODING_PCM_FLOAT),
        /** Audio data format: AC-3 compressed */
        ENCODING_AC3(android.media.AudioFormat.ENCODING_AC3),
        /** Audio data format: E-AC-3 compressed */
        ENCODING_E_AC3(android.media.AudioFormat.ENCODING_E_AC3);
//        /** Audio data format: DTS compressed */
//        ENCODING_DTS(android.media.AudioFormat.ENCODING_DTS),
//        /** Audio data format: DTS HD compressed */
//        ENCODING_DTS_HD(android.media.AudioFormat.ENCODING_DTS_HD);

        public int val;
        AudioFormat(int value)
        {
            val = value;
        }
    }

    public enum ChannelFormat {
        CHANNEL_IN_DEFAULT(android.media.AudioFormat.CHANNEL_IN_DEFAULT),
        CHANNEL_IN_LEFT(android.media.AudioFormat.CHANNEL_IN_LEFT),
        CHANNEL_IN_RIGHT(android.media.AudioFormat.CHANNEL_IN_RIGHT),
        CHANNEL_IN_FRONT(android.media.AudioFormat.CHANNEL_IN_FRONT),
        CHANNEL_IN_BACK(android.media.AudioFormat.CHANNEL_IN_BACK),
        CHANNEL_IN_LEFT_PROCESSED(android.media.AudioFormat.CHANNEL_IN_LEFT_PROCESSED),
        CHANNEL_IN_RIGHT_PROCESSED(android.media.AudioFormat.CHANNEL_IN_RIGHT_PROCESSED),
        CHANNEL_IN_FRONT_PROCESSED(android.media.AudioFormat.CHANNEL_IN_FRONT_PROCESSED),
        CHANNEL_IN_BACK_PROCESSED(android.media.AudioFormat.CHANNEL_IN_BACK_PROCESSED),
        CHANNEL_IN_PRESSURE(android.media.AudioFormat.CHANNEL_IN_PRESSURE),
        CHANNEL_IN_X_AXIS(android.media.AudioFormat.CHANNEL_IN_X_AXIS),
        CHANNEL_IN_Y_AXIS(android.media.AudioFormat.CHANNEL_IN_Y_AXIS),
        CHANNEL_IN_Z_AXIS(android.media.AudioFormat.CHANNEL_IN_Z_AXIS),
        CHANNEL_IN_VOICE_UPLINK(android.media.AudioFormat.CHANNEL_IN_VOICE_UPLINK),
        CHANNEL_IN_VOICE_DNLINK(android.media.AudioFormat.CHANNEL_IN_VOICE_DNLINK),
        CHANNEL_IN_MONO (android.media.AudioFormat.CHANNEL_IN_MONO),
        CHANNEL_IN_STEREO (android.media.AudioFormat.CHANNEL_IN_STEREO);

        public int val;
        ChannelFormat(int value)
        {
            val = value;
        }
    }

    public enum SocketType
    {
        UDP,
        TCP
    }

    public enum ImageFormat {
        NV21(android.graphics.ImageFormat.NV21),
        FLEX_RGBA_8888(0x2A), //android.graphics.ImageFormat.FLEX_RGBA_8888
        FLEX_RGB_888(0x29), //android.graphics.ImageFormat.FLEX_RGB_888
        YUV_420_888(0x23), //android.graphics.ImageFormat.YUV_420_888)
        YV12(android.graphics.ImageFormat.YV12);

        public int val;
        ImageFormat(int value)
        {
            val = value;
        }
    }

    public enum CameraType
	{
        BACK_CAMERA(0),
        FRONT_CAMERA(1);

		public final int val;

		CameraType(int val)
		{
			this.val = val;
		}
	}
}
