/*
 * ElementView.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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

package hcm.ssjclay.view;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

import hcm.ssj.core.Component;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.Transformer;
import hcm.ssjclay.ComponentOptionsActivity;
import hcm.ssjclay.OptionsActivity;
import hcm.ssjclay.SensorOptionsActivity;

/**
 * Draws elements.<br>
 * Created by Frank Gaibler on 12.05.2016.
 */
class ElementView extends View
{
    private static Paint[] paintsElementBox;
    private static Paint paintElementBorder;
    private static Paint paintElementText;
    private final static int[] paints = {Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA};
    private final static float STROKE_WIDTH = 0.25f;
    protected final static int BOX_SIZE = PipeView.GRID_SIZE * 2;
    private final static float TEXT_SIZE = BOX_SIZE / 2.5f;
    //
    private Object element;
    private int[] connectionHashes;
    private String text;
    private int gridX = -1;
    private int gridY = -1;
    //
    private int paintType;

    /**
     * @param context Context
     */
    private ElementView(Context context)
    {
        super(context);
    }

    /**
     * @param context Context
     * @param element Object
     */
    protected ElementView(Context context, Object element)
    {
        super(context);
        this.element = element;
        initPaint();
        initName();
        //add click listener
        OnClickListener onClickListener = new OnClickListener()
        {
            /**
             * @param v View
             */
            @Override
            public void onClick(View v)
            {
                Intent intent;
                Activity activity = (Activity) getContext();
                if (ElementView.this.element instanceof Sensor)
                {
                    SensorOptionsActivity.object = ElementView.this.element;
                    intent = new Intent(activity, SensorOptionsActivity.class);
                } else if (ElementView.this.element instanceof SensorProvider)
                {
                    OptionsActivity.object = ElementView.this.element;
                    intent = new Intent(activity, OptionsActivity.class);
                } else
                {
                    ComponentOptionsActivity.object = ElementView.this.element;
                    intent = new Intent(activity, ComponentOptionsActivity.class);
                }
                activity.startActivity(intent);
            }
        };
        this.setOnClickListener(onClickListener);
        //add touch listener
        OnLongClickListener onTouchListener = new OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                ClipData.Item item = new ClipData.Item("DragEvent");
                ClipData dragData = new ClipData("DragEvent", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDrag(dragData, shadowBuilder, v, 0);
                ((ViewGroup) v.getParent()).removeView(v);
                return true;
            }
        };
        this.setOnLongClickListener(onTouchListener);
    }

    /**
     * @return Object
     */
    protected Object getElement()
    {
        return element;
    }

    /**
     * @return int
     */
    protected int getElementHash()
    {
        return element.hashCode();
    }

    /**
     * @return int[]
     */
    protected int[] getConnectionHashes()
    {
        return connectionHashes;
    }

    /**
     * @param connectionHashes int[]
     */
    protected void setConnectionHashes(int[] connectionHashes)
    {
        this.connectionHashes = connectionHashes;
    }

    /**
     * @param text String
     */
    protected void setText(String text)
    {
        this.text = text;
    }

    /**
     * @param x int
     */
    protected void setGridX(int x)
    {
        gridX = x;
    }

    /**
     * @return int
     */
    protected int getGridX()
    {
        return gridX;
    }

    /**
     * @param y int
     */
    protected void setGridY(int y)
    {
        gridY = y;
    }

    /**
     * @return int
     */
    protected int getGridY()
    {
        return gridY;
    }

    /**
     * @return boolean
     */
    protected boolean isPositioned()
    {
        return gridY >= 0 && gridX >= 0;
    }

    /**
     *
     */
    private void initPaint()
    {
        if (paintsElementBox == null)
        {
            paintsElementBox = new Paint[paints.length];
            for (int i = 0; i < paintsElementBox.length; i++)
            {
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(paints[i]);
                paintsElementBox[i] = paint;
            }
        }
        if (paintElementBorder == null)
        {
            paintElementBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintElementBorder.setStyle(Paint.Style.STROKE);
            paintElementBorder.setColor(Color.BLACK);
            paintElementBorder.setStrokeWidth(STROKE_WIDTH);
        }
        if (paintElementText == null)
        {
            paintElementText = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintElementText.setStyle(Paint.Style.FILL);
            paintElementText.setColor(Color.BLACK);
            paintElementText.setTextSize(TEXT_SIZE);
            paintElementText.setTextAlign(Paint.Align.CENTER);
        }
        //color depends on element type
        if (element instanceof Sensor)
        {
            paintType = 0 % paints.length;
        } else if (element instanceof SensorProvider)
        {
            paintType = 1 % paints.length;
        } else if (element instanceof Transformer)
        {
            paintType = 2 % paints.length;
        } else if (element instanceof Consumer)
        {
            paintType = 3 % paints.length;
        }
    }

    /**
     * Take all upper case letters and fill remaining with trailing lower case letters
     */
    private void initName()
    {
        String componentName = ((Component) element).getComponentName();
        componentName = componentName.substring(componentName.lastIndexOf("_") + 1);
        char[] acComponentName = componentName.toCharArray();
        String shortName = "";
        int j = 0, max = 3, last = 0;
        for (int i = 0; i < acComponentName.length && j < max; i++)
        {
            if (Character.isUpperCase(acComponentName[i]))
            {
                shortName += acComponentName[i];
                j++;
                last = i;
            }
        }
        if (j < max)
        {
            while (j++ < max && ++last < acComponentName.length)
            {
                shortName += acComponentName[last];
            }
        }
        if (shortName.equals("Com"))
        {
            Log.e("Name: " + element.getClass().getSimpleName());
        }
        setText(shortName);
    }

    /**
     * @param canvas Canvas
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, BOX_SIZE, BOX_SIZE, paintsElementBox[paintType]);
        canvas.drawRect(0, 0, BOX_SIZE, BOX_SIZE, paintElementBorder);
        //draws the text in the middle of the box
        canvas.drawText(text, TEXT_SIZE * (5.f / 4.f), TEXT_SIZE * (8.f / 5.f), paintElementText);
    }
}
