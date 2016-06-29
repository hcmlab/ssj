package hcm.ssj.angelsensor;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;

import com.angel.sdk.BleCharacteristic;
import com.angel.sdk.BleDevice;
import com.angel.sdk.ChAccelerationEnergyMagnitude;
import com.angel.sdk.ChAccelerationWaveform;
import com.angel.sdk.ChBatteryLevel;
import com.angel.sdk.ChHeartRateMeasurement;
import com.angel.sdk.ChOpticalWaveform;
import com.angel.sdk.ChStepCount;
import com.angel.sdk.ChTemperatureMeasurement;
import com.angel.sdk.SrvActivityMonitoring;
import com.angel.sdk.SrvBattery;
import com.angel.sdk.SrvHealthThermometer;
import com.angel.sdk.SrvHeartRate;
import com.angel.sdk.SrvWaveformSignal;

import junit.framework.Assert;

import java.util.Vector;

import hcm.ssj.core.Sensor;
import hcm.ssj.core.SSJApplication;

/**
 * Created by simon on 17.06.16.
 */
public class AngelSensorListener
{
	private BleDevice mBleDevice;

	private Handler         mHandler;
	private Vector<Integer> opticalGreenLED;
	private Vector<Integer> opticalBlueLED;


	//private Vector<Tupel<float, float, float>> acceleration;

	public AngelSensorListener()
	{
		reset();
	}

	public void reset()
	{
		if (opticalGreenLED == null)
		{
			opticalGreenLED = new Vector<Integer>();

		}
		else
		{
			opticalGreenLED.removeAllElements();
		}
		if (opticalBlueLED == null)
		{
			opticalBlueLED = new Vector<Integer>();

		}
		else
		{
			opticalBlueLED.removeAllElements();
		}

	}

	private final BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue> mAccelerationWaveformListener = new BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue>()
	{
		@Override
		public void onValueReady(ChAccelerationWaveform.AccelerationWaveformValue accelerationWaveformValue)
		{
			if (accelerationWaveformValue != null && accelerationWaveformValue.wave != null)
			{
				for (Integer item : accelerationWaveformValue.wave)
				{
					//mAccelerationWaveformView.addValue(item);
					//vector push
					//provide()

				}
			}

		}
	};


	public void connect(String deviceAddress)
	{
		// A device has been chosen from the list. Create an instance of BleDevice,
		// populate it with interesting services and then connect

		if (mBleDevice != null)
		{
			mBleDevice.disconnect();
		}
		Context context = SSJApplication.getAppContext();
		mHandler = new Handler(context.getMainLooper());
		mBleDevice = new BleDevice(context, mDeviceLifecycleCallback, mHandler);


		try
		{
			mBleDevice.registerServiceClass(SrvWaveformSignal.class);
			mBleDevice.registerServiceClass(SrvHeartRate.class);
			mBleDevice.registerServiceClass(SrvHealthThermometer.class);
			mBleDevice.registerServiceClass(SrvBattery.class);
			mBleDevice.registerServiceClass(SrvActivityMonitoring.class);

		}
		catch (NoSuchMethodException e)
		{
			throw new AssertionError();
		}
		catch (IllegalAccessException e)
		{
			throw new AssertionError();
		}
		catch (InstantiationException e)
		{
			throw new AssertionError();
		}

		mBleDevice.connect(deviceAddress);


	}


	private final BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue> mOpticalWaveformListener = new BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue>()
	{
		@Override
		public void onValueReady(ChOpticalWaveform.OpticalWaveformValue opticalWaveformValue)
		{

			if (opticalWaveformValue != null && opticalWaveformValue.wave != null)
			{
				for (ChOpticalWaveform.OpticalSample item : opticalWaveformValue.wave)
				{
					opticalGreenLED.add(item.green);
					opticalBlueLED.add(item.blue);
					//mBlueOpticalWaveformView.addValue(item.blue);

				}
			}
		}
	};

	private final BleDevice.LifecycleCallback mDeviceLifecycleCallback = new BleDevice.LifecycleCallback()
	{
		@Override
		public void onBluetoothServicesDiscovered(BleDevice bleDevice)
		{
			bleDevice.getService(SrvWaveformSignal.class).getAccelerationWaveform().enableNotifications(mAccelerationWaveformListener);
			bleDevice.getService(SrvWaveformSignal.class).getOpticalWaveform().enableNotifications(mOpticalWaveformListener);
		}

		@Override
		public void onBluetoothDeviceDisconnected()
		{

		}

		@Override
		public void onReadRemoteRssi(int i)
		{

		}
	};

	public int getBvp()
	{
		if (opticalGreenLED.size() > 0)
		{
			int tmp = opticalGreenLED.lastElement();
			if (opticalGreenLED.size() > 1)
			{
				opticalGreenLED.removeElementAt(opticalGreenLED.size() - 1);
			}
			return tmp;

		}
		else
		{
			return 0;
		}
	}

};
