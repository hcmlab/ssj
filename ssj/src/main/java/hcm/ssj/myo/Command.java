/*
 * Command.java
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

package hcm.ssj.myo;

import android.util.Log;

import com.thalmic.myo.Hub;
import com.thalmic.myo.internal.ble.BleManager;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * ParentClass for executing commands
 */
public class Command {

    String _name = "SSJ_sensor_Myo_Cmd";
    BleManager mBleManager;

    public Command(Hub hub) {
        /* try to access mBleManager field which is private by using reflection */
        /* this field is required as this is the communication-module */
        Field field;
        try {
            field = Hub.class.getDeclaredField("mBleManager");
            field.setAccessible(true);
            mBleManager = (BleManager) field.get(hub);
        }
        catch (NoSuchFieldException e) {Log.w(_name, "unable to access BleManager", e);}
        catch (IllegalAccessException e) {Log.w(_name, "unable to access BleManager", e);}
    }

    static final UUID CONTROL_SERVICE_UUID = UUID.fromString("d5060001-a904-deb9-4748-2c7f4a124842");
    static final UUID COMMAND_CHAR_UUID = UUID.fromString("d5060401-a904-deb9-4748-2c7f4a124842");

    void writeControlCommand(String address, byte[] controlCommand) {
        UUID serviceUuid = CONTROL_SERVICE_UUID;
        UUID charUuid = COMMAND_CHAR_UUID;
        this.mBleManager.getBleGatt().writeCharacteristic(address, serviceUuid, charUuid, controlCommand);
    }
}
