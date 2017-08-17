/*
 * CameraUtil.java
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

package hcm.ssj.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.io.File;
import java.io.FileOutputStream;

import hcm.ssj.core.Log;
import hcm.ssj.file.LoggingConstants;

/**
 * Utility class for the camera. <br>
 * Created by Frank Gaibler on 26.01.2016.
 */
@SuppressWarnings("deprecation")
class CameraUtil
{
    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     *
     * @param mimeType String
     * @return MediaCodecInfo
     */
    public static MediaCodecInfo selectCodec(String mimeType)
    {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++)
        {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder())
            {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types)
            {
                if (type.equalsIgnoreCase(mimeType))
                {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Returns a color format that is supported by the codec and by this code.  If no
     * match is found, an exception is thrown.
     *
     * @param codecInfo MediaCodecInfo
     * @param mimeType  String
     * @return int
     */
    public static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType)
    {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++)
        {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat))
            {
                return colorFormat;
            }
        }
        Log.e("couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;   // not reached
    }

    /**
     * Returns true if this is a common color format.
     *
     * @param colorFormat int
     * @return boolean
     */
    private static boolean isRecognizedFormat(int colorFormat)
    {
        switch (colorFormat)
        {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYCrYCb:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    /**
     * Decodes YUVNV21 color space into a regular RGB format.
     *
     * @param argb Output array for RGB values.
     * @param yuv YUV byte data to decode.
     * @param width Width of image in pixels.
     * @param height Height of image in pixels.
     */
    public static void convertNV21ToRGB(byte[] argb, byte[] yuv, int width, int height) {
        final int frameSize = width * height;
        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) yuv[ci * width + cj]));
                int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
                int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
                int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

                argb[a++] = (byte)(r < 0 ? 0 : (r > 255 ? 255 : r)); // red
                argb[a++] = (byte)(g < 0 ? 0 : (g > 255 ? 255 : g)); // green
                argb[a++] = (byte)(b < 0 ? 0 : (b > 255 ? 255 : b)); // blue
            }
        }
    }

    /**
     * Decodes YUVNV21 color space into a regular RGB format.
     *
     * @param argb Output array for RGB values.
     * @param yuv YUV byte data to decode.
     * @param width Width of image in pixels.
     * @param height Height of image in pixels.
     */
    public static void convertNV21ToARGBInt(int[] argb, byte[] yuv, int width, int height) {
        final int frameSize = width * height;
        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) yuv[ci * width + cj]));
                int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
                int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
                int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

                 r = r < 0 ? 0 : (r > 255 ? 255 : r);
                 g = g < 0 ? 0 : (g > 255 ? 255 : g);
                 b = b < 0 ? 0 : (b > 255 ? 255 : b);

                 argb[a++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    /**
     * Saved bitmap to external storage.
     *
     * @param bitmap Bitmap to save.
     * @param filename Name of the file to be saved as.
     */
    public static void saveBitmap(final Bitmap bitmap, final String filename) {
        final String root =
                LoggingConstants.SSJ_EXTERNAL_STORAGE + File.separator + "tensorflow";
        final File myDir = new File(root);

        if (!myDir.mkdirs()) {
            Log.i("Make dir failed");
        }

        final String fname = filename;
        final File file = new File(myDir, fname);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            Log.e("tf_ssj", "Exception!");
        }
    }

    /**
     * Returns a transformation matrix from one reference frame into another.
     * Handles cropping (if maintaining aspect ratio is desired) and rotation.
     *
     * @param srcWidth Width of source frame.
     * @param srcHeight Height of source frame.
     * @param dstWidth Width of destination frame.
     * @param dstHeight Height of destination frame.
     * @param applyRotation Amount of rotation to apply from one frame to another.
     *  Must be a multiple of 90.
     * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
     * cropping the image if necessary.
     * @return The transformation fulfilling the desired requirements.
     */
    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

    /**
     * Converts RGB bytes to RGB ints.
     *
     * @param rgbBytes RGB color bytes.
     * @return RGB color integers.
     */
    public static int[] decodeBytes(byte[] rgbBytes, int width, int height)
    {
        int[] rgb = new int[width * height];

        for (int i = 0; i < width * height; i++)
        {
            int r = rgbBytes[i * 3];
            int g = rgbBytes[i * 3 + 1];
            int b = rgbBytes[i * 3 + 2];

            if (r < 0)
                r += 256;
            if (g < 0)
                g += 256;
            if (b < 0)
                b += 256;

            rgb[i] = 0xff000000 | (r << 16) | (g << 8) | b;
        }

        return rgb;
    }
}
