/*
 * Util.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 26.03.2015.
 */
public class Util
{
    public static int sizeOf(Cons.Type type)
    {
        switch(type)
        {
            case CHAR:
                return 2;
            case SHORT:
                return 2;
            case INT:
                return 4;
            case LONG:
                return 8;
            case FLOAT:
                return 4;
            case DOUBLE:
                return 8;
            case BOOL:
                return 1;
        }
        return 0;
    }
    public static int sizeOf(byte x)
    {
        return 1;
    }
    public static int sizeOf(char x)
    {
        return 2;
    }
    public static int sizeOf(short x)
    {
        return 2;
    }
    public static int sizeOf(int x)
    {
        return 4;
    }
    public static int sizeOf(long x)
    {
        return 8;
    }
    public static int sizeOf(float x)
    {
        return 4;
    }
    public static int sizeOf(double x)
    {
        return 8;
    }
    public static int sizeOf(boolean x)
    {
        return 1;
    }

    public static double max(double[] data, int offset, int len)
    {
        double max = Double.MIN_VALUE;
        for (int i = offset; i < offset + len; i++)
            if (data[i] > max)
                max = data[i];
        return max;
    }

    public static float max(float[] data, int offset, int len)
    {
        float max = Float.MIN_VALUE;
        for (int i = offset; i < offset + len; i++)
            if (data[i] > max)
                max = data[i];
        return max;
    }

    public static double min(double[] data, int offset, int len)
    {
        double min = Double.MAX_VALUE;
        for (int i = offset; i < offset + len; i++)
            if (data[i] < min)
                min = data[i];
        return min;
    }

    public static float min(float[] data, int offset, int len)
    {
        float min = Float.MAX_VALUE;
        for (int i = offset; i < offset + len; i++)
            if (data[i] < min)
                min = data[i];
        return min;
    }

    public static double mean(double[] data, int offset, int len)
    {
        if (data.length == 0)
            return 0;

        double sum = 0;
        for (int i = offset; i < offset + len; i++)
            sum += data[i];
        return sum / data.length;
    }

    public static float mean(float[] data, int offset, int len)
    {
        if (data.length == 0)
            return 0;

        float sum = 0;
        for (int i = offset; i < offset + len; i++)
            sum += data[i];
        return sum / data.length;
    }

    public static double median(double[] data, int offset, int len)
    {
        Arrays.sort(data, offset, offset + len);
        double median;
        if (len % 2 == 0)
            median = (data[offset + len / 2] + data[offset + len / 2 - 1]) / 2;
        else
            median = data[offset + len / 2];

        return median;
    }

    public static float median(float[] data, int offset, int len)
    {
        Arrays.sort(data, offset, offset + len);
        float median;
        if (len % 2 == 0)
            median = (data[offset + len / 2] + data[offset + len / 2 - 1]) / 2;
        else
            median = data[offset + len / 2];

        return median;
    }

    /**
     * Copy an array from src to dst.
     * Types do not need to match (currently only BYTE -> ANY and ANY -> BYTE is supported)
     *
     * ByteOrder: Little-Endian
     *
     * @param src source array
     * @param srcPosBytes position in source array
     * @param dst destination array
     * @param dstPosBytes position in destination array
     * @param numBytes number of bytes to copy
     */
    public static void arraycopy(Object src, int srcPosBytes, Object dst, int dstPosBytes, int numBytes)
    {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }

        if(src instanceof byte[])
        {
            if(dst instanceof byte[]) System.arraycopy((byte[]) src, srcPosBytes, (byte[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof char[]) arraycopy((byte[]) src, srcPosBytes, (char[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof short[]) arraycopy((byte[]) src, srcPosBytes, (short[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof int[]) arraycopy((byte[]) src, srcPosBytes, (int[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof long[]) arraycopy((byte[]) src, srcPosBytes, (long[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof float[]) arraycopy((byte[]) src, srcPosBytes, (float[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof double[]) arraycopy((byte[]) src, srcPosBytes,(double[])  dst, dstPosBytes, numBytes);
            else if(dst instanceof boolean[]) arraycopy((byte[]) src, srcPosBytes, (boolean[]) dst, dstPosBytes, numBytes);
            else throw new UnsupportedOperationException();
        }
        else if(src instanceof char[])
        {
            if(dst instanceof byte[]) arraycopy((char[]) src, srcPosBytes, (byte[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof char[]) System.arraycopy((char[]) src, srcPosBytes / 2, (char[]) dst, dstPosBytes, numBytes / 2);
            else throw new UnsupportedOperationException();
        }
        else if(src instanceof short[])
        {
            if(dst instanceof byte[]) arraycopy((short[]) src, srcPosBytes, (byte[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof short[]) System.arraycopy((short[]) src, srcPosBytes / 2, (short[]) dst, dstPosBytes, numBytes / 2);
            else throw new UnsupportedOperationException();
        }
        else if(src instanceof int[])
        {
            if(dst instanceof byte[]) arraycopy((int[]) src, srcPosBytes, (byte[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof int[]) System.arraycopy((int[]) src, srcPosBytes / 4, (int[]) dst, dstPosBytes, numBytes / 4);
            else throw new UnsupportedOperationException();
        }
        else if(src instanceof long[])
        {
            if(dst instanceof byte[]) arraycopy((long[]) src, srcPosBytes, (byte[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof long[]) System.arraycopy((long[]) src, srcPosBytes / 8, (long[]) dst, dstPosBytes, numBytes / 8);
            else throw new UnsupportedOperationException();
        }
        else if(src instanceof float[])
        {
            if(dst instanceof byte[]) arraycopy((float[]) src, srcPosBytes, (byte[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof float[]) System.arraycopy((float[]) src, srcPosBytes / 4, (float[]) dst, dstPosBytes, numBytes / 4);
            else throw new UnsupportedOperationException();
        }
        else if(src instanceof double[])
        {
            if(dst instanceof byte[]) arraycopy((double[]) src, srcPosBytes, (byte[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof double[]) System.arraycopy((double[]) src, srcPosBytes / 8, (double[]) dst, dstPosBytes, numBytes / 8);
            else throw new UnsupportedOperationException();
        }
        else if(src instanceof boolean[])
        {
            if(dst instanceof byte[]) arraycopy((boolean[]) src, srcPosBytes, (byte[]) dst, dstPosBytes, numBytes);
            else if(dst instanceof boolean[]) System.arraycopy((boolean[]) src, srcPosBytes, (boolean[]) dst, dstPosBytes, numBytes / sizeOf(Cons.Type.BOOL));
            else throw new UnsupportedOperationException();
        }
        else throw new UnsupportedOperationException();
    }

    private static void arraycopy(byte[] src, int srcPosBytes, char[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || srcPosBytes > src.length - numBytes || numBytes > dst.length * 2 - dstPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        int iter = dstPosBytes / 2;
        for (int i = 0; i < numBytes; i += 2) {
            dst[iter++] = (char)((src[srcPosBytes++] & 0xFF) | (src[srcPosBytes++] & 0xFF) << 8);
        }
    }

    private static void arraycopy(char[] src, int srcPosBytes, byte[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || dstPosBytes > dst.length - numBytes || numBytes > src.length * 2 - srcPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        char bits;
        int iter = srcPosBytes / 2;
        for (int i = 0; i < numBytes; i += 2)
        {
            bits = src[iter++];
            dst[dstPosBytes++] = (byte)bits;
            dst[dstPosBytes++] = (byte)(bits >> 8);
        }
    }

    private static void arraycopy(byte[] src, int srcPosBytes, float[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || srcPosBytes > src.length - numBytes || numBytes > dst.length * 4 - dstPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                    " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        int iter = dstPosBytes / 4;
        for (int i = 0; i < numBytes; i += 4) {
            dst[iter++] = Float.intBitsToFloat((src[srcPosBytes++] & 0xFF)
                                               | (src[srcPosBytes++] & 0xFF) << 8
                                               | (src[srcPosBytes++] & 0xFF) << 16
                                               | (src[srcPosBytes++] & 0xFF) << 24);
        }
    }

    private static void arraycopy(float[] src, int srcPosBytes, byte[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || dstPosBytes > dst.length - numBytes || numBytes > src.length * 4 - srcPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        int bits;
        int iter = srcPosBytes / 4;
        for (int i = 0; i < numBytes; i += 4)
        {
            bits = Float.floatToIntBits(src[iter++]);
            dst[dstPosBytes++] = (byte)bits;
            dst[dstPosBytes++] = (byte)(bits >> 8);
            dst[dstPosBytes++] = (byte)(bits >> 16);
            dst[dstPosBytes++] = (byte)(bits >> 24);
        }
    }

    private static void arraycopy(byte[] src, int srcPosBytes, double[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || srcPosBytes > src.length - numBytes || numBytes > dst.length * 8 - dstPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        int iter = dstPosBytes / 8;
        for (int i = 0; i < numBytes; i += 8) {
            dst[iter++] = Double.longBitsToDouble(
                    (src[srcPosBytes++] & (long) 0xFF)
                            | (src[srcPosBytes++] & (long) 0xFF) << 8
                            | (src[srcPosBytes++] & (long) 0xFF) << 16
                            | (src[srcPosBytes++] & (long) 0xFF) << 24
                            | (src[srcPosBytes++] & (long) 0xFF) << 32
                            | (src[srcPosBytes++] & (long) 0xFF) << 40
                            | (src[srcPosBytes++] & (long) 0xFF) << 48
                            | (src[srcPosBytes++] & (long) 0xFF) << 56);
        }
    }

    private static void arraycopy(double[] src, int srcPosBytes, byte[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || dstPosBytes > dst.length - numBytes || numBytes > src.length * 8 - srcPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        long bits;
        int iter = srcPosBytes / 8;
        for (int i = 0; i < numBytes; i += 8)
        {
            bits = Double.doubleToLongBits(src[iter++]);
            dst[dstPosBytes++] = (byte)bits;
            dst[dstPosBytes++] = (byte)(bits >> 8);
            dst[dstPosBytes++] = (byte)(bits >> 16);
            dst[dstPosBytes++] = (byte)(bits >> 24);
            dst[dstPosBytes++] = (byte)(bits >> 32);
            dst[dstPosBytes++] = (byte)(bits >> 40);
            dst[dstPosBytes++] = (byte)(bits >> 48);
            dst[dstPosBytes++] = (byte)(bits >> 56);
        }
    }

    private static void arraycopy(byte[] src, int srcPosBytes, short[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || srcPosBytes > src.length - numBytes || numBytes > dst.length * 2 - dstPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        int iter = dstPosBytes / 2;
        for (int i = 0; i < numBytes; i += 2) {
            dst[iter++] = (short)((src[srcPosBytes++] & 0xFF) | (src[srcPosBytes++] & 0xFF) << 8);
        }
    }

    private static void arraycopy(short[] src, int srcPosBytes, byte[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || dstPosBytes > dst.length - numBytes || numBytes > src.length * 2 - srcPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        short bits;
        int iter = srcPosBytes / 2;
        for (int i = 0; i < numBytes; i += 2)
        {
            bits = src[iter++];
            dst[dstPosBytes++] = (byte)bits;
            dst[dstPosBytes++] = (byte)(bits >> 8);
        }
    }

    private static void arraycopy(byte[] src, int srcPosBytes, int[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || srcPosBytes > src.length - numBytes || numBytes > dst.length * 4 - dstPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        int iter = dstPosBytes / 4;
        for (int i = 0; i < numBytes; i += 4) {
            dst[iter++] = (src[srcPosBytes++] & 0xFF)
                         | (src[srcPosBytes++] & 0xFF) << 8
                         | (src[srcPosBytes++] & 0xFF) << 16
                         | (src[srcPosBytes++] & 0xFF) << 24;
        }
    }

    private static void arraycopy(int[] src, int srcPosBytes, byte[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || dstPosBytes > dst.length - numBytes || numBytes > src.length * 4 - srcPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        int bits;
        int iter = srcPosBytes / 4;
        for (int i = 0; i < numBytes; i += 4)
        {
            bits = src[iter++];
            dst[dstPosBytes++] = (byte)bits;
            dst[dstPosBytes++] = (byte)(bits >> 8);
            dst[dstPosBytes++] = (byte)(bits >> 16);
            dst[dstPosBytes++] = (byte)(bits >> 24);
        }
    }

    private static void arraycopy(byte[] src, int srcPosBytes, long[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || srcPosBytes > src.length - numBytes || numBytes > dst.length * 8 - dstPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        int iter = dstPosBytes / 8;
        for (int i = 0; i < numBytes; i += 8) {
            dst[iter++] = (src[srcPosBytes++] & (long) 0xFF)
                            | (src[srcPosBytes++] & (long) 0xFF) << 8
                            | (src[srcPosBytes++] & (long) 0xFF) << 16
                            | (src[srcPosBytes++] & (long) 0xFF) << 24
                            | (src[srcPosBytes++] & (long) 0xFF) << 32
                            | (src[srcPosBytes++] & (long) 0xFF) << 40
                            | (src[srcPosBytes++] & (long) 0xFF) << 48
                            | (src[srcPosBytes++] & (long) 0xFF) << 56;
        }
    }

    private static void arraycopy(long[] src, int srcPosBytes, byte[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || dstPosBytes > dst.length - numBytes || numBytes > src.length * 8 - srcPosBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        long bits;
        int iter = srcPosBytes / 8;
        for (int i = 0; i < numBytes; i += 8)
        {
            bits = src[iter++];
            dst[dstPosBytes++] = (byte)bits;
            dst[dstPosBytes++] = (byte)(bits >> 8);
            dst[dstPosBytes++] = (byte)(bits >> 16);
            dst[dstPosBytes++] = (byte)(bits >> 24);
            dst[dstPosBytes++] = (byte)(bits >> 32);
            dst[dstPosBytes++] = (byte)(bits >> 40);
            dst[dstPosBytes++] = (byte)(bits >> 48);
            dst[dstPosBytes++] = (byte)(bits >> 56);
        }
    }

    private static void arraycopy(byte[] src, int srcPosBytes, boolean[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || srcPosBytes > src.length - numBytes || dstPosBytes > dst.length - numBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        for (int i = 0; i < numBytes; ++i) dst[dstPosBytes + i] = src[srcPosBytes + i] != 0;
    }

    private static void arraycopy(boolean[] src, int srcPosBytes, byte[] dst, int dstPosBytes, int numBytes)
    {
        if (srcPosBytes < 0 || dstPosBytes < 0 || numBytes < 0 || srcPosBytes > src.length - numBytes || dstPosBytes > dst.length - numBytes)
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPosBytes=" + srcPosBytes +
                                                             " dst.length=" + dst.length + " dstPosBytes=" + dstPosBytes + " numBytes=" + numBytes);

        for (int i = 0; i < numBytes; ++i) dst[dstPosBytes + i] = src[srcPosBytes + i] ? (byte)1 : 0;
    }

    /**
     * Fill array with zeroes
     *
     * @param arr source array
     * @param posSamples position in samples of first element to be of bytes to copy
     * @param numSamples number of zero samples to write
     */
    public static void fillZeroes(Object arr, int posSamples, int numSamples)
    {
        if (arr == null) {
            throw new NullPointerException("src == null");
        }

        if(arr instanceof byte[])
        {
            for (int i = 0; i < numSamples; ++i) ((byte[])arr)[posSamples + i] = 0;
        }
        else if(arr instanceof char[])
        {
            for (int i = 0; i < numSamples; ++i) ((char[])arr)[posSamples + i] = 0;
        }
        else if(arr instanceof short[])
        {
            for (int i = 0; i < numSamples; ++i) ((short[])arr)[posSamples + i] = 0;
        }
        else if(arr instanceof int[])
        {
            for (int i = 0; i < numSamples; ++i) ((int[])arr)[posSamples + i] = 0;
        }
        else if(arr instanceof long[])
        {
            for (int i = 0; i < numSamples; ++i) ((long[])arr)[posSamples + i] = 0;
        }
        else if(arr instanceof float[])
        {
            for (int i = 0; i < numSamples; ++i) ((float[])arr)[posSamples + i] = 0;
        }
        else if(arr instanceof double[])
        {
            for (int i = 0; i < numSamples; ++i) ((double[])arr)[posSamples + i] = 0;
        }
        else if(arr instanceof boolean[])
        {
            for (int i = 0; i < numSamples; ++i) ((boolean[])arr)[posSamples + i] = false;
        }
        else throw new UnsupportedOperationException();
    }

    public static byte[] serialize(Object obj) {
        try
        {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(obj);
            return b.toByteArray();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Object deserialize(byte[] bytes) {
        try
        {
            ByteArrayInputStream b = new ByteArrayInputStream(bytes);
            ObjectInputStream o = new ObjectInputStream(b);
            return o.readObject();
        }
        catch (IOException | ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String xmlToString(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        if (parser.getEventType() == XmlPullParser.TEXT)
        {
            return parser.getText();
        }

        int depth = parser.getDepth();
        String tag;
        StringBuilder str = new StringBuilder();
        do
        {
            if (parser.getEventType() == XmlPullParser.START_TAG)
            {
                str.append("<").append(parser.getName());

                //add attributes
                for (int i = 0; i < parser.getAttributeCount(); ++i)
                    str.append(" ").append(parser.getAttributeName(i)).append("=").append("\"").append(parser.getAttributeValue(i)).append("\"");
                str.append(">");

                //go deeper
                parser.next();
                str.append(xmlToString(parser));
            }

            if (parser.getEventType() == XmlPullParser.END_TAG)
            {
                str.append("</").append(parser.getName()).append(">");
            }

            parser.next();
        }
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT && parser.getDepth() >= depth);

        return str.toString();
    }

    public static float calcSampleRate(Transformer t, Stream stream_in)
    {
        double dur = stream_in.num * stream_in.step;
        return (float)(t.getSampleNumber(stream_in.num) / dur);
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) throws SocketException
    {
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface intf : interfaces) {

            //for google glass: interface is named wlan0
            String name = intf.getName();
            if(!name.contains("wlan"))
                continue;

            List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
            for (InetAddress addr : addrs) {
                if (!addr.isLoopbackAddress()) {
                    String sAddr = addr.getHostAddress().toUpperCase();
                    boolean isIPv4 = addr instanceof Inet4Address;
                    if (useIPv4) {
                        if (isIPv4)
                            return sAddr;
                    } else {
                        if (!isIPv4) {
                            int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                            return delim<0 ? sAddr : sAddr.substring(0, delim);
                        }
                    }
                }
            }
        }

        return "";
    }

    public static InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager)SSJApplication.getAppContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if(dhcp == null)
            throw new IOException("dhcp is null");

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }
}
