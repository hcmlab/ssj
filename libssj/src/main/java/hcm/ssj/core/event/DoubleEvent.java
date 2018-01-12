/*
 * DoubleEvent.java
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

package hcm.ssj.core.event;

import java.util.Arrays;

import hcm.ssj.core.Cons;

/**
 * Created by Johnny on 19.03.2015.
 */
public class DoubleEvent extends Event {

    public double[] data;

    public DoubleEvent() {
        type = Cons.Type.DOUBLE;
        data = null;
    }

    public DoubleEvent(double[] data) {
        type = Cons.Type.DOUBLE;
        this.data = data;
    }

    public double[] ptr() {
        return data;
    }

    public double[] ptrD() {
        return data;
    }

    public String ptrStr() {
        return Arrays.toString(data);
    }

    public void setData(Object data) {
        this.data = (double[])data;
    }
    public void setData(double[] data) {
        this.data = data;
    }
}
