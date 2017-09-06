/*
 * FeedbackManager.java
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

package hcm.ssj.feedback;

import android.content.Context;
import android.util.Xml;
import android.widget.TableLayout;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.feedback.feedbackmanager.classes.FeedbackClass;
import hcm.ssj.feedback.feedbackmanager.classes.FeedbackListener;
import hcm.ssj.file.LoggingConstants;

/**
 * Created by Johnny on 02.12.2014.
 */
public class FeedbackManager extends EventHandler
{
    private static int MANAGER_UPDATE_TIMEOUT = 100; //ms

    public class Options extends OptionList
    {
        public final Option<String> strategyFilePath = new Option<>("strategyFilePath", LoggingConstants.SSJ_EXTERNAL_STORAGE, String.class, "location of strategy file");
        public final Option<String> strategyFileName = new Option<>("strategyFileName", null, String.class, "name of strategy file");
        public final Option<Boolean> fromAsset = new Option<>("fromAsset", false, Boolean.class, "load feedback strategy file from assets");
        public final Option<Float> progression = new Option<>("progression", 12f, Float.class, "timeout for progressing to the next feedback level");
        public final Option<Float> regression = new Option<>("regression", 60f, Float.class, "timeout for going back to the previous feedback level");
        public final Option<TableLayout> layout = new Option<>("layout", null, TableLayout.class, "TableLayout in which to render visual feedback");

        private Options()
        {
            addOptions();
        }
    }
    public Options options = new Options();

    protected Context context;

    protected ArrayList<FeedbackClass> classes = new ArrayList<FeedbackClass>();

    protected int level = 0;
    private int max_level = 3;
    private long lastDesireableState;
    private long lastUndesireableState;
    private long progressionTimeout;
    private long regressionTimeout;

    public FeedbackManager()
    {
        context = SSJApplication.getAppContext();
        _name = "FeedbackManager";
    }

    public ArrayList<FeedbackClass> getClasses()
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
            String file = options.strategyFileName.get();
            if(!options.fromAsset.get())
                file = options.strategyFilePath.get() + File.separator + options.strategyFileName.get();

            load(file, options.fromAsset.get());
        }
        catch (IOException | XmlPullParserException e)
        {
            Log.e("error reading strategy file");
        }
    }

    @Override
    public void process() {

        for(FeedbackClass i : classes)
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
        for(FeedbackClass f : classes)
        {
            f.release();
        }
        classes.clear();
    }

    @Override
    public void notify(hcm.ssj.core.event.Event behavEvent) {

        if(classes.size() == 0)
            return;

        //validate feedback
        for(FeedbackClass i : classes)
        {
            if(i.getLevel() == level)
            {
                if(i.getValence() == FeedbackClass.Valence.Desirable && i.getLastExecutionTime() > lastDesireableState)
                {
                    lastDesireableState = i.getLastExecutionTime();
                }
                else if(i.getValence() == FeedbackClass.Valence.Undesirable && i.getLastExecutionTime() > lastUndesireableState)
                {
                    lastUndesireableState = i.getLastExecutionTime();
                }
            }
        }

        //if all current feedback classes are in a non desirable state, check if we should progress to next level
        if (System.currentTimeMillis() - progressionTimeout > lastDesireableState && level < max_level) {
            level++;
            lastDesireableState = System.currentTimeMillis();
            Log.d("activating level " + level);
        }
        //if all current feedback classes are in a desirable state, check if we can go back to the previous level
        else if (System.currentTimeMillis() - regressionTimeout > lastUndesireableState && level > 0) {
            level--;
            lastUndesireableState = System.currentTimeMillis();
            Log.d("activating level " + level);
        }

        //execute feedback
        for(FeedbackClass i : classes)
        {
            if(i.getLevel() == level)
            {
                try
                {
                    i.process(behavEvent);
                }
                catch (Exception e)
                {
                    Log.e("error processing event", e);
                }
            }
        }
    }

    private void load(String filename, boolean fromAsset) throws IOException, XmlPullParserException
    {
        InputStream in;
        if(!fromAsset)
            in = new FileInputStream(new File(filename));
        else
            in = context.getAssets().open(filename);

        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

        parser.setInput(in, null);

        while(parser.next() != XmlPullParser.END_DOCUMENT)
        {
            switch(parser.getEventType())
            {
                case XmlPullParser.START_TAG:
                    if(parser.getName().equalsIgnoreCase("strategy"))
                    {
                        load(parser);
                    }
                    break;
            }
        }

        //find max progression level
        max_level = 0;
        for(FeedbackClass i : classes) {
            if(i.getLevel() > max_level)
                max_level = i.getLevel();
        }

        in.close();
    }

    private void load(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        parser.require(XmlPullParser.START_TAG, null, "strategy");

        //iterate through classes
        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            if(parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("feedback"))
            {
                //parse feedback classes
                FeedbackClass c = FeedbackClass.create(parser, context, options);
                classes.add(c);
            }
            else if(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("strategy"))
                break; //jump out once we reach end tag for classes
        }

        parser.require(XmlPullParser.END_TAG, null, "strategy");

        Log.i("loaded " + classes.size() + " feedback classes");
    }

    public void addFeedbackListener(FeedbackListener listener)
    {
        for(FeedbackClass i : classes)
        {
            i.addFeedbackListener(listener);
        }
    }
}
