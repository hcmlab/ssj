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

package hcm.ssj.creator.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

import hcm.ssj.core.TheFramework;
import hcm.ssj.creator.R;
import hcm.ssj.creator.util.Util;
import hcm.ssj.file.LoggingConstants;

/**
 * Annotation tab for main activity.<br>
 * Created by Frank Gaibler on 23.09.2016.
 */
class Annotation implements ITab
{
    Activity activity;

    //tab
    private View view;
    private String title;
    private int icon;
    //annotation
    private double curAnnoStartTime = 0;
    private FloatingActionButton floatingActionButton = null;
    private EditText editTextPathAnno = null;
    private EditText editTextNameAnno = null;
    private LinearLayout annoClassList = null;
    private File fileAnno = null;
    private final static String SUFFIX = ".anno";
    private boolean running = false;

    /**
     * @param activity Activity
     */
    Annotation(Activity activity)
    {
        this.activity = activity;
        view = createContent(activity);
        title = activity.getResources().getString(R.string.str_annotation);
        icon = android.R.drawable.ic_menu_agenda;
    }

    /**
     * @param context Context
     */
    private View createContent(final Context context)
    {
        //layouts
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        //
        ScrollView scrollViewAnno = new ScrollView(context);
        scrollViewAnno.setLayoutParams(new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT));
        scrollViewAnno.addView(linearLayout);
        //
        CoordinatorLayout coordinatorLayout = new CoordinatorLayout(context);
        coordinatorLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        coordinatorLayout.addView(scrollViewAnno);
        //add annotation button
        floatingActionButton = new FloatingActionButton(context);
        floatingActionButton.setVisibility(View.INVISIBLE);
        floatingActionButton.setImageResource(R.drawable.ic_add_white_24dp);
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0,0,30,30);
        floatingActionButton.setLayoutParams(params);
        floatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (annoClassList != null)
                {
                    annoClassList.addView(createClassSwitch(context));
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
        annoClassList = new LinearLayout(context);
        annoClassList.addView(createClassSwitch(context));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(50,50,50,0);
        annoClassList.setLayoutParams(lp);
        annoClassList.setOrientation(LinearLayout.VERTICAL);
        annoClassList.setGravity(Gravity.CENTER_HORIZONTAL);
        annoClassList.setVisibility(View.INVISIBLE);
        linearLayout.addView(annoClassList);

        editTextNameAnno.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        if(s.length() > 0) {
                            floatingActionButton.setVisibility(View.VISIBLE);
                            annoClassList.setVisibility(View.VISIBLE);
                        }
                        else
                        {
                            floatingActionButton.setVisibility(View.INVISIBLE);
                            annoClassList.setVisibility(View.INVISIBLE);
                        }
                    }
                }
        );

        return coordinatorLayout;
    }

    /**
     * @param context Context
     * @return CompoundButton
     */
    private LinearLayout createClassSwitch(final Context context)
    {
        SwitchCompat switchButton = new SwitchCompat(context);
        switchButton.setEnabled(false);

        TextView textView = new TextView(context);
        textView.setText(context.getString(R.string.str_defaultAnno, annoClassList.getChildCount()));
        textView.setTextSize(textView.getTextSize() * 0.5f);

        LinearLayout layout = new LinearLayout(context);
        layout.setBackgroundColor(Color.parseColor("#EEEEEE"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(20,20,20,0);
        layout.setLayoutParams(params);

        layout.addView(textView);
        layout.addView(switchButton);

        switchButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        LinearLayout.LayoutParams paramsBtn = (LinearLayout.LayoutParams)switchButton.getLayoutParams();
        paramsBtn.height = LinearLayout.LayoutParams.MATCH_PARENT;
        paramsBtn.width = LinearLayout.LayoutParams.MATCH_PARENT;
        switchButton.setLayoutParams(paramsBtn);

        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    for (int i = 0; i < annoClassList.getChildCount(); i++)
                    {
                        SwitchCompat button = (SwitchCompat) ((LinearLayout)(annoClassList.getChildAt(i))).getChildAt(1);
                        if ( button != buttonView )
                        {
                            button.setChecked(false);
                        }
                    }
                }

                if (fileAnno != null)
                {
                    //only append to running pipeline
                    if (TheFramework.getFramework().isRunning())
                    {
                        if(isChecked) {
                            curAnnoStartTime = Util.getAnnotationTime();
                        }
                        else {
                            String name = ((TextView)(((ViewGroup)(buttonView.getParent())).getChildAt(0))).getText().toString();
                            Util.appendFile(fileAnno, curAnnoStartTime + " " + Util.getAnnotationTime() + " " + name + LoggingConstants.DELIMITER_LINE);
                            curAnnoStartTime = 0;
                        }
                    }
                }
            }
        });

        textView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                //only allow edits while pipe is not active
                if (!running)
                {
                    //content
                    LinearLayout linearLayout = new LinearLayout(context);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);

                    final EditText editText = new EditText(context);
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    editText.setText(((TextView) v).getText(), TextView.BufferType.NORMAL);
                    linearLayout.addView(editText);

                    //dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.str_annotation);
                    builder.setView(linearLayout);

                    builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            ViewGroup viewGroup = (ViewGroup) v.getParent();
                            ((TextView) viewGroup.getChildAt(0)).setText(editText.getText().toString().trim());
                        }
                    });

                    builder.setNegativeButton(R.string.str_cancel, null);

                    builder.setNeutralButton(R.string.str_delete, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            ViewGroup viewGroup = (ViewGroup) v.getParent();
                            if (viewGroup != null)
                            {
                                ((SwitchCompat) viewGroup.getChildAt(1)).setChecked(false);
                                ((ViewGroup)viewGroup.getParent()).removeView(viewGroup);
                            }
                            v.invalidate();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        });

        return layout;
    }

    /**
     *
     */
    void startAnnotation()
    {
        enableComponents(false);
        running = true;
        String name = editTextNameAnno.getText().toString().trim();
        if (!name.isEmpty() && annoClassList.getChildCount() > 0)
        {
            String path = editTextPathAnno.getText().toString();
            //parse wildcards
            if (path.contains("%ts")) {
                path = path.replace("%ts", hcm.ssj.core.Util.getTimestamp(TheFramework.getFramework().getCreateTimeMs()) );
            }

            File parent = new File(path);
            if (parent.exists() || parent.mkdirs())
            {
                fileAnno = new File(parent, name.endsWith(SUFFIX) ? name : name + SUFFIX);
                //delete existing annotation file
                if (fileAnno.exists())
                {
                    fileAnno.delete();
                }

                //activate buttons
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < annoClassList.getChildCount(); i++)
                        {
                            SwitchCompat button = (SwitchCompat) ((LinearLayout)(annoClassList.getChildAt(i))).getChildAt(1);
                            button.setEnabled(true);
                        }
                    }
                });
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
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < annoClassList.getChildCount(); i++)
                {
                    SwitchCompat button = (SwitchCompat) ((LinearLayout)(annoClassList.getChildAt(i))).getChildAt(1);
                    button.setChecked(false);
                    button.setEnabled(false);
                }
            }
        });

        //wait for buttons to "uncheck"
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        fileAnno = null;
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
