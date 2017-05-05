/*
 * UtilTest.java
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

package hcm.ssj;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Xml;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.Arrays;

import hcm.ssj.core.Log;
import hcm.ssj.core.Util;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class UtilTest
{
    @Test
    public void test() throws Exception
    {
        int[] x = new int[]{0, 2, 9, -20};
        byte[] y = new byte[1024];
        int[] z = new int[1024];

        Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
        Util.arraycopy(y, 0, z, 0, x.length * Util.sizeOf(z[0]));

        for(int i = 0; i < x.length; i++)
            if(x[i] !=  z[i])
                throw new RuntimeException();
    }

    @Test
    public void test2() throws Exception
    {
        long[] x = new long[]{0, 2, 9, -20};
        byte[] y = new byte[1024];
        long[] z = new long[1024];

        Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
        Util.arraycopy(y, 0, z, 0, x.length * Util.sizeOf(z[0]));

        for(int i = 0; i < x.length; i++)
            if(x[i] !=  z[i])
                throw new RuntimeException();
    }

    @Test
    public void test3() throws Exception
    {
        short[] x = new short[]{0, 2, 9, -20};
        byte[] y = new byte[1024];
        short[] z = new short[1024];

        Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
        Util.arraycopy(y, 0, z, 0, x.length * Util.sizeOf(z[0]));

        for(int i = 0; i < x.length; i++)
            if(x[i] !=  z[i])
                throw new RuntimeException();
    }

    @Test
    public void test4() throws Exception
    {
        int ITER = 100;
        long start, delta, ff = 0, fb = 0, bf = 0;
        for (int i = 0; i < ITER; i++)
        {
            float[] x = new float[48000];
            for(int j = 0; j< x.length; ++j)
                x[j] = (float)Math.random();
            byte[] y = new byte[48000 * 4];
            float[] z = new float[48000];

            start = System.nanoTime();
            Util.arraycopy(x, 0, z, 0, x.length * Util.sizeOf(z[0]));
            delta = System.nanoTime() - start;
            Log.i("float-float copy: " + delta + "ns");
            ff += delta;

            start = System.nanoTime();
            Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
            delta = System.nanoTime() - start;
            Log.i("float-byte copy: " + delta + "ns");
            fb += delta;

            Arrays.fill(x, (float)0);

            start = System.nanoTime();
            Util.arraycopy(y, 0, x, 0, x.length * Util.sizeOf(x[0]));
            delta = System.nanoTime() - start;
            Log.i("byte-float copy: " + delta + "ns");
            bf += delta;

            Thread.sleep(1000);
        }
        Log.i("avg float-float copy: " + ff / ITER + "ns");
        Log.i("avg float-byte copy: " + fb / ITER + "ns");
        Log.i("avg byte-float copy: " + bf / ITER + "ns");

    }

    @Test
    public void test5() throws Exception
    {
        double[] x = new double[]{0.7985, 2, 9, -20.556999};
        byte[] y = new byte[1024];
        double[] z = new double[1024];

        Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
        Util.arraycopy(y, 0, z, 0, x.length * Util.sizeOf(z[0]));

        for(int i = 0; i < x.length; i++)
            if(x[i] !=  z[i])
                throw new RuntimeException();
    }

    @Test
    public void test6() throws Exception
    {
        char[] x = new char[]{0, 2, 9};
        byte[] y = new byte[1024];
        char[] z = new char[1024];

        Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
        Util.arraycopy(y, 0, z, 0, x.length * Util.sizeOf(z[0]));

        for(int i = 0; i < x.length; i++)
            if(x[i] !=  z[i])
                throw new RuntimeException();
    }

    @Test
    public void test7() throws Exception
    {
        boolean[] x = new boolean[]{true, false, true, true};
        byte[] y = new byte[1024];
        boolean[] z = new boolean[1024];

        Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
        Util.arraycopy(y, 0, z, 0, x.length * Util.sizeOf(z[0]));

        for(int i = 0; i < x.length; i++)
            if(x[i] !=  z[i])
                throw new RuntimeException();
    }

    @Test
    public void testXmlToStr() throws Exception
    {
        String str = "<ssj><test attr=\"val\">text</test><test attr=\"val2\">text2</test></ssj>";
        Log.i("input: " + str);

        XmlPullParser xml = Xml.newPullParser();
        xml.setInput(new StringReader(str));
        xml.next();

        Log.i("output: " + Util.xmlToString(xml));
    }
}