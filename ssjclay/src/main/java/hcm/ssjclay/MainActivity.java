package hcm.ssjclay;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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
                addDialog.setOption(Builder.sensors);
                break;
            }
            case R.id.action_providers:
            {
                addDialog.setTitleMessage(R.string.str_providers);
                addDialog.setOption(Builder.sensorProviders);
                break;
            }
            case R.id.action_transformers:
            {
                addDialog.setTitleMessage(R.string.str_transformers);
                addDialog.setOption(Builder.transformers);
                break;
            }
            case R.id.action_consumers:
            {
                addDialog.setTitleMessage(R.string.str_consumers);
                addDialog.setOption(Builder.consumers);
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
        final PipeView pipeView = (PipeView) this.findViewById(R.id.id_pipe_view);
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
        actualizeContent();
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
    }
}
