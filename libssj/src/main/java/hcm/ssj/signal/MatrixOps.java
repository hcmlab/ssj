/*
 * MatrixOps.java
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

package hcm.ssj.signal;

import hcm.ssj.core.Log;

/**
 * Created by Michael Dietz on 12.08.2015.
 */
public class MatrixOps
{
	private static MatrixOps _instance = null;

	/**
	 * Private constructor for singleton pattern.
	 */
	private MatrixOps()
	{
	}

	/**
	 * Method for accessing the singleton instance.
	 *
	 * @return singleton instance
	 */
	public static MatrixOps getInstance()
	{
		if (_instance == null)
		{
			_instance = new MatrixOps();
		}

		return _instance;
	}

	public Matrix<Float> array (float start, float delta, float end, Matrix.MATRIX_DIMENSION dimension)
	{
		int steps = (int) (((end - start) / (double)(delta)) + 1.001);
		Matrix<Float> matrix;

		if (steps <= 0) {
			matrix = new Matrix<> (0,0);
			return matrix;
		}

		switch (dimension) {
			case ROW:
				matrix = new Matrix<> (1, steps);
				break;
			case COL:
			default:
				matrix = new Matrix<> (steps, 1);
				break;
		}

		int dataptr = 0;
		matrix.setData(dataptr, start);

		for (int i = 0; i < steps-1; i++)
		{
			matrix.setData(dataptr+1, matrix.getData(dataptr) + delta);
			dataptr++;
		}

		return matrix;
	}

	public Complex multiplyVector(Matrix<Complex> vec1, Matrix<Complex> vec2)
	{
		Complex result = new Complex(0, 0);

		int len = vec1.getCols() * vec1.getRows();

		for (int i = 0; i < len; i++)
		{
			Complex c = vec1.getData(i).times(vec2.getData(i));
			result = result.plus(c);
		}

		return result;
	}

	public void plus (Matrix<Float> matrix, float scalar)
	{
		if (matrix.getData().isEmpty()) {
			return;
		}

		for (int i = 0; i < matrix.getSize(); i++) {
			matrix.setData(i, matrix.getData(i) + scalar);
		}
	}

	public void mult (Matrix<Float> matrix, float scalar)
	{
		if (matrix.getData().isEmpty()) {
			return;
		}

		for (int i = 0; i < matrix.getSize(); i++) {
			matrix.setData(i, matrix.getData(i) * scalar);
		}
	}

	public void mult(Matrix<Float> a, Matrix<Float> b)
	{
		if(a.getRows() != b.getRows() || a.getCols() != b.getCols())
		{
			Log.w("matrices not matching");
			return;
		}

		if(a.getData().isEmpty())
			return;

		for(int i = 0; i < a.getSize(); i++)
		{
			a.setData(i, a.getData(i) * b.getData(i));
		}
	}

	public void multM(Matrix<Float> a, Matrix<Float> b, Matrix<Float> dst)
	{
		if(a.getCols() != b.getRows() || dst.getRows() != a.getRows() || dst.getCols() != b.getCols())
		{
			Log.w("matrices not matching");
			return;
		}

		dst.fillValue(0f);

		int aptr = 0;
		int bptr = 0;
		int dstptr = 0;
		int dstptr2 = 0;

		for (int i = 0; i < a.getRows(); i++) {
			bptr = 0;
			for (int j = 0; j < b.getRows(); j++) {
				dstptr2 = dstptr;
				for (int k = 0; k < b.getCols(); k++) {
					dst.setData(dstptr2, dst.getData(dstptr2) + b.getData(bptr) * a.getData(aptr));
					bptr++;
					dstptr2++;
				}
				aptr++;
			}
			dstptr = dstptr2;
		}
	}

	public void div(Matrix<Float> a, Matrix<Float> b)
	{
		if(a.getRows() != b.getRows() || a.getCols() != b.getCols())
		{
			Log.w("matrices not matching");
			return;
		}

		if(a.getData().isEmpty())
			return;

		int elements = a.getRows() * a.getCols();
		for(int i = 0; i < elements; i++)
		{
			a.setData(i, a.getData(i) / b.getData(i));
		}
	}

	public void div (Matrix<Float> matrix, float scalar)
	{
		if (matrix.getData().isEmpty()) {
			return;
		}

		int elems  = matrix.getRows() * matrix.getCols();

		for (int i = 0; i < elems; i++) {
			matrix.setData(i, matrix.getData(i) / scalar);
		}
	}

	public float sum(Matrix<Float> matrix)
	{
		float sum = 0;
		int elems = matrix.getRows() * matrix.getCols();

		for (int i = 0; i < elems; i++) {
			sum += matrix.getData(i);
		}

		return sum;
	}

	public void cos (Matrix<Float> matrix)
	{
		if (matrix.getData().isEmpty()) {
			return;
		}

		for (int i = 0; i < matrix.getSize(); i++) {
			matrix.setData(i, (float)Math.cos(matrix.getData(i)));
		}
	}

	public void log10 (Matrix<Float> matrix)
	{
		if (matrix.getData().isEmpty()) {
			return;
		}

		for (int i = 0; i < matrix.getSize(); i++) {
			float val = matrix.getData(i);
			matrix.setData(i, (val <= 0) ? 0 : (float)Math.log10(val));
		}
	}
}
