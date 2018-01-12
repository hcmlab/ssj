/*
 * Matrix.java
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

package hcm.ssj.signal;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michael Dietz on 11.08.2015.
 */
public class Matrix<T>
{
	public enum MATRIX_DIMENSION {

		//! rowwise
		ROW,
		//! columnwise
		COL
	};

	private int rows;
	private int cols;

	ArrayList<T> data;

	public Matrix(int rows, int cols)
	{
		reset(rows, cols);
	}

	public void reset(int rows, int cols)
	{
		if (rows * cols > 0)
		{
			this.rows = rows;
			this.cols = cols;

			data = new ArrayList<T>(rows * cols);
			while(data.size() < rows * cols) data.add(null);
		}
	}

	public Matrix<T> clone()
	{
		Matrix<T> ret = new Matrix<>(rows, cols);
		for (int i = 0; i < getSize(); i++)
		{
			ret.setData(i, data.get(i));
		}

		return ret;
	}

	public List<T> getData()
	{
		return data;
	}

	public T getData(int index)
	{
		return data.get(index);
	}

	public T getData(int row, int col)
	{
		return data.get(row * cols + col);
	}

	public void setData(int index, T value)
	{
		data.set(index, value);
	}

	public void setData(int row, int col, T value)
	{
		data.set(row * cols + col, value);
	}

	public void fillValue(T value)
	{
		for (int row = 0; row < rows; row++)
		{
			for (int col = 0; col < cols; col++)
			{
				setData(row, col, value);
			}
		}
	}

	public int getRows()
	{
		return rows;
	}

	public int getCols()
	{
		return cols;
	}

	public int getSize()
	{
		return cols * rows;
	}

	public void transpose ()
	{
		if (data.isEmpty()) {
			return;
		}

		if (rows > 1 && cols > 1) {

			Matrix<T> tmp = this.clone();

			int srcrows = tmp.getRows();
			int srccols = tmp.getCols();

			int srcptr = 0;
			int dstptr = 0;

			for (int i = 0; i < srccols; i++)
			{
				srcptr = i;
				for (int j = 0; j < srcrows; j++)
				{
					data.set(dstptr, tmp.getData(srcptr));
					srcptr += srccols;
					dstptr++;
				}
			}
		}

		int tmp = cols;
		cols = rows;
		rows = tmp;
	}

	public void setSubMatrix(int row, int col, Matrix<T> submaxtrix)
	{
		setSubMatrix(row, col, 0, 0, submaxtrix.getRows(), submaxtrix.getCols(), submaxtrix);
	}

	public void setSubMatrix(int row_dst, int col_dst, int row_src, int col_src, int row_number, int col_number, Matrix<T> src)
	{
		if (row_dst + row_number > rows
				|| col_dst + col_number > cols
				|| row_src + row_number > src.getRows()
				|| col_src + col_number > src.getCols())
			return;

		int srccols = src.getCols();
		int srcptr = row_src * srccols + col_src;
		int dstcols = cols;
		int dstptr = row_dst * dstcols + col_dst;

		for (int i = 0; i < row_number; i++) {

			for(int j = 0; j < col_number; j++)
				data.set(dstptr + j, src.getData(srcptr + j));

			srcptr += srccols;
			dstptr += dstcols;
		}
	}
}
