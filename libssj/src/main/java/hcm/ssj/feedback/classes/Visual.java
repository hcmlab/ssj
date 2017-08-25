/*
 * Visual.java
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

package hcm.ssj.feedback.classes;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ViewSwitcher;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.security.InvalidParameterException;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.actions.Action;
import hcm.ssj.feedback.actions.VisualAction;


/**
 * Created by Johnny on 01.12.2014.
 */
public class Visual extends FeedbackClass
{
    protected ImageSwitcher img[];
    protected float defBrightness = 0.5f;

	protected static long s_lock = 0;

    protected long timeout = 0;
    private boolean isSetup;
    private int position = 0;

    private Activity activity;

    public Visual(Context context)
    {
        this.context = context;
        type = Type.Visual;
        isSetup = false;
    }

    @Override
    public boolean execute(Action action)
    {
        if(!isSetup)
            return false;

        VisualAction ev = (VisualAction) action;

        //check locks
		//global
        if(System.currentTimeMillis() < s_lock)
        {
            Log.i("ignoring event, global lock active for another " + (s_lock - System.currentTimeMillis()) + "ms");
            return false;
        }
		//local
		if (System.currentTimeMillis() - ev.lastExecutionTime < ev.lockSelf + ev.dur)
		{
			Log.i("ignoring event, self lock active for another " + (ev.lockSelf + ev.dur - (System.currentTimeMillis() - ev.lastExecutionTime)) + "ms");
			return false;
		}

        updateIcons(ev.icons);
        updateBrightness(ev.brightness);

        //set dur
        if(ev.dur > 0)
            //return to default (first) event after dur milliseconds has passed
            timeout = System.currentTimeMillis() + (long) ev.dur;
        else
            timeout = 0;

        //set global lock
        if(ev.lock > 0)
            s_lock = System.currentTimeMillis() + (long) ev.dur + (long) ev.lock;
        else
            s_lock = 0;

        return true;
    }

    public void update()
    {
        if(timeout == 0 || System.currentTimeMillis() < timeout)
            return;

        //if a lock is set, return icons to default configuration
        Log.i("restoring default icons");
        updateIcons(new Drawable[]{null, null});
        updateBrightness(defBrightness);
        timeout = 0;
    }

    protected void updateIcons(Drawable icons[])
    {
        //set feedback icon
        updateImageSwitcher(img[0], icons[0]);

        //set quality icon
        if(icons.length == 2 && img.length == 2 && img[1] != null)
        {
            updateImageSwitcher(img[1], icons[1]);
        }
    }

    protected void updateBrightness(final float brightness)
    {
        activity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
                lp.screenBrightness = brightness;
                activity.getWindow().setAttributes(lp);
            }
        });
    }

    protected void updateImageSwitcher(final ImageSwitcher view, final Drawable img)
    {
        view.post(new Runnable()
        {
            public void run()
            {
                view.setImageDrawable(img);
            }
        });
    }

    protected void load(XmlPullParser xml, final Context context)
    {
        String layout_name = null;
        int fade = 0;
        try
        {
            xml.require(XmlPullParser.START_TAG, null, "feedback");

            layout_name = xml.getAttributeValue(null, "layout");
            if(layout_name == null)
                throw new InvalidParameterException("layout not set");

            String fade_str = xml.getAttributeValue(null, "fade");
            if (fade_str != null)
                fade = Integer.valueOf(fade_str);

            String pos_str = xml.getAttributeValue(null, "position");
            if (pos_str != null)
                position = Integer.valueOf(pos_str);

            String bright_str = xml.getAttributeValue(null, "def_brightness");
            if (bright_str != null)
                defBrightness = Float.valueOf(bright_str);
        }
        catch(IOException | XmlPullParserException | InvalidParameterException e)
        {
            Log.e("error parsing config file", e);
        }

        super.load(xml, context);

        buildLayout(context, layout_name, fade);
    }

    private void buildLayout(final Context context, final String layout_name, final int fade)
    {
        Handler handler = new Handler(context.getMainLooper());
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                int layout_id = activity.getResources().getIdentifier(layout_name, "id", context.getPackageName());
                TableLayout table = (TableLayout) activity.findViewById(layout_id);
                table.setStretchAllColumns(true);

                activity = (Activity) table.findViewById(android.R.id.content).getContext();

                int rows = ((VisualAction) action).icons.length;
                img = new ImageSwitcher[rows];

                //if this is the first visual class, init rows
                if (table.getChildCount() == 0)
                    for(int i = 0; i < rows; ++i)
                        table.addView(new TableRow(context), i);

                for(int i = 0; i < rows; ++i)
                {
                    TableRow tr = (TableRow) table.getChildAt(i);
                    tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1f));

                    //if the image switcher has already been initialized by a class on previous level
                    if(tr.getChildAt(position) != null)
                    {
                        img[i] = (ImageSwitcher)tr.getChildAt(position);
                    }
                    else
                    {
                        img[i] = new ImageSwitcher(context);
                        img[i].setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1f));

                        Animation in = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
                        in.setDuration(fade);
                        Animation out = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
                        out.setDuration(fade);

                        img[i].setInAnimation(in);
                        img[i].setOutAnimation(out);

                        img[i].setFactory(new ViewSwitcher.ViewFactory() {
                            @Override
                            public View makeView() {
                                ImageView imageView = new ImageView(context);
                                imageView.setLayoutParams(new ImageSwitcher.LayoutParams(ImageSwitcher.LayoutParams.MATCH_PARENT, ImageSwitcher.LayoutParams.MATCH_PARENT));
                                return imageView;
                            }
                        });

                        tr.addView(img[i], position);
                    }
                }

                isSetup = true;

                //init view
                updateIcons(new Drawable[]{null, null});
                updateBrightness(defBrightness);
            }
        });
    }
}
