/*
 * AudioUtil.java
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

package hcm.ssj.audio;

/**
 * Created by Ionut Damian on 21.12.2017.
 */
public class AudioUtil
{
	/**
	 * Modified Bessel function I0. Abramowicz and Stegun, p. 378.
	 *
	 * Based on code from the PRAAT Toolbox by Paul Boersma and David Weenink.
	 * http://www.fon.hum.uva.nl/praat/
	 *
	 * @param x Input
	 * @return Output
	 */
	public static double bessel_i0_f(double x)
	{
		if (x < 0.0) return bessel_i0_f(-x);
		if (x < 3.75)
		{
            /* Formula 9.8.1. Accuracy 1.6e-7. */
			double t = x / 3.75;
			t *= t;
			return 1.0 + t * (3.5156229 + t * (3.0899424 + t * (1.2067492
					+ t * (0.2659732 + t * (0.0360768 + t * 0.0045813)))));
		}
        /*
            otherwise: x >= 3.75
        */
        /* Formula 9.8.2. Accuracy of the polynomial factor 1.9e-7. */
		double t = 3.75 / x;   /* <= 1.0 */
		return Math.exp(x) / Math.sqrt(x) * (0.39894228 + t * (0.01328592
				+ t * (0.00225319 + t * (-0.00157565 + t * (0.00916281
				+ t * (-0.02057706 + t * (0.02635537 + t * (-0.01647633
				+ t * 0.00392377))))))));
	}
}
