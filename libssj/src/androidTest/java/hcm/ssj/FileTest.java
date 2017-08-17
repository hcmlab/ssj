/*
 * FileTest.java
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

package hcm.ssj;

import android.os.Environment;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.core.Pipeline;
import hcm.ssj.file.FileReader;
import hcm.ssj.file.FileReaderChannel;
import hcm.ssj.file.FileWriter;
import hcm.ssj.file.LoggingConstants;
import hcm.ssj.file.SimpleXmlParser;
import hcm.ssj.test.Logger;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * Tests all classes in the logging package.<br>
 * Created by Frank Gaibler on 09.09.2015.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FileTest
{
    /**
     * @throws Exception
     */
    @Test
    public void testInternalStorage() throws Exception
    {
        testWriteRead(true);
    }

    /**
     * @throws Exception
     */
    @Test
    public void testExternalStorage() throws Exception
    {
        testWriteRead(false);
    }

    /**
     * @throws Exception
     */
    @Test
    public void testXmlParserTag() throws Exception
    {
        String xmlString = "<?xml version=\"1.0\" ?>\n"
                + "<foo>My name is Molly!</foo>";
        SimpleXmlParser xmlParser = new SimpleXmlParser();
        SimpleXmlParser.XmlValues xmlValues = xmlParser.parse(new ByteArrayInputStream(xmlString.getBytes(Charset.forName("UTF-8"))),
                new String[]{"foo"},
                null
        );
        ArrayList<String> foundTag = xmlValues.foundTag;
        ArrayList<String[]> foundAttributes = xmlValues.foundAttributes;
        if (!foundAttributes.isEmpty())
        {
            throw new RuntimeException("attributes should be empty");
        }
        if (foundTag.size() != 1
                || !foundTag.get(0).equals("My name is Molly!"))
        {
            throw new RuntimeException("tag not found");
        }
    }

    /**
     * @throws Exception
     */
    @Test
    public void testXmlParserAttribute() throws Exception
    {
        String xmlString = "<?xml version=\"1.0\" ?>"
                + "<options>"
                + "  <foo>My name is Molly!</foo>"
                + "  <item name=\"log\" type=\"BOOL\" num=\"1\" value=\"true\" help=\"user log normal distribution\" />"
                + "  <item name=\"prior\" type=\"BOOL\" num=\"1\" value=\"false\" help=\"use prior probability\" />"
                + "</options>";
        SimpleXmlParser xmlParser = new SimpleXmlParser();
        SimpleXmlParser.XmlValues xmlValues = xmlParser.parse(new ByteArrayInputStream(xmlString.getBytes(Charset.forName("UTF-8"))),
                new String[]{"options", "item"},
                new String[]{"name", "value"}
        );
        ArrayList<String> foundTag = xmlValues.foundTag;
        ArrayList<String[]> foundAttributes = xmlValues.foundAttributes;
        if (!foundTag.isEmpty())
        {
            throw new RuntimeException("tag should be empty");
        }
        if (foundAttributes.size() != 2
                || !foundAttributes.get(0)[0].equals("log")
                || !foundAttributes.get(0)[1].equals("true")
                || !foundAttributes.get(1)[0].equals("prior")
                || !foundAttributes.get(1)[1].equals("false"))
        {
            throw new RuntimeException("attribute not found");
        }
    }

    /**
     * @param internalStorage boolean
     * @throws Exception
     */
    private void testWriteRead(boolean internalStorage) throws Exception
    {
        File dir = internalStorage ? getInstrumentation().getContext().getFilesDir()
                : Environment.getExternalStorageDirectory();
        String fileName = getClass().getSimpleName() + "." + getClass().getSimpleName();
        File fileHeader = new File(dir, fileName);
        //write
        buildPipeline(fileHeader, true);
        //read
        buildPipeline(fileHeader, false);
        //cleanup
        if (fileHeader.exists())
        {
            if (!fileHeader.delete())
            {
                throw new RuntimeException("Header file could not be deleted");
            }
            File fileReal = new File(dir, fileName + LoggingConstants.FILE_EXTENSION_STREAM);
            if (fileReal.exists())
            {
                if (!fileReal.delete())
                {
                    throw new RuntimeException("Real file could not be deleted");
                }
            }
        }
    }

    /**
     * @param file  File
     * @param write boolean
     * @throws Exception
     */
    private void buildPipeline(File file, boolean write) throws Exception
    {
        //setup
        Pipeline framework = Pipeline.getInstance();
        framework.options.bufferSize.set(10.0f);
        if (write)
        {
            write(framework, file);
        } else
        {
            read(framework, file);
        }
        //start framework
        framework.start();
        //run for two minutes
        long end = System.currentTimeMillis() + TestHelper.DUR_TEST_NORMAL;
        try
        {
            while (System.currentTimeMillis() < end)
            {
                Thread.sleep(1);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        framework.stop();
        framework.release();
    }

    /**
     * @param frame TheFramework
     * @param file  File
     * @throws Exception
     */
    private void write(Pipeline frame, File file) throws Exception
    {
        //sensor
        AndroidSensor sensorConnection = new AndroidSensor();
        sensorConnection.options.sensorType.set(SensorType.ACCELEROMETER);

        //channel
        AndroidSensorChannel sensorConnectionChannel = new AndroidSensorChannel();
        frame.addSensor(sensorConnection,sensorConnectionChannel);
        //consumer
        FileWriter fileWriter = new FileWriter();
        fileWriter.options.filePath.set(file.getParent());
        fileWriter.options.fileName.set(file.getName());
        frame.addConsumer(fileWriter, sensorConnectionChannel, 0.25, 0);
    }

    /**
     * @param frame TheFramework
     * @param file  File
     * @throws Exception
     */
    private void read(Pipeline frame, File file) throws Exception
    {
        //sensor
        FileReader fileReader = new FileReader();
        fileReader.options.filePath.set(file.getParent());
        fileReader.options.fileName.set(file.getName());
        fileReader.options.loop.set(true);

        //channel
        FileReaderChannel fileReaderChannel = new FileReaderChannel();
        frame.addSensor(fileReader, fileReaderChannel);
        //logger
        Logger log = new Logger();
        frame.addConsumer(log, fileReaderChannel, 0.25, 0);
    }
}