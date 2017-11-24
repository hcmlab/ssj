/*
 * AnnotationTab.java
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

package hcm.ssj.creator.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

import hcm.ssj.core.Annotation;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.event.StringEvent;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.BandComm;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.util.Util;

/**
 * Annotation tab for main activity.<br>
 * Created by Frank Gaibler on 23.09.2016.
 */
public class AnnotationTab implements ITab
{
    private Activity activity = null;
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
    private boolean running = false;
    private BandComm bandComm;
    private int annoWithBand = -1;
    private CheckBox externalAnno = null;

    private Annotation anno = null;

    /**
     * @param activity Activity
     */
    AnnotationTab(Activity activity)
    {
        this.activity = activity;

        anno = PipelineBuilder.getInstance().getAnnotation();
        view = createContent(activity);

        title = activity.getResources().getString(R.string.str_annotation);
        icon = android.R.drawable.ic_menu_agenda;

        bandComm = new BandComm(activity);
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
        floatingActionButton.setImageResource(R.drawable.ic_add_white_24dp);
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        int dpValue = 12; // margin in dips
        float d = context.getResources().getDisplayMetrics().density;
        int margin = (int) (dpValue * d); // margin in pixels
        params.setMargins(0, 0, margin, margin);
        floatingActionButton.setLayoutParams(params);
        floatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (annoClassList != null)
                {
                    String name = context.getString(R.string.str_defaultAnno, annoClassList.getChildCount());
                    annoClassList.addView(createClassSwitch(context, name));
                    anno.addClass(name);
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
        editTextNameAnno.setText(anno.getFileName(), TextView.BufferType.NORMAL);
        editTextNameAnno.addTextChangedListener(new TextWatcher()
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
                anno.setFileName(s.toString().trim());
            }
        });
        linearLayout.addView(editTextNameAnno);

        //file path
        TextView textViewDescriptionPath = new TextView(context);
        textViewDescriptionPath.setText(R.string.str_filePath);
        textViewDescriptionPath.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        linearLayout.addView(textViewDescriptionPath);
        editTextPathAnno = new EditText(context);
        editTextPathAnno.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextPathAnno.setText(anno.getFilePath(), TextView.BufferType.NORMAL);
        editTextPathAnno.addTextChangedListener(new TextWatcher()
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
                anno.setFilePath(s.toString().trim());
            }
        });
        linearLayout.addView(editTextPathAnno);

        //other options
        externalAnno = new CheckBox(context);
        externalAnno.setText(R.string.anno_msband);
        externalAnno.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    bandComm.create();
                    annoWithBand = 0; //associate with first element in anno list
                } else
                {
                    bandComm.destroy();
                    annoWithBand = -1;
                }
            }
        });
        linearLayout.addView(externalAnno);
        //annotations
        dpValue = 12; // margin in dips
        d = context.getResources().getDisplayMetrics().density;
        margin = (int) (dpValue * d); // margin in pixels

        annoClassList = new LinearLayout(context);
        setAnnoClasses(anno.getClasses());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(margin, margin, margin, 0);
        annoClassList.setLayoutParams(lp);
        annoClassList.setOrientation(LinearLayout.VERTICAL);
        annoClassList.setGravity(Gravity.CENTER_HORIZONTAL);
        linearLayout.addView(annoClassList);

        return coordinatorLayout;
    }

    /**
     * @param context Context
     * @return CompoundButton
     */
    private LinearLayout createClassSwitch(final Context context, String name)
    {
        SwitchCompat switchButton = new SwitchCompat(context);
        switchButton.setEnabled(false);
        //
        TextView textView = new TextView(context);
        textView.setText(name);
        textView.setTextSize(textView.getTextSize() * 0.5f);
        //
        LinearLayout layout = new LinearLayout(context);
        layout.setBackgroundColor(Color.parseColor("#EEEEEE"));
        //
        int dpValue = 8; // margin in dips
        float d = context.getResources().getDisplayMetrics().density;
        int margin = (int) (dpValue * d); // margin in pixels
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin, margin, margin, 0);
        layout.setLayoutParams(params);
        layout.addView(textView);
        layout.addView(switchButton);
        //
        switchButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        LinearLayout.LayoutParams paramsBtn = (LinearLayout.LayoutParams) switchButton.getLayoutParams();
        paramsBtn.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        paramsBtn.width = LinearLayout.LayoutParams.MATCH_PARENT;
        switchButton.setLayoutParams(paramsBtn);
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                double time = Util.getAnnotationTime();
                if (isChecked)
                {
                    for (int i = 0; i < annoClassList.getChildCount(); i++)
                    {
                        SwitchCompat button = (SwitchCompat) ((LinearLayout) (annoClassList.getChildAt(i))).getChildAt(1);
                        if (button != buttonView)
                        {
                            button.setChecked(false);
                        }
                    }
                }

                //only modify anno when pipeline is running
                if (Pipeline.getInstance().isRunning())
                {
                    String name = ((TextView) (((ViewGroup) (buttonView.getParent())).getChildAt(0))).getText().toString();

                    if (isChecked)
                    {
                        curAnnoStartTime = time;

                        //create start event
                        StringEvent ev = new StringEvent(name);
                        ev.time = (long)(time * 1000);
                        ev.dur = 0;
                        ev.state = Event.State.CONTINUED;
                        anno.getChannel().pushEvent(ev);
                    }
                    else
                    {
                        anno.addEntry(name, curAnnoStartTime, time);

                        //create end event
                        StringEvent ev = new StringEvent(name);
                        ev.time = (long)(curAnnoStartTime * 1000);
                        ev.dur = (int)((time - curAnnoStartTime) * 1000);
                        ev.state = Event.State.COMPLETED;
                        anno.getChannel().pushEvent(ev);

                        curAnnoStartTime = 0;
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
                            String name = editText.getText().toString().trim();
                            ((TextView) viewGroup.getChildAt(0)).setText(name);
                            anno.setClasses(getAnnoClasses());
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
                                anno.removeClass(((TextView) viewGroup.getChildAt(0)).getText().toString());
                                ((ViewGroup) viewGroup.getParent()).removeView(viewGroup);
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
        syncWithModel();
        enableComponents(false);
        running = true;

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                //activate buttons
                for (int i = 0; i < annoClassList.getChildCount(); i++)
                {
                    SwitchCompat button = (SwitchCompat) ((LinearLayout) (annoClassList.getChildAt(i))).getChildAt(1);
                    button.setEnabled(true);
                }
            }
        });
    }

    /**
     *
     */
    void finishAnnotation()
    {
        try
        {
            anno.save();
        }
        catch (IOException | XmlPullParserException e)
        {
            Log.e("unnable to save annotation file", e);
        }

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < annoClassList.getChildCount(); i++)
                {
                    SwitchCompat button = (SwitchCompat) ((LinearLayout) (annoClassList.getChildAt(i))).getChildAt(1);
                    button.setChecked(false);
                    button.setEnabled(false);
                }
            }
        });

        //wait for buttons to "uncheck"
        try
        {
            Thread.sleep(100);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }

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

    /**
     * @return int
     */
    public int getBandAnnoButton()
    {
        return annoWithBand;
    }

    /**
     * @param id    int
     * @param value boolean
     * @return boolean
     */
    public boolean toggleAnnoButton(int id, final boolean value)
    {
        if (annoClassList == null || activity == null || !running)
        {
            return false;
        }
        final LinearLayout anno = (LinearLayout) annoClassList.getChildAt(id);
        if (anno == null)
        {
            return false;
        }
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                SwitchCompat button = (SwitchCompat) anno.getChildAt(1);
                if (button.isEnabled())
                    button.setChecked(value);
            }
        });
        return true;
    }

    public ArrayList<String> getAnnoClasses()
    {
        if (annoClassList == null)
            return null;

        ArrayList<String> classes = new ArrayList<>();
        for(int i = 0; i < annoClassList.getChildCount(); i++)
        {
            LinearLayout anno = (LinearLayout) annoClassList.getChildAt(i);
            classes.add(((TextView) anno.getChildAt(0)).getText().toString());
        }
        return classes;
    }

    public void setAnnoClasses(ArrayList<String> classes)
    {
        if (annoClassList == null)
            return;

        //clear existing annotations
        annoClassList.removeAllViews();

        for(String anno : classes)
        {
            annoClassList.addView(createClassSwitch(activity, anno));
        }
    }

    public void syncWithModel()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setAnnoClasses(anno.getClasses());

                if (editTextPathAnno != null)
                {
                    editTextPathAnno.setText(anno.getFilePath());
                }
                if (editTextNameAnno != null)
                {
                    editTextNameAnno.setText(anno.getFileName());
                }
            }
        });
    }
}
