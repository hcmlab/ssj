/*
 * Vibrate2Command.java
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

package hcm.ssj.myo;

import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;

import java.util.Arrays;

/**
 * Approach to use vibrate2 without having to modify the existing library
 * (by using reflection and only call methods necessary)
 */
public class Vibrate2Command extends Command {
    private static final byte COMMAND_VIBRATION2 = 0x07; //NEW

    private static enum Vibration2 {
        COMMAND_TYPE,
        PAYLOAD_SIZE,
        DURATION,
        STRENGTH;


        private Vibration2() {
        }
    }

    public Vibrate2Command(Hub hub) {
        super(hub);
    }

    public void vibrate(Myo myo, int duration, byte strength) {
        byte[] vibrate2Command = createForVibrate2(duration, strength);
        this.writeControlCommand(myo.getMacAddress(), vibrate2Command);
    }

    /**
     * Vibrate pattern, up to 6 times duration/strength
     */
    public void vibrate(Myo myo, int[] duration, byte[] strength) {
        byte[] vibrate2Command = createForVibrate2(duration, strength);
        this.writeControlCommand(myo.getMacAddress(), vibrate2Command);
    }

    public void vibrate(Myo myo,
                        int duration1, byte strength1,
                        int duration2, byte strength2,
                        int duration3, byte strength3,
                        int duration4, byte strength4,
                        int duration5, byte strength5,
                        int duration6, byte strength6) {
        byte[] vibrate2Command = createForVibrate2(duration1, strength1, duration2, strength2, duration3, strength3, duration4, strength4, duration5, strength5, duration6, strength6);
        this.writeControlCommand(myo.getMacAddress(), vibrate2Command);
    }

    // #see https://github.com/thalmiclabs/myo-bluetooth/blob/master/myohw.h
    public static byte[] createForVibrate2(int duration, byte strength) {
        byte[] command = new byte[20]; //COMMAND, PAYLOAD, 6*(Duration (uint16), Strength (uint8))
        Arrays.fill(command, (byte) 0);
        command[Vibration2.COMMAND_TYPE.ordinal() /* 0 */] = COMMAND_VIBRATION2;
        command[Vibration2.PAYLOAD_SIZE.ordinal() /* 1 */] = 18; /* MYOHW_PAYLOAD = 18 */
        command[2 /* duration 1/2 */] = (byte) (duration & 0xFF);
        command[3 /* duration 2/2 */] = (byte) ((duration >> 8) & 0xFF);
        command[4 /* strength */] = strength;
        /* rest of the byte-array should be 0 */
        return command;
    }

    // #see https://github.com/thalmiclabs/myo-bluetooth/blob/master/myohw.h
    public static byte[] createForVibrate2(int[] duration, byte[] strength) {
        byte[] command = new byte[20]; //COMMAND, PAYLOAD, 6*(Duration (uint16), Strength (uint8))
        Arrays.fill(command, (byte) 0);
        command[Vibration2.COMMAND_TYPE.ordinal() /* 0 */] = COMMAND_VIBRATION2;
        command[Vibration2.PAYLOAD_SIZE.ordinal() /* 1 */] = 18; /* MYOHW_PAYLOAD = 18 */

        /* maximum of 6 times, but not more often than duration or strength array*/
        int iterations = Math.min(6, Math.min(duration.length, strength.length));
        for (int i = 0; i < iterations; i++) {
            command[3 * i + 2 /* duration 1/2 */] = (byte) (duration[i] & 0xFF);
            command[3 * i + 3 /* duration 2/2 */] = (byte) ((duration[i] >> 8) & 0xFF);
            command[3 * i + 4 /* strength */] = strength[i];
        }
        return command;
    }

    // #see https://github.com/thalmiclabs/myo-bluetooth/blob/master/myohw.h
    public static byte[] createForVibrate2(int duration1, byte strength1,
                                           int duration2, byte strength2,
                                           int duration3, byte strength3,
                                           int duration4, byte strength4,
                                           int duration5, byte strength5,
                                           int duration6, byte strength6) {
        return createForVibrate2(new int[]{duration1, duration2, duration3, duration4, duration5, duration6}, new byte[]{strength1, strength2, strength3, strength4, strength5, strength6});
    }
}
