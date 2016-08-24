/*
 * Event.java
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

package hcm.ssj.core.event;

import hcm.ssj.core.Cons;

/**
 * Created by Johnny on 19.03.2015.
 */
public abstract class Event {

    public enum State
    {
        COMPLETED,
        CONTINUED
    }

    public String name;
    public String sender;
    public int time;
    public int dur;
    public State state;
    public Cons.Type type;

    public int id;

    public static Event create(Cons.Type type)
    {
        switch(type)
        {
            case BYTE:
                return new ByteEvent();
            case CHAR:
            case STRING:
                return new StringEvent();
            case SHORT:
                return new ShortEvent();
            case INT:
                return new IntEvent();
            case LONG:
                return new LongEvent();
            case FLOAT:
                return new FloatEvent();
            case DOUBLE:
                return new DoubleEvent();
            case BOOL:
                return new BoolEvent();
            case EMPTY:
                return new EmptyEvent();
            default:
                throw new UnsupportedOperationException("Event type not supported");
        }
    }

    public Event()
    {
        this.name = "";
        this.sender = "";
        this.time = 0;
        this.dur = 0;
        this.state = State.COMPLETED;
    }

    public abstract void setData(Object data);
    public void setData(byte[] data) { throw new UnsupportedOperationException(); }
    public void setData(char[] data) { throw new UnsupportedOperationException(); }
    public void setData(int[] data) { throw new UnsupportedOperationException(); }
    public void setData(long[] data) { throw new UnsupportedOperationException(); }
    public void setData(float[] data) { throw new UnsupportedOperationException(); }
    public void setData(double[] data) { throw new UnsupportedOperationException(); }
    public void setData(boolean[] data) { throw new UnsupportedOperationException(); }
    public void setData(String data) { throw new UnsupportedOperationException(); }
    
    public abstract Object ptr();
    public byte[] ptrB() { throw new UnsupportedOperationException(); }
    public char[] ptrC() { throw new UnsupportedOperationException(); }
    public short[] ptrShort() { throw new UnsupportedOperationException(); }
    public int[] ptrI() { throw new UnsupportedOperationException(); }
    public long[] ptrL() { throw new UnsupportedOperationException(); }
    public float[] ptrF() { throw new UnsupportedOperationException(); }
    public double[] ptrD() { throw new UnsupportedOperationException(); }
    public boolean[] ptrBool() { throw new UnsupportedOperationException(); }
    public String ptrStr() { throw new UnsupportedOperationException(); }
}
