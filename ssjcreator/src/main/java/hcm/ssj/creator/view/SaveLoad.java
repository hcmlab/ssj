/*
 * SaveLoad.java
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

package hcm.ssj.creator.view;

import android.graphics.Point;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import hcm.ssj.core.Annotation;
import hcm.ssj.core.Log;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.util.Util;

/**
 * Save and load files for {@link hcm.ssj.creator.view.PipeView} placements.<br>
 * Created by Frank Gaibler on 15.03.2017.
 */
abstract class SaveLoad
{
    private final static String SUFFIX = ".layout";
    private final static String ROOT = "ssjCreator";
    private final static String PIPE_GROUP = "pipeLayout";
    private final static String ANNO_GROUP = "annotation";
    private final static String ANNO = "annoClass";
    private final static String NAME = "name";
    private final static String FILE_NAME = "fileName";
    private final static String FILE_PATH = "filePath";
    private final static String VERSION = "version";
    private final static String VERSION_NUMBER = "1";
    private final static String COMPONENT = "component";
    private final static String X = "X";
    private final static String Y = "Y";

    /**
     * @param o            Object
     * @param providers    ArrayList<ComponentView>
     * @param sensors      ArrayList<ComponentView>
     * @param transformers ArrayList<ComponentView>
     * @param consumers    ArrayList<ComponentView>
     * @return boolean
     */
    static boolean save(Object o,
                        ArrayList<ComponentView> providers, ArrayList<ComponentView> sensors,
                        ArrayList<ComponentView> transformers, ArrayList<ComponentView> consumers,
                        ArrayList<ComponentView> eventHandlers)
    {
        File fileOrig = (File) o;
        File file = new File(fileOrig.getParentFile().getPath(), fileOrig.getName().replace(Util.SUFFIX, "") + SUFFIX);
        try
        {
            file.createNewFile();
        } catch (IOException ex)
        {
            Log.e("could not create file");
            return false;
        }
        //open stream
        FileOutputStream fileOutputStream;
        try
        {
            fileOutputStream = new FileOutputStream(file);

        } catch (FileNotFoundException e)
        {
            Log.e("file not found");
            return false;
        }
        XmlSerializer serializer = Xml.newSerializer();
        try
        {
            //start document
            serializer.setOutput(fileOutputStream, "UTF-8");
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, ROOT);
            serializer.attribute(null, VERSION, VERSION_NUMBER);

            //pipe layout
            serializer.startTag(null, PIPE_GROUP);
            //sensorChannels
            for (ComponentView componentView : providers)
            {
                addComponentView(serializer, componentView);
            }
            //sensors
            for (ComponentView componentView : sensors)
            {
                addComponentView(serializer, componentView);
            }
            //transformers
            for (ComponentView componentView : transformers)
            {
                addComponentView(serializer, componentView);
            }
            //consumers
            for (ComponentView componentView : consumers)
            {
                addComponentView(serializer, componentView);
            }
            //eventHandlers
            for (ComponentView componentView : eventHandlers)
            {
                addComponentView(serializer, componentView);
            }
            serializer.endTag(null, PIPE_GROUP);

            //annotations
            Annotation anno = PipelineBuilder.getInstance().getAnnotation();
            serializer.startTag(null, ANNO_GROUP);
            serializer.attribute(null, FILE_NAME, anno.getFileName());
            serializer.attribute(null, FILE_PATH, anno.getFilePath());

            String[] anno_classes = anno.getClassArray();
            for (String anno_class : anno_classes)
            {
                serializer.startTag(null, ANNO);
                serializer.attribute(null, NAME, anno_class);
                serializer.endTag(null, ANNO);
            }
            serializer.endTag(null, ANNO_GROUP);

            //finish document
            serializer.endTag(null, ROOT);
            serializer.endDocument();
            serializer.flush();
        } catch (IOException ex)
        {
            Log.e("could not save file");
            return false;
        } finally
        {
            try
            {
                fileOutputStream.close();
            } catch (IOException ex)
            {
                Log.e("could not close stream");
            }
        }
        return true;
    }

    /**
     * @param o Object
     * @return ArrayList
     */
    static ArrayList<Point> load(Object o)
    {
        File fileOrig = (File) o;
        File file = new File(fileOrig.getParentFile().getPath(), fileOrig.getName().replace(Util.SUFFIX, "") + SUFFIX);
        if (!file.exists())
        {
            return null;
        }
        FileInputStream fileInputStream;
        try
        {
            fileInputStream = new FileInputStream(file);

        } catch (FileNotFoundException e)
        {
            Log.e("file not found");
            return null;
        }
        return load(fileInputStream);
    }

    /**
     * @param fileInputStream FileInputStream
     * @return ArrayList
     */
    static ArrayList<Point> load(FileInputStream fileInputStream)
    {
        try
        {
            //check file version
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(fileInputStream, null);
            parser.nextTag();
            if (parser.getName().equals(ROOT))
            {
                String value = parser.getAttributeValue(null, VERSION);
                if (!value.equals(VERSION_NUMBER))
                {
                    return null;
                }
            } else
            {
                return null;
            }
            //load classes
            parser.nextTag();
            String tag;
            ArrayList<Point> alPoints = new ArrayList<>();

            while (!(tag = parser.getName()).equals(ROOT))
            {
                if (parser.getEventType() == XmlPullParser.START_TAG)
                {
                    switch (tag)
                    {
                        case COMPONENT:
                        {
                            String x = parser.getAttributeValue(null, X);
                            String y = parser.getAttributeValue(null, Y);
                            alPoints.add(new Point(Integer.valueOf(x), Integer.valueOf(y)));
                            break;
                        }
                        case ANNO_GROUP:
                        {
                            PipelineBuilder.getInstance().getAnnotation().setFileName(parser.getAttributeValue(null, FILE_NAME));
                            PipelineBuilder.getInstance().getAnnotation().setFilePath(parser.getAttributeValue(null, FILE_PATH));
                            break;
                        }
                        case ANNO:
                        {
                            String name = parser.getAttributeValue(null, NAME);
                            PipelineBuilder.getInstance().getAnnotation().appendClass(name);
                            break;
                        }
                    }
                }
                parser.nextTag();
            }
            return alPoints;
        } catch (IOException | XmlPullParserException ex)
        {
            Log.e("could not parse file", ex);
            return null;
        } finally
        {
            try
            {
                fileInputStream.close();
            } catch (IOException ex)
            {
                Log.e("could not close stream", ex);
            }
        }
    }

    /**
     * @param serializer    XmlSerializer
     * @param componentView ComponentView
     */
    private static void addComponentView(XmlSerializer serializer, ComponentView componentView) throws IOException
    {
        serializer.startTag(null, COMPONENT);
        serializer.attribute(null, X, String.valueOf(componentView.getGridX()));
        serializer.attribute(null, Y, String.valueOf(componentView.getGridY()));
        serializer.endTag(null, COMPONENT);
    }
}
