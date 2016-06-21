package hcm.ssjclay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TableLayout;

import hcm.ssj.core.TheFramework;
import hcm.ssj.core.option.Option;
import hcm.ssjclay.creator.Linker;

public class OptionsActivity extends AppCompatActivity
{
    public static Object object;

    /**
     *
     */
    private void init()
    {
        Option[] options;
        if (object != null)
        {
            //change title
            setTitle(((hcm.ssj.core.Component) object).getComponentName());
            options = Linker.getOptionList(object);
        } else
        {
            //change title
            setTitle("SSJ_Framework");
            options = Linker.getOptionList(TheFramework.getFramework());
        }
        object = null;
        //stretch columns
        TableLayout tableLayout = (TableLayout) findViewById(R.id.id_tableLayout);
        tableLayout.setStretchAllColumns(true);
        //add options
        if (options != null && options.length > 0)
        {
            tableLayout.addView(OptionTable.createTable(this, options));
        }
    }

    /**
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
        init();
    }
}
