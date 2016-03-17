/*
 * Cons.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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
    public final static String VERSION = "0.3.0";

    public final static String LOGTAG = "SSJ";
    public final static float DFLT_SYNC_INTERVAL = 5.0f; //in seconds
    public final static float DFLT_WATCH_INTERVAL = 1.0f; //in seconds
    public final static long SLEEP_ON_IDLE = 10; //in ms
    public final static long SLEEP_ON_TERMINATE = 100; //in ms
    public final static long WAIT_SENSOR_CONNECT = 60000; //in ms
    public final static long WAIT_THREAD_TERMINATION = 60000; //in ms
    public final static int MAX_EVENT_SIZE = 4096; //in bytes
    public final static int MAX_NUM_EVENTS_PER_CHANNEL = 128; //in bytes

    public final static int THREAD_PRIORIIY_LOW = 10; //nice scale, -20 highest priority, 19 lowest priority
    public final static int THREAD_PRIORITY_NORMAL = -2; //nice scale, -20 highest priority, 19 lowest priority
    public final static int THREAD_PRIORIIY_HIGH = -19; //nice scale, -20 highest priority, 19 lowest priority

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
        CUSTOM,
        STRING
    }
}
