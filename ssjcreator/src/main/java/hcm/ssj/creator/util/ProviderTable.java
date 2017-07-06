/*
 * ProviderTable.java
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

package hcm.ssj.creator.util;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import hcm.ssj.core.Component;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.PipelineBuilder;

/**
 * Create a table row which includes viable providers
 * Created by Frank Gaibler on 19.05.2016.
 */
public class ProviderTable
{
    /**
     * @param activity   Activity
     * @param mainObject Object
     * @param dividerTop boolean
     * @return TableRow
     */
    public static TableRow createStreamTable(Activity activity, final Object mainObject, boolean dividerTop, int heading)
    {
        TableRow tableRow = new TableRow(activity);
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (dividerTop)
        {
            //add divider
            linearLayout.addView(Util.addDivider(activity));
        }
        TextView textViewName = new TextView(activity);
        textViewName.setText(heading);
        textViewName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        textViewName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        linearLayout.addView(textViewName);
        //get possible providers
        final Object[] objects = getStreamProvider(mainObject);
        //
        if (objects.length > 0) {
            for (int i = 0; i < objects.length; i++) {
                CheckBox checkBox = new CheckBox(activity);
                checkBox.setText(objects[i].getClass().getSimpleName());
                checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                Object[] providers = PipelineBuilder.getInstance().getStreamProviders(mainObject);
                if (providers != null) {
                    for (Object provider : providers) {
                        if (objects[i].equals(provider)) {
                            checkBox.setChecked(true);
                            break;
                        }
                    }
                }
                final int count = i;
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    final Object o = objects[count];

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            PipelineBuilder.getInstance().addStreamProvider(mainObject, (Provider) o);
                        } else {
                            PipelineBuilder.getInstance().removeStreamProvider(mainObject, (Provider) o);
                        }
                    }
                });
                linearLayout.addView(checkBox);
            }
        } else
        {
            return null;
        }
        tableRow.addView(linearLayout);
        return tableRow;
    }

    /**
     * @param activity   Activity
     * @param mainObject Object
     * @param dividerTop boolean
     * @return TableRow
     */
    public static TableRow createEventTable(Activity activity, final Object mainObject, boolean dividerTop, int heading)
    {
        TableRow tableRow = new TableRow(activity);
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (dividerTop)
        {
            //add divider
            linearLayout.addView(Util.addDivider(activity));
        }
        TextView textViewName = new TextView(activity);
        textViewName.setText(R.string.str_event_input);
        textViewName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        textViewName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        linearLayout.addView(textViewName);
        //get possible providers
        final Object[] objects = getEventProvider(mainObject);
        //
        if (objects.length > 0) {
            for (int i = 0; i < objects.length; i++) {
                CheckBox checkBox = new CheckBox(activity);
                checkBox.setText(objects[i].getClass().getSimpleName());
                checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                Object[] providers = PipelineBuilder.getInstance().getEventProviders(mainObject);
                if (providers != null) {
                    for (Object provider : providers) {
                        if (objects[i].equals(provider)) {
                            checkBox.setChecked(true);
                            break;
                        }
                    }
                }
                final int count = i;
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    final Object o = objects[count];

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            PipelineBuilder.getInstance().addEventProvider(mainObject, (Component) o);
                        } else {
                            PipelineBuilder.getInstance().removeEventProvider(mainObject, (Component) o);
                        }
                    }
                });
                linearLayout.addView(checkBox);
            }
        } else
        {
            return null;
        }
        tableRow.addView(linearLayout);
        return tableRow;
    }

    /**
     * @return Object[]
     */
    private static Object[] getEventProvider(Object mainObject)
    {
        //add possible providers
        ArrayList<Object> alCandidates = new ArrayList<>();
        alCandidates.addAll(Arrays.asList(PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.Sensor)));
        alCandidates.addAll(Arrays.asList(PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.SensorChannel)));
        alCandidates.addAll(Arrays.asList(PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.Transformer)));
        alCandidates.addAll(Arrays.asList(PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.Consumer)));
        alCandidates.addAll(Arrays.asList(PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.EventHandler)));

        alCandidates.remove(mainObject);

        return alCandidates.toArray();
    }

    /**
     * @return Object[]
     */
    private static Object[] getStreamProvider(Object mainObject)
    {
        //add possible providers
        Object[] sensProvCandidates = PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.SensorChannel);
        ArrayList<Object> alCandidates = new ArrayList<>();
        //only add sensorChannels for sensors
        if (!(mainObject instanceof Sensor))
        {
            alCandidates.addAll(Arrays.asList(PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.Transformer)));
            //remove itself
            for (int i = 0; i < alCandidates.size(); i++)
            {
                if (mainObject.equals(alCandidates.get(i)))
                {
                    alCandidates.remove(i);
                }
            }
        }
        alCandidates.addAll(0, Arrays.asList(sensProvCandidates));
        return alCandidates.toArray();
    }
}
