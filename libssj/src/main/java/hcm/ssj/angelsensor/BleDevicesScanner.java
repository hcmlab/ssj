/*
 * BleDevicesScanner.java
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

package hcm.ssj.angelsensor;
/*
 * 
 * https://github.com/StevenRudenko/BleSensorTag/blob/master/src/sample/ble/sensortag/ble/BleDevicesScanner.java
 * 
 */


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;


public class BleDevicesScanner implements Runnable, BluetoothAdapter.LeScanCallback {
    private static final String TAG = BleDevicesScanner.class.getSimpleName();

    private static final long DEFAULT_SCAN_PERIOD = 500L;
    public static final long PERIOD_SCAN_ONCE = -1;

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final LeScansPoster leScansPoster;

    private long scanPeriod = DEFAULT_SCAN_PERIOD;
    private Thread scanThread;
    private volatile boolean isScanning = false;

    public BleDevicesScanner(BluetoothAdapter adapter, BluetoothAdapter.LeScanCallback callback) {
        bluetoothAdapter = adapter;
        leScansPoster = new LeScansPoster(callback);
    }

    public synchronized void setScanPeriod(long scanPeriod) {
        this.scanPeriod = scanPeriod < 0 ? PERIOD_SCAN_ONCE : scanPeriod;
    }

    public boolean isScanning() {
        return scanThread != null && scanThread.isAlive();
    }

    public synchronized void start() {
        if (isScanning())
            return;

        if (scanThread != null) {
            scanThread.interrupt();
        }
        scanThread = new Thread(this);
        scanThread.setName(TAG);
        scanThread.start();
    }

    public synchronized void stop() {
        isScanning = false;
        if (scanThread != null) {
            scanThread.interrupt();
            scanThread = null;
        }
        bluetoothAdapter.stopLeScan(this);
    }

    @Override
    public void run() {
        try {
            isScanning = true;
            do {
                synchronized (this) {
                    bluetoothAdapter.startLeScan(this);
                }

                if (scanPeriod > 0)
                    Thread.sleep(scanPeriod);

                synchronized (this) {
                    bluetoothAdapter.stopLeScan(this);
                }
            } while (isScanning && scanPeriod > 0);
        } catch (InterruptedException ignore) {
        } finally {
            synchronized (this) {
                bluetoothAdapter.stopLeScan(this);
            }
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        synchronized (leScansPoster) {
            leScansPoster.set(device, rssi, scanRecord);
            mainThreadHandler.post(leScansPoster);
        }
    }

    private static class LeScansPoster implements Runnable {
        private final BluetoothAdapter.LeScanCallback leScanCallback;

        private BluetoothDevice device;
        private int rssi;
        private byte[] scanRecord;

        private LeScansPoster(BluetoothAdapter.LeScanCallback leScanCallback) {
            this.leScanCallback = leScanCallback;
        }

        public void set(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        @Override
        public void run() {
            leScanCallback.onLeScan(device, rssi, scanRecord);
        }
    }
}
