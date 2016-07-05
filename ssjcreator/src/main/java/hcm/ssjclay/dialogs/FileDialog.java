/*
 * FileDialog.java
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
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import hcm.ssjclay.R;
import hcm.ssjclay.creator.SaveLoad;

/**
 * Dialog to save and choose files.<br>
 * Created by Frank Gaibler on 04.07.2016.
 */
public class FileDialog extends DialogFragment
{
    private final static int MAX_LENGTH = 50;
    private final static String DIR_1 = "SSJ", DIR_2 = "Creator", SUFFIX = ".xml";

    public enum Type
    {
        SAVE, LOAD, DELETE
    }

    private Type type = Type.SAVE;
    private int titleMessage = R.string.app_name;
    private ArrayList<Listener> alListeners = new ArrayList<>();
    private ListView listView;
    private EditText editText;
    private File[] xmlFiles = null;

    /**
     * @param savedInstanceState Bundle
     * @return Dialog
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleMessage);
        builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        if (type == Type.SAVE)
                        {
                            String fileName = editText.getText().toString().trim();
                            File dir1 = new File(Environment.getExternalStorageDirectory(), DIR_1);
                            File dir2 = new File(dir1.getPath(), DIR_2);
                            if (dir2.exists() || dir2.mkdirs())
                            {
                                if (!fileName.endsWith(SUFFIX))
                                {
                                    fileName += SUFFIX;
                                }
                                if (isValidFileName(fileName) && SaveLoad.save(new File(dir2, fileName)))
                                {
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
                        } else
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
                            if (xmlFiles != null && selected > 0)
                            {
                                for (int i = 0; i < listView.getAdapter().getCount(); i++)
                                {
                                    if (checked.get(i))
                                    {
                                        if (type == Type.LOAD)
                                        {
                                            if (SaveLoad.load(xmlFiles[i]))
                                            {
                                                for (Listener listener : alListeners)
                                                {
                                                    listener.onPositiveEvent(null);
                                                }
                                                break;
                                            }
                                        } else if (type == Type.DELETE)
                                        {
                                            if (xmlFiles[i].delete())
                                            {
                                                for (Listener listener : alListeners)
                                                {
                                                    listener.onPositiveEvent(null);
                                                }
                                                break;
                                            }
                                        }
                                        for (Listener listener : alListeners)
                                        {
                                            listener.onNegativeEvent(null);
                                        }
                                        break;
                                    }
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
        switch (type)
        {
            case SAVE:
            {
                editText = new EditText(getContext());
                editText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(editText);
                break;
            }
            case LOAD:
            case DELETE:
            {
                listView = new ListView(getContext());
                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                File dir1 = new File(Environment.getExternalStorageDirectory(), DIR_1);
                File dir2 = new File(dir1.getPath(), DIR_2);
                xmlFiles = dir2.listFiles(new FilenameFilter()
                {
                    @Override
                    public boolean accept(File folder, String name)
                    {
                        return name.toLowerCase().endsWith(SUFFIX);
                    }
                });
                if (xmlFiles != null && xmlFiles.length > 0)
                {
                    String[] ids = new String[xmlFiles.length];
                    for (int i = 0; i < ids.length; i++)
                    {
                        ids[i] = xmlFiles[i].getName();
                    }
                    listView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_single_choice, ids));
                } else
                {
                    listView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_single_choice));
                }
                builder.setView(listView);
            }
        }
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
     * @param type Type
     */
    public void setType(Type type)
    {
        this.type = type;
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

    /**
     * @param file String
     * @return boolean
     */
    private boolean isValidFileName(String file)
    {
        return file.matches("^.*[^a-zA-Z0-9._-].*$") && file.length() <= MAX_LENGTH;
    }
}
