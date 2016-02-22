/*
 * SimpleXmlParser.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.file;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * A generic XML-Parser for one tag and its attributes.<br>
 * Created by Frank Gaibler on 23.09.2015.
 */
public class SimpleXmlParser
{
    private static final String namespace = null;
    private String[] searchPath = null;
    private String[] searchAttributes = null;
    private XmlValues xmlValues;

    /**
     * @param in               InputStream
     * @param searchPath       String[]
     * @param searchAttributes String[]
     * @return XmlValues
     * @throws XmlPullParserException
     * @throws IOException
     */
    public XmlValues parse(InputStream in, String[] searchPath, String[] searchAttributes) throws XmlPullParserException, IOException
    {
        this.searchPath = searchPath;
        this.searchAttributes = searchAttributes;
        xmlValues = new XmlValues();
        try
        {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            search(parser, 0);
            return xmlValues;
        } finally
        {
            in.close();
        }
    }

    /**
     * @param parser XmlPullParser
     * @param depth  int
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void search(XmlPullParser parser, int depth) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, namespace, searchPath[depth]);
        if (searchPath.length - 1 == depth)
        {
            readValues(parser, depth);
        } else
        {
            while (parser.next() != XmlPullParser.END_TAG)
            {
                if (parser.getEventType() == XmlPullParser.START_TAG)
                {
                    if (parser.getName().equals(searchPath[depth + 1]))
                    {
                        search(parser, depth + 1);
                    } else
                    {
                        skip(parser);
                    }
                }
            }
        }
    }

    /**
     * @param parser XmlPullParser
     * @param depth  int
     * @throws IOException
     * @throws XmlPullParserException
     */
    private void readValues(XmlPullParser parser, int depth) throws IOException, XmlPullParserException
    {
        String name = searchPath[depth];
        parser.require(XmlPullParser.START_TAG, namespace, name);
        if (searchAttributes == null)
        {
            xmlValues.foundTag.add(readText(parser));
        } else
        {
            String tag = parser.getName();
            if (tag.equals(name))
            {
                String[] attributes = new String[searchAttributes.length];
                for (int i = 0; i < searchAttributes.length; i++)
                {
                    attributes[i] = parser.getAttributeValue(null, searchAttributes[i]);
                }
                xmlValues.foundAttributes.add(attributes);
                parser.nextTag();
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, name);
    }

    /**
     * @param parser XmlPullParser
     * @return String
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT)
        {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /**
     * @param parser XmlPullParser
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        if (parser.getEventType() != XmlPullParser.START_TAG)
        {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0)
        {
            switch (parser.next())
            {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    /**
     * To return the found values.
     */
    public class XmlValues
    {
        public ArrayList<String> foundTag = new ArrayList<>();
        public ArrayList<String[]> foundAttributes = new ArrayList<>();

        private XmlValues()
        {
        }
    }
}
