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
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import hcm.ssj.core.Log;
import hcm.ssjclay.R;
import hcm.ssjclay.creator.SaveLoad;

/**
 * Dialog to save and choose files.<br>
 * Created by Frank Gaibler on 04.07.2016.
 */
public class FileDialog extends DialogFragment
{
    private final static String DIR_1 = "SSJ", DIR_2 = "Creator", SUFFIX = ".xml",
            DEMO = "demo", DEMO_SUFFIX = ".demo";

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleMessage);
        builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        switch (type)
                        {
                            case SAVE:
                            {
                                String fileName = editText.getText().toString().trim();
                                File dir1 = new File(Environment.getExternalStorageDirectory(), DIR_1);
                                File dir2 = new File(dir1.getPath(), DIR_2);
                                if (!fileName.isEmpty() && (dir2.exists() || dir2.mkdirs()))
                                {
                                    if (!fileName.endsWith(SUFFIX))
                                    {
                                        fileName += SUFFIX;
                                    }
                                    File file = new File(dir2, fileName);
                                    if (isValidFileName(file) && SaveLoad.save(file))
                                    {
                                        for (Listener listener : alListeners)
                                        {
                                            listener.onPositiveEvent(null);
                                        }
                                        return;
                                    }
                                }
                                for (Listener listener : alListeners)
                                {
                                    listener.onNegativeEvent(new Boolean[]{false});
                                }
                                return;
                            }
                            case LOAD:
                            case DELETE:
                            {
                                if (xmlFiles != null && xmlFiles.length > 0)
                                {
                                    int pos = listView.getCheckedItemPosition();
                                    if (pos > AbsListView.INVALID_POSITION)
                                    {
                                        if (type == Type.DELETE && xmlFiles[pos].delete())
                                        {
                                            for (Listener listener : alListeners)
                                            {
                                                listener.onPositiveEvent(null);
                                            }
                                            return;
                                        } else if (type == Type.LOAD)
                                        {
                                            //use different load with demo files
                                            if (xmlFiles[pos].getName().endsWith(DEMO_SUFFIX))
                                            {
                                                try
                                                {
                                                    String path = DEMO + "/" + xmlFiles[pos].getName();
                                                    FileInputStream fileInputStream = getContext().getAssets().openFd(path).createInputStream();
                                                    if (SaveLoad.load(fileInputStream))
                                                    {
                                                        for (Listener listener : alListeners)
                                                        {
                                                            listener.onPositiveEvent(null);
                                                        }
                                                        return;
                                                    }
                                                } catch (IOException ex)
                                                {
                                                    ex.printStackTrace();
                                                    for (Listener listener : alListeners)
                                                    {
                                                        listener.onNegativeEvent(new Boolean[]{false});
                                                    }
                                                    return;
                                                }
                                            } else
                                            {
                                                if (SaveLoad.load(xmlFiles[pos]))
                                                {
                                                    for (Listener listener : alListeners)
                                                    {
                                                        listener.onPositiveEvent(null);
                                                    }
                                                    return;
                                                }
                                            }
                                        }
                                        for (Listener listener : alListeners)
                                        {
                                            listener.onNegativeEvent(new Boolean[]{false});
                                        }
                                        return;
                                    }
                                }
                                for (Listener listener : alListeners)
                                {
                                    listener.onNegativeEvent(null);
                                }
                                break;
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
        //set up the input
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
                if (type == Type.LOAD)
                {
                    //add demo files
                    try
                    {
                        String[] demoFileNames = getContext().getAssets().list(DEMO);
                        File[] demoFiles = new File[demoFileNames.length];
                        for (int i = 0; i < demoFiles.length; i++)
                        {
                            demoFiles[i] = new File(demoFileNames[i]);
                        }
                        xmlFiles = xmlFiles == null ? demoFiles : concat(xmlFiles, demoFiles);
                    } catch (IOException ex)
                    {
                        ex.printStackTrace();
                    }
                }
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
                break;
            }
        }
        return builder.create();
    }

    /**
     * @param a File[]
     * @param b File[]
     * @return File[]
     */
    private File[] concat(File[] a, File[] b)
    {
        int aLen = a.length;
        int bLen = b.length;
        File[] c = new File[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
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
     * @param file File
     * @return boolean
     */
    private boolean isValidFileName(File file)
    {
        try
        {
            file.createNewFile();
        } catch (IOException ex)
        {
            Log.e("could not create file");
            return false;
        }
        return true;
    }
}
