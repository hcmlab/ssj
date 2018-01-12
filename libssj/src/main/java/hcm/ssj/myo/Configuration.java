/*
 * Configuration.java
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

package hcm.ssj.myo;

import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import hcm.ssj.core.Log;

/**
 * Approach to add a ValueListener to the hub-interface
 *
 * Authors: Daniel Langerenken, Ionut Damian, Michael Dietz
 */
public class Configuration extends Command {

    private static final byte EMG_MODE_DISABLED = 0x00; //New
    private static final byte EMG_MODE_FILTERED = 0x02;
    private static final byte EMG_MODE_RAW = 0x03;
    private static final byte IMU_MODE_DISABLED = 0;
    private static final byte IMU_MODE_ENABLED = 1;
    private static final byte COMMAND_SET_MODE = 0x01;
    private static final byte COMMAND_SLEEP_MODE = 0x09;

    static final UUID EMG0_DATA_CHAR_UUID = UUID.fromString("d5060105-a904-deb9-4748-2c7f4a124842");
    static final UUID EMG1_DATA_CHAR_UUID = UUID.fromString("d5060205-a904-deb9-4748-2c7f4a124842");
    static final UUID EMG2_DATA_CHAR_UUID = UUID.fromString("d5060305-a904-deb9-4748-2c7f4a124842");
    static final UUID EMG3_DATA_CHAR_UUID = UUID.fromString("d5060405-a904-deb9-4748-2c7f4a124842");

    private static final byte CLASSIFIER_MODE_DISABLED = 0;
    private static final byte CLASSIFIER_MODE_ENABLED = 1;

    private Hub hub;
    private MyoListener mListener;

    private EmgMode emgMode;
    private boolean imuMode;
    private boolean gesturesMode;

    private SleepMode sleepMode = SleepMode.NEVER_SLEEP;

    Object myValueListener;

    public Configuration(Hub hub, final MyoListener listener, EmgMode emg, boolean imu, boolean gestures)
    {
        super(hub);
        _name = "Myo_Config";

        this.hub = hub;
        mListener = listener;

        this.emgMode = emg;
        this.imuMode = imu;
        this.gesturesMode = gestures;
    }

    public void apply(String macAddress)
    {
        configureDataAcquisition(macAddress, emgMode, imuMode, gesturesMode);
        configureSleepMode(macAddress, sleepMode);

        try
        {
            Method method = null;
            for (Method mt : hub.getClass().getDeclaredMethods()) {
                if (mt.getName().contains("addGattValueListener")) {
                    method = mt;
                    break;
                }
            }
            if (method == null) {
                Log.e("Method not found!!");
                return;
            }
            method.setAccessible(true);
            Class<?> valueListener = method.getParameterTypes()[0];
            myValueListener = Proxy.newProxyInstance(valueListener.getClassLoader(), new Class<?>[]{valueListener}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("onCharacteristicChanged")) {
                        Myo myo = (Myo) args[0];
                        UUID uuid = (UUID) args[1];
                        byte[] data = (byte[]) args[2];
                        if (EMG0_DATA_CHAR_UUID.equals(uuid) || EMG1_DATA_CHAR_UUID.equals(uuid) || EMG2_DATA_CHAR_UUID.equals(uuid) || EMG3_DATA_CHAR_UUID.equals(uuid)) {
                            onEMGData(myo, uuid, data);
                            return 1;
                        }
                    }
                    //because invoke replaces ALL method call ups to this instance, we need to catch ALL possible call ups
                    //because the listeners are stored in a hashmap, the hashmap calls equals when a new listener is added
                    else if (method.getName().equals("equals"))
                    {
                        return args[0].equals(this);
                    }
                    return -1;
                }
            });
            Object o = method.invoke(hub, myValueListener);
        } catch (Exception e) {
            Log.w("unable to set EMG listener", e);
        }
    }

    public void undo(String macAddress)
    {
        configureSleepMode(macAddress, SleepMode.NORMAL);

        // Get field
        Field field = null;
        try
        {
            field = hub.getClass().getDeclaredField("mGattCallback");

            // Make it accessible
            field.setAccessible(true);

            // Obtain the field value from the object instance
            Object fieldValue = field.get(hub);

            // Get remove method
            Method myMethod = null;
            for (Method mt : fieldValue.getClass().getDeclaredMethods()) {
                if (mt.getName().contains("removeValueListener")) {
                    myMethod = mt;
                    break;
                }
            }

            if(myMethod == null)
                throw new NoSuchMethodException();

            myMethod.setAccessible(true);

            // Invoke removeValueListener on instance field mGattCallback
            myMethod.invoke(fieldValue, myValueListener);
        }
        catch (NoSuchFieldException e) { Log.w("unable to undo config", e); }
        catch (InvocationTargetException e) { Log.w("unable to undo config", e); }
        catch (NoSuchMethodException e) { Log.w("unable to undo config", e); }
        catch (IllegalAccessException e) { Log.w("unable to undo config", e); }
    }

    public void onEMGData(Myo myo, UUID uuid, byte[] data)
    {
        mListener.onEMGData(myo, uuid, data);
    }

    public void configureDataAcquisition(String address, EmgMode mode, boolean streamImu, boolean enableClassifier) {
        byte[] enableCommand = createForSetMode(mode, streamImu, enableClassifier);
        writeControlCommand(address, enableCommand);
    }

    static byte[] createForSetMode(EmgMode streamEmg, boolean streamImu, boolean enableClassifier) {
        byte emgMode = EMG_MODE_DISABLED;
        switch (streamEmg) {
            case FILTERED: {
                emgMode = EMG_MODE_FILTERED;
                break;
            }
            case RAW: {
                emgMode = EMG_MODE_RAW;
            }
        }
        byte imuMode = streamImu ? IMU_MODE_ENABLED : IMU_MODE_DISABLED;
        byte classifierMode = enableClassifier ? CLASSIFIER_MODE_ENABLED : CLASSIFIER_MODE_DISABLED;
        return createForSetMode(emgMode, imuMode, classifierMode);
    }

    private static byte[] createForSetMode(byte emgMode, byte imuMode, byte classifierMode) {
        byte[] controlCommand = new byte[SetModeCmd.values().length];
        controlCommand[SetModeCmd.COMMAND_TYPE.ordinal()] = COMMAND_SET_MODE;
        controlCommand[SetModeCmd.PAYLOAD_SIZE.ordinal()] = (byte) (controlCommand.length - 2);
        controlCommand[SetModeCmd.EMG_MODE.ordinal()] = emgMode;
        controlCommand[SetModeCmd.IMU_MODE.ordinal()] = imuMode;
        controlCommand[SetModeCmd.CLASSIFIER_MODE.ordinal()] = classifierMode;
        return controlCommand;
    }

    public void configureSleepMode(String address, SleepMode mode) {
        byte[] cmd = createSleepMode(mode.id);
        writeControlCommand(address, cmd);
    }

    private static byte[] createSleepMode(byte sleepMode) {
        byte[] controlCommand = new byte[SleepModeCmd.values().length];
        controlCommand[SleepModeCmd.COMMAND_TYPE.ordinal()] = COMMAND_SLEEP_MODE;
        controlCommand[SleepModeCmd.PAYLOAD_SIZE.ordinal()] = (byte) (controlCommand.length - 2);
        controlCommand[SleepModeCmd.SLEEP_MODE.ordinal()] = sleepMode;
        return controlCommand;
    }

    private enum SetModeCmd {
        COMMAND_TYPE,
        PAYLOAD_SIZE,
        EMG_MODE,
        IMU_MODE,
        CLASSIFIER_MODE;
    }

    public enum EmgMode {
        DISABLED,
        FILTERED,
        RAW;
    }

    public enum SleepMode {
        NORMAL((byte)0), ///< Normal sleep mode; Myo will sleep after a period of inactivity.
        NEVER_SLEEP ((byte)1); ///< Never go to sleep.

        public final byte id;
        SleepMode(byte id) { this.id = id; }
    }

    public enum SleepModeCmd {
        COMMAND_TYPE,
        PAYLOAD_SIZE,
        SLEEP_MODE;
    }
}
