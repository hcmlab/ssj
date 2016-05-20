/*
 * ClassList.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import hcm.ssj.core.Provider;
import hcm.ssjclay.creator.Linker;

/**
 * Create a table row which includes viable providers
 * Created by Frank Gaibler on 19.05.2016.
 */
public class ProviderTable
{
    /**
     * @param activity      Activity
     * @param choiceMode    int
     * @param mainObject    Object
     * @param dividerTop    boolean
     * @param dividerBottom boolean
     * @return TableRow
     */
    public static TableRow createTable(Activity activity, int choiceMode, final Object mainObject, boolean dividerTop, boolean dividerBottom)
    {
        TableRow tableRow = new TableRow(activity);
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (dividerTop)
        {
            //add divider
            linearLayout.addView(addDivider(activity));
        }
        TextView textViewName = new TextView(activity);
        textViewName.setText(R.string.str_providers);
        textViewName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        linearLayout.addView(textViewName);
        //get possible providers
        final Object[] objects = getProvider(mainObject);
        //
        final ListView listView = new ListView(activity);
        listView.setChoiceMode(choiceMode);
        int adapterResource = choiceMode == ListView.CHOICE_MODE_MULTIPLE ? android.R.layout.simple_list_item_multiple_choice : android.R.layout.simple_list_item_single_choice;
        ArrayAdapter arrayAdapter;
        if (objects.length > 0)
        {
            String[] ids = new String[objects.length];
            for (int i = 0; i < ids.length; i++)
            {
                ids[i] = objects[i].getClass().getSimpleName();
            }
            arrayAdapter = new ArrayAdapter<>(activity, adapterResource, ids);
            listView.setAdapter(arrayAdapter);
            //preselect used already added items
            Object[] providers = Linker.getInstance().getProviders(mainObject);
            if (providers != null)
            {
                for (Object provider : providers)
                {
                    for (int i = 0; i < objects.length; i++)
                    {
                        if (objects[i].equals(provider))
                        {
                            listView.setItemChecked(i, true);
                            break;
                        }
                    }
                }
            }
            //ensure correct size
            int maxCount = 3;
            if (arrayAdapter.getCount() > maxCount)
            {
                View item = arrayAdapter.getView(0, null, listView);
                item.measure(0, 0);
                ViewGroup.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, (int) ((maxCount + 0.75f) * item.getMeasuredHeight()));
                listView.setLayoutParams(params);
            }
            //handle click events
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    if (listView.isItemChecked(position))
                    {
                        Linker.getInstance().addProvider(mainObject, (Provider) objects[position]);
                    } else
                    {
                        Linker.getInstance().removeProvider(mainObject, (Provider) objects[position]);
                    }
                }
            });
        } else
        {
            arrayAdapter = new ArrayAdapter<>(activity, adapterResource);
            listView.setAdapter(arrayAdapter);
        }
        linearLayout.addView(listView);
        if (dividerBottom)
        {
            //add divider
            linearLayout.addView(addDivider(activity));
        }
        tableRow.addView(linearLayout);
        return tableRow;
    }

    /**
     * @param activity Activity
     * @return View
     */
    private static View addDivider(Activity activity)
    {
        View view = new View(activity);
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 6, 1f));
        view.setBackgroundColor(Color.CYAN);
        return view;
    }

    /**
     * @return Object[]
     */
    private static Object[] getProvider(Object mainObject)
    {
        //add possible providers
        Object[] sensProvCandidates = Linker.getInstance().getAll(Linker.Type.SensorProvider);
        ArrayList<Object> alCandidates = new ArrayList<>();
        alCandidates.addAll(Arrays.asList(Linker.getInstance().getAll(Linker.Type.Transformer)));
        //remove itself
        for (int i = 0; i < alCandidates.size(); i++)
        {
            if (mainObject.equals(alCandidates.get(i)))
            {
                alCandidates.remove(i);
            }
        }
        alCandidates.addAll(0, Arrays.asList(sensProvCandidates));
        return alCandidates.toArray();
    }
}
