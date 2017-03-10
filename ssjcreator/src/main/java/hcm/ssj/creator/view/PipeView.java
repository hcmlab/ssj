/*
 * PipeView.java
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

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.HashSet;

import hcm.ssj.core.Log;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.creator.core.Pipeline;
import hcm.ssj.creator.main.TwoDScrollView;
import hcm.ssj.creator.util.Util;

/**
 * Draws a pipe<br>
 * Created by Frank Gaibler on 29.04.2016.
 */
public class PipeView extends ViewGroup
{
    //elements
    private ArrayList<ComponentView> componentViewsSensor = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsProvider = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsTransformer = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsConsumer = new ArrayList<>();
    //connections
    private ArrayList<ConnectionView> connectionViews = new ArrayList<>();
    //colors
    private Paint paintElementGrid;
    private Paint paintElementShadow;
    //layout
    private final static int LANDSCAPE_NUMBER_OF_BOXES = 10; //@todo adjust to different screen sizes (e.g. show all boxes on tablet)
    private final static int PORTRAIT_NUMBER_OF_BOXES = LANDSCAPE_NUMBER_OF_BOXES * 2;
    private int iOrientation = Configuration.ORIENTATION_UNDEFINED;
    //grid
    private GridLayout gridLayout;
    private int iGridBoxSize = 0; //box size depends on screen width
    private final int iGridWidthNumberOfBoxes = 50; //chosen box number
    private final int iGridHeightNumberOfBoxes = 50; //chosen box number
    private int iGridPadWPix = 0; //left and right padding to center grid
    private int iGridPadHPix = 0; //top and bottom padding to center grid
    private int iSizeWidth = 0; //draw size width
    private int iSizeHeight = 0; //draw size height
    //listeners
    private HashSet<PipeListener> hsPipeListener = new HashSet<>();

    /**
     * @param context Context
     */
    public PipeView(Context context)
    {
        super(context);
        init();
    }

    /**
     * @param context Context
     * @param attrs   AttributeSet
     */
    public PipeView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    /**
     *
     */
    private void init()
    {
        Log.i("init pipeview");
        //children should not be clipped
        setClipToPadding(false);
        //create grid
        gridLayout = new GridLayout(iGridWidthNumberOfBoxes, iGridHeightNumberOfBoxes);
        //add drag listener
        setOnDragListener(new PipeOnDragListener());
        //initiate colors
        paintElementGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintElementGrid.setStyle(Paint.Style.STROKE);
        paintElementGrid.setColor(Color.GRAY);
        //
        paintElementShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintElementShadow.setStyle(Paint.Style.FILL);
        paintElementShadow.setColor(Color.LTGRAY);
    }

    /**
     * @param buttonAction Util.ButtonAction
     */
    public final void recalculate(Util.ButtonAction buttonAction)
    {
        switch (buttonAction)
        {
            case CLEAR: //fallthrough
                gridLayout.clear();
            case ADD: //fallthrough
            case LOAD: //fallthrough
                createElements();
                placeElements();
                informListeners();
                break;
            default:
                break;
        }
    }

    /**
     * Inform listeners about changed components.<br>
     * Mainly used for informing tab holder about new or deleted painters or writers.<br>
     */
    protected void informListeners()
    {
        for (PipeListener pipeListener : hsPipeListener)
        {
            pipeListener.viewChanged();
        }
    }

    /**
     * @param pipeListener PipeListener
     */
    public final void addViewListener(PipeListener pipeListener)
    {
        hsPipeListener.add(pipeListener);
    }

    /**
     * @param pipeListener PipeListener
     */
    public final void removeViewListener(PipeListener pipeListener)
    {
        hsPipeListener.remove(pipeListener);
    }

    /**
     *
     */
    protected void createElements()
    {
        //cleanup
        removeAllViews();
        //add connections
        connectionViews.clear();
        for (int i = 0; i < Pipeline.getInstance().getNumberOfConnections(); i++)
        {
            connectionViews.add(new ConnectionView(getContext()));
            addView(connectionViews.get(i));
        }
        //add sensors
        componentViewsSensor = fillList(componentViewsSensor, Pipeline.Type.Sensor);
        //add providers
        componentViewsProvider = fillList(componentViewsProvider, Pipeline.Type.SensorChannel);
        //add transformers
        componentViewsTransformer = fillList(componentViewsTransformer, Pipeline.Type.Transformer);
        //add consumers
        componentViewsConsumer = fillList(componentViewsConsumer, Pipeline.Type.Consumer);
    }

    /**
     * @param alView ArrayList
     * @param type   Linker.Type
     */
    private ArrayList<ComponentView> fillList(ArrayList<ComponentView> alView, Pipeline.Type type)
    {
        //get all pipe components of specific type
        Object[] objects = Pipeline.getInstance().getAll(type);
        //copy to new list to delete unused components
        ArrayList<ComponentView> alInterim = new ArrayList<>();
        for (Object object : objects)
        {
            //check of components already exist in list
            boolean found = false;
            for (ComponentView v : alView)
            {
                if (v.getElement().equals(object))
                {
                    found = true;
                    v.setConnectionHashes(Pipeline.getInstance().getConnectionHashes(object));
                    alInterim.add(v);
                    break;
                }
            }
            //create new ComponentView if not
            if (!found)
            {
                ComponentView view = new ComponentView(getContext(), object);
                view.setConnectionHashes(Pipeline.getInstance().getConnectionHashes(object));
                alInterim.add(view);
            }
        }
        //replace old list
        alView = alInterim;
        //add views
        for (View view : alView)
        {
            addView(view);
        }
        return alView;
    }

    /**
     *
     */
    protected void placeElements()
    {
        //elements
        int initHeight = 0;
        int divider = 5;
        setLayouts(componentViewsSensor, initHeight);
        initHeight += divider;
        setLayouts(componentViewsProvider, initHeight);
        initHeight += divider;
        setLayouts(componentViewsTransformer, initHeight);
        initHeight += divider;
        setLayouts(componentViewsConsumer, initHeight);
        //connections
        for (ConnectionView connectionView : connectionViews)
        {
            connectionView.layout(0, 0, iSizeWidth, iSizeHeight);
        }
        int connections = 0;
        for (ComponentView componentViewSensor : componentViewsSensor)
        {
            int[] hashes = componentViewSensor.getConnectionHashes();
            connections = checkConnections(hashes, connections, componentViewSensor, componentViewsProvider, false);
        }
        for (ComponentView componentViewTransformer : componentViewsTransformer)
        {
            int[] hashes = componentViewTransformer.getConnectionHashes();
            connections = checkConnections(hashes, connections, componentViewTransformer, componentViewsProvider, true);
            connections = checkConnections(hashes, connections, componentViewTransformer, componentViewsTransformer, true);
        }
        for (ComponentView componentViewConsumer : componentViewsConsumer)
        {
            int[] hashes = componentViewConsumer.getConnectionHashes();
            connections = checkConnections(hashes, connections, componentViewConsumer, componentViewsProvider, true);
            connections = checkConnections(hashes, connections, componentViewConsumer, componentViewsTransformer, true);
        }
    }

    /**
     * @param hashes              int[]
     * @param connections         int
     * @param destination         View
     * @param componentViews      ArrayList
     * @param standardOrientation boolean
     * @return int
     */
    private int checkConnections(int[] hashes, int connections, View destination, ArrayList<ComponentView> componentViews, boolean standardOrientation)
    {
        if (hashes != null)
        {
            for (int hash : hashes)
            {
                for (ComponentView componentView : componentViews)
                {
                    if (hash == componentView.getElementHash())
                    {
                        ConnectionView connectionView = connectionViews.get(connections);
                        //arrow from child to parent (e.g. transformer to consumer)
                        if (standardOrientation)
                        {
                            connectionView.setLine(
                                    destination.getX(), destination.getY(),
                                    componentView.getX(), componentView.getY(),
                                    iGridBoxSize);
                            connectionView.invalidate();
                        } else
                        //arrow from parent to child (e.g. sensor to sensorChannel)
                        {
                            connectionView.setLine(
                                    componentView.getX(), componentView.getY(),
                                    destination.getX(), destination.getY(),
                                    iGridBoxSize);
                            connectionView.invalidate();
                        }
                        connections++;
                        break;
                    }
                }
            }
        }
        return connections;
    }

    /**
     * @param views      ArrayList
     * @param initHeight int
     */
    private void setLayouts(ArrayList<ComponentView> views, int initHeight)
    {
        for (ComponentView view : views)
        {
            if (view.isPositioned())
            {
                placeElementView(view);
            } else
            {
                boolean placed = false;
                //place elements as chess grid
                for (int j = initHeight; !placed && j < iGridHeightNumberOfBoxes; j += 2)
                {
                    for (int i = j % 4; !placed && i < iGridWidthNumberOfBoxes; i += 4)
                    {
                        if (gridLayout.isGridFree(i, j))
                        {
                            view.setGridX(i);
                            view.setGridY(j);
                            placeElementView(view);
                            placed = true;
                        }
                    }
                }
                //try from zero if placement didn't work
                for (int j = 0; !placed && j < iGridHeightNumberOfBoxes && j < initHeight; j++)
                {
                    for (int i = 0; !placed && i < iGridWidthNumberOfBoxes; i++)
                    {
                        if (gridLayout.isGridFree(i, j))
                        {
                            view.setGridX(i);
                            view.setGridY(j);
                            placeElementView(view);
                            placed = true;
                        }
                    }
                }
                if (!placed)
                {
                    Log.e("Too many elements in view. Could not place all.");
                }
            }
        }
    }

    /**
     * @param view ElementView
     */
    protected void placeElementView(ComponentView view)
    {
        gridLayout.setGridValue(view.getGridX(), view.getGridY(), true);
        int xPos = view.getGridX() * iGridBoxSize + iGridPadWPix;
        int yPos = view.getGridY() * iGridBoxSize + iGridPadHPix;
        int componentSize = iGridBoxSize * 2;
        view.layout(xPos, yPos, xPos + componentSize, yPos + componentSize);
    }

    /**
     * @return int
     */
    protected int getGridBoxSize()
    {
        return iGridBoxSize;
    }

    /**
     * Translates one pixel axis position to grid coordinate
     *
     * @param pos float
     * @return int
     */
    protected int getGridCoordinate(float pos)
    {
        int i = (int) (pos / iGridBoxSize + 0.5f) - 1;
        return i < 0 ? 0 : i;
    }

    /**
     * @param object Object
     * @param x      int
     * @param y      int
     * @return boolean
     */
    protected boolean checkCollisionConnection(Object object, int x, int y)
    {
        boolean result = false;
        if (object instanceof Sensor)
        {
            result = addCollisionConnection(object, x, y, componentViewsProvider, false);
        } else if (object instanceof Provider)
        {
            boolean found = addCollisionConnection(object, x, y, componentViewsTransformer, true);
            result = found;
            if (!found)
            {
                result = addCollisionConnection(object, x, y, componentViewsConsumer, true);
            }
        }
        return result;
    }

    /**
     * @param object         Object
     * @param x              int
     * @param y              int
     * @param componentViews ArrayList
     * @param standard       boolean
     * @return boolean
     */
    private boolean addCollisionConnection(Object object, int x, int y, ArrayList<ComponentView> componentViews, boolean standard)
    {
        for (ComponentView componentView : componentViews)
        {
            int colX = componentView.getGridX();
            int colY = componentView.getGridY();
            if ((colX == x || colX == x - 1 || colX == x + 1) && (colY == y || colY == y - 1 || colY == y + 1))
            {
                if (standard)
                {
                    Pipeline.getInstance().addProvider(componentView.getElement(), (Provider) object);
                } else
                {
                    Pipeline.getInstance().addProvider(object, (Provider) componentView.getElement());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @return GridLayout
     */
    protected GridLayout getGrid()
    {
        return gridLayout;
    }

    /**
     * @param canvas Canvas
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        canvas.save();
        int left = iGridPadWPix;
        int right = iSizeWidth - iGridPadWPix;
        int top = iGridPadHPix;
        int bottom = iSizeHeight - iGridPadHPix;
        for (int i = 0; i < iGridWidthNumberOfBoxes + 1; i++)
        {
            canvas.drawLine(iGridPadWPix + i * iGridBoxSize, top,
                    iGridPadWPix + i * iGridBoxSize, bottom, paintElementGrid);
        }
        for (int i = 0; i < iGridHeightNumberOfBoxes + 1; i++)
        {
            canvas.drawLine(left, iGridPadHPix + i * iGridBoxSize,
                    right, iGridPadHPix + i * iGridBoxSize, paintElementGrid);
        }
        for (int i = 0; i < gridLayout.getWidth(); i++)
        {
            for (int j = 0; j < gridLayout.getHeight(); j++)
            {
                if (gridLayout.getValue(i, j))
                {
                    float xS = iGridBoxSize * i + iGridPadWPix;
                    float yS = iGridBoxSize * j + iGridPadHPix;
                    float xE = iGridBoxSize * i + iGridPadWPix + iGridBoxSize;
                    float yE = iGridBoxSize * j + iGridPadHPix + iGridBoxSize;
                    canvas.drawRect(xS, yS, xE, yE, paintElementShadow);
                }
            }
        }
        canvas.restore();
    }

    /**
     * @param w    int
     * @param h    int
     * @param oldw int
     * @param oldh int
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * @param changed boolean
     * @param l       int
     * @param t       int
     * @param r       int
     * @param b       int
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        //only change grid box size on orientation change
        int orientation = getResources().getConfiguration().orientation;
        if (iOrientation != orientation)
        {
            iOrientation = orientation;
            //reset scroll
            ViewParent viewParent = getParent();
            if (viewParent != null && viewParent instanceof TwoDScrollView)
            {
                ((TwoDScrollView) viewParent).setScrollX(0);
                ((TwoDScrollView) viewParent).setScrollY(0);
            }
            //get displayed screen size
            Rect rectSizeDisplayed = new Rect();
            getGlobalVisibleRect(rectSizeDisplayed);
            int width = rectSizeDisplayed.width();
            int height = rectSizeDisplayed.height();
            iGridBoxSize = width > height
                    ? height / LANDSCAPE_NUMBER_OF_BOXES
                    : width / PORTRAIT_NUMBER_OF_BOXES;
            if (iGridBoxSize <= 0)
            {
                iGridBoxSize = 50;
            }
            calcDerivedSizes(width, height);
            //check if display wouldn't be filled
            if (iSizeWidth < width)
            {
                iGridBoxSize = width / (iGridWidthNumberOfBoxes);
            } else if (iSizeHeight < height)
            {
                iGridBoxSize = height / (iGridHeightNumberOfBoxes);
            }
            calcDerivedSizes(width, height);
            //set size in handler to force correct size and scroll view behaviour
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable()
            {
                public void run()
                {
                    setMinimumHeight(iSizeHeight);
                    setMinimumWidth(iSizeWidth);
                    //place elements anew
                    placeElements();
                }
            };
            handler.postDelayed(runnable, 50);
        }
    }

    /**
     * @param width  int
     * @param height int
     */
    private void calcDerivedSizes(int width, int height)
    {
        iGridPadWPix = width % iGridBoxSize / 2;
        iGridPadHPix = height % iGridBoxSize / 2;
        iSizeWidth = iGridBoxSize * iGridWidthNumberOfBoxes + (2 * iGridPadWPix);
        iSizeHeight = iGridBoxSize * iGridHeightNumberOfBoxes + (2 * iGridPadHPix);
    }
}
