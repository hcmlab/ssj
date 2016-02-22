/*
 * UtilAsTrans.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.androidSensor.transformer;

import android.util.Log;

import hcm.ssj.core.stream.Stream;

/**
 * Utility class used by the transformers in the android sensor package.<br>
 * Created by Frank Gaibler on 10.09.2015.
 */
class UtilAsTrans
{
    /**
     * Transforms an input array of a stream into a float array and returns it.
     *
     * @param stream Stream
     * @param name   String
     * @return float[]
     */
    protected static float[] getValuesAsFloat(Stream stream, String name)
    {
        switch (stream.type)
        {
            case CHAR:
                char[] chars = stream.ptrC();
                float[] floatsC = new float[stream.num * stream.dim];
                for (int i = 0; i < floatsC.length; i++)
                {
                    floatsC[i] = (float) chars[i];
                }
                return floatsC;
            case SHORT:
                short[] shorts = stream.ptrS();
                float[] floatsS = new float[stream.num * stream.dim];
                for (int i = 0; i < floatsS.length; i++)
                {
                    floatsS[i] = (float) shorts[i];
                }
                return floatsS;
            case INT:
                int[] ints = stream.ptrI();
                float[] floatsI = new float[stream.num * stream.dim];
                for (int i = 0; i < floatsI.length; i++)
                {
                    floatsI[i] = (float) ints[i];
                }
                return floatsI;
            case LONG:
                long[] longs = stream.ptrL();
                float[] floatsL = new float[stream.num * stream.dim];
                for (int i = 0; i < floatsL.length; i++)
                {
                    floatsL[i] = (float) longs[i];
                }
                return floatsL;
            case FLOAT:
                return stream.ptrF();
            case DOUBLE:
                double[] doubles = stream.ptrD();
                float[] floatsD = new float[stream.num * stream.dim];
                for (int i = 0; i < floatsD.length; i++)
                {
                    floatsD[i] = (float) doubles[i];
                }
                return floatsD;
            default:
                Log.e(name, "invalid input stream type");
                return new float[stream.num * stream.dim];
        }
    }
}
