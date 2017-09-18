/*
 * SSITransformer.java
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

package hcm.ssj.mobileSSI;

import android.content.pm.ApplicationInfo;

import java.io.File;
import java.io.IOException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;

/**
 * File writer for SSJ.<br>
 * Created by Frank Gaibler on 20.08.2015.
 */
public class SSITransformer extends Transformer
{
    /**
     *
     */
    public class Options extends OptionList
    {
        public final Option<SSI.ObjectName> name = new Option<>("name", SSI.ObjectName.Mean, SSI.ObjectName.class, "name of the SSI transformer");
        public final Option<String> libdir = new Option<>("libdir", "", String.class, "location of SSI libraries (leave blank to download from remote server)");

        public final Option<String[]> ssioptions = new Option<>("ssioptions", null, String[].class, "options of the SSI transformer. Format: [name->value, name->value, ...]");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();

    private long ssi_object = 0;

    public SSITransformer()
    {
        _name = this.getClass().getSimpleName();
    }

    @Override
    public void init(double frame, double delta) throws SSJException
    {
        String path;
        ApplicationInfo info = SSJApplication.getAppContext().getApplicationInfo();

        if(options.libdir.get() != null &&
            (options.libdir.get().startsWith(info.nativeLibraryDir)|| options.libdir.get().startsWith(info.dataDir)))
        {
            //file is already in internal memory
            path = options.libdir.get();
        }
        else
        {
            //copy file to internal memory
            try
            {
                SSI.prepareLibrary(options.name.get().lib, options.libdir.get());
                path = FileCons.INTERNAL_LIB_DIR;
            }
            catch (IOException e)
            {
                throw new SSJException("error preparing library " + options.name.get().lib, e);
            }
        }

        ssi_object = SSI.create(options.name.get().toString(), options.name.get().lib, path + File.separator);
        if(ssi_object == 0)
            Log.e("error creating SSI transformer");

        //set options
        if(options.ssioptions.get() != null)
        {
            for (String option : options.ssioptions.get())
            {
                String[] pair = option.split("->");
                if (pair.length != 2)
                {
                    Log.w("misformed ssi option: " + option + ". Should be 'name->pair'.");
                    continue;
                }

                if (!SSI.setOption(ssi_object, pair[0], pair[1]))
                {
                    Log.w("unable to set option " + pair[0] + " to value " + pair[1]);
                }
            }
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void enter(Stream[] stream_in, Stream stream_out)
    {
        if(ssi_object > 0)
            SSI.transformEnter(ssi_object, stream_in[0], stream_out);
    }

    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        if(ssi_object > 0)
            SSI.transform(ssi_object, stream_in[0], stream_out);
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[], Stream stream_out)
    {
        if(ssi_object > 0)
            SSI.transformFlush(ssi_object, stream_in[0], stream_out);
    }

    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        if(ssi_object == 0)
        {
            Log.e("ssi interface not initialized");
            return 0;
        }

        return SSI.getSampleDimensionOut(ssi_object, stream_in[0].dim);
    }

    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        if(ssi_object == 0)
        {
            Log.e("ssi interface not initialized");
            return 0;
        }

        return SSI.getSampleBytesOut(ssi_object, stream_in[0].bytes);
    }

    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        if(ssi_object == 0)
        {
            Log.e("ssi interface not initialized");
            return null;
        }

        int type = SSI.getSampleTypeOut(ssi_object, stream_in[0].type);

        switch(type)
        {
            default: //unsigned and unknown
            case 0: //SSI_UNDEF
                return Cons.Type.UNDEF;
            case 1: //SSI_CHAR
                return Cons.Type.BYTE;
            case 3: //SSI_SHORT
                return Cons.Type.SHORT;
            case 5: //SSI_INT
                return Cons.Type.INT;
            case 7: //SSI_LONG
                return Cons.Type.LONG;
            case 9: //SSI_FLOAT
                return Cons.Type.FLOAT;
            case 10: //SSI_DOUBLE
                return Cons.Type.DOUBLE;
            case 13: //SSI_IMAGE
                return Cons.Type.IMAGE;
            case 14: //SSI_BOOL
                return Cons.Type.BOOL;
        }
    }

    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return SSI.getSampleNumberOut(ssi_object, sampleNumber_in);
    }

    @Override
    protected void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        stream_out.desc = new String[stream_out.dim];

        for(int i = 0; i < stream_out.dim; i++)
            stream_out.desc[0] ="SSI_" + options.name.get() + "_" + i;
    }
}
