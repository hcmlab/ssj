/*
 * LoggingConstants.java
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

import android.os.Build;
import android.os.Environment;

import java.io.File;

import hcm.ssj.core.SSJApplication;

/**
 * Constants used for the file operations<br>
 * Created by Frank Gaibler on 31.08.2015.
 */
public class FileCons
{
    public static final String DELIMITER_ATTRIBUTE = " ";
    public static final String DELIMITER_LINE = "\r\n"; //works on android and windows, System.getProperty("line.separator") might not
    public static final String TAG_DATA_FILE = "~";
    public static final String FILE_EXTENSION_STREAM = "stream";
    public static final String FILE_EXTENSION_EVENT = "events";
    public static final String FILE_EXTENSION_ANNO_PLAIN = "anno";
    public static final String SSJ_EXTERNAL_STORAGE = new File(Environment.getExternalStorageDirectory(), "SSJ").getPath();
    public static final String DOWNLOAD_DIR = SSJ_EXTERNAL_STORAGE + File.separator + "download";
    public static final String INTERNAL_LIB_DIR = SSJApplication.getAppContext().getApplicationInfo().nativeLibraryDir + File.separator; //getFilesDir().toString() + "/lib";
    public static final String REMOTE_LIB_PATH = "https://hcm-lab.de/downloads/ssj/lib/" + Build.CPU_ABI;
}
