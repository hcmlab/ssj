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

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Xml;

import com.microsoft.band.sensors.MotionType;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

import hcm.ssj.core.Log;
import hcm.ssj.core.Util;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class UtilTest extends ApplicationTestCase<Application> {

    public UtilTest() {
        super(Application.class);
    }

    public void test() throws Exception
    {
        int tord = MotionType.JOGGING.ordinal();


        int[] x = new int[]{0, 2, 9, -20};
        byte[] y = new byte[1024];
        int[] z = new int[1024];

        Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
        Util.arraycopy(y, 0, z, 0, x.length * Util.sizeOf(z[0]));

        for(int i = 0; i < x.length; i++)
            if(x[i] !=  z[i])
                throw new RuntimeException();
    }

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

    public void test4() throws Exception
    {
        float[] x = new float[]{0, 2, 9, -20, -5654684, 78725};
        byte[] y = new byte[1024];
        float[] z = new float[1024];

        Util.arraycopy(x, 0, y, 0, x.length * Util.sizeOf(x[0]));
        Util.arraycopy(y, 0, z, 0, x.length * Util.sizeOf(z[0]));

        for(int i = 0; i < x.length; i++)
            if(x[i] !=  z[i])
                throw new RuntimeException();
    }

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