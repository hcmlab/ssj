/*
 * FileEventWriter.java
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

package hcm.ssj.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import hcm.ssj.core.Cons;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;

import static hcm.ssj.file.FileCons.FILE_EXTENSION_ANNO_PLAIN;
import static hcm.ssj.file.FileCons.FILE_EXTENSION_EVENT;

/**
 * writes events to file
 * Created by Johnny on 05.03.2015.
 */
public class FileEventWriter extends EventHandler implements IFileWriter
{
    public enum Format
    {
        EVENT,
        ANNO_PLAIN
    }

    public class Options extends IFileWriter.Options
    {
        public final Option<Format> format = new Option<>("format", Format.EVENT, Format.class, "format of event file");
    }
    public Options options = new Options();

    StringBuilder _builder = new StringBuilder();
    byte[] _buffer;

    ArrayList<Event> unprocessedEvents = new ArrayList<>();

    private File file;
    private FileOutputStream fileOutputStream = null;

    private boolean headerWritten = false;

    public FileEventWriter()
    {
        _name = "FileEventWriter";
    }

    @Override
	public void enter() throws SSJFatalException
    {
		if (_evchannel_in == null || _evchannel_in.size() == 0)
		{
			throw new RuntimeException("no incoming event channels defined");
		}

        _buffer = new byte[Cons.MAX_EVENT_SIZE];
        _builder.delete(0, _builder.length());

        //create file
        if (options.filePath.get() == null)
        {
            Log.w("file path not set, setting to default " + FileCons.SSJ_EXTERNAL_STORAGE);
            options.filePath.set(FileCons.SSJ_EXTERNAL_STORAGE);
        }
        File fileDirectory = Util.createDirectory(options.filePath.parseWildcards());

        if (options.fileName.get() == null)
        {
            String defaultName = "events";
            switch(options.format.get())
            {
                case EVENT:
                    defaultName += "." + FILE_EXTENSION_EVENT;
                    break;
                case ANNO_PLAIN:
                    defaultName += "." + FILE_EXTENSION_ANNO_PLAIN;
                    break;
            }

            Log.w("file name not set, setting to " + defaultName);
            options.fileName.set(defaultName);
        }
        file = new File(fileDirectory, options.fileName.get());
        fileOutputStream = getFileConnection(file, fileOutputStream);

        headerWritten = false;
        unprocessedEvents.clear();
    }

    @Override
    public synchronized void notify(Event event)
    {
        //write header
        if(!headerWritten && options.format.get() == Format.EVENT)
        {
            _builder.delete(0, _builder.length());
            _builder.append("<events ssi-v=\"2\" ssj-v=\"");
            _builder.append(_frame.getVersion());
            _builder.append("\">");
            _builder.append(FileCons.DELIMITER_LINE);

            SimpleDateFormat sdf = new SimpleDateFormat(SimpleHeader.DATE_FORMAT, Locale.getDefault());

            Date date = new Date(_frame.getStartTimeMs());
            String local = sdf.format(date);

            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String system = sdf.format(date);

            _builder.append("<time ms=\"").append(_frame.getStartTimeMs()).append("\" local=\"").append(local).append("\" system=\"").append(system).append("\"/>");
            _builder.append(FileCons.DELIMITER_LINE);

            write(_builder.toString(), fileOutputStream);
            headerWritten = true;
        }

        _builder.delete(0, _builder.length());

        if(options.format.get() == Format.EVENT)
        {
            Util.eventToXML(_builder, event);
            _builder.append(FileCons.DELIMITER_LINE);

            write(_builder.toString(), fileOutputStream);
        }
        else if(options.format.get() == Format.ANNO_PLAIN)
        {
            if(event.state == Event.State.CONTINUED) {
                unprocessedEvents.add(event);
            }
            else
            {
                //search for event start
                Event start = null;
                for(int j = 0; j < unprocessedEvents.size(); j++)
                {
                    if(unprocessedEvents.get(j).name.equals(event.name))
                    {
                        start = unprocessedEvents.get(j);
                        unprocessedEvents.remove(j);
                        break;
                    }
                }

                double to = (event.time + event.dur) / 1000.0;
                double from = (start != null) ? start.time / 1000.0 : event.time / 1000.0;
                _builder.append(from).append(" ").append(to).append(" ").append(event.name);

                writeLine(_builder.toString(), fileOutputStream);
            }
        }
    }

    public void flush() throws SSJFatalException
    {
        //write footer
        if(options.format.get() == Format.EVENT) {
            _builder.delete(0, _builder.length());
            _builder.append("</events>");
            writeLine(_builder.toString(), fileOutputStream);
        }

        fileOutputStream = closeStream(fileOutputStream);
    }


    /**
     * @param stream FileOutputStream
     * @return FileOutputStream
     */
    private FileOutputStream closeStream(FileOutputStream stream)
    {
        if (stream != null)
        {
            try
            {
                stream.close();
                stream = null;
            } catch (IOException e)
            {
                Log.e("could not close writer");
            }
        }
        return stream;
    }

    /**
     * @param file   File
     * @param stream FileOutputStream
     * @return FileOutputStream
     */
    private FileOutputStream getFileConnection(File file, FileOutputStream stream)
    {
        try
        {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e)
        {
            Log.e("file not found");
        }
        return stream;
    }

    /**
     * @param line   String
     * @param stream FileOutputStream
     */
    private void write(String line, FileOutputStream stream)
    {
        if (stream != null)
        {
            try
            {
                stream.write(line.getBytes());
            } catch (IOException e)
            {
                Log.e("could not write data");
            }
        }
    }

    /**
     * @param line   String
     * @param stream FileOutputStream
     */
    private void writeLine(String line, FileOutputStream stream)
    {
        if (stream != null)
        {
            try
            {
                line += FileCons.DELIMITER_LINE;
                stream.write(line.getBytes());
            } catch (IOException e)
            {
                Log.e("could not write line");
            }
        }
    }
}
