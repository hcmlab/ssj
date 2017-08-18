/*
 * MainActivity.java
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


package hcm.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import hcm.ssj.core.ExceptionHandler;

public class MainActivity extends Activity implements ExceptionHandler
{
    private PipelineRunner _pipe = null;
    private static final int REQUEST_DANGEROUS_PERMISSIONS = 108;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();
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

        if(_pipe == null || !_pipe.isRunning())
        {

            _pipe = new PipelineRunner(this);
            _pipe.setExceptionHandler(this);
            _pipe.start();
        }
        else
        {
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

                if (running)
                {
                    btn.setText(R.string.stop);
                    btn.setEnabled(true);
                    btn.setAlpha(1.0f);
                } else
                {
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


    @Override
    public void handle(final String location, final String msg, final Throwable t) {

        _pipe.terminate(); //attempt to shut down framework

        this.runOnUiThread(
                new Runnable() {
                   @Override
                   public void run() {
                       Toast.makeText(getApplicationContext(), "Exception in Pipeline\n" + msg, Toast.LENGTH_LONG).show();
                   }
               });

    }


    private void checkPermissions()
    {
        if (Build.VERSION.SDK_INT >= 23)
        {
            //dangerous permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BODY_SENSORS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_DANGEROUS_PERMISSIONS);
            }
        }
    }
}
