/*
 * OptionsActivity.java
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
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.util.OptionTable;
import hcm.ssj.creator.util.ProviderTable;

public class OptionsActivity extends AppCompatActivity
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
        Option[] options;
        if (innerObject == null)
        {
            //change title
            setTitle("SSJ_Framework");
            options = PipelineBuilder.getOptionList(Pipeline.getInstance());
        } else
        {
            //change title
            setTitle(((hcm.ssj.core.Component) innerObject).getComponentName());
            options = PipelineBuilder.getOptionList(innerObject);
        }
        //stretch columns
        TableLayout tableLayout = (TableLayout) findViewById(R.id.id_tableLayout);
        tableLayout.setStretchAllColumns(true);
        //add frame size and delta for transformer and consumer
        if (innerObject != null
                && (innerObject instanceof Transformer
                || innerObject instanceof Consumer))
        {
            //add frame size and delta
            tableLayout.addView(createTextView(true));
            tableLayout.addView(createTextView(false));
        }
        //add options
        if (options != null && options.length > 0)
        {
            tableLayout.addView(OptionTable.createTable(this, options,
                    innerObject != null
                            && (innerObject instanceof Transformer
                            || innerObject instanceof Consumer)));
        }
        //add possible providers for sensor, transformer or consumer
        if (innerObject != null
                && (innerObject instanceof Sensor
                || innerObject instanceof Transformer
                || innerObject instanceof Consumer))
        {
            //add possible providers
            TableRow tableRow = ProviderTable.createTable(this, innerObject,
                    (innerObject instanceof Transformer || innerObject instanceof Consumer)
                            || (innerObject instanceof Sensor && options != null && options.length > 0));
            if (tableRow != null)
            {
                tableLayout.addView(tableRow);
            }
        }
    }

    /**
     * @param isFrameSize boolean
     * @return TableRow
     */
    private TableRow createTextView(final boolean isFrameSize)
    {
        TableRow tableRow = new TableRow(this);
        tableRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setWeightSum(1.0f);
        //
        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setEms(10);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f));
        editText.setText(String.valueOf(isFrameSize
                ? PipelineBuilder.getInstance().getFrameSize(innerObject)
                : PipelineBuilder.getInstance().getDelta(innerObject)));
        editText.addTextChangedListener(new TextWatcher()
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
                if (isFrameSize)
                {
                    try
                    {
                        double d = Double.parseDouble(s.toString());
                        if (d > 0)
                        {
                            PipelineBuilder.getInstance().setFrameSize(innerObject, d);
                        }
                    } catch (NumberFormatException ex)
                    {
                        Log.w("Invalid input for frameSize double: " + s.toString());
                    }
                } else
                {
                    try
                    {
                        double d = Double.parseDouble(s.toString());
                        if (d >= 0)
                        {
                            PipelineBuilder.getInstance().setDelta(innerObject, d);
                        }
                    } catch (NumberFormatException ex)
                    {
                        Log.w("Invalid input for delta double: " + s.toString());
                    }
                }
            }
        });
        linearLayout.addView(editText);
        //
        TextView textView = new TextView(this);
        textView.setText(isFrameSize ? R.string.str_frameSize : R.string.str_delta);
        textView.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f));
        linearLayout.addView(textView);
        tableRow.addView(linearLayout);
        return tableRow;
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
