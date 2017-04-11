/*
 * Feedback.java
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.behaviours.Behaviour;
import hcm.ssj.feedback.events.Event;


/**
 * Created by Johnny on 01.12.2014.
 */
public abstract class Feedback
{
    protected Type type;
    protected Behaviour behaviour;
    protected ArrayList<Event> events = new ArrayList<Event>();
    protected Event lastEvent = null;
    protected int level = 0;
    private ArrayList<FeedbackListener> listeners = new ArrayList<>();

    public static Feedback create(XmlPullParser xml, Activity activity)
    {
        Feedback f = null;

        if(xml.getAttributeValue(null, "type").equalsIgnoreCase("visual"))
            f = new VisualFeedback(activity);
        else if(xml.getAttributeValue(null, "type").equalsIgnoreCase("tactile"))
            f = new TactileFeedback(activity);
        else if(xml.getAttributeValue(null, "type").equalsIgnoreCase("audio"))
            f = new AudioFeedback(activity);
        else
            throw new UnsupportedOperationException("feedback type "+ xml.getAttributeValue(null, "type") +" not yet implemented");

        f.load(xml, activity.getApplicationContext());
        return f;
    }

    public int getLevel() {
        return level;
    }

    public Valence getValence() {
        if(lastEvent == null)
            return Valence.Unknown;

        return lastEvent.getValence();
    }

    public void release()
    {
        for(Event ev : events)
        {
            ev.release();
        }
        events.clear();
    }

    public Behaviour getBehaviour()
    {
        return behaviour;
    }

    public ArrayList<Event> getEvents()
    {
        return events;
    }

    /*
     * called every frame by the manager
     */
    public void update() {}

    public void process(hcm.ssj.core.event.Event behavEvent)
    {
        if(!behaviour.checkEvent(behavEvent))
            return;

        float value = behaviour.parseEvent(behavEvent);

        Event ev = getEvent(value);
        if(ev == null)
            return;

        if(execute(ev)) {
            ev.lastExecutionTime = System.currentTimeMillis();
            lastEvent = ev;
        }

        // Notify event listeners
        callPostFeedback(behavEvent, ev, value);
    }

    private void callPostFeedback(final hcm.ssj.core.event.Event ssjEvent, final Event ev, final float value)
    {
        for (final FeedbackListener listener : listeners)
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    listener.onPostFeedback(ssjEvent, ev, value);
                }
            }).start();
        }
    }

    public abstract boolean execute(Event event);

    //returns currently active event
    public Event getEvent(float value)
    {
        Iterator<Event> iter = events.iterator();
        Event inst = null;
        while(iter.hasNext())
        {
            inst = iter.next();
            if((value == inst.thres_lower) || (value >= inst.thres_lower && value < inst.thres_upper))
                return inst;
        }
        return null;
    }

    protected void load(XmlPullParser xml, Context context)
    {
        try
        {
            xml.require(XmlPullParser.START_TAG, null, "class");

            String level_str = xml.getAttributeValue(null, "level");
            if(level_str != null)
                level = Integer.parseInt(level_str);

            while (xml.next() != XmlPullParser.END_DOCUMENT)
            {
                if (xml.getEventType() == XmlPullParser.START_TAG && xml.getName().equalsIgnoreCase("behaviour"))
                {
                    behaviour = Behaviour.create(xml, context);
                }
                else if (xml.getEventType() == XmlPullParser.START_TAG && xml.getName().equalsIgnoreCase("event"))
                {
                    Event t = Event.create(type, xml, context);
                    events.add(t);
                }
                else if (xml.getEventType() == XmlPullParser.END_TAG && xml.getName().equalsIgnoreCase("class"))
                    break; //jump out once we reach end tag
            }
        }
        catch(IOException | XmlPullParserException e)
        {
            Log.e("error parsing config file", e);
        }
    }

    public void addFeedbackListener(FeedbackListener listener)
    {
        listeners.add(listener);
    }

    public enum Type
    {
        Visual,
        Tactile,
        Audio
    }

    public enum Valence
    {
        Unknown,
        Desirable,
        Undesirable
    }
}
