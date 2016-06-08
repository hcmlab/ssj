package hcm.ssjclay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssjclay.creator.Linker;

public class ComponentOptionsActivity extends AppCompatActivity
{
    public static Object object;
    private Object innerObject;

    /**
     *
     */
    private void init()
    {
        if (object == null)
        {
            Log.e("Set an object before calling this activity");
        } else
        {
            innerObject = object;
            object = null;
            //change title
            setTitle(((hcm.ssj.core.Component) innerObject).getComponentName());
            Option[] options = Linker.getOptionList(innerObject);
            //stretch columns
            TableLayout tableLayout = (TableLayout) findViewById(R.id.id_tableLayout);
            tableLayout.setStretchAllColumns(true);
            //fill frameSize and delta
            EditText editTextFrameSize = (EditText) findViewById(R.id.id_editText);
            editTextFrameSize.setText(String.valueOf(Linker.getInstance().getFrameSize(innerObject)));
            editTextFrameSize.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after)
                {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count)
                {
                }

                @Override
                public void afterTextChanged(Editable s)
                {
                    try
                    {
                        double d = Double.parseDouble(s.toString());
                        if (d > 0)
                        {
                            Linker.getInstance().setFrameSize(innerObject, d);
                        }
                    } catch (NumberFormatException ex)
                    {
                        Log.w("Invalid input for frameSize double: " + s.toString());
                    }
                }
            });
            EditText editTextDelta = (EditText) findViewById(R.id.id_editText2);
            editTextDelta.setText(String.valueOf(Linker.getInstance().getDelta(innerObject)));
            editTextDelta.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after)
                {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count)
                {
                }

                @Override
                public void afterTextChanged(Editable s)
                {
                    try
                    {
                        double d = Double.parseDouble(s.toString());
                        if (d >= 0)
                        {
                            Linker.getInstance().setDelta(innerObject, d);
                        }
                    } catch (NumberFormatException ex)
                    {
                        Log.w("Invalid input for delta double: " + s.toString());
                    }
                }
            });
            //add possible providers
            tableLayout.addView(ProviderTable.createTable(this, ListView.CHOICE_MODE_MULTIPLE, innerObject, true, true));
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
        setContentView(R.layout.activity_component_options);
        init();
    }
}
