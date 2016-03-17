/*
 * EmpaticaTest.java
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

package hcm.ssj;

import android.app.Application;
import android.test.ApplicationTestCase;

import hcm.ssj.core.Log;
import hcm.ssj.core.TheFramework;
import hcm.ssj.empatica.AccelerationProvider;
import hcm.ssj.empatica.BVPProvider;
import hcm.ssj.empatica.Empatica;
import hcm.ssj.empatica.GSRProvider;
import hcm.ssj.empatica.IBIProvider;
import hcm.ssj.empatica.TemperatureProvider;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 15.04.2015.
 */
public class EmpaticaTest extends ApplicationTestCase<Application>
{
	String _name = "SSJ_test_Empatica";

	public EmpaticaTest()
	{
		super(Application.class);
	}

	public void testAcc() throws Exception {
		TheFramework frame = TheFramework.getFramework();

		Empatica empatica = new Empatica();
		AccelerationProvider acc = new AccelerationProvider();
		empatica.addProvider(acc);
		frame.addSensor(empatica);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, acc, 0.1, 0);

		try {
			frame.Start();

			long start = System.currentTimeMillis();
			while (true) {
				if (System.currentTimeMillis() > start + 10 * 1000) {
					break;
				}

				Thread.sleep(1);
			}

			frame.Stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.i("ACC test finished");
	}

	public void testGsr() throws Exception {
		TheFramework frame = TheFramework.getFramework();

		Empatica empatica = new Empatica();
		GSRProvider data = new GSRProvider();
		empatica.addProvider(data);
		frame.addSensor(empatica);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, data, 0.25, 0);

		try {
			frame.Start();

			long start = System.currentTimeMillis();
			while (true) {
				if (System.currentTimeMillis() > start + 10 * 1000) {
					break;
				}

				Thread.sleep(1);
			}

			frame.Stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.i("GSR test finished");
	}

	public void testIBI() throws Exception {
		TheFramework frame = TheFramework.getFramework();

		Empatica empatica = new Empatica();
		IBIProvider data = new IBIProvider();
		empatica.addProvider(data);
		frame.addSensor(empatica);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, data, 0.1, 0);

		try {
			frame.Start();

			long start = System.currentTimeMillis();
			while (true) {
				if (System.currentTimeMillis() > start + 10 * 1000) {
					break;
				}

				Thread.sleep(1);
			}

			frame.Stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.i("IBI test finished");
	}

	public void testTemp() throws Exception {
		TheFramework frame = TheFramework.getFramework();

		Empatica empatica = new Empatica();
		TemperatureProvider data = new TemperatureProvider();
		empatica.addProvider(data);
		frame.addSensor(empatica);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, data, 0.25, 0);

		try {
			frame.Start();

			long start = System.currentTimeMillis();
			while (true) {
				if (System.currentTimeMillis() > start + 10 * 1000) {
					break;
				}

				Thread.sleep(1);
			}

			frame.Stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.i("Temp test finished");
	}

	public void testBVP() throws Exception {
		TheFramework frame = TheFramework.getFramework();

		Empatica empatica = new Empatica();
		BVPProvider data = new BVPProvider();
		empatica.addProvider(data);
		frame.addSensor(empatica);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, data, 0.1, 0);

		try {
			frame.Start();

			long start = System.currentTimeMillis();
			while (true) {
				if (System.currentTimeMillis() > start + 10 * 1000) {
					break;
				}

				Thread.sleep(1);
			}

			frame.Stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.i("BVP test finished");
	}
}
