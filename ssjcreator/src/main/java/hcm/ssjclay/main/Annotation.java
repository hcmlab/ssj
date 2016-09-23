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

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

import hcm.ssj.file.LoggingConstants;
import hcm.ssjclay.util.Util;

/**
 * Annotation tab for main activity.<br>
 * Created by Frank Gaibler on 23.09.2016.
 */
class Annotation implements Tab
{
    //tab
    private View view;
    private String title;
    private int icon;
    //annotation
    private String lastAnno = null;
    private EditText editTextPathAnno = null;
    private EditText editTextNameAnno = null;
    private RadioGroup radioGroupAnno = null;
    private File fileAnno = null;
    private final static String SUFFIX = ".anno";

    /**
     * @param context Context
     */
    Annotation(Context context)
    {
        //view
        view = createContent(context);
        //title
        title = "Annotation";
        //icon
        icon = android.R.drawable.ic_menu_agenda;
    }

    /**
     * @param context Context
     */
    private View createContent(Context context)
    {
        ScrollView scrollViewAnno = new ScrollView(context);
        //

        //
        editTextNameAnno = new EditText(context);
        editTextNameAnno.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextNameAnno.setText("test", TextView.BufferType.NORMAL);
        editTextPathAnno = new EditText(context);
        editTextPathAnno.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextPathAnno.setText("test", TextView.BufferType.NORMAL);
        radioGroupAnno = new RadioGroup(context);
        RadioButton radioButton1 = new RadioButton(context);
        radioButton1.setText("Stand");
        RadioButton radioButton2 = new RadioButton(context);
        radioButton2.setText("Walk");
        RadioButton radioButton3 = new RadioButton(context);
        radioButton3.setText("Run");
        radioGroupAnno.addView(radioButton1);
        radioGroupAnno.addView(radioButton2);
        radioGroupAnno.addView(radioButton3);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(editTextNameAnno);
        linearLayout.addView(editTextPathAnno);
        linearLayout.addView(radioGroupAnno);
        scrollViewAnno.addView(linearLayout);
        radioGroupAnno.check(radioGroupAnno.getChildAt(0).getId());
        radioGroupAnno.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if (fileAnno != null)
                {
                    if (lastAnno == null)
                    {
                        lastAnno = ((RadioButton) group.getChildAt(0)).getText().toString();
                        Util.appendFile(fileAnno, "0.0");
                    }
                    Util.appendFile(fileAnno, " " + Util.getAnnotationTime() + " " + lastAnno + LoggingConstants.DELIMITER_LINE);
                    Util.appendFile(fileAnno, String.valueOf(Util.getAnnotationTime()));
                }
                lastAnno = ((RadioButton) group.getChildAt(checkedId - 1)).getText().toString();
            }
        });
        return scrollViewAnno;
    }

    /**
     *
     */
    void startAnnotation()
    {
        String name = editTextNameAnno.getText().toString().trim();
        lastAnno = null;
        if (!name.isEmpty())
        {
            fileAnno = Util.getFile("ssj", name.endsWith(SUFFIX) ? name : name + SUFFIX);
            if (fileAnno.exists())
            {
                fileAnno.delete();
            }
            Util.appendFile(fileAnno, "");
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
