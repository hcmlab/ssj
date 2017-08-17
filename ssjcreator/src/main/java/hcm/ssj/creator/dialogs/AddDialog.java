/*
 * AddDialog.java
 * Copyright (c) 2017
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

package hcm.ssj.creator.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import hcm.ssj.creator.R;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.core.SSJDescriptor;

/**
 * A Dialog to confirm actions.<br>
 * Created by Frank Gaibler on 16.09.2015.
 */
public class AddDialog extends DialogFragment
{
    private int titleMessage = R.string.app_name;
    private LinkedHashMap<String, ArrayList<Class>> hashMap = null;
    private ArrayList<Listener> alListeners = new ArrayList<>();
    private ExpandableListView listView;
    //ExpandableListView doesn't track selected items correctly, so it is done manually
    private boolean[][] itemState = null;
    private int allItems = 0;

    /**
     * @param savedInstanceState Bundle
     * @return Dialog
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (hashMap == null)
        {
            throw new RuntimeException();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleMessage);
        builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        boolean written = false;
                        if (hashMap != null)
                        {
                            int x = 0;
                            for (Map.Entry<String, ArrayList<Class>> entry : hashMap.entrySet())
                            {
                                int y = 0;
                                ArrayList<Class> arrayList = entry.getValue();
                                for (Class clazz : arrayList)
                                {
                                    if (itemState[x][y])
                                    {
                                        written = true;
                                        PipelineBuilder.getInstance().add(SSJDescriptor.instantiate(clazz));
                                    }
                                    y++;
                                }
                                x++;
                            }
                        }
                        for (Listener listener : alListeners)
                        {
                            if (written)
                            {
                                listener.onPositiveEvent(null);
                            } else
                            {
                                listener.onNegativeEvent(null);
                            }
                        }
                    }
                }
        );
        builder.setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        for (Listener listener : alListeners)
                        {
                            listener.onNegativeEvent(null);
                        }
                    }
                }
        );
        //set up input
        listView = new ExpandableListView(getContext());
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        setListListeners();

        if (hashMap != null && hashMap.size() > 0)
        {
            ListAdapter listAdapter = new ListAdapter(getContext(), hashMap);
            listView.setAdapter(listAdapter);
        }

        builder.setView(listView);
        return builder.create();
    }

    /**
     * ExpandableListView doesn't track selected items correctly, so it is done manually
     */
    private void setListListeners()
    {
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener()
        {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
            {
                int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
                parent.setItemChecked(index, !parent.isItemChecked(index));
                itemState[groupPosition][childPosition] = !itemState[groupPosition][childPosition];
                return true;
            }
        });
        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener()
        {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id)
            {
                //get current position
                int currentPosition = 1;
                for (int i = 0; i < groupPosition; i++, currentPosition++)
                {
                    if (listView.isGroupExpanded(i))
                    {
                        currentPosition += listView.getExpandableListAdapter().getChildrenCount(i);
                    }
                }
                //shift values as needed
                int children = listView.getExpandableListAdapter().getChildrenCount(groupPosition);
                if (listView.isGroupExpanded(groupPosition))
                {
                    //currently closing
                    for (int i = currentPosition; i < allItems; i++)
                    {
                        listView.setItemChecked(i, listView.isItemChecked(i + children));
                    }
                } else
                {
                    //currently expanding
                    for (int i = allItems + 1; i > currentPosition; i--)
                    {
                        listView.setItemChecked(i, listView.isItemChecked(i - children));
                    }
                    //set values for expanded group from memory
                    for (int i = currentPosition, j = 0; j < children; i++, j++)
                    {
                        listView.setItemChecked(i, itemState[groupPosition][j]);
                    }
                }
                return false;
            }
        });
    }

    /**
     * @param title int
     */
    public void setTitleMessage(int title)
    {
        this.titleMessage = title;
    }

    /**
     * @param clazzes Class[]
     */
    public void setOption(ArrayList<Class> clazzes)
    {
        hashMap = new LinkedHashMap<>();
        //get each package once
        HashSet<Package> hashSet = new HashSet<>();
        for (Class clazz : clazzes)
        {
            hashSet.add(clazz.getPackage());
        }
        //only show last part of the package name
        String[] packages = new String[hashSet.size()];
        int count = 0;
        for (Package pack : hashSet)
        {
            String name = pack.getName();
            packages[count++] = name.substring(name.lastIndexOf(".") + 1);
        }
        //sort packages by name and add them to map
        Arrays.sort(packages);
        for (String name : packages)
        {
            hashMap.put(name, new ArrayList<Class>());
        }
        //sort classes by name
        Collections.sort(clazzes, new Comparator<Class>()
        {
            @Override
            public int compare(Class lhs, Class rhs)
            {
                return lhs.getSimpleName().compareTo(rhs.getSimpleName());
            }
        });
        //add every class to its corresponding package
        for (Class clazz : clazzes)
        {
            String name = clazz.getPackage().getName();
            hashMap.get(name.substring(name.lastIndexOf(".") + 1)).add(clazz);
        }
        //create internal variables to save the state of the list
        itemState = new boolean[hashMap.size()][];
        count = 0;
        allItems = 0;
        allItems += hashMap.size();
        for (Map.Entry<String, ArrayList<Class>> entry : hashMap.entrySet())
        {
            itemState[count++] = new boolean[entry.getValue().size()];
            allItems += entry.getValue().size();
        }
    }

    /**
     * @param listener Listener
     */
    public void addListener(Listener listener)
    {
        alListeners.add(listener);
    }

    /**
     * @param listener Listener
     */
    public void removeListener(Listener listener)
    {
        alListeners.remove(listener);
    }
}
