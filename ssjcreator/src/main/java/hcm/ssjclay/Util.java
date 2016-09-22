/*
 * Util.java
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

package hcm.ssjclay;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;

import hcm.ssj.core.Log;
import hcm.ssj.core.TheFramework;

/**
 * Utility class.<br>
 * Created by Frank Gaibler on 15.09.2016.
 */
public abstract class Util
{
    public final static String DIR_1 = "SSJ", DIR_2 = "Creator", SUFFIX = ".xml", DEMO = "demo";

    /**
     *
     */
    private Util()
    {
    }

    /**
     * @param folder String
     * @param name   String
     * @return File
     */
    public static File getFile(String folder, String name)
    {
        File parent = getDirectory(folder);
        if (!parent.exists())
        {
            parent.mkdirs();
        }
        return new File(parent, name);
    }

    /**
     * @param file    File
     * @param content String
     */
    public static synchronized void appendFile(File file, String content)
    {
        try (PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file, true))))
        {
            printWriter.print(content);
        } catch (IOException e)
        {
            e.printStackTrace();
            Log.e("error in appendFile(): " + file.getName());
        }
    }

    /**
     * @param dirName String
     * @return boolean
     */
    public static synchronized boolean deleteDirectory(String dirName)
    {
        File file = getDirectory(dirName);
        return file.exists() && deleteRecursive(file);
    }

    /**
     * Java can't delete non-empty directories
     *
     * @param file File
     * @return boolean
     */
    private static boolean deleteRecursive(File file)
    {
        boolean ret = true;
        if (file.isDirectory())
        {
            for (File f : file.listFiles())
            {
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && file.delete();
    }

    /**
     * @param dirName String
     * @return File
     */
    public static File getDirectory(String dirName)
    {
        File file = new File(Environment.getExternalStorageDirectory(), dirName);
        if (!file.exists())
        {
            file.mkdirs();
        }
        return file;
    }

    /**
     * @param dirName String
     * @return File[]
     */
    public static File[] getFilesInDirectory(String dirName)
    {
        return getDirectory(dirName).listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File current, String name)
            {
                return new File(current, name).isDirectory();
            }
        });
    }

    /**
     * @return double
     */
    public static double getAnnotationTime()
    {
        return TheFramework.getFramework().getTime();
    }
}
