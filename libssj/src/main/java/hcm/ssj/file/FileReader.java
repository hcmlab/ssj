/*
 * FileReader.java
 * Copyright (c) 2018
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


import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.FilePath;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * File reader for SSJ.<br>
 * Created by Frank Gaibler on 20.08.2015.
 */
public class FileReader extends Sensor
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	/**
     *
     */
    public class Options extends OptionList
    {
        public final Option<FilePath> file = new Option<>("file", null, FilePath.class, "file path");
        public final Option<Boolean> loop = new Option<>("loop", true, Boolean.class, "");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    private File fileHeader;
    private File fileReal;
    private BufferedInputStream inputBinary = null;
    private BufferedReader inputASCII = null;
    private int pos;
    private SimpleHeader simpleHeader = null;
    private boolean initialized = false;

    /**
     *
     */
    public FileReader()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
     * Initialize reader
     *
     * @throws IOException IO Exception
     */
    protected final void readerInit() throws IOException
    {
        if (!initialized)
        {
            initialized = true;

            if (options.file.get() == null)
            {
                throw new IOException("file not specified");
            }

            this.fileHeader = new File(options.file.get().value);
            setFiles();
        }
    }

    /**
	 *
     */
    @Override
    protected boolean connect() throws SSJFatalException
    {
        try
        {
            readerInit();
            simpleHeader = getSimpleHeader();
        }
        catch (IOException | XmlPullParserException e)
        {
            throw new SSJFatalException("unable to initialize file reader", e);
        }

		if (simpleHeader._ftype.equals("BINARY"))
		{
			inputBinary = getFileConnection(fileReal, inputBinary);
		}
		else if (simpleHeader._ftype.equals("ASCII"))
		{
			inputASCII = getFileConnection(fileReal, inputASCII);
		}

        pos = 0;
        return true;
    }

    /**
     * Get simple header
     *
     * @return SimpleHeader
     * @throws IOException IO Exception
     * @throws XmlPullParserException XML Exception
     */
    protected SimpleHeader getSimpleHeader() throws IOException, XmlPullParserException
    {
        if (simpleHeader == null)
        {
            SimpleXmlParser simpleXmlParser = new SimpleXmlParser();
            SimpleXmlParser.XmlValues xmlValues = simpleXmlParser.parse(
                    new FileInputStream(fileHeader),
                    new String[]{"stream", "info"},
                    new String[]{"ftype", "sr", "dim", "byte", "type"}
            );
            simpleHeader = new SimpleHeader();
            simpleHeader._ftype = xmlValues.foundAttributes.get(0)[0];
            simpleHeader._sr = xmlValues.foundAttributes.get(0)[1];
            simpleHeader._dim = xmlValues.foundAttributes.get(0)[2];
            simpleHeader._byte = xmlValues.foundAttributes.get(0)[3];
            simpleHeader._type = xmlValues.foundAttributes.get(0)[4];
            xmlValues = simpleXmlParser.parse(
                    new FileInputStream(fileHeader),
                    new String[]{"stream", "chunk"},
                    new String[]{"from", "to", "num"}
            );
            simpleHeader._from = xmlValues.foundAttributes.get(0)[0];
            simpleHeader._to = xmlValues.foundAttributes.get(0)[1];
            simpleHeader._num = xmlValues.foundAttributes.get(0)[2];
        }
        return simpleHeader;
    }

    /**
     *
     */
    private void setFiles()
    {
        String path = fileHeader.getPath();
        if (path.endsWith(FileCons.TAG_DATA_FILE))
        {
            fileReal = fileHeader;
            fileHeader = new File(path.substring(0, path.length() - 1));
        } else if (fileHeader.getName().contains("."))
        {
            fileReal = new File(path + FileCons.TAG_DATA_FILE);
        } else
        {
            fileHeader = new File(path + "." + FileCons.FILE_EXTENSION_STREAM);
            fileReal = new File(path + "." + FileCons.FILE_EXTENSION_STREAM + FileCons.TAG_DATA_FILE);
        }
    }

    /**
     * @param reader BufferedReader
     * @return BufferedReader
     */
    private BufferedInputStream closeStream(BufferedInputStream reader)
    {
        if (reader != null)
        {
            try
            {
                reader.close();
                reader = null;
            } catch (IOException e)
            {
                Log.e("could not close reader", e);
            }
        }
        return reader;
    }

    /**
     * @param reader BufferedReader
     * @return BufferedReader
     */
    private BufferedReader closeStream(BufferedReader reader)
    {
        if (reader != null)
        {
            try
            {
                reader.close();
                reader = null;
            } catch (IOException e)
            {
                Log.e("could not close reader", e);
            }
        }
        return reader;
    }

    /**
	 *
     */
    @Override
    protected void disconnect() throws SSJFatalException
    {
        inputBinary = closeStream(inputBinary);
        inputASCII = closeStream(inputASCII);
        initialized = false;
    }

    /**
     * @param file   File
     * @param stream BufferedInputStream
     * @return BufferedInputStream
     */
    private BufferedInputStream getFileConnection(File file, BufferedInputStream stream)
    {
        if (stream != null)
        {
            stream = closeStream(stream);
        }
        try
        {
            InputStream inputStream = new FileInputStream(file);
            stream = new BufferedInputStream(inputStream);
        } catch (FileNotFoundException e)
        {
            Log.e("file not found", e);
        }
        return stream;
    }

    /**
     * @param file   File
     * @param reader BufferedReader
     * @return BufferedReader
     */
    private BufferedReader getFileConnection(File file, BufferedReader reader)
    {
        if (reader != null)
        {
            reader = closeStream(reader);
        }
        try
        {
            InputStream inputStream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(inputStream));

        } catch (FileNotFoundException e)
        {
            Log.e("file not found", e);
        }
        return reader;
    }

    /**
     * @param reader BufferedReader
     * @return String
     */
    private String readLine(BufferedReader reader)
    {
        String line = null;
        if (reader != null)
        {
            try
            {
                line = reader.readLine();
            } catch (IOException e)
            {
                Log.e("could not read line", e);
            }
        }
        return line;
    }

    /**
     * @param stream BufferedInputStream
     * @return String
     */
    private int read(BufferedInputStream stream, byte[] buffer, int numBytes)
    {
        int ret = 0;
        if (stream != null)
        {
            try
            {
                ret = stream.read(buffer, 0, numBytes);
            } catch (IOException e)
            {
                Log.e("could not read line", e);
            }
        }
        return ret;
    }

    /**
        * @return String
    */
    protected String getDataASCII()
    {

        String data = readLine(inputASCII);
        if (data == null && options.loop.get())
        {
            Log.d("end of file reached, looping");
            inputASCII = getFileConnection(fileReal, inputASCII);
            data = readLine(inputASCII);
        }
        return data;
    }


    protected int getDataBinary(byte[] buffer, int numBytes)
    {
        int ret = read(inputBinary, buffer, numBytes);
        if(ret == -1 && options.loop.get())
        {
            Log.d("end of file reached, looping");
            inputBinary = getFileConnection(fileReal, inputBinary);
            ret = read(inputBinary, buffer, numBytes);

            if(ret <= 0)
                Log.e("unexpected error reading from file");
        }

        if(numBytes != ret)
            Log.e("unexpected amount of bytes read from file");

        return ret;
    }

    protected void skip(int bytes)
    {
        try {
            inputBinary.skip(bytes);
        } catch (IOException e) {
            Log.e("exception while skipping bytes", e);
        }
    }
}
