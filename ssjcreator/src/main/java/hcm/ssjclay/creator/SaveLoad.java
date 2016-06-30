/*
 * SaveLoad.java
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

package hcm.ssjclay.creator;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import hcm.ssj.core.Log;

/**
 * Save and load files in a <Code>Linker.java</Code> friendly format.<br>
 * Created by Frank Gaibler on 28.06.2016.
 */
public class SaveLoad
{
    private static SaveLoad instance = null;

    /**
     *
     */
    private SaveLoad()
    {
    }

    /**
     * @return SaveLoad
     */
    public static synchronized SaveLoad getInstance()
    {
        if (instance == null)
        {
            instance = new SaveLoad();
        }
        return instance;
    }

    public final boolean save(File file)
    {
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
            serializer.setOutput(fileOutputStream, "UTF-8");
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "root");
            serializer.startTag(null, "Child1");
            serializer.endTag(null, "Child1");
            serializer.startTag(null, "Child2");
            serializer.attribute(null, "attribute", "value");
            serializer.endTag(null, "Child2");
            serializer.startTag(null, "Child3");
            serializer.text("Some text inside child 3");
            serializer.endTag(null, "Child3");
            serializer.endTag(null, "root");
            serializer.endDocument();
            serializer.flush();
            fileOutputStream.close();
        } catch (IOException ex)
        {
            Log.e("could not save file");
            return false;
        }
        return true;
    }

    public final boolean load(File file)
    {
        return false;
    }
}
