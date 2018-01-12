/*
 * ConnectionView.java
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

package hcm.ssj.creator.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import hcm.ssj.creator.util.ConnectionType;


/**
 * Draws connections between elements. Directions are shown with an arrow.<br>
 * Created by Frank Gaibler on 12.05.2016.
 */
public class ConnectionView extends View
{
	private final static float STROKE_WIDTH = 2.0f;
	private final static int ARROW_ANGLE = 35;
	private final static int HITBOX_FACTOR = 5;
	private static Bitmap intersectionBitmap;
	private static Canvas intersectionCanvas;
	private Paint paintConnection;
	private Path path;
	private ConnectionType connectionType;
	private ComponentView startComponentView;
	private ComponentView destinationComponentView;

	/**
	 * @param context Context
	 */
	public ConnectionView(Context context)
	{
		super(context);
		if (paintConnection == null)
		{
			DisplayMetrics dm = getResources().getDisplayMetrics();
			float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, STROKE_WIDTH, dm);
			paintConnection = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintConnection.setStyle(Paint.Style.STROKE);
			paintConnection.setStrokeWidth(strokeWidth);
		}
	}

	public ConnectionType getConnectionType()
	{
		return connectionType;
	}

	public void setConnectionType(ConnectionType connectionType)
	{
		this.connectionType = connectionType;
		// PathEffect
		paintConnection.setPathEffect(connectionType.getPathEffect());
		//Color
		paintConnection.setColor(getResources().getColor(connectionType.getColor()));
	}

	public ComponentView getStartComponentView()
	{
		return startComponentView;
	}

	public ComponentView getDestinationComponentView()
	{
		return destinationComponentView;
	}

	protected void drawConnectionViews(ComponentView start, ComponentView destination, final int boxSize)
	{
		this.startComponentView = start;
		this.destinationComponentView = destination;

		setLine(destinationComponentView.getX(), destinationComponentView.getY(),
				startComponentView.getX(), startComponentView.getY(),
				boxSize,
				connectionType == ConnectionType.EVENTCONNECTION,
				connectionType != ConnectionType.MODELCONNECTION);
	}

	/**
	 * @param stopX   float
	 * @param stopY   float
	 * @param startX  float
	 * @param startY  float
	 * @param boxSize int
	 */
	private void setLine(float stopX, float stopY, float startX, float startY, final int boxSize, boolean curved, boolean arrow)
	{
		//calc triangle
		int arrowLength = (int) (boxSize / 1.42f + 0.5f);
		double triangleA = arrowLength * Math.cos(Math.toRadians(90 - ARROW_ANGLE));
		double triangleB = Math.sqrt(Math.pow(arrowLength, 2) - Math.pow(triangleA, 2));
		double triangleAlpha = Math.toRadians(ARROW_ANGLE);

		//draw line
		//start and end are in the middle of the element box
		stopX += boxSize;
		stopY += boxSize;
		startX += boxSize;
		startY += boxSize;
		path = new Path();

		//direction
		float dx = stopX - startX;
		float dy = stopY - startY;
		double theta = Math.atan2(dy, dx);
		//get middle of line
		double middleX = (stopX + startX) / 2;
		double middleY = (stopY + startY) / 2;

		path.moveTo(stopX, stopY);

		// Draw connection as a curve if it's a event connection
		if (curved)
		{
			float vLength = (float) Math.sqrt(dx * dx + dy * dy);
			float normX = (dx / vLength) * boxSize * 2.0f;
			float normY = (dy / vLength) * boxSize * 2.0f;
			path.quadTo((float) (normY + middleX), (float) (-normX + middleY), startX, startY);
			middleX += (normY / 2.0);
			middleY += (-normX / 2.0);
		}
		else
		{
			path.lineTo(startX, startY);
		}

		//draw arrow
		if(arrow)
		{
			//position arrow in the middle
			middleX += triangleB / 2 * Math.cos(theta);
			middleY += triangleB / 2 * Math.sin(theta);
			//draw first line
			double x = middleX - arrowLength * Math.cos(theta + triangleAlpha);
			double y = middleY - arrowLength * Math.sin(theta + triangleAlpha);
			path.moveTo((float) middleX, (float) middleY);
			path.lineTo((float) x, (float) y);
			//draw second line
			double x2 = middleX - arrowLength * Math.cos(theta - triangleAlpha);
			double y2 = middleY - arrowLength * Math.sin(theta - triangleAlpha);
			path.moveTo((float) middleX, (float) middleY);
			path.lineTo((float) x2, (float) y2);
		}
	}

	/**
	 * @param canvas Canvas
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		canvas.save();
		if (path != null)
		{
			canvas.drawPath(path, paintConnection);
		}
		canvas.restore();
	}

	/**
	 * Checks if the motionEvent is on the drawn hidden bitmap.
	 *
	 * @param motionEvent MotionEvent
	 * @return Motion event is on path.
	 */
	protected boolean isOnPath(MotionEvent motionEvent)
	{
		if (path == null)
		{
			return false;
		}

		// Create intersection bitmap and canvas if not yet done.
		if (intersectionBitmap == null || intersectionCanvas == null)
		{
			intersectionBitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.RGB_565);
			intersectionCanvas = new Canvas(intersectionBitmap);
		}

		// Fill bitmap with white. ("reset")
		intersectionBitmap.eraseColor(Color.WHITE);

		// Draw with higher strokewith in a hidden bitmap.
		ConnectionType oldType = connectionType;
		setConnectionType(ConnectionType.STREAMCONNECTION);
		paintConnection.setStrokeWidth(paintConnection.getStrokeWidth() * HITBOX_FACTOR);
		intersectionCanvas.drawPath(path, paintConnection);
		paintConnection.setStrokeWidth(paintConnection.getStrokeWidth() / HITBOX_FACTOR);
		setConnectionType(oldType);

		// Get color of the touched point and check whether it's WHITE or not.
		int touchPointColor = intersectionBitmap.getPixel((int) motionEvent.getX(), (int) motionEvent.getY());
		return touchPointColor != Color.WHITE;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		// set intersectionBitmap and -Canvas to null,  to have them reinstantiated on resize
		intersectionBitmap = null;
		intersectionCanvas = null;
	}

}
