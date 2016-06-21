package hcm.ssjclay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.TableLayout;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssjclay.creator.Linker;

public class SensorOptionsActivity extends AppCompatActivity
{
    public static Object object;
    private Object innerObject;

    /**
     *
     */
    private void init()
    {
        innerObject = object;
        object = null;
        if (innerObject == null)
        {
            Log.e("Set an object before calling this activity");
        } else
        {
            //change title
            setTitle(((hcm.ssj.core.Component) innerObject).getComponentName());
            //
            Option[] options = Linker.getOptionList(innerObject);
            //stretch columns
            TableLayout tableLayout = (TableLayout) findViewById(R.id.id_tableLayout);
            tableLayout.setStretchAllColumns(true);
            //add possible providers
            tableLayout.addView(ProviderTable.createTable(this, ListView.CHOICE_MODE_SINGLE, innerObject, false, true));
            //add options
            if (options != null && options.length > 0)
            {
                tableLayout.addView(OptionTable.createTable(this, options));
            }
        }
    }

    /**
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_options);
        init();
    }
}
