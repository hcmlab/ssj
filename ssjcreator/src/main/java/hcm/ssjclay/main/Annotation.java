/*
 * Annotation.java
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

package hcm.ssjclay.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

import hcm.ssj.core.TheFramework;
import hcm.ssj.file.LoggingConstants;
import hcm.ssjclay.R;
import hcm.ssjclay.util.Util;

/**
 * Annotation tab for main activity.<br>
 * Created by Frank Gaibler on 23.09.2016.
 */
class Annotation implements ITab
{
    //tab
    private View view;
    private String title;
    private int icon;
    //annotation
    private String lastAnno = null;
    private FloatingActionButton floatingActionButton = null;
    private EditText editTextPathAnno = null;
    private EditText editTextNameAnno = null;
    private RadioGroup radioGroupAnno = null;
    private File fileAnno = null;
    private final static String SUFFIX = ".anno", START = "0.0";
    private boolean running = false;

    /**
     * @param context Context
     */
    Annotation(Context context)
    {
        //view
        view = createContent(context);
        //title
        title = context.getResources().getString(R.string.str_annotation);
        //icon
        icon = android.R.drawable.ic_menu_agenda;
    }

    /**
     * @param context Context
     */
    private View createContent(final Context context)
    {
        ScrollView scrollViewAnno = new ScrollView(context);
        //layouts
        CoordinatorLayout coordinatorLayout = new CoordinatorLayout(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        coordinatorLayout.addView(linearLayout);
        //add annotation
        floatingActionButton = new FloatingActionButton(context);
        floatingActionButton.setImageResource(R.drawable.ic_add_white_24dp);
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.END | Gravity.BOTTOM;
        floatingActionButton.setLayoutParams(params);
        floatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (radioGroupAnno != null)
                {
                    radioGroupAnno.addView(getRadioButton(context));
                }
            }
        });
        coordinatorLayout.addView(floatingActionButton);
        //file name
        TextView textViewDescriptionName = new TextView(context);
        textViewDescriptionName.setText(R.string.str_fileName);
        textViewDescriptionName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        linearLayout.addView(textViewDescriptionName);
        editTextNameAnno = new EditText(context);
        editTextNameAnno.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextNameAnno.setText("", TextView.BufferType.NORMAL);
        linearLayout.addView(editTextNameAnno);
        //file path
        TextView textViewDescriptionPath = new TextView(context);
        textViewDescriptionPath.setText(R.string.str_filePath);
        textViewDescriptionPath.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        linearLayout.addView(textViewDescriptionPath);
        editTextPathAnno = new EditText(context);
        editTextPathAnno.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextPathAnno.setText((Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + Util.DIR_1), TextView.BufferType.NORMAL);
        linearLayout.addView(editTextPathAnno);
        //annotations
        radioGroupAnno = new RadioGroup(context);
        radioGroupAnno.addView(getRadioButton(context));
        linearLayout.addView(radioGroupAnno);
        //
        scrollViewAnno.addView(coordinatorLayout);
        //
        radioGroupAnno.check(radioGroupAnno.getChildAt(0).getId());
        radioGroupAnno.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if (fileAnno != null)
                {
                    //only append to running pipeline
                    if (TheFramework.getFramework().isRunning())
                    {
                        Util.appendFile(fileAnno, " " + Util.getAnnotationTime() + " " + lastAnno + LoggingConstants.DELIMITER_LINE);
                        Util.appendFile(fileAnno, String.valueOf(Util.getAnnotationTime()));
                    }
                    lastAnno = ((RadioButton) group.getChildAt(checkedId - 1)).getText().toString();
                }
            }
        });
        return scrollViewAnno;
    }

    /**
     * @param context Context
     * @return RadioButton
     */
    private RadioButton getRadioButton(final Context context)
    {
        RadioButton radioButton = new RadioButton(context);
        radioButton.setText(R.string.str_default);
        //increase text size
        radioButton.setTextSize(radioButton.getTextSize() * 0.75f);
        radioButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(final View v)
            {
                //only allow edits while pipe is not active
                if (running)
                {
                    return false;
                } else
                {
                    //content
                    LinearLayout linearLayout = new LinearLayout(context);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    final EditText editText = new EditText(context);
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    editText.setText(((RadioButton) v).getText(), TextView.BufferType.NORMAL);
                    linearLayout.addView(editText);
                    final CheckBox checkBox = new CheckBox(context);
                    checkBox.setText(R.string.str_delete);
                    linearLayout.addView(checkBox);
                    //dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.str_annotation);
                    builder.setView(linearLayout);
                    builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            if (checkBox.isChecked())
                            {
                                ViewGroup viewGroup = (ViewGroup) v.getParent();
                                if (viewGroup != null)
                                {
                                    ((RadioButton) v).setChecked(false);
                                    viewGroup.removeView(v);
                                }
                                v.invalidate();
                            } else if (!editText.getText().toString().isEmpty())
                            {
                                ((RadioButton) v).setText(editText.getText());
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.str_cancel, null);
                    AlertDialog alert = builder.create();
                    alert.show();
                    return true;
                }
            }
        });
        return radioButton;
    }

    /**
     *
     */
    void startAnnotation()
    {
        enableComponents(false);
        running = true;
        String name = editTextNameAnno.getText().toString().trim();
        lastAnno = null;
        if (!name.isEmpty() && radioGroupAnno.getChildCount() > 0)
        {
            File parent = new File(editTextPathAnno.getText().toString());
            if (parent.exists() || parent.mkdirs())
            {
                fileAnno = new File(parent, name.endsWith(SUFFIX) ? name : name + SUFFIX);
                //delete existing annotation file
                if (fileAnno.exists())
                {
                    fileAnno.delete();
                }
                //get current annotation
                int selected = radioGroupAnno.getCheckedRadioButtonId();
                if (selected < 0)
                {
                    radioGroupAnno.check(0);
                }
                lastAnno = ((RadioButton) radioGroupAnno.getChildAt(
                        radioGroupAnno.getCheckedRadioButtonId() - 1)).getText().toString();
                //start file
                Util.appendFile(fileAnno, START);
            } else
            {
                fileAnno = null;
            }
        }
    }

    /**
     *
     */
    void finishAnnotation()
    {
        if (fileAnno != null && lastAnno != null)
        {
            Util.appendFile(fileAnno, " " + Util.getAnnotationTime() + " " + lastAnno + LoggingConstants.DELIMITER_LINE);
        }
        fileAnno = null;
        lastAnno = null;
        running = false;
        enableComponents(true);
    }

    /**
     * @param enable boolean
     */
    private void enableComponents(final boolean enable)
    {
        if (floatingActionButton != null)
        {
            floatingActionButton.post(new Runnable()
            {
                public void run()
                {
                    floatingActionButton.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
                    floatingActionButton.setEnabled(enable);
                }
            });
        }
        if (editTextPathAnno != null)
        {
            editTextPathAnno.post(new Runnable()
            {
                public void run()
                {
                    editTextPathAnno.setEnabled(enable);
                }
            });
        }
        if (editTextNameAnno != null)
        {
            editTextNameAnno.post(new Runnable()
            {
                public void run()
                {
                    editTextNameAnno.setEnabled(enable);
                }
            });
        }
    }

    /**
     * @return View
     */
    @Override
    public View getView()
    {
        return view;
    }

    /**
     * @return String
     */
    @Override
    public String getTitle()
    {
        return title;
    }

    /**
     * @return int
     */
    @Override
    public int getIcon()
    {
        return icon;
    }
}
