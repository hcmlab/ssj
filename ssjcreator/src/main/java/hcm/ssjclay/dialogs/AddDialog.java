/*
 * AddDialog.java
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

package hcm.ssjclay.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import hcm.ssjclay.R;
import hcm.ssjclay.creator.Builder;
import hcm.ssjclay.creator.Linker;

/**
 * A Dialog to confirm actions.<br>
 * Created by Frank Gaibler on 16.09.2015.
 */
public class AddDialog extends DialogFragment
{
    private int titleMessage = R.string.app_name;
    private ArrayList<Class> clazzes = null;
    private ArrayList<Listener> alListeners = new ArrayList<>();
    private ListView listView;

    /**
     * @param savedInstanceState Bundle
     * @return Dialog
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (clazzes == null)
        {
            throw new RuntimeException();
        }
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleMessage);
        builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        SparseBooleanArray checked = listView.getCheckedItemPositions();
                        int selected = 0;
                        for (int i = 0; i < listView.getAdapter().getCount(); i++)
                        {
                            if (checked.get(i))
                            {
                                selected++;
                            }
                        }
                        if (clazzes != null && selected > 0)
                        {
                            for (int i = 0; i < listView.getAdapter().getCount(); i++)
                            {
                                if (checked.get(i))
                                {
                                    Linker.getInstance().add(Builder.instantiate(clazzes.get(i)));
                                }
                            }
                            for (Listener listener : alListeners)
                            {
                                listener.onPositiveEvent(null);
                            }
                        } else
                        {
                            for (Listener listener : alListeners)
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
        // Set up the input
        listView = new ListView(getContext());
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        if (clazzes != null && clazzes.size() > 0)
        {
            String[] ids = new String[clazzes.size()];
            for (int i = 0; i < ids.length; i++)
            {
                ids[i] = clazzes.get(i).getSimpleName();
            }
            listView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice, ids));
        } else
        {
            listView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice));
        }
        builder.setView(listView);
        // Create the AlertDialog object and return it
        return builder.create();
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
        this.clazzes = clazzes;
        Collections.sort(this.clazzes, new Comparator<Class>()
        {
            @Override
            public int compare(Class lhs, Class rhs)
            {
                return lhs.getSimpleName().compareTo(rhs.getSimpleName());
            }
        });
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
