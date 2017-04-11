/*
 * VisualFeedback.java
 * Copyright (c) 2015
 * Author: Ionut Damian
 * *****************************************************
 * This file is part of the Logue project developed at the Lab for Human Centered Multimedia
 * of the University of Augsburg.
 *
 * The applications and libraries are free software; you can redistribute them and/or modify them
 * under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * The software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.feedback.classes;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
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
import hcm.ssj.feedback.events.Event;
import hcm.ssj.feedback.events.VisualEvent;


/**
 * Created by Johnny on 01.12.2014.
 */
public class VisualFeedback extends Feedback
{
    Activity activity;

    protected static int s_id[] = new int[32]; //max levels
    protected int id;
    protected ImageSwitcher img[];

    protected Drawable defIcon[] = null;
    protected float defBrightness = 0;

    protected long timeout = 0;
    protected long lock = 0;
    private boolean isSetup;

    public VisualFeedback(Activity activity)
    {
        this.activity = activity;
        type = Type.Visual;
        isSetup = false;
    }

    @Override
    public boolean execute(Event event)
    {
        if(!isSetup)
            return false;

        VisualEvent ve = (VisualEvent) event;

        //update only if the lock has passed
        if(System.currentTimeMillis() < lock)
        {
            Log.i("ignoring event, lock active for another " + (lock - System.currentTimeMillis()) + "ms");
            return false;
        }

        updateIcons(ve.icons);
        updateBrightness(ve.brightness);

        //set lock
        if(ve.dur > 0 && ve.icons != defIcon)
            //return to default (first) event after dur milliseconds has passed
            timeout = System.currentTimeMillis() + (long) ve.dur;
        else
            timeout = 0;

        //set lock
        if(ve.lock > 0)
            lock = System.currentTimeMillis() + (long) ve.dur + (long) ve.lock;
        else
            lock = 0;

        return true;
    }

    public void update()
    {
        if(timeout == 0 || System.currentTimeMillis() < timeout)
            return;

        //if a lock is set, return icons to default configuration
        Log.i("restoring default icons");
        updateIcons(defIcon);
        updateBrightness(defBrightness);
        timeout = 0;
    }

    protected void updateIcons(Drawable icons[])
    {
        //set feedback icon
        updateImageSwitcher(activity, img[0], icons[0]);

        //set quality icon
        if(icons.length == 2 && img[1] != null)
        {
            updateImageSwitcher(activity, img[1], icons[1]);
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

    protected void updateImageSwitcher(final Activity act, final ImageSwitcher view, final Drawable img)
    {
        act.runOnUiThread(new Runnable()
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
            xml.require(XmlPullParser.START_TAG, null, "class");

            layout_name = xml.getAttributeValue(null, "layout_id");
            if(layout_name == null)
                throw new InvalidParameterException("layout not set");

            String fade_str = xml.getAttributeValue(null, "fade");
            if (fade_str != null)
                fade = Integer.valueOf(fade_str);
        }
        catch(IOException | XmlPullParserException | InvalidParameterException e)
        {
            Log.e("error parsing config file", e);
        }

        super.load(xml, context);
        id = s_id[level]++;

        //default icons
        defIcon = ((VisualEvent) events.get(0)).icons;
        defBrightness = ((VisualEvent) events.get(0)).brightness;

        buildLayout(context, layout_name, fade);
    }

    private void buildLayout(final Context context, final String layout_name, final int fade)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                int layout_id = activity.getResources().getIdentifier(layout_name, "id", context.getPackageName());
                TableLayout table = (TableLayout) activity.findViewById(layout_id);
                table.setStretchAllColumns(true);

                int rows = ((VisualEvent) events.get(0)).icons.length;
                for(Event e : events)
                    if(((VisualEvent)e).icons.length > rows)
                        rows = ((VisualEvent)e).icons.length;

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
                    if(level > 0 && id < s_id[level-1])
                    {
                        img[i] = (ImageSwitcher)tr.getChildAt(id);
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

                        tr.addView(img[i]);
                    }
                }

                isSetup = true;

                //init view
                execute(events.get(0));
            }
        });
    }
}
