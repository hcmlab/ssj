/*
 * SimpleFileWriter.java
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

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.stream.Stream;

/**
 * File writer for SSJ.<br>
 * Created by Frank Gaibler on 20.08.2015.
 */
public class SimpleFileWriter extends Consumer
{
    /**
     *
     */
    public class Options
    {
        public File file = null;
        public String separator = LoggingConstants.DELIMITER_ATTRIBUTE;
    }

    public Options options = new Options();
    private FileOutputStream fileOutputStream = null;
    private FileOutputStream fileOutputStreamHeader = null;
    private int lineCount = 0;
    private SimpleHeader simpleHeader;

    public SimpleFileWriter()
    {
        _name = "SSJ_consumer_" + this.getClass().getSimpleName();
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void enter(Stream[] stream_in)
    {
        if (stream_in.length > 1 || stream_in.length < 1)
        {
            Log.e(_name, "stream count not supported");
            return;
        }
        if (stream_in[0].type == Cons.Type.CUSTOM || stream_in[0].type == Cons.Type.UNDEF)
        {
            Log.e(_name, "stream type not supported");
            return;
        }
        start(stream_in[0]);
    }

    /**
     * @param stream Stream
     */
    private void start(Stream stream)
    {
        File fileHeader = options.file;
        File fileReal;
        String path = fileHeader.getPath();

        if (path.endsWith("." + LoggingConstants.FILE_EXTENSION))
        {
            fileReal = new File(path + LoggingConstants.TAG_DATA_FILE);
        }
        else if (path.endsWith("." + LoggingConstants.FILE_EXTENSION + LoggingConstants.TAG_DATA_FILE))
        {
            fileReal = fileHeader;
            fileHeader = new File(path.substring(0, path.length() - 1));
        } else
        {
            fileHeader = new File(path + "." + LoggingConstants.FILE_EXTENSION);
            fileReal = new File(path + "." + LoggingConstants.FILE_EXTENSION + LoggingConstants.TAG_DATA_FILE);
        }

        fileOutputStreamHeader = getFileConnection(fileHeader, fileOutputStreamHeader);
        simpleHeader = new SimpleHeader();
        simpleHeader._sr = String.valueOf(stream.sr);
        simpleHeader._dim = String.valueOf(stream.dim);
        simpleHeader._byte = String.valueOf(stream.bytes);
        simpleHeader._type = stream.type.name();
        simpleHeader._from = String.valueOf(stream.time);
        writeLine(simpleHeader.getLine1(), fileOutputStreamHeader);
        writeLine(simpleHeader.getLine2(), fileOutputStreamHeader);
        writeLine(simpleHeader.getLine3(), fileOutputStreamHeader);
        lineCount = 0;
        fileOutputStream = getFileConnection(fileReal, fileOutputStream);
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    protected final void consume(Stream[] stream_in)
    {
        switch (stream_in[0].type)
        {
            case BOOL:
            {
                boolean[] in = stream_in[0].ptrBool();
                for (int i = 0, j = 0; i < stream_in[0].num; i++)
                {
                    String line = "";
                    for (int k = 0; k < stream_in[0].dim; k++, j++)
                    {
                        line += in[j] + options.separator;
                    }
                    writeLine(line, fileOutputStream);
                }
                break;
            }
            case BYTE:
            {
                byte[] in = stream_in[0].ptrB();
                for (int i = 0, j = 0; i < stream_in[0].num; i++)
                {
                    String line = "";
                    for (int k = 0; k < stream_in[0].dim; k++, j++)
                    {
                        line += in[j] + options.separator;
                    }
                    writeLine(line, fileOutputStream);
                }
                break;
            }
            case CHAR:
            {
                char[] in = stream_in[0].ptrC();
                for (int i = 0, j = 0; i < stream_in[0].num; i++)
                {
                    String line = "";
                    for (int k = 0; k < stream_in[0].dim; k++, j++)
                    {
                        line += in[j] + options.separator;
                    }
                    writeLine(line, fileOutputStream);
                }
                break;
            }
            case SHORT:
            {
                short[] in = stream_in[0].ptrS();
                for (int i = 0, j = 0; i < stream_in[0].num; i++)
                {
                    String line = "";
                    for (int k = 0; k < stream_in[0].dim; k++, j++)
                    {
                        line += in[j] + options.separator;
                    }
                    writeLine(line, fileOutputStream);
                }
                break;
            }
            case INT:
            {
                int[] in = stream_in[0].ptrI();
                for (int i = 0, j = 0; i < stream_in[0].num; i++)
                {
                    String line = "";
                    for (int k = 0; k < stream_in[0].dim; k++, j++)
                    {
                        line += in[j] + options.separator;
                    }
                    writeLine(line, fileOutputStream);
                }
                break;
            }
            case LONG:
            {
                long[] in = stream_in[0].ptrL();
                for (int i = 0, j = 0; i < stream_in[0].num; i++)
                {
                    String line = "";
                    for (int k = 0; k < stream_in[0].dim; k++, j++)
                    {
                        line += in[j] + options.separator;
                    }
                    writeLine(line, fileOutputStream);
                }
                break;
            }
            case FLOAT:
            {
                float[] in = stream_in[0].ptrF();
                for (int i = 0, j = 0; i < stream_in[0].num; i++)
                {
                    String line = "";
                    for (int k = 0; k < stream_in[0].dim; k++, j++)
                    {
                        line += in[j] + options.separator;
                    }
                    writeLine(line, fileOutputStream);
                }
                break;
            }
            case DOUBLE:
            {
                double[] in = stream_in[0].ptrD();
                for (int i = 0, j = 0; i < stream_in[0].num; i++)
                {
                    String line = "";
                    for (int k = 0; k < stream_in[0].dim; k++, j++)
                    {
                        line += in[j] + options.separator;
                    }
                    writeLine(line, fileOutputStream);
                }
                break;
            }
            default:
                Log.w(_name, "unsupported data type");
                break;
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[])
    {
        end(stream_in[0]);
    }

    /**
     * @param stream Stream
     */
    private void end(Stream stream)
    {
        fileOutputStream = closeStream(fileOutputStream);
        simpleHeader._num = String.valueOf(lineCount);
        simpleHeader._to = String.valueOf(stream.time);
        writeLine(simpleHeader.getLine4(), fileOutputStreamHeader);
        writeLine(simpleHeader.getLine5(), fileOutputStreamHeader);
        writeLine(simpleHeader.getLine6(), fileOutputStreamHeader);
        fileOutputStreamHeader = closeStream(fileOutputStreamHeader);
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
                Log.e(_name, "could not close writer");
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
            Log.e(_name, "file not found");
        }
        return stream;
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
                lineCount++;
            } catch (IOException e)
            {
                Log.e(_name, "could not write line");
            }
        }
    }
}
