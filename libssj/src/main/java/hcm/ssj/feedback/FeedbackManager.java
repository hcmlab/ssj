/*
 * FeedbackManager.java
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

package hcm.ssj.feedback;

import android.app.Activity;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.feedback.classes.Feedback;
import hcm.ssj.feedback.classes.FeedbackListener;

/**
 * Created by Johnny on 02.12.2014.
 */
public class FeedbackManager extends EventHandler
{
    private static int MANAGER_UPDATE_TIMEOUT = 100; //ms

    public class Options extends OptionList
    {
        public final Option<String> strategy = new Option<>("strategy", "", String.class, "feedback strategy file");
        public final Option<Boolean> fromAsset = new Option<>("fromAsset", false, Boolean.class, "load feedback strategy file from assets");
        public final Option<Float> progression = new Option<>("progression", 12f, Float.class, "timeout for progressing to the next feedback level");
        public final Option<Float> regression = new Option<>("regression", 60f, Float.class, "timeout for going back to the previous feedback level");
    }
    public Options options = new Options();

    protected Activity activity;

    protected ArrayList<Feedback> classes = new ArrayList<Feedback>();

    protected int level = 0;
    private int max_level = 3;
    private long lastDesireableState;
    private long lastUndesireableState;
    private long progressionTimeout;
    private long regressionTimeout;

    public FeedbackManager(Activity act)
    {
        activity = act;
    }

    public ArrayList<Feedback> getClasses()
    {
        return classes;
    }

    @Override
    public void enter()
    {
        lastDesireableState = lastUndesireableState = System.currentTimeMillis();

        progressionTimeout = (int)(options.progression.get() * 1000);
        regressionTimeout = (int)(options.regression.get() * 1000);

        try
        {
            load(options.strategy.get(), options.fromAsset.get());
        }
        catch (IOException | XmlPullParserException e)
        {
            Log.e("error reading strategy file");
        }
    }

    @Override
    public void process() {

        for(Feedback i : classes)
        {
            i.update();
        }

        try {
            Thread.sleep(MANAGER_UPDATE_TIMEOUT);
        } catch (InterruptedException e) {
            Log.w("thread interrupted");
        }
    }

    public void flush()
    {
        for(Feedback f : classes)
        {
            f.release();
        }
    }

    @Override
    public void notify(hcm.ssj.core.event.Event behavEvent) {

        if(classes.size() == 0)
            return;

        //validate feedback
        for(Feedback i : classes)
        {
            if(i.getLevel() == level)
            {
                if(i.getValence() == Feedback.Valence.Desirable)
                {
                    lastDesireableState = System.currentTimeMillis();
                }
                else if(i.getValence() == Feedback.Valence.Undesirable)
                {
                    lastUndesireableState = System.currentTimeMillis();
                }
            }
        }

        //if all current feedback classes are in a non desirable state, check if we should progress to next level
        if (System.currentTimeMillis() - progressionTimeout > lastDesireableState && level < max_level) {
            level++;
            lastDesireableState = System.currentTimeMillis();
        }
        //if all current feedback classes are in a desirable state, check if we can go back to the previous level
        else if (System.currentTimeMillis() - regressionTimeout > lastUndesireableState && level > 0) {
            level--;
            lastUndesireableState = System.currentTimeMillis();
        }

        //execute feedback
        for(Feedback i : classes)
        {
            if(i.getLevel() == level)
                i.process(behavEvent);
        }
    }

    private void load(String filename, boolean fromAsset) throws IOException, XmlPullParserException
    {
        InputStream in;
        if(!fromAsset)
            in = new FileInputStream(new File(filename));
        else
            in = activity.getAssets().open(filename);

        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

        parser.setInput(in, null);

        while(parser.next() != XmlPullParser.END_DOCUMENT)
        {
            switch(parser.getEventType())
            {
                case XmlPullParser.START_TAG:
                    if(parser.getName().equalsIgnoreCase("feedback"))
                    {
                        load(parser);
                    }
                    break;
            }
        }

        //find max progression level
        max_level = 0;
        for(Feedback i : classes) {
            if(i.getLevel() > max_level)
                max_level = i.getLevel();
        }

        in.close();
    }

    private void load(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        parser.require(XmlPullParser.START_TAG, null, "feedback");

        //iterate through classes
        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            if(parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("class"))
            {
                //parse feedback classes
                Feedback c = Feedback.create(parser, activity);
                classes.add(c);
            }
            else if(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("feedback"))
                break; //jump out once we reach end tag for classes
        }

        parser.require(XmlPullParser.END_TAG, null, "feedback");

        Log.i("loaded " + classes.size() + " classes");
    }

    public void addFeedbackListener(FeedbackListener listener)
    {
        for(Feedback i : classes)
        {
            i.addFeedbackListener(listener);
        }
    }
}
