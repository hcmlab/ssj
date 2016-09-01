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

package hcm.ssjclay;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hcm.ssj.camera.CameraPainter;
import hcm.ssj.core.Cons;
import hcm.ssj.core.ExceptionHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.Monitor;
import hcm.ssj.core.TheFramework;
import hcm.ssj.file.LoggingConstants;
import hcm.ssj.graphic.SignalPainter;
import hcm.ssjclay.creator.Builder;
import hcm.ssjclay.creator.Linker;
import hcm.ssjclay.dialogs.AddDialog;
import hcm.ssjclay.dialogs.FileDialog;
import hcm.ssjclay.dialogs.Listener;
import hcm.ssjclay.view.PipeView;

public class MainActivity extends AppCompatActivity
{
    private static boolean ready = true;
    private static final int REQUEST_DANGEROUS_PERMISSIONS = 108;
    //visual pipe editor
    private PipeView pipeView = null;
    //tabs
    private TabHost tabHost = null;
    private ArrayList<Object> alAdditionalTabs = new ArrayList<>();
    private ArrayList<TabSpec> alTabSpecs = new ArrayList<>();
    private final static int FIX_TAB_NUMBER = 2;
    //console
    private TextView textViewConsole = null;
    private String strLogMsg = "";
    private boolean handleLogMessages = false;
    private Thread threadLog = new Thread()
    {
        private final int sleepTime = 100;
        private Handler handlerLog = new Handler(Looper.getMainLooper());
        private Runnable runnableLog = new Runnable()
        {
            public void run()
            {
                if (textViewConsole != null)
                {
                    textViewConsole.setText(strLogMsg);
                }
            }
        };

        @Override
        public void run()
        {
            while (handleLogMessages)
            {
                try
                {
                    handlerLog.post(runnableLog);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    };
    private Log.LogListener logListener = new Log.LogListener()
    {
        private String[] tags = {"0", "1", "V", "D", "I", "W", "E", "A"};
        private final int max = 100000;
        private final int interim = 100000 / 2;

        /**
         * @param msg String
         */
        public void msg(int type, String msg)
        {
            strLogMsg += (type > 0 && type < tags.length ? tags[type] : type) + "/" + Cons.LOGTAG + ": " + msg + LoggingConstants.DELIMITER_LINE;
            int length = strLogMsg.length();
            if (length > max)
            {
                strLogMsg = strLogMsg.substring(length - interim);
                strLogMsg = strLogMsg.substring(strLogMsg.indexOf(LoggingConstants.DELIMITER_LINE) + LoggingConstants.DELIMITER_LINE.length());
            }
        }
    };
    private PipeView.ViewListener viewListener = new PipeView.ViewListener()
    {
        @Override
        public void viewChanged()
        {
            checkAdditionalTabs();
        }
    };

    /**
     *
     */
    private void init()
    {
        tabHost = (TabHost) findViewById(R.id.id_tabHost);
        if (tabHost != null)
        {
            tabHost.setup();
            //pipe
            pipeView = new PipeView(MainActivity.this);
            pipeView.setWillNotDraw(false);
            addTab(pipeView, getResources().getString(R.string.str_pipe), android.R.drawable.ic_menu_edit);
            //console
            addTabConsole();
            //add log listener
            Log.addLogListener(logListener);
            handleLogMessages = true;
            threadLog.start();
            //add view listener
            pipeView.addViewListener(viewListener);
        }
        //handle permissions
        checkPermissions();
        //set exception handler
        setExceptionHandler();
    }

    /**
     *
     */
    private void setExceptionHandler()
    {
        ExceptionHandler exceptionHandler = new ExceptionHandler()
        {
            @Override
            public void handle(final String location, final String msg, final Throwable t)
            {
                Monitor.notifyMonitor();
                Handler handler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable()
                {
                    public void run()
                    {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.str_error)
                                .setMessage(location + ": " + msg)
                                .setPositiveButton(R.string.str_ok, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                };
                handler.post(runnable);
            }
        };
        TheFramework.getFramework().setExceptionHandler(exceptionHandler);
    }

    /**
     * @param view    View
     * @param tabName String
     * @param image   int
     */
    private void addTab(final View view, final String tabName, int image)
    {
        final TabSpec tabSpec = tabHost.newTabSpec(tabName);
        tabSpec.setContent(new TabHost.TabContentFactory()
        {
            /**
             * @param tag String
             * @return View
             */
            public View createTabContent(String tag)
            {
                return view;
            }
        });
        tabSpec.setIndicator("", ContextCompat.getDrawable(MainActivity.this, image));
        tabHost.addTab(tabSpec);
        //necessary to reset tab strip
        int tab = tabHost.getCurrentTab();
        tabHost.setCurrentTab(tabHost.getTabWidget().getTabCount() - 1);
        tabHost.setCurrentTab(tab);
        alTabSpecs.add(tabSpec);
    }

    /**
     * @param tab int
     */
    private void removeTab(final int tab)
    {
        int current = tabHost.getCurrentTab();
        alTabSpecs.remove(tab);
        tabHost.setCurrentTab(0);
        tabHost.clearAllTabs();
        for (TabSpec tabSpec : alTabSpecs)
        {
            tabHost.addTab(tabSpec);
        }
        tabHost.setCurrentTab(current == 1 ? 1 : 0);
    }

    /**
     *
     */
    private void addTabConsole()
    {
        ScrollView scrollViewConsole = new ScrollView(MainActivity.this);
        textViewConsole = new TextView(MainActivity.this);
        scrollViewConsole.addView(textViewConsole);
        //
        addTab(scrollViewConsole, getResources().getString(R.string.str_console), android.R.drawable.ic_menu_info_details);
    }

    /**
     *
     */
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

    /**
     * @param view View
     */
    public void buttonPressed(View view)
    {
        switch (view.getId())
        {
            case R.id.id_imageButton:
            {
                handlePipe();
                break;
            }
        }
    }

    /**
     * Start or stop pipe
     */
    private void handlePipe()
    {
        if (ready)
        {
            ready = false;
            new Thread()
            {
                @Override
                public void run()
                {
                    //clear console
                    strLogMsg = "";
                    //save framework options
                    TheFramework framework = TheFramework.getFramework();
                    //remove old content
                    framework.clear();
                    //add components
                    Linker.getInstance().buildPipe();
                    //change button text
                    changeImageButton(android.R.drawable.ic_media_pause);
                    //start framework
                    framework.Start();
                    //run
                    Monitor.waitMonitor();
                    //stop framework
                    try
                    {
                        framework.Stop();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    ready = true;
                    //change button text
                    changeImageButton(android.R.drawable.ic_media_play);
                }
            }.start();
        } else
        {
            Monitor.notifyMonitor();
        }
    }

    /**
     * @param idImage int
     */
    private void changeImageButton(final int idImage)
    {
        final ImageButton imageButton = (ImageButton) findViewById(R.id.id_imageButton);
        if (imageButton != null)
        {
            imageButton.post(new Runnable()
            {
                public void run()
                {
                    imageButton.setImageResource(idImage);
                }
            });
        }
    }

    /**
     * @param menu Menu
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    /**
     * @param item MenuItem
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_framework:
            {
                Intent intent = new Intent(getApplicationContext(), OptionsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_sensors:
            {
                showAddDialog(R.string.str_sensors, Builder.getInstance().sensors);
                return true;
            }
            case R.id.action_providers:
            {
                showAddDialog(R.string.str_providers, Builder.getInstance().sensorProviders);
                return true;
            }
            case R.id.action_transformers:
            {
                showAddDialog(R.string.str_transformers, Builder.getInstance().transformers);
                return true;
            }
            case R.id.action_consumers:
            {
                showAddDialog(R.string.str_consumers, Builder.getInstance().consumers);
                return true;
            }
            case R.id.action_save:
            {
                showFileDialog(R.string.str_save, FileDialog.Type.SAVE, R.string.str_saveError);
                return true;
            }
            case R.id.action_load:
            {
                showFileDialog(R.string.str_load, FileDialog.Type.LOAD, R.string.str_loadError);
                return true;
            }
            case R.id.action_delete:
            {
                showFileDialog(R.string.str_delete, FileDialog.Type.DELETE, R.string.str_deleteError);
                return true;
            }
            case R.id.action_clear:
            {
                Linker.getInstance().clear();
                actualizeContent();
                return true;
            }
        }
        return true;
    }

    /**
     * @param resource int
     * @param list     ArrayList
     */
    private void showAddDialog(int resource, ArrayList<Class> list)
    {
        final AddDialog addDialog = new AddDialog();
        addDialog.setTitleMessage(resource);
        addDialog.setOption(list);
        Listener listener = new Listener()
        {
            @Override
            public void onPositiveEvent(Object[] o)
            {
                addDialog.removeListener(this);
                actualizeContent();
            }

            @Override
            public void onNegativeEvent(Object[] o)
            {
                addDialog.removeListener(this);
            }
        };
        addDialog.addListener(listener);
        addDialog.show(getSupportFragmentManager(), MainActivity.this.getClass().getSimpleName());
    }

    /**
     * @param title   int
     * @param type    FileDialog.Type
     * @param message int
     */
    private void showFileDialog(final int title, FileDialog.Type type, final int message)
    {
        final FileDialog fileDialog = new FileDialog();
        fileDialog.setTitleMessage(title);
        fileDialog.setType(type);
        fileDialog.show(getSupportFragmentManager(), MainActivity.this.getClass().getSimpleName());
        Listener listener = new Listener()
        {
            @Override
            public void onPositiveEvent(Object[] o)
            {
                fileDialog.removeListener(this);
                actualizeContent();
            }

            @Override
            public void onNegativeEvent(Object[] o)
            {
                if (o != null)
                {
                    Log.e(getResources().getString(message));
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(message)
                            .setPositiveButton(R.string.str_ok, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                fileDialog.removeListener(this);
            }
        };
        fileDialog.addListener(listener);
    }

    /**
     *
     */
    private void actualizeContent()
    {
        if (pipeView != null)
        {
            pipeView.recalculate();
        }
    }

    /**
     * Add or remove additional tabs
     */
    private void checkAdditionalTabs()
    {
        Object[] consumers = Linker.getInstance().getAll(Linker.Type.Consumer);
        //add additional tabs
        for (Object object : consumers)
        {
            if (object instanceof SignalPainter)
            {
                GraphView graphView = ((SignalPainter) object).options.graphView.get();
                if (graphView == null)
                {
                    graphView = new GraphView(MainActivity.this);
                    ((SignalPainter) object).options.graphView.set(graphView);
                    addTab(graphView, "GraphView", android.R.drawable.ic_menu_view);
                    alAdditionalTabs.add(object);
                }
            } else if (object instanceof CameraPainter)
            {
                SurfaceView surfaceView = ((CameraPainter) object).options.surfaceView.get();
                if (surfaceView == null)
                {
                    surfaceView = new SurfaceView(MainActivity.this);
                    ((CameraPainter) object).options.surfaceView.set(surfaceView);
                    addTab(surfaceView, "SurfaceView", android.R.drawable.ic_menu_camera);
                    alAdditionalTabs.add(object);
                }
            }
        }
        //remove obsolete tabs
        List list = Arrays.asList(consumers);
        for (int i = alAdditionalTabs.size() - 1; i >= 0; i--)
        {
            if (!list.contains(alAdditionalTabs.get(i)))
            {
                alAdditionalTabs.remove(i);
                removeTab(i + FIX_TAB_NUMBER);
            }
        }
    }

    /**
     *
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        actualizeContent();
        if (!ready)
        {
            changeImageButton(android.R.drawable.ic_media_pause);
        }
    }

    /**
     *
     */
    @Override
    protected void onPause()
    {
        super.onPause();
    }

    /**
     *
     */
    @Override
    protected void onDestroy()
    {
        handleLogMessages = false;
        Log.removeLogListener(logListener);
        if (pipeView != null)
        {
            pipeView.removeViewListener(viewListener);
        }
        TheFramework framework = TheFramework.getFramework();
        if (framework.isRunning())
        {
            framework.Stop();
        }
        Linker.getInstance().clear();
        super.onDestroy();
    }

    /**
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        startTutorial();
        setContentView(R.layout.activity_main);
        init();
    }

    /**
     * Override back-button to function like home-button
     */
    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }

    /**
     *
     */
    private void startTutorial()
    {
        //declare a new thread to do a preference check
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                String preference = "firstStart";
                //initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
                //create a new boolean and preference and set it to true
                boolean isFirstStart = getPrefs.getBoolean(preference, true);
                //  If the activity has never started before...
                if (isFirstStart)
                {
                    //launch app intro
                    Intent i = new Intent(MainActivity.this, Tutorial.class);
                    startActivity(i);
                    //make a new preferences editor
                    SharedPreferences.Editor e = getPrefs.edit();
                    //edit preference to make it false because we don't want this to run again
                    e.putBoolean(preference, false);
                    //apply changes
                    e.apply();
                }
            }
        });
        //start the thread
        t.start();
    }
}
