/*
 * WavWriter.java
 * Copyright (c) 2018
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

package hcm.ssj.audio;

import android.media.AudioFormat;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.FolderPath;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;
import hcm.ssj.file.IFileWriter;

/**
 * Writes wav files.<br>
 * Created by Frank Gaibler and Ionut Damian on 12.12.2016.
 */
public class WavWriter extends Consumer implements IFileWriter
{
    @Override
    public OptionList getOptions()
    {
        return options;
    }

    private enum DataFormat
    {
        BYTE(Microphone.audioFormatSampleBytes(AudioFormat.ENCODING_PCM_8BIT)),
        SHORT(Microphone.audioFormatSampleBytes(AudioFormat.ENCODING_PCM_16BIT)),
        FLOAT_8(BYTE.size),
        FLOAT_16(SHORT.size);

        private int size;

        /**
         * @param i int
         */
        DataFormat(int i)
        {
            size = i;
        }
    }

    //encoder
    protected double dFrameRate;
    //
    protected byte[] aByShuffle;
    protected long lFrameIndex;
    //
    protected File file = null;
    private BufferedOutputStream outputStream;

    public final WavWriter.Options options = new WavWriter.Options();
    //
    private int iSampleRate;
    private int iSampleNumber;
    private int iSampleDimension;
    //
    private WavWriter.DataFormat dataFormat = null;


    /**
     * All options for the audio writer
     */
    public class Options extends IFileWriter.Options
    {
        public final Option<Cons.AudioFormat> audioFormat = new Option<>("audioFormat", Cons.AudioFormat.ENCODING_DEFAULT, Cons.AudioFormat.class, "");

        /**
         *
         */
        private Options()
        {
            super();
            addOptions();
        }
    }

    /**
     *
     */
    public WavWriter()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 * @param stream_in Stream[]
	 */
    @Override
    public final void enter(Stream[] stream_in) throws SSJFatalException
    {
        if (stream_in.length != 1)
        {
            throw new SSJFatalException("Stream count not supported");
        }
        switch (stream_in[0].type)
        {
            case BYTE:
            {
                dataFormat = WavWriter.DataFormat.BYTE;
                break;
            }
            case SHORT:
            {
                dataFormat = WavWriter.DataFormat.SHORT;
                break;
            }
            case FLOAT:
            {
                if (options.audioFormat.get() == Cons.AudioFormat.ENCODING_DEFAULT || options.audioFormat.get() == Cons.AudioFormat.ENCODING_PCM_16BIT)
                {
                    dataFormat = WavWriter.DataFormat.FLOAT_16;
                } else if (options.audioFormat.get() == Cons.AudioFormat.ENCODING_PCM_8BIT)
                {
                    dataFormat = WavWriter.DataFormat.FLOAT_8;
                } else
                {
                    throw new SSJFatalException("Audio format not supported");
                }
                break;
            }
            default:
            {
                throw new SSJFatalException("Stream type not supported");
            }
        }

        try
        {
            initFiles(stream_in[0], options);
        }
        catch (IOException e)
        {
            throw new SSJFatalException("error initializing files", e);
        }

        Log.d("Format: " + dataFormat.toString());
        iSampleRate = (int) stream_in[0].sr;
        iSampleDimension = stream_in[0].dim;
        iSampleNumber = iSampleRate * dataFormat.size * iSampleDimension;
        //recalculate frame rate
        dFrameRate = stream_in[0].sr / stream_in[0].num;
        aByShuffle = new byte[(int) (iSampleNumber / dFrameRate + 0.5)];
        lFrameIndex = 0;

        try
        {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (IOException ex)
        {
            throw new SSJFatalException("RawEncoder creation failed: " + ex.getMessage());
        }
    }

    /**
     * @param stream_in Stream[]
	 * @param trigger Event trigger
     */
    @Override
    protected final void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        switch (dataFormat)
        {
            case BYTE:
            {
                byte[] in = stream_in[0].ptrB();
                for (int i = 0; i < in.length; i += aByShuffle.length)
                {
                    System.arraycopy(in, i, aByShuffle, 0, aByShuffle.length);
                    write(aByShuffle);
                }
                break;
            }
            case SHORT:
            {
                short[] in = stream_in[0].ptrS();
                for (int i = 0; i < in.length; i += aByShuffle.length)
                {
                    ByteBuffer.wrap(aByShuffle).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(in, i / 2, aByShuffle.length / 2);
                    write(aByShuffle);
                }
                break;
            }
            case FLOAT_8:
                float[] in = stream_in[0].ptrF();
                for (int i = 0; i < in.length; )
                {
                    for (int j = 0; j < aByShuffle.length; j++, i += aByShuffle.length)
                    {
                        aByShuffle[j] = (byte) (in[i] * 128);
                    }
                    write(aByShuffle);
                }
                break;
            case FLOAT_16:
            {
                float[] in16 = stream_in[0].ptrF();
                for (int i = 0; i < in16.length; )
                {
                    for (int j = 0; j < aByShuffle.length; i++, j += 2)
                    {
                        short value = (short) (in16[i] * 32768);
                        aByShuffle[j] = (byte) (value & 0xff);
                        aByShuffle[j + 1] = (byte) ((value >> 8) & 0xff);
                    }
                    write(aByShuffle);
                }
                break;
            }
            default:
            {
                Log.e("Data format not supported");
                break;
            }
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[]) throws SSJFatalException
    {
        if (outputStream != null)
        {
            try
            {
                outputStream.flush();
                outputStream.close();
            } catch (IOException ex)
            {
                Log.e("RawEncoder closing: " + ex.getMessage());
            }
        }

        try
        {
            writeWavHeader(file);
        }
        catch (IOException e)
        {
            throw new SSJFatalException("error writing header", e);
        }

        dataFormat = null;
    }

    /**
     * Checks for file consistency
     *
     * @param in Input stream
     * @param options Options
     * @throws IOException IO Exception
     */
    protected final void initFiles(Stream in, Options options) throws IOException
    {
        if (options.filePath.get() == null)
        {
            Log.w("file path not set, setting to default " + FileCons.SSJ_EXTERNAL_STORAGE);
            options.filePath.set(new FolderPath(FileCons.SSJ_EXTERNAL_STORAGE));
        }
        File fileDirectory = new File(options.filePath.parseWildcards());
        if (!fileDirectory.exists())
        {
            if (!fileDirectory.mkdirs())
            {
                throw new IOException(fileDirectory.getName() + " could not be created");
            }
        }
        if (options.fileName.get() == null)
        {
            String defaultName = TextUtils.join("_", in.desc) + ".wav";
            Log.w("file name not set, setting to " + defaultName);
            options.fileName.set(defaultName);
        }
        file = new File(fileDirectory, options.fileName.get());
    }

    /**
     * @param frameData byte[]
     */
    protected final void write(byte[] frameData)
    {
        try
        {
            outputStream.write(frameData, 0, frameData.length);
        } catch (IOException ex)
        {
            Log.e("RawEncoder: " + ex.getMessage());
        }
    }

    /**
     * Writes a PCM Wav header at the start of the provided file
     * code taken from : http://stackoverflow.com/questions/9179536/writing-pcm-recorded-data-into-a-wav-file-java-android
     * by: Oliver Mahoney, Oak Bytes
     * @param fileToConvert File
     */
    private void writeWavHeader(File fileToConvert) throws IOException
    {
        long mySubChunk1Size = 16;
        int myBitsPerSample = 16;
        int myFormat = 1;
        long myChannels = iSampleDimension;
        long mySampleRate = iSampleRate;
        long myByteRate = mySampleRate * myChannels * myBitsPerSample / 8;
        int myBlockAlign = (int) (myChannels * myBitsPerSample / 8);

        int size = (int) fileToConvert.length();
        byte[] bytes = new byte[size];
        try
        {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(fileToConvert));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        long myDataSize = bytes.length;
        long myChunk2Size = myDataSize * myChannels * myBitsPerSample / 8;
        long myChunkSize = 36 + myChunk2Size;

        OutputStream os;
        os = new FileOutputStream(new File(fileToConvert.getPath()));
        BufferedOutputStream bos = new BufferedOutputStream(os);
        DataOutputStream outFile = new DataOutputStream(bos);

        outFile.writeBytes("RIFF");                                 // 00 - RIFF
        outFile.write(intToByteArray((int) myChunkSize), 0, 4);      // 04 - how big is the rest of this file?
        outFile.writeBytes("WAVE");                                 // 08 - WAVE
        outFile.writeBytes("fmt ");                                 // 12 - fmt
        outFile.write(intToByteArray((int) mySubChunk1Size), 0, 4);  // 16 - size of this chunk
        outFile.write(shortToByteArray((short) myFormat), 0, 2);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
        outFile.write(shortToByteArray((short) myChannels), 0, 2);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
        outFile.write(intToByteArray((int) mySampleRate), 0, 4);     // 24 - samples per second (numbers per second)
        outFile.write(intToByteArray((int) myByteRate), 0, 4);       // 28 - bytes per second
        outFile.write(shortToByteArray((short) myBlockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
        outFile.write(shortToByteArray((short) myBitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
        outFile.writeBytes("data");                                 // 36 - data
        outFile.write(intToByteArray((int) myDataSize), 0, 4);       // 40 - how big is this data chunk
        outFile.write(bytes);                                    // 44 - the actual data itself - just a long string of numbers

        outFile.flush();
        outFile.close();
        if (!fileToConvert.delete())
        {
            throw new IOException("error deleting temp file");
        }
    }

    /**
     * Code taken from : http://stackoverflow.com/questions/9179536/writing-pcm-recorded-data-into-a-wav-file-java-android
     * by: Oliver Mahoney, Oak Bytes
     */
    private static byte[] intToByteArray(int i)
    {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0x00FF);
        b[1] = (byte) ((i >> 8) & 0x000000FF);
        b[2] = (byte) ((i >> 16) & 0x000000FF);
        b[3] = (byte) ((i >> 24) & 0x000000FF);
        return b;
    }

    /**
     * Convert a short to a byte array
     * code taken from : http://stackoverflow.com/questions/9179536/writing-pcm-recorded-data-into-a-wav-file-java-android
     * by: Oliver Mahoney, Oak Bytes
     */
    private static byte[] shortToByteArray(short data)
    {
        return new byte[]{(byte) (data & 0xff), (byte) ((data >>> 8) & 0xff)};
    }
}
