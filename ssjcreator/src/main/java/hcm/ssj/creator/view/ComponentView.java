/*
 * ElementView.java
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

package hcm.ssj.creator.view;

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
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Transformer;
import hcm.ssj.creator.OptionsActivity;
import hcm.ssj.creator.R;

/**
 * Draws elements.<br>
 * Created by Frank Gaibler on 12.05.2016.
 */
class ComponentView extends View
{
    private final static int[] boxColor = {R.color.colorSensor, R.color.colorProvider, R.color.colorTransformer, R.color.colorConsumer};
    private final static int[] textColor = {Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE};

    private static Paint[] paintsElementBox;
    private static Paint[] paintElementText;
    private static Paint paintElementBorder;

    private final static float STROKE_WIDTH = 0.25f;
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
    private ComponentView(Context context)
    {
        super(context);
    }

    /**
     * @param context Context
     * @param element Object
     */
    protected ComponentView(Context context, Object element)
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
                Activity activity = (Activity) getContext();
                OptionsActivity.object = ComponentView.this.element;
                activity.startActivity(new Intent(activity, OptionsActivity.class));
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
            paintsElementBox = new Paint[boxColor.length];
            for (int i = 0; i < paintsElementBox.length; i++)
            {
                paintsElementBox[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintsElementBox[i].setStyle(Paint.Style.FILL);
                paintsElementBox[i].setColor(getResources().getColor(boxColor[i]));
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
            paintElementText = new Paint[textColor.length];
            for (int i = 0; i < paintElementText.length; i++)
            {
                paintElementText[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintElementText[i].setStyle(Paint.Style.FILL);
                paintElementText[i].setColor(textColor[i]);
                paintElementText[i].setTextAlign(Paint.Align.CENTER);
            }

        }

        // color depends on element type
        if (element instanceof Sensor)
        {
            paintType = 0;
        } else if (element instanceof SensorChannel)
        {
            paintType = 1;
        } else if (element instanceof Transformer)
        {
            paintType = 2;
        } else if (element instanceof Consumer)
        {
            paintType = 3;
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
        int boxSize = getBoxSize();;
        canvas.drawRect(0, 0, boxSize, boxSize, paintsElementBox[paintType]);
        canvas.drawRect(0, 0, boxSize, boxSize, paintElementBorder);
        //draws the text in the middle of the box
        float textSize = boxSize / 2.5f;
        paintElementText[paintType].setTextSize(textSize);
        canvas.drawText(text, textSize * (5.f / 4.f), textSize * (8.f / 5.f), paintElementText[paintType]);
    }

    /**
     * @return int
     */
    protected static int getBoxSize() {
        return PipeView.GRID_BOX_SIZE * 2;
    }
}
