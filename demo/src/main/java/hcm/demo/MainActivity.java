/*
 * MainActivity.java
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


package hcm.demo;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;

import hcm.ssj.core.ExceptionHandler;
import hcm.ssj.core.Pipeline;
import hcm.ssj.myo.Vibrate2Command;

public class MainActivity extends Activity implements ExceptionHandler
{
    private PipelineRunner _pipe = null;
    private String _ssj_version = null;
    private String _error_msg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _ssj_version = "SSJ v" + Pipeline.getInstance().getVersion();

        TextView text = (TextView) findViewById(R.id.txt_ssj);
        text.setText(_ssj_version);
    }

    @Override
    protected void onDestroy()
    {
        if (_pipe != null && _pipe.isRunning())
            _pipe.terminate();

        super.onDestroy();
        Log.i("LogueWorker", "destroyed");
    }

    /**
     * Prevent activity from being destroyed once back button is pressed
     */
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }

    public void onStartPressed(View v)
    {
        Button btn = (Button) findViewById(R.id.btn_start);
        btn.setAlpha(0.5f);
        btn.setEnabled(false);
        getCacheDir().getAbsolutePath();

        AssetManager am = getApplicationContext().getAssets();
        getAssets();
        TextView text = (TextView) findViewById(R.id.txt_ssj);

        if(_pipe == null || !_pipe.isRunning())
        {
            text.setText(_ssj_version + " - starting");



            GraphView graph = (GraphView) findViewById(R.id.graph);
            graph.removeAllSeries();
//            graph.getSecondScale().removeAllSeries(); //not implemented in GraphView 4.0.1
            GraphView graph2 = (GraphView) findViewById(R.id.graph2);
            graph2.removeAllSeries();
//            graph2.getSecondScale().removeAllSeries(); //not implemented in GraphView 4.0.1

            GraphView graphs[] = new GraphView[]{graph, graph2};

            _pipe = new PipelineRunner(this, graphs);
            _pipe.setExceptionHandler(this);
            _pipe.start();
        }
        else
        {
            text.setText(_ssj_version + " - stopping");
            _pipe.terminate();
        }
    }

    public void notifyPipeState(final boolean running)
    {
        this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Button btn = (Button) findViewById(R.id.btn_start);
                TextView text = (TextView) findViewById(R.id.txt_ssj);

                if (running)
                {
                    text.setText(_ssj_version + " - running");
                    btn.setText(R.string.stop);
                    btn.setEnabled(true);
                    btn.setAlpha(1.0f);
                } else
                {
                    text.setText(_ssj_version + " - not running");
                    if(_error_msg != null)
                    {
                        String str = text.getText() + "\nERROR: " + _error_msg;
                        text.setText(str);
                    }

                    btn.setText(R.string.start);
                    btn.setEnabled(true);
                    btn.setAlpha(1.0f);
                }
            }
        });
    }

    public void onClosePressed(View v)
    {
        finish();
    }

    public void onDemoPressed(View v)
    {
        final Hub hub = Hub.getInstance();
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                if(hub.getConnectedDevices().isEmpty())
                {
                    Log.e("Logue_SSJ", "device not found");
                }
                else
                {
                    com.thalmic.myo.Myo myo = hub.getConnectedDevices().get(0);
                    startVibrate(myo, hub);
                }
            }
        });
        t1.start();
    }

    private void startVibrate(Myo myo, Hub hub) {
        String _name = "test";
        Log.i(_name, "connected");
        try {
            Vibrate2Command vibrate2Command = new Vibrate2Command(hub);

            Log.i(_name, "vibrate 1...");
            myo.vibrate(Myo.VibrationType.MEDIUM);
            Thread.sleep(3000);

            Log.i(_name, "vibrate 2...");
            //check strength 50
            vibrate2Command.vibrate(myo, 1000, (byte) 50);
            Thread.sleep(3000);

            Log.i(_name, "vibrate 3 ...");
            //check strength 100
            vibrate2Command.vibrate(myo, 1000, (byte) 100);
            Thread.sleep(3000);

            Log.i(_name, "vibrate 4 ...");
            //check strength 100
            vibrate2Command.vibrate(myo, 1000, (byte) 150);
            Thread.sleep(3000);

            Log.i(_name, "vibrate 5...");
            //check strength 250
            vibrate2Command.vibrate(myo, 1000, (byte) 200);
            Thread.sleep(3000);

            Log.i(_name, "vibrate 6...");
            //check strength 250
            vibrate2Command.vibrate(myo, 1000, (byte) 250);
            Thread.sleep(3000);

            Log.i(_name, "vibrate pattern...");
            //check vibrate pattern
            vibrate2Command.vibrate(myo, new int[]{500, 500, 500, 500, 500, 500}, new byte[]{25, 50, 100, (byte) 150, (byte) 200, (byte) 250});
            Thread.sleep(3000);
        } catch (Exception e) {
            Log.e(_name, "exception in vibrate test", e);
        }
    }

    @Override
    public void handle(final String location, final String msg, final Throwable t) {

        _error_msg = msg;
        _pipe.terminate(); //attempt to shut down framework

        this.runOnUiThread(
                new Runnable() {
                   @Override
                   public void run() {
                       Toast.makeText(getApplicationContext(), "Exception in Pipeline\n" + msg, Toast.LENGTH_LONG).show();
                   }
               });

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item)
//    {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings)
//        {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
