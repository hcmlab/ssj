/*
 * ComponentOptionsActivity.java
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

package hcm.ssj.creator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TableLayout;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.creator.core.Pipeline;
import hcm.ssj.creator.util.OptionTable;
import hcm.ssj.creator.util.ProviderTable;

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
            Option[] options = Pipeline.getOptionList(innerObject);
            //stretch columns
            TableLayout tableLayout = (TableLayout) findViewById(R.id.id_tableLayout);
            tableLayout.setStretchAllColumns(true);
            //fill frameSize and delta
            EditText editTextFrameSize = (EditText) findViewById(R.id.id_editText);
            editTextFrameSize.setText(String.valueOf(Pipeline.getInstance().getFrameSize(innerObject)));
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
                            Pipeline.getInstance().setFrameSize(innerObject, d);
                        }
                    } catch (NumberFormatException ex)
                    {
                        Log.w("Invalid input for frameSize double: " + s.toString());
                    }
                }
            });
            EditText editTextDelta = (EditText) findViewById(R.id.id_editText2);
            editTextDelta.setText(String.valueOf(Pipeline.getInstance().getDelta(innerObject)));
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
                            Pipeline.getInstance().setDelta(innerObject, d);
                        }
                    } catch (NumberFormatException ex)
                    {
                        Log.w("Invalid input for delta double: " + s.toString());
                    }
                }
            });
            //add options
            if (options != null && options.length > 0)
            {
                tableLayout.addView(OptionTable.createTable(this, options, true));
            }
            //add possible providers
            tableLayout.addView(ProviderTable.createTable(this, innerObject, true));
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
