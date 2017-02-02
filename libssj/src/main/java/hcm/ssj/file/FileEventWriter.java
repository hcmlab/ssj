/*
 * FileEventWriter.java
 * Copyright (c) 2017
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

package hcm.ssj.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;

import static hcm.ssj.file.LoggingConstants.FILE_EXTENSION_ANNO_PLAIN;
import static hcm.ssj.file.LoggingConstants.FILE_EXTENSION_EVENT;

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
    int _evID[];

    ArrayList<Event> unprocessedEvents = new ArrayList<>();

    private File file;
    private FileOutputStream fileOutputStream = null;

    public FileEventWriter()
    {
        _name = "FileEventWriter";
    }

    @Override
    public void enter()
    {
        if(_evchannel_in == null || _evchannel_in.size() == 0)
            throw new RuntimeException("no incoming event channels defined");

        _buffer = new byte[Cons.MAX_EVENT_SIZE];
        _evID = new int[_evchannel_in.size()];
        Arrays.fill(_evID, 0);
        _builder.delete(0, _builder.length());

        //create file
        if (options.filePath.get() == null)
        {
            Log.w("file path not set, setting to default " + LoggingConstants.SSJ_EXTERNAL_STORAGE);
            options.filePath.set(LoggingConstants.SSJ_EXTERNAL_STORAGE);
        }

        File fileDirectory = new File(options.filePath.parseWildcards());
        if (!fileDirectory.exists())
        {
            if (!fileDirectory.mkdirs())
            {
                Log.e(fileDirectory.getName() + " could not be created");
                return;
            }
        }
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

        //write header
        if(options.format.get() == Format.EVENT) {
            _builder.delete(0, _builder.length());
            _builder.append("<events fw=\"ssj\" v=\"");
            _builder.append(_frame.getVersion());
            _builder.append("\">");

            writeLine(_builder.toString(), fileOutputStream);
        }

        unprocessedEvents.clear();
    }

    @Override
    protected void process()
    {
        _builder.delete(0, _builder.length());


        for(int i = 0; i < _evchannel_in.size(); ++i)
        {
            Event ev = _evchannel_in.get(i).getEvent(_evID[i], false);
            if (ev == null)
                continue;

            _evID[i] = ev.id + 1;

            if(options.format.get() == Format.EVENT) {
                //build event
                _builder.append("<event sender=\"").append(ev.sender).append("\"");
                _builder.append(" event=\"").append(ev.name).append("\"");
                _builder.append(" from=\"").append(ev.time).append("\"");
                _builder.append(" dur=\"").append(ev.dur).append("\"");
                _builder.append(" prob=\"1.00000\"");
                _builder.append(" type=\"STRING\"");
                _builder.append(" type=\"").append(ev.state).append("\"");
                _builder.append(" glue=\"0\">");
                _builder.append(LoggingConstants.DELIMITER_LINE);

                switch(ev.type)
                {
                    case BYTE:
                        for(byte v : ev.ptrB())
                            _builder.append(v).append(" ");
                        break;

                    case SHORT:
                        for(short v : ev.ptrShort())
                            _builder.append(v).append(" ");
                        break;

                    case INT:
                        for(int v : ev.ptrI())
                            _builder.append(v).append(" ");
                        break;

                    case LONG:
                        for(long v : ev.ptrL())
                            _builder.append(v).append(" ");
                        break;

                    case FLOAT:
                        for(float v : ev.ptrF())
                            _builder.append(v).append(" ");
                        break;

                    case DOUBLE:
                        for(double v : ev.ptrD())
                            _builder.append(v).append(" ");
                        break;

                    case STRING:
                        _builder.append(ev.ptrStr());
                        break;
                }
                _builder.append(LoggingConstants.DELIMITER_LINE);

                _builder.append("</event>");
                _builder.append(LoggingConstants.DELIMITER_LINE);

                write(_builder.toString(), fileOutputStream);
            }
            else if(options.format.get() == Format.ANNO_PLAIN)
            {
                if(ev.state == Event.State.CONTINUED) {
                    unprocessedEvents.add(ev);
                }
                else
                {
                    //search for event start
                    Event start = null;
                    for(int j = 0; i < unprocessedEvents.size(); i++)
                    {
                        if(unprocessedEvents.get(i).name.equals(ev.name))
                        {
                            start = unprocessedEvents.get(i);
                            unprocessedEvents.remove(i);
                            break;
                        }
                    }

                    double to = (ev.time + ev.dur) / 1000.0;
                    double from = (start != null) ? start.time / 1000.0 : to;
                    _builder.append(from).append(" ").append(to).append(" ").append(ev.name);

                    writeLine(_builder.toString(), fileOutputStream);
                }
            }
        }
    }

    public void flush()
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
                line += LoggingConstants.DELIMITER_LINE;
                stream.write(line.getBytes());
            } catch (IOException e)
            {
                Log.e("could not write line");
            }
        }
    }
}
