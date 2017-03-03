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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashSet;

import hcm.ssj.core.Log;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.creator.core.Pipeline;

/**
 * Draws a pipe<br>
 * Created by Frank Gaibler on 29.04.2016.
 */
public class PipeView extends ViewGroup
{
    /**
     * Interface to register listeners to
     */
    public interface ViewListener
    {
        void viewChanged();
    }

    //elements
    private ArrayList<ComponentView> componentViewsSensor = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsProvider = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsTransformer = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsConsumer = new ArrayList<>();
    //connections
    private ArrayList<ConnectionView> connectionViews = new ArrayList<>();
    //layout
    private Paint paintElementGrid;
    private Paint paintElementShadow;
    //
    private final int landscapeNumberOfBoxes = 15;
    private final int portraitBoxes = 20;
    //
    private int gridBoxSize = 0;
    private int gridWidthNumberOfBoxes = 0;
    private int gridHeightNumberOfBoxes = 0;
    private int gridPadWPix = 0;
    private int gridPadHPix = 0;
    private boolean[][] grid = null;
    //
    private HashSet<ViewListener> hsViewListener = new HashSet<>();

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
        //add drag listener
        OnDragListener onDragListener = new OnDragListener()
        {
            private ImageView imageView;
            private boolean dropped;
            private float xCoord, yCoord;

            /**
             * @param event DragEvent
             */
            private void cleanup(DragEvent event)
            {
                //remove view from owner
                ComponentView view = (ComponentView) event.getLocalState();
                try
                {
                    //check collision
                    Rect rectBin = new Rect();
                    imageView.getHitRect(rectBin);
                    //delete element
                    if (rectBin.contains((int) xCoord, (int) yCoord))
                    {
                        setGridValue(view.getGridX(), view.getGridY(), false);
                        Pipeline.getInstance().remove(view.getElement());
                    } //reposition
                    else
                    {
                        int x = getGridCoordinate(xCoord);
                        int y = getGridCoordinate(yCoord);
                        if (dropped)
                        {
                            if (isGridFree(x, y))
                            {
                                //change position
                                setGridValue(view.getGridX(), view.getGridY(), false);
                                view.setGridX(x);
                                view.setGridY(y);
                                placeElementView(view);
                            } else
                            {
                                //check for collision to add a connection
                                checkCollisionConnection(view.getElement(), x, y);
                            }
                        }
                        PipeView.this.addView(view);
                    }
                } finally
                {
                    //remove recycle bin
                    ViewGroup viewGroup = (ViewGroup) imageView.getParent();
                    if (viewGroup != null)
                    {
                        viewGroup.removeView(imageView);
                    }
                    imageView.invalidate();
                    imageView = null;
                    view.invalidate();
                    //recalculate view after delay to fix shadow behaviour
                    Handler handler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable()
                    {
                        public void run()
                        {
                            recalculate();
                        }
                    };
                    handler.postDelayed(runnable, 50);
                }
            }

            /**
             * @param v     View
             * @param event DragEvent
             * @return boolean
             */
            @Override
            public boolean onDrag(View v, DragEvent event)
            {
                switch (event.getAction())
                {
                    case DragEvent.ACTION_DRAG_STARTED:
                        //init values
                        xCoord = 0;
                        yCoord = 0;
                        dropped = false;
                        //create recycle bin
                        if (imageView != null)
                        {
                            ViewGroup parent = ((ViewGroup) imageView.getParent());
                            if (parent != null)
                            {
                                parent.removeView(imageView);
                            }
                            imageView.invalidate();
                            imageView = null;
                        }
                        imageView = new ImageView(getContext());
                        imageView.setImageResource(android.R.drawable.ic_menu_delete);
                        int width = PipeView.this.getWidth();
                        int height = PipeView.this.getHeight();
                        imageView.layout(width - (gridBoxSize * 3), height - (gridBoxSize * 3), width, height);
                        addView(imageView);
                        break;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                        break;
                    case DragEvent.ACTION_DROP:
                        //update drop location
                        xCoord = event.getX();
                        yCoord = event.getY();
                        dropped = true;
                        ComponentView view = (ComponentView) event.getLocalState();
                        setGridValue(view.getGridX(), view.getGridY(), false);
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        cleanup(event);
                        break;
                    default:
                        break;
                }
                return true;
            }
        };
        setOnDragListener(onDragListener);
        //initiate colors
        paintElementGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintElementGrid.setStyle(Paint.Style.STROKE);
        paintElementGrid.setColor(Color.GRAY);
        //
        paintElementShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintElementShadow.setStyle(Paint.Style.FILL);
        paintElementShadow.setColor(Color.LTGRAY);
        //
        recalculate();
    }

    /**
     *
     */
    public final void recalculate()
    {
        if (this.isLaidOut())
        {
            calculateGrid();
            createElements();
            placeElements();
            for (ViewListener viewListener : hsViewListener)
            {
                viewListener.viewChanged();
            }
        }
    }

    /**
     * @param viewListener ViewListener
     */
    public final void addViewListener(ViewListener viewListener)
    {
        hsViewListener.add(viewListener);
    }

    /**
     * @param viewListener ViewListener
     */
    public final void removeViewListener(ViewListener viewListener)
    {
        hsViewListener.remove(viewListener);
    }

    /**
     *
     */
    private void createElements()
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
        Object[] objects = Pipeline.getInstance().getAll(type);
        ArrayList<ComponentView> alInterim = new ArrayList<>();
        for (Object object : objects)
        {
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
            if (!found)
            {
                ComponentView view = new ComponentView(getContext(), object);
                view.setConnectionHashes(Pipeline.getInstance().getConnectionHashes(object));
                alInterim.add(view);
            }
        }
        alView = alInterim;
        for (View view : alView)
        {
            addView(view);
        }
        return alView;
    }

    /**
     *
     */
    private void placeElements()
    {
        //elements
        int initHeight = 0;
        int divider = 4;
        setLayouts(componentViewsSensor, initHeight);
        initHeight += gridHeightNumberOfBoxes / divider;
        setLayouts(componentViewsProvider, initHeight);
        initHeight += gridHeightNumberOfBoxes / divider;
        setLayouts(componentViewsTransformer, initHeight);
        initHeight += gridHeightNumberOfBoxes / divider;
        setLayouts(componentViewsConsumer, initHeight);
        //connections
        for (ConnectionView connectionView : connectionViews)
        {
            connectionView.layout(0, 0, getWidth(), getHeight());
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
                                    gridBoxSize);
                            connectionView.invalidate();
                        } else
                        //arrow from parent to child (e.g. sensor to sensorChannel)
                        {
                            connectionView.setLine(
                                    componentView.getX(), componentView.getY(),
                                    destination.getX(), destination.getY(),
                                    gridBoxSize);
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
                for (int j = initHeight; !placed && j < gridHeightNumberOfBoxes; j += 2)
                {
                    for (int i = j % 4; !placed && i < gridWidthNumberOfBoxes; i += 4)
                    {
                        if (isGridFree(i, j))
                        {
                            view.setGridX(i);
                            view.setGridY(j);
                            placeElementView(view);
                            placed = true;
                        }
                    }
                }
                //try from zero if placement didn't work
                for (int j = 0; !placed && j < gridHeightNumberOfBoxes && j < initHeight; j++)
                {
                    for (int i = 0; !placed && i < gridWidthNumberOfBoxes; i++)
                    {
                        if (isGridFree(i, j))
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
    private void placeElementView(ComponentView view)
    {
        setGridValue(view.getGridX(), view.getGridY(), true);
        int xPos = view.getGridX() * gridBoxSize + gridPadWPix;
        int yPos = view.getGridY() * gridBoxSize + gridPadHPix;
        int componentSize = gridBoxSize * 2;
        view.layout(xPos, yPos, xPos + componentSize, yPos + componentSize);
    }

    /**
     * Translates one pixel axis position to grid coordinate
     *
     * @param pos float
     * @return int
     */
    private int getGridCoordinate(float pos)
    {
        int i = (int) (pos / gridBoxSize + 0.5f) - 1;
        return i < 0 ? 0 : i;
    }

    /**
     * Checks free grid from top left corner
     *
     * @param x int
     * @param y int
     * @return boolean
     */
    private boolean isGridFree(int x, int y)
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
    private void setGridValue(int x, int y, boolean placed)
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

    /**
     *
     */
    private void calculateGrid()
    {
        int gridWPix = getWidth();
        int gridHPix = getHeight();
        gridBoxSize = gridWPix > gridHPix ? gridHPix / landscapeNumberOfBoxes : gridWPix / portraitBoxes;
        if (gridBoxSize <= 0)
        {
            gridBoxSize = 50;
        }
        gridPadWPix = gridWPix % gridBoxSize / 2;
        gridPadHPix = gridHPix % gridBoxSize / 2;
        //
        int width = gridWPix / gridBoxSize;
        int height = gridHPix / gridBoxSize;
        if (gridWidthNumberOfBoxes != width || gridHeightNumberOfBoxes != height)
        {
            //clear element placements
            for (ComponentView view : componentViewsSensor)
            {
                view.setGridX(-1);
                view.setGridY(-1);
            }
            for (ComponentView view : componentViewsProvider)
            {
                view.setGridX(-1);
                view.setGridY(-1);
            }
            for (ComponentView view : componentViewsTransformer)
            {
                view.setGridX(-1);
                view.setGridY(-1);
            }
            for (ComponentView view : componentViewsConsumer)
            {
                view.setGridX(-1);
                view.setGridY(-1);
            }
        }
        gridWidthNumberOfBoxes = width;
        gridHeightNumberOfBoxes = height;
        grid = new boolean[gridWidthNumberOfBoxes][];
        for (int i = 0; i < grid.length; i++)
        {
            grid[i] = new boolean[gridHeightNumberOfBoxes];
        }
    }

    /**
     * @param object Object
     * @param x      int
     * @param y      int
     */
    private void checkCollisionConnection(Object object, int x, int y)
    {
        if (object instanceof Sensor)
        {
            addCollisionConnection(object, x, y, componentViewsProvider, false);
        } else if (object instanceof Provider)
        {
            boolean found = addCollisionConnection(object, x, y, componentViewsTransformer, true);
            if (!found)
            {
                addCollisionConnection(object, x, y, componentViewsConsumer, true);
            }
        }
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
     * @param canvas Canvas
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        int left = 0;
        int right = getWidth();
        int top = 0;
        int bottom = getHeight();
        for (int i = left + gridPadWPix; i <= right - gridPadWPix; i += gridBoxSize)
        {
            canvas.drawLine(i, top + gridPadHPix, i, bottom - gridPadHPix, paintElementGrid);
        }
        for (int i = top + gridPadHPix; i <= bottom - gridPadHPix; i += gridBoxSize)
        {
            canvas.drawLine(left + gridPadWPix, i, right - gridPadWPix, i, paintElementGrid);
        }
        if (grid != null)
        {
            for (int i = 0; i < grid.length; i++)
            {
                for (int j = 0; j < grid[i].length; j++)
                {
                    if (grid[i][j])
                    {
                        float xS = gridBoxSize * i + gridPadWPix;
                        float yS = gridBoxSize * j + gridPadHPix;
                        float xE = gridBoxSize * i + gridPadWPix + gridBoxSize;
                        float yE = gridBoxSize * j + gridPadHPix + gridBoxSize;
                        canvas.drawRect(xS, yS, xE, yE, paintElementShadow);
                    }
                }
            }
        }
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
        calculateGrid();
        placeElements();
    }
}
