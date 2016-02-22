/*
 * Sound_to_Intensity.java
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

/*
 * The following source code has been taken from the PRAAT framework and converted to java
 */

/*
 * pb 2002/07/16 GPL
 * pb 2003/05/20 default time step is four times oversampling
 * pb 2003/07/10 NUMbessel_i0_f
 * pb 2003/11/19 Sound_to_Intensity veryAccurate
 * pb 2003/12/15 removed bug introduced by previous change
 * pb 2004/10/27 subtractMean
 * pb 2006/12/31 compatible with stereo sounds
 * pb 2007/01/27 for stereo sounds, add channel energies
 * pb 2007/02/14 honoured precondition of Sampled_shortTermAnalysis (by checking whether minimumPitch is defined)
 * pb 2008/01/19 double
 * pb 2011/03/04 C++
 * pb 2011/03/28 C++
 */

package hcm.ssj.praat.helper;

import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 03.06.2015.
Function:
    smooth away the periodic part of a signal,
    by convolving the square of the signal with a Kaiser(20.24) window;
    and resample on original sample points.
Arguments:
    'minimumPitch':
        the minimum periodicity frequency that will be smoothed away
        to at most 0.00001 %.
        The Hanning/Hamming-equivalent window length will be 3.2 / 'minimumPitch'.
        The actual window length will be twice that.
    'timeStep':
        if <= 0.0, then 0.8 / minimumPitch.
Performance:
    every periodicity frequency greater than 'minimumPitch'
    will be smoothed away to at most 0.00001 %;
    if 'timeStep' is 0 or less than 3.2 / 'minimumPitch',
    aliased frequencies will be at least 140 dB down.
Example:
    minimumPitch = 100 Hz;
    Hanning/Hanning-equivalent window duration = 32 ms;
    actual window duration = 64 ms;
*/
public class Sound_to_Intensity
{
    static public class ShortTermAnalysis
    {
        public int numberOfFrames;
        public double firstTime;
    }

    /**
     *
     * @param me array with audio samples
     * @param minimumPitch the minimum periodicity frequency that will be smoothed away to at most 0.00001 %. The Hanning/Hamming-equivalent window length will be 3.2 / 'minimumPitch'. The actual window length will be twice that.
     * @param timeStep if <= 0.0, then 0.8 / minimumPitch.
     * @param subtractMeanPressure
     * @return intensity of the sound in DB
     */
    public static double[] compute (Stream me, double minimumPitch, double timeStep, boolean subtractMeanPressure) {

        /*
        * Preconditions.
        */
        double dx = 1.0 / me.sr;
        double myDuration = dx * me.num;
        double xmin = me.time;
        double xmax = me.time + myDuration;

        if (timeStep < 0.0) throw new IllegalArgumentException("(Sound-to-Intensity:) Time step should be zero or positive instead of " + timeStep + ".");
        if (minimumPitch <= 0.0) throw new IllegalArgumentException("(Sound-to-Intensity:) Minimum pitch should be positive.");
        if (dx <= 0.0) throw new IllegalArgumentException ("(Sound-to-Intensity:) The Sound's time step should be positive.");
        /*
        * Defaults.
        */
        if (timeStep == 0.0) timeStep = 0.8 / minimumPitch;   // default: four times oversampling Hanning-wise

        double windowDuration = getWindowDuration(minimumPitch);
        if (windowDuration <= 0.0) throw new AssertionError();

        double halfWindowDuration = 0.5 * windowDuration;
        int halfWindowSamples = (int)(halfWindowDuration / dx);
        double[] amplitude = new double[2 * halfWindowSamples +1];
        double[] window = new double[2 * halfWindowSamples +1];

        for (int i = - halfWindowSamples; i <= halfWindowSamples; i ++) {
            double x = i * dx / halfWindowDuration, root = 1 - x * x;
            window [i + halfWindowSamples] = root <= 0.0 ? 0.0 : NUM.bessel_i0_f((2 * NUM.pi * NUM.pi + 0.5) * Math.sqrt(root));
        }

        ShortTermAnalysis analysis = null;
        try {
            analysis = Sampled_shortTermAnalysis(me, windowDuration, timeStep);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The duration of the sound in an intensity analysis should be at least 6.4 divided by the minimum pitch (" +  minimumPitch + " Hz), " +
                          "i.e. at least " + 6.4 / minimumPitch + " s, instead of " + (xmax - xmin) + " s.");
        }

        float[] data = me.ptrF();
        double[] out = new double[analysis.numberOfFrames];

        double out_dx = myDuration / analysis.numberOfFrames;

        for (int iframe = 0; iframe < analysis.numberOfFrames; iframe++) {
            double midTime = xmin + iframe * out_dx;
            int midSample = (int)(Math.round((midTime - xmin) / dx + 1.0));
            int leftSample = midSample - halfWindowSamples, rightSample = midSample + halfWindowSamples;
            double sumxw = 0.0, sumw = 0.0, intensity;
            if (leftSample < 0) leftSample = 0;
            if (rightSample >= me.num) rightSample = me.num -1;

            for (int channel = 0; channel < me.dim; channel ++) {
                for (int i = leftSample; i <= rightSample; i ++) {
                    amplitude [i - midSample + halfWindowSamples] = data[i * me.dim + channel];
                }
                if (subtractMeanPressure) {
                    double sum = 0.0;
                    for (int i = leftSample; i <= rightSample; i ++) {
                        sum += amplitude [i - midSample + halfWindowSamples];
                    }
                    double mean = sum / (rightSample - leftSample +1);
                    for (int i = leftSample; i <= rightSample; i ++) {
                        amplitude [i - midSample + halfWindowSamples] -= mean;
                    }
                }
                for (int i = leftSample; i <= rightSample; i ++) {
                    sumxw += amplitude [i - midSample + halfWindowSamples] * amplitude [i - midSample + halfWindowSamples] * window [i - midSample + halfWindowSamples];
                    sumw += window [i - midSample + halfWindowSamples];
                }
            }
            intensity = sumxw / sumw;
            if (intensity != 0.0) intensity /= 4e-10;
            out[iframe] = (float)(intensity < 1e-30 ? -300 : 10 * Math.log10(intensity));
        }
        return out;
    }

    public static ShortTermAnalysis Sampled_shortTermAnalysis (Stream me, double windowDuration, double timeStep)
    {
        if (windowDuration <= 0.0) throw new AssertionError();
        if (timeStep <= 0.0) throw new AssertionError();

        double dx = 1.0 / me.sr;
        double myDuration = dx * me.num;

        if (windowDuration > myDuration)
            throw new IllegalArgumentException("audio duration shorter than window length.");

        ShortTermAnalysis res = new ShortTermAnalysis();

        res.numberOfFrames = getSampleNumber(myDuration, windowDuration, timeStep);
        if (res.numberOfFrames < 1)  throw new AssertionError();

        double ourMidTime = me.time - 0.5 * dx + 0.5 * myDuration;
        double thyDuration = res.numberOfFrames * timeStep;

        res.firstTime = ourMidTime - 0.5 * thyDuration + 0.5 * timeStep;

        return res;
    }


    public static double getWindowDuration(double minimumPitch)
    {
        return 6.4 / minimumPitch;
    }

    public static int getSampleNumber(double frameDuration, double windowDuration, double timeStep)
    {
        return (int)(Math.floor ((frameDuration - windowDuration) / timeStep) + 1);
    }

//    public static double[] Sound_to_Intensity (Stream me, double minimumPitch, double timeStep, boolean subtractMeanPressure) {
//        boolean veryAccurate = false;
//        if (veryAccurate) {
//            autoSound up = Sound_upsample (me);   // because squaring doubles the frequency content, i.e. you get super-Nyquist components
//            autoIntensity thee = Sound_to_Intensity_ (up.peek(), minimumPitch, timeStep, subtractMeanPressure);
//            return thee.transfer();
//        } else {
//            autoIntensity thee = Sound_to_Intensity_ (me, minimumPitch, timeStep, subtractMeanPressure);
//            return thee.transfer();
//        }
//    }

//    IntensityTier Sound_to_IntensityTier (Sound me, double minimumPitch, double timeStep, int subtractMean) {
//        try {
//            autoIntensity intensity = Sound_to_Intensity (me, minimumPitch, timeStep, subtractMean);
//            autoIntensityTier thee = Intensity_downto_IntensityTier (intensity.peek());
//            return thee.transfer();
//        } catch (MelderError) {
//            Melder_throw (me, ": no IntensityTier created.");
//        }
//    }
}
