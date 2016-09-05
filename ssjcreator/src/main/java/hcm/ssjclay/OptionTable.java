/*
 * OptionTable.java
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

import android.app.Activity;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hcm.ssj.core.option.Option;

/**
 * Create a table row which includes every option
 * Created by Frank Gaibler on 18.05.2016.
 */
public class OptionTable
{
    /**
     * @param activity Activity
     * @param options  Option[]
     * @return TableRow
     */
    public static TableRow createTable(Activity activity, Option[] options)
    {
        TableRow tableRow = new TableRow(activity);
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        //options
        ScrollView scrollView = new ScrollView(activity);
        LinearLayout linearLayoutOptions = new LinearLayout(activity);
        linearLayoutOptions.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0, j = 0; i < options.length; i++)
        {
            if (j > 0)
            {
                //add divider
                View view = new View(activity);
                view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 2, 1f));
                view.setBackgroundColor(Color.CYAN);
                linearLayoutOptions.addView(view);
            }
            if (options[i].isAssignableByString())
            {
                linearLayoutOptions.addView(addOption(activity, options[i]));
                j++;
            }
        }
        scrollView.addView(linearLayoutOptions);
        tableRow.addView(scrollView);
        return tableRow;
    }

    /**
     * @param activity Activity
     * @param option   Option
     * @return LinearLayout
     */
    private static LinearLayout addOption(final Activity activity, final Option option)
    {
        Object value = option.get();
        // Set up the view
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        //description of the object
        LinearLayout linearLayoutDescription = new LinearLayout(activity);
        linearLayoutDescription.setOrientation(LinearLayout.HORIZONTAL);
        //name
        TextView textViewName = new TextView(activity);
        textViewName.setText(option.getName());
        textViewName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        linearLayoutDescription.addView(textViewName);
        //help
        final String helpText = option.getHelp();
        if (!helpText.isEmpty())
        {
            TextView textViewHelp = new TextView(activity);
            textViewHelp.setText(helpText);
            textViewHelp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            textViewHelp.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            linearLayoutDescription.addView(textViewHelp);
        }
        linearLayout.addView(linearLayoutDescription);
        //edit field
        View inputView;
        if (option.getType() == Boolean.class)
        {
            //checkbox for boolean values
            inputView = new CheckBox(activity);
            ((CheckBox) inputView).setChecked((Boolean) value);
            ((CheckBox) inputView).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    option.set(isChecked);
                }
            });
        } else if (value != null && value.getClass().isEnum())
        {
            //create spinner selection for enums which are not null
            inputView = new Spinner(activity);
            Object[] enums = value.getClass().getEnumConstants();
            ((Spinner) inputView).setAdapter(new ArrayAdapter<>(
                    activity, android.R.layout.simple_spinner_item, enums));
            //preselect item
            for (int i = 0; i < enums.length; i++)
            {
                if (enums[i].equals(value))
                {
                    ((Spinner) inputView).setSelection(i);
                    break;
                }
            }
            ((Spinner) inputView).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    option.set(parent.getItemAtPosition(position));
                }

                @Override
                public void onNothingSelected(AdapterView parent)
                {
                }
            });
        } else
        {
            //normal text view for everything else
            inputView = new EditText(activity);
            ((EditText) inputView).setTextColor(Color.BLACK);
            //specify the expected input type
            Class<?> type = option.getType();
            if (type == Byte.class || type == Short.class || type == Integer.class || type == Long.class)
            {
                ((TextView) inputView).setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                ((TextView) inputView).setText(value != null ? value.toString() : "", TextView.BufferType.NORMAL);
            } else if (type == Float.class || type == Double.class)
            {
                ((TextView) inputView).setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                ((TextView) inputView).setText(value != null ? value.toString() : "", TextView.BufferType.NORMAL);
            } else if (value != null && value.getClass().isArray())
            {
                Object[] objects;
                Class ofArray = value.getClass().getComponentType();
                if (ofArray.isPrimitive())
                {
                    List ar = new ArrayList();
                    int length = Array.getLength(value);
                    for (int i = 0; i < length; i++)
                    {
                        ar.add(Array.get(value, i));
                    }
                    objects = ar.toArray();
                    ((TextView) inputView).setInputType(InputType.TYPE_CLASS_TEXT);
                    ((TextView) inputView).setText(Arrays.toString(objects), TextView.BufferType.NORMAL);
                } else if (String.class.isAssignableFrom(ofArray))
                {
                    objects = (Object[]) value;
                    ((TextView) inputView).setInputType(InputType.TYPE_CLASS_TEXT);
                    ((TextView) inputView).setText(Arrays.toString(objects), TextView.BufferType.NORMAL);
                } else
                {
                    ((TextView) inputView).setInputType(InputType.TYPE_CLASS_TEXT);
                    ((TextView) inputView).setText(value.toString(), TextView.BufferType.NORMAL);
                }
            } else
            {
                ((TextView) inputView).setInputType(InputType.TYPE_CLASS_TEXT);
                ((TextView) inputView).setText(value != null ? value.toString() : "", TextView.BufferType.NORMAL);
            }
            ((EditText) inputView).addTextChangedListener(new TextWatcher()
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
                    option.setValue(s.toString());
                }
            });
        }
        linearLayout.addView(inputView);
        return linearLayout;
    }
}
