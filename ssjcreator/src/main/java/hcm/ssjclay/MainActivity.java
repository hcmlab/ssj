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
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import hcm.ssj.core.Monitor;
import hcm.ssj.core.TheFramework;
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
    //
    LocalActivityManager mlam = null;
    private TabHost tabHost = null;
    private PipeView pipeView = null;
    //test
    private static int timer = 0;
    private Handler handlerTest;
    Runnable threadTest = new Runnable()
    {
        /**
         *
         */
        @Override
        public void run()
        {
            try
            {

                System.out.println("Test" + (++timer));
            } finally
            {
                if (timer < 200)
                {
                    handlerTest.postDelayed(threadTest, 1000);
                }
            }
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
        if (tabHost != null)
        {
            tabHost.setup(mlam);
            //pipe
            pipeView = new PipeView(MainActivity.this);
            pipeView.setWillNotDraw(false);
            addTab(pipeView, getResources().getString(R.string.str_pipe));
            //console
            addTabConsole();
        }
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
        final TextView textView = new TextView(MainActivity.this);
        System.setOut(new PrintStream(new OutputStream()
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            @Override
            public void write(int oneByte) throws IOException
            {
                outputStream.write(oneByte);
                textView.setText(new String(outputStream.toByteArray()));
            }
        }));
        scrollView.addView(textView);
        //
        addTab(scrollView, getResources().getString(R.string.str_console));
        //adjust size
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
        //testing the System.out stream
        handlerTest = new Handler(Looper.getMainLooper());
        handlerTest.postDelayed(threadTest, 1000);
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
     *
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
