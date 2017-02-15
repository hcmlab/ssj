/*
 * SimpleHeader.java
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

package hcm.ssj.file;

/**
 * Simple header file.<br>
 * Created by Frank Gaibler on 31.08.2015.
 */
class SimpleHeader
{
    protected final static String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS";
    private final static String _version = "1.0";
    private final static String _ssi_v = "2";
    protected String _ftype = "ASCII";
    protected String _sr = "50.0";
    protected String _dim = "1";
    protected String _byte = "4";
    protected String _type = "FLOAT";
    protected String _ms = "0";
    protected String _local = "00/00/00 00:00:00:0";
    protected String _system = "00/00/00 00:00:00:0";
    protected String _from = "0.0";
    protected String _to = "0.0";
    private final static String _byte2 = "0";
    protected String _num = "0";

    /**
     * @return String
     */
    protected String getLine1()
    {
        return "<?xml version=\"" + _version + "\" ?>";
    }

    /**
     * @return String
     */
    protected String getLine2()
    {
        return "<stream ssi-v=\"" + _ssi_v + "\">";
    }

    /**
     * @return String
     */
    protected String getLine3()
    {
        return "<info ftype=\"" + _ftype + "\" sr=\"" + _sr + "\" dim=\"" + _dim + "\" byte=\"" + _byte + "\" type=\"" + _type + "\" />";
    }

    /**
     * @return String
     */
    protected String getLine4()
    {
        return "<time ms=\"" + _ms + "\" local=\"" + _local + "\" system=\"" + _system + "\"/>";
    }

    /**
     * @return String
     */
    protected String getLine5()
    {
        return "<chunk from=\"" + _from + "\" to=\"" + _to + "\" byte=\"" + _byte2 + "\" num=\"" + _num + "\"/>";
    }

    /**
     * @return String
     */
    protected String getLine6()
    {
        return "</stream>";
    }
}
