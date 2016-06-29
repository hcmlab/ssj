package hcm.ssjclay;

import android.Manifest;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hcm.ssj.camera.CameraPainter;
import hcm.ssj.core.Log;
import hcm.ssj.core.Monitor;
import hcm.ssj.core.TheFramework;
import hcm.ssj.graphic.SignalPainter;
import hcm.ssjclay.creator.Builder;
import hcm.ssjclay.creator.Linker;
import hcm.ssjclay.dialogs.AddDialog;
import hcm.ssjclay.dialogs.Listener;
import hcm.ssjclay.view.PipeView;

public class MainActivity extends AppCompatActivity
{
    private static boolean ready = true;
    private static final int REQUEST_DANGEROUS_PERMISSIONS = 108;
    private static final int REQUEST_SYSTEM_PERMISSIONS = 109;
    //visual pipe editor
    private PipeView pipeView = null;
    //tabs
    private LocalActivityManager mlam = null;
    private TabHost tabHost = null;
    private ArrayList<Object> alAdditionalTabs = new ArrayList<>();
    private final static int FIX_TAB_NUMBER = 2;
    //console
    private TextView textViewConsole = null;
    private String strLogMsg = "";
    private Log.LogListener logListener = new Log.LogListener()
    {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                if (textViewConsole != null)
                {
                    textViewConsole.setText(strLogMsg);
                }
            }
        };

        /**
         * @param msg String
         */
        public void send(String msg)
        {
            strLogMsg += msg;
            handler.post(runnable);
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
    private void init(Bundle savedInstanceState)
    {
        mlam = new LocalActivityManager(this, false);
        mlam.dispatchCreate(savedInstanceState);
        tabHost = (TabHost) findViewById(R.id.id_tabHost);
        pipeView = new PipeView(MainActivity.this);
        if (tabHost != null)
        {
            tabHost.setup(mlam);
            //pipe
            pipeView.setWillNotDraw(false);
            addTab(pipeView, getResources().getString(R.string.str_pipe));
            //console
            addTabConsole();
        }
        //add log listener
        Log.addLogListener(logListener);
        //add view listener
        pipeView.addViewListener(viewListener);
        //handle permissions
        checkPermissions();
    }

    /**
     * @param view    View
     * @param tabName String
     */
    private void addTab(final View view, final String tabName)
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
        tabSpec.setIndicator(tabName);
        tabHost.addTab(tabSpec);
        int height = tabHost.getHeight();
        int width = tabHost.getWidth();
        //expand to full size
        if (height > 0 && width > 0)
        {
            view.setMinimumHeight(height);
            view.setMinimumWidth(width);
        }
    }

    /**
     * @param tab int
     */
    private void removeTab(int tab)
    {
        tabHost.getTabWidget().removeView(tabHost.getTabWidget().getChildTabViewAt(tab));
    }

    /**
     *
     */
    private void addTabConsole()
    {
        final ScrollView scrollView = new ScrollView(MainActivity.this);
        textViewConsole = new TextView(MainActivity.this);
        scrollView.addView(textViewConsole);
        //
        addTab(scrollView, getResources().getString(R.string.str_console));
        //workaround to adjust size retroactively
        Handler handler = new Handler(Looper.getMainLooper());
        Thread thread = new Thread()
        {
            /**
             *
             */
            @Override
            public void run()
            {
                scrollView.setMinimumHeight(tabHost.getHeight());
                scrollView.setMinimumWidth(tabHost.getWidth());
            }
        };
        handler.postDelayed(thread, 200);
    }

    /**
     *
     */
    private void checkPermissions()
    {
        if (Build.VERSION.SDK_INT >= 23)
        {
            //dangerous permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BODY_SENSORS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_DANGEROUS_PERMISSIONS);
            }
            //system permissions
            if (!Settings.canDrawOverlays(this))
            {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_SYSTEM_PERMISSIONS);
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
            case R.id.id_button_start:
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
                    MainActivity.this.logListener.send("");
                    //save framework options
                    TheFramework framework = TheFramework.getFramework();
                    //remove old content
                    framework.clear();
                    //add components
                    Linker.getInstance().buildPipe();
                    //change button text
                    changeButtonText(R.string.str_stop);
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
                    changeButtonText(R.string.str_start);
                }
            }.start();
        } else
        {
            Monitor.notifyMonitor();
        }
    }

    /**
     * @param idText int
     */
    private void changeButtonText(final int idText)
    {
        final Button button = (Button) findViewById(R.id.id_button_start);
        if (button != null)
        {
            button.post(new Runnable()
            {
                public void run()
                {
                    button.setText(idText);
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
        final AddDialog addDialog = new AddDialog();
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
                addDialog.setTitleMessage(R.string.str_sensors);
                addDialog.setOption(Builder.getInstance().sensors);
                break;
            }
            case R.id.action_providers:
            {
                addDialog.setTitleMessage(R.string.str_providers);
                addDialog.setOption(Builder.getInstance().sensorProviders);
                break;
            }
            case R.id.action_transformers:
            {
                addDialog.setTitleMessage(R.string.str_transformers);
                addDialog.setOption(Builder.getInstance().transformers);
                break;
            }
            case R.id.action_consumers:
            {
                addDialog.setTitleMessage(R.string.str_consumers);
                addDialog.setOption(Builder.getInstance().consumers);
                break;
            }
            case R.id.action_save:
            {
                /*
                File dir = new File(Environment.getExternalStorageDirectory(), "SSJ");
                if (dir.mkdirs())
                {
                    SaveLoad.getInstance().save(new File(dir, "test"));
                }
                Class<?> affe = Float.class;
                float x = 2;
                Float p = (Float) affe.cast(x);
                */
                return true;
            }
            case R.id.action_load:
            {
                /*
                SaveLoad.getInstance().load(null);
                */
                return true;
            }
        }
        Listener listener = new Listener()
        {
            @Override
            public void onPositiveEvent(Object[] o)
            {
                actualizeContent();
                addDialog.removeListener(this);
            }

            @Override
            public void onNegativeEvent(Object[] o)
            {
                addDialog.removeListener(this);
            }
        };
        addDialog.addListener(listener);
        addDialog.show(getSupportFragmentManager(), MainActivity.this.getClass().getSimpleName());
        return true;
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
                GraphView graphView = ((SignalPainter) object).options.graphView.getValue();
                if (graphView == null)
                {
                    graphView = new GraphView(MainActivity.this);
                    ((SignalPainter) object).options.graphView.setValue(graphView);
                    addTab(graphView, "GraphView");
                    alAdditionalTabs.add(object);
                }
            } else if (object instanceof CameraPainter)
            {
                SurfaceView surfaceView = ((CameraPainter) object).options.surfaceView.getValue();
                if (surfaceView == null)
                {
                    surfaceView = new SurfaceView(MainActivity.this);
                    ((CameraPainter) object).options.surfaceView.setValue(surfaceView);
                    addTab(surfaceView, "SurfaceView");
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
        mlam.dispatchResume();
        actualizeContent();
    }

    /**
     *
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        mlam.dispatchPause(isFinishing());
    }

    /**
     *
     */
    @Override
    protected void onDestroy()
    {
        Log.removeLogListener(logListener);
        pipeView.removeViewListener(viewListener);
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
        setContentView(R.layout.activity_main);
        init(savedInstanceState);
    }
}
