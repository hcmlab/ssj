/*
 * DemoHandler.java
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

package hcm.ssj.creator.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import hcm.ssj.core.Log;
import hcm.ssj.file.FileUtils;

/**
 * Handle demo files.<br>
 * Created by Frank Gaibler on 22.09.2016.
 */
public abstract class DemoHandler
{
    /**
     *
     */
    private DemoHandler()
    {
    }

    /**
     * @param context Context
     */
    public static void copyFiles(Context context)
    {
        //in v0.7, the location of the pipelines changed
        copyFiles(null,
                  Environment.getExternalStorageDirectory() + File.separator + Util.SSJ + File.separator + Util.CREATOR,
                  Environment.getExternalStorageDirectory() + File.separator + Util.SSJ + File.separator + Util.CREATOR + File.separator + Util.PIPELINES,
                  new FilenameFilter() {
                      @Override
                      public boolean accept(File dir, String name)
                      {
                          if(name.endsWith(".xml") || name.endsWith(".layout"))
                              return true;
                          else
                              return false;
                      }
                  });

        AssetManager assetManager = context.getAssets();
        copyFiles(assetManager,
                  Util.DEMO,
                  Environment.getExternalStorageDirectory() + File.separator + Util.SSJ + File.separator + Util.CREATOR, null);
        copyFiles(assetManager,
                  Util.DEMO + File.separator + Util.RES,
                  Environment.getExternalStorageDirectory() + File.separator + Util.SSJ + File.separator + Util.CREATOR + File.separator + Util.RES, null);
        copyFiles(assetManager,
                  Util.DEMO + File.separator + Util.PIPELINES,
                  Environment.getExternalStorageDirectory() + File.separator + Util.SSJ + File.separator + Util.CREATOR + File.separator + Util.PIPELINES, null);
        copyFiles(assetManager,
                  Util.DEMO + File.separator + Util.STRATEGIES,
                  Environment.getExternalStorageDirectory() + File.separator + Util.SSJ + File.separator + Util.CREATOR + File.separator + Util.STRATEGIES, null);
    }

    public static void copyFiles(AssetManager assetManager, String src_dir, String dst_dir, FilenameFilter filter)
    {
        String[] filenames;
        try
        {
            if(assetManager != null)
            {
                filenames = assetManager.list(src_dir);
            }
            else
            {
                filenames = new File(src_dir).list(filter);
            }
        }
        catch (IOException e)
        {
            Log.e("error accessing folder: " + src_dir, e);
            return;
        }

        if(filenames == null || filenames.length == 0)
            return;

        for (String file : filenames)
        {
            try
            {
                InputStream in = null;
                if(assetManager != null)
                {
                    in = assetManager.open(src_dir + File.separator + file);
                }
                else
                {
                    in = new FileInputStream(new File(src_dir, file));
                }

                File dir = new File(dst_dir);
                if(!dir.exists())
                    dir.mkdirs();
                OutputStream out = new FileOutputStream(new File(dir, file));
                FileUtils.copyFile(in, out);
                in.close();
                out.flush();
                out.close();
            }
            catch (Exception e)
            {
                Log.i("skipping copying file: " + file);
            }
        }
    }
}
