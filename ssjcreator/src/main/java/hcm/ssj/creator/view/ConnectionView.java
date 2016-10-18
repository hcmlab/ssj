/*
 * ConnectionView.java
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

package hcm.creator.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import hcm.creator.R;

/**
 * Draws connections between elements. Directions are shown with an arrow.<br>
 * Created by Frank Gaibler on 12.05.2016.
 */
class ConnectionView extends View
{
    private static Paint paintConnection;
    private final static float STROKE_WIDTH = 5;
    private final static int ARROW_LENGTH = 35;
    private final static int ARROW_ANGLE = 35;
    private final static double TRIANGLE_A = ARROW_LENGTH * Math.cos(Math.toRadians(90 - ARROW_ANGLE));
    private final static double TRIANGLE_B = Math.sqrt(Math.pow(ARROW_LENGTH, 2) - Math.pow(TRIANGLE_A, 2));
    private final static double TRIANGLE_ALPHA = Math.toRadians(ARROW_ANGLE);
    //
    private Path path;

    /**
     * @param context Context
     */
    protected ConnectionView(Context context)
    {
        super(context);
        if (paintConnection == null)
        {
            paintConnection = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintConnection.setStyle(Paint.Style.STROKE);
            paintConnection.setColor(getResources().getColor(R.color.colorConnectionStream));
            paintConnection.setStrokeWidth(STROKE_WIDTH);
        }
    }

    /**
     * @param stopX  float
     * @param stopY  float
     * @param startX float
     * @param startY float
     */
    protected void setLine(float stopX, float stopY, float startX, float startY)
    {
        //draw line
        //start and end are in the middle of the element box
        float half = ElementView.BOX_SIZE / 2.f;
        stopX += half;
        stopY += half;
        startX += half;
        startY += half;
        path = new Path();
        path.moveTo(stopX, stopY);
        path.lineTo(startX, startY);
        //draw arrow
        //direction
        float dx = stopX - startX;
        float dy = stopY - startY;
        double theta = Math.atan2(dy, dx);
        //get middle of line
        double middleX = (stopX + startX) / 2;
        double middleY = (stopY + startY) / 2;
        //position arrow in the middle
        middleX += TRIANGLE_B / 2 * Math.cos(theta);
        middleY += TRIANGLE_B / 2 * Math.sin(theta);
        //draw first line
        double x = middleX - ARROW_LENGTH * Math.cos(theta + TRIANGLE_ALPHA);
        double y = middleY - ARROW_LENGTH * Math.sin(theta + TRIANGLE_ALPHA);
        path.moveTo((float) middleX, (float) middleY);
        path.lineTo((float) x, (float) y);
        //draw second line
        double x2 = middleX - ARROW_LENGTH * Math.cos(theta - TRIANGLE_ALPHA);
        double y2 = middleY - ARROW_LENGTH * Math.sin(theta - TRIANGLE_ALPHA);
        path.moveTo((float) middleX, (float) middleY);
        path.lineTo((float) x2, (float) y2);
    }

    /**
     * @param canvas Canvas
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if (path != null)
        {
            canvas.drawPath(path, paintConnection);
        }
    }
}
