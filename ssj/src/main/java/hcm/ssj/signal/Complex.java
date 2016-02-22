/*
 * Complex.java
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

package hcm.ssj.signal;

/**
 * Created by Michael Dietz on 13.08.2015.
 */
public class Complex
{
	private double real;
	private double imag;

	public Complex(double real, double imaginary)
	{
		this.real = real;
		this.imag = imaginary;
	}

	public double real()
	{
		return this.real;
	}

	public double imag()
	{
		return this.imag;
	}

	public double mod()
	{
		return this.real == 0.0d && this.imag == 0.0d ? 0.0d : Math.sqrt(this.real * this.real + this.imag * this.imag);
	}

	public double arg()
	{
		return Math.atan2(this.imag, this.real);
	}

	public Complex conj()
	{
		return new Complex(this.real, -this.imag);
	}

	public Complex plus(Complex w)
	{
		return new Complex(this.real + w.real(), this.imag + w.imag());
	}

	public Complex minus(Complex w)
	{
		return new Complex(this.real - w.real(), this.imag - w.imag());
	}

	public Complex times(Complex w)
	{
		return new Complex(this.real * w.real() - this.imag * w.imag(), this.real * w.imag() + this.imag * w.real());
	}

	public Complex times(double factor)
	{
		return new Complex(this.real * factor, this.imag * factor);
	}

	public Complex div(Complex w)
	{
		double den = Math.pow(w.mod(), 2.0d);
		return new Complex((this.real * w.real() + this.imag * w.imag()) / den, (this.imag * w.real() - this.real * w.imag()) / den);
	}

	public Complex exp()
	{
		return new Complex(Math.exp(this.real) * Math.cos(this.imag), Math.exp(this.real) * Math.sin(this.imag));
	}

	public Complex log()
	{
		return new Complex(Math.log(this.mod()), this.arg());
	}

	public Complex sqrt()
	{
		double r = Math.sqrt(this.mod());
		double theta = this.arg() / 2.0d;
		return new Complex(r * Math.cos(theta), r * Math.sin(theta));
	}

	private double cosh(double theta)
	{
		return (Math.exp(theta) + Math.exp(-theta)) / 2.0d;
	}

	private double sinh(double theta)
	{
		return (Math.exp(theta) - Math.exp(-theta)) / 2.0d;
	}

	public Complex sin()
	{
		return new Complex(this.cosh(this.imag) * Math.sin(this.real), this.sinh(this.imag) * Math.cos(this.real));
	}

	public Complex cos()
	{
		return new Complex(this.cosh(this.imag) * Math.cos(this.real), -this.sinh(this.imag) * Math.sin(this.real));
	}

	public Complex sinh()
	{
		return new Complex(this.sinh(this.real) * Math.cos(this.imag), this.cosh(this.real) * Math.sin(this.imag));
	}

	public Complex cosh()
	{
		return new Complex(this.cosh(this.real) * Math.cos(this.imag), this.sinh(this.real) * Math.sin(this.imag));
	}

	public Complex tan()
	{
		return this.sin().div(this.cos());
	}

	public Complex chs()
	{
		return new Complex(-this.real, -this.imag);
	}

	public String toString()
	{
		return this.real != 0.0d && this.imag > 0.0d ? this.real + " + " + this.imag + "i" : (this.real != 0.0d && this.imag < 0.0d ? this.real + " - " + -this.imag + "i" : (this.imag == 0.0d ? String.valueOf(this.real) : (this.real == 0.0d ? this.imag + "i" : this.real + " + i*" + this.imag)));
	}
}