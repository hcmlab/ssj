/*
 * GridLayout.java
 * Copyright (c) 2017
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

package hcm.ssj.creator.view;

/**
 * Grid layout for pipe <br>
 * Created by Frank on 03.03.2017.
 */

class GridLayout
{
    private boolean[][] grid = null;

    GridLayout()
    {
    }

    void setGrid(int width, int height)
    {
        grid = new boolean[width][height];
    }

    /**
     * @return int
     */
    int getWidth()
    {
        return grid != null ? grid.length : 0;
    }

    /**
     * @return int
     */
    int getHeight()
    {
        return grid != null ? grid[0].length : 0;
    }

    /**
     * @param x int
     * @param y int
     * @return boolean
     */
    boolean getValue(int x, int y)
    {
        return grid != null && grid[x][y];
    }

    /**
     * Checks free grid from top left corner
     *
     * @param x int
     * @param y int
     * @return boolean
     */
    boolean isGridFree(int x, int y)
    {
        //check for valid input
        return grid != null && x + 1 < grid.length && y + 1 < grid[0].length &&
                //check grid
                !grid[x][y] && !grid[x + 1][y] && !grid[x][y + 1] && !grid[x + 1][y + 1];
    }

    /**
     * @param x      int
     * @param y      int
     * @param placed boolean
     */
    void setGridValue(int x, int y, boolean placed)
    {
        //check for valid input
        if (grid != null && x + 1 < grid.length && y + 1 < grid[0].length)
        {
            grid[x][y] = placed;
            grid[x + 1][y] = placed;
            grid[x][y + 1] = placed;
            grid[x + 1][y + 1] = placed;
        }
    }
}
