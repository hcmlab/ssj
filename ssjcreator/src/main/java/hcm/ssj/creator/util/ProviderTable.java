/*
 * ProviderTable.java
 * Copyright (c) 2018
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
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

import hcm.ssj.core.Component;
import hcm.ssj.core.Provider;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.ml.IModelHandler;

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
        final Object[] objects = PipelineBuilder.getInstance().getPossibleStreamConnections(mainObject);
        //
        if (objects.length > 0) {
            for (int i = 0; i < objects.length; i++) {
                CheckBox checkBox = new CheckBox(activity);
                checkBox.setText(objects[i].getClass().getSimpleName());
                checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                Object[] providers = PipelineBuilder.getInstance().getStreamConnections(mainObject);
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
                            PipelineBuilder.getInstance().addStreamConnection(mainObject, (Provider) o);
                        } else {
                            PipelineBuilder.getInstance().removeStreamConnection(mainObject, (Provider) o);
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
    public static TableRow createEventTable(Activity activity, final Object mainObject, boolean dividerTop)
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
        final Object[] objects = PipelineBuilder.getInstance().getPossibleEventConnections(mainObject);
        //
        if (objects.length > 0) {
            for (int i = 0; i < objects.length; i++) {

                if(PipelineBuilder.getInstance().isManagedFeedback(objects[i]))
                    continue;

                CheckBox checkBox = new CheckBox(activity);
                checkBox.setText(objects[i].getClass().getSimpleName());
                checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                Object[] providers = PipelineBuilder.getInstance().getEventConnections(mainObject);
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
                            PipelineBuilder.getInstance().addEventConnection(mainObject, (Component) o);
                        } else {
                            PipelineBuilder.getInstance().removeEventConnection(mainObject, (Component) o);
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
    public static TableRow createModelTable(Activity activity, final Object mainObject, boolean dividerTop, int heading)
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
        final Object[] objects = (mainObject instanceof IModelHandler) ?
                PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.Model) :
                PipelineBuilder.getInstance().getModelHandlers();

        if (objects.length > 0) {
            for (int i = 0; i < objects.length; i++) {
                CheckBox checkBox = new CheckBox(activity);
                checkBox.setText(objects[i].getClass().getSimpleName());
                checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);

                Object[] connections = PipelineBuilder.getInstance().getModelConnections(mainObject);
                if (connections != null) {
                    for (Object conn : connections) {
                        if (objects[i].equals(conn)) {
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
                            PipelineBuilder.getInstance().addModelConnection((Component) mainObject, (Component) o);
                        } else {
                            PipelineBuilder.getInstance().removeModelConnection((Component) mainObject, (Component) o);
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
}
