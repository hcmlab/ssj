/*
 * PipeOnDragListener.java
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

package hcm.ssj.creator.view;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.main.TwoDScrollView;

/**
 * On drag listener for pipe <br>
 * Created by Frank on 03.03.2017.
 */
class PipeOnDragListener implements View.OnDragListener
{
    private ImageView recycleBin;
    private boolean dropped;
    private float xCoord, yCoord;

    private enum Result
    {
        NOTHING, PLACED, DELETED, CONNECTED
    }

    /**
     *
     */
    PipeOnDragListener()
    {
    }

    /**
     * @param pipeView PipeView
     * @param event    DragEvent
     */
    private void cleanup(final PipeView pipeView, final DragEvent event)
    {
        //remove view from owner
        ComponentView componentView = (ComponentView) event.getLocalState();
        Result result = Result.NOTHING;
        try
        {
            result = handleCollision(pipeView, componentView);
        } finally
        {
            //remove recycle bin
            ViewParent viewParent = recycleBin.getParent();
            if (viewParent != null && viewParent instanceof ViewGroup)
            {
                ((ViewGroup) viewParent).removeView(recycleBin);
            }
            recycleBin.invalidate();
            recycleBin = null;
            componentView.invalidate();
            switch (result)
            {
                case NOTHING:
                    break;
                case PLACED:
                    pipeView.placeElements();
                    break;
                case CONNECTED:
                    pipeView.createElements();
                    pipeView.placeElements();
                    break;
                case DELETED:
                    pipeView.createElements();
                    pipeView.placeElements();
                    //inform listeners after delay to avoid null pointer exception on tab deletion
                    Handler handler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable()
                    {
                        public void run()
                        {
                            pipeView.informListeners();
                        }
                    };
                    handler.postDelayed(runnable, 50);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @param pipeView      PipeView
     * @param componentView ComponentView
     * @return Result
     */
    private Result handleCollision(final PipeView pipeView, final ComponentView componentView)
    {
        Result result = Result.NOTHING;
        //check collision
        Rect rectBin = new Rect();
        recycleBin.getHitRect(rectBin);
        //delete element
        if (rectBin.contains((int) xCoord, (int) yCoord))
        {
            pipeView.getGrid().setGridValue(componentView.getGridX(), componentView.getGridY(), false);
            PipelineBuilder.getInstance().remove(componentView.getElement());
            result = Result.DELETED;
        } //reposition
        else
        {
            int x = pipeView.getGridCoordinate(xCoord);
            int y = pipeView.getGridCoordinate(yCoord);
            if (dropped)
            {
                if (pipeView.getGrid().isGridFree(x, y))
                {
                    //change position
                    pipeView.getGrid().setGridValue(componentView.getGridX(), componentView.getGridY(), false);
                    componentView.setGridX(x);
                    componentView.setGridY(y);
                    pipeView.placeElementView(componentView);
                    result = Result.PLACED;
                } else
                {
                    //check for collision to add a connection
                    boolean conn = pipeView.checkCollisionConnection(componentView.getElement(), x, y);
                    if (conn)
                    {
                        result = Result.CONNECTED;
                    }
                }
            }
            pipeView.addView(componentView);
        }
        return result;
    }

    /**
     * @param pipeView PipeView
     */
    private void createRecycleBin(final PipeView pipeView)
    {
        if (recycleBin != null)
        {
            ViewGroup parent = ((ViewGroup) recycleBin.getParent());
            if (parent != null)
            {
                parent.removeView(recycleBin);
            }
            recycleBin.invalidate();
            recycleBin = null;
        }
        recycleBin = new ImageView(pipeView.getContext());
        recycleBin.setImageResource(android.R.drawable.ic_menu_delete);
        //determine shown width of the view
        Rect rectSizeDisplayed = new Rect();
        pipeView.getGlobalVisibleRect(rectSizeDisplayed);
        int width = rectSizeDisplayed.width();
        int height = rectSizeDisplayed.height();
        //determine scroll changes
        int scrollX = 0, scrollY = 0;
        ViewParent viewParent = pipeView.getParent();
        if (viewParent != null && viewParent instanceof TwoDScrollView)
        {
            scrollX = ((TwoDScrollView) viewParent).getScrollX();
            scrollY = ((TwoDScrollView) viewParent).getScrollY();
        }
        width += scrollX;
        height += scrollY;
        int gridBoxSize = pipeView.getGridBoxSize();
        //place recycle bin
        recycleBin.layout(width - (gridBoxSize * 3), height - (gridBoxSize * 3), width, height);
        pipeView.addView(recycleBin);
    }

    /**
     * @param v     View
     * @param event DragEvent
     * @return boolean
     */
    @Override
    public boolean onDrag(final View v, final DragEvent event)
    {
        if (v instanceof PipeView)
        {
            final PipeView pipeView = (PipeView) v;
            switch (event.getAction())
            {
                case DragEvent.ACTION_DRAG_STARTED:
                {
                    //init values
                    xCoord = 0;
                    yCoord = 0;
                    dropped = false;
                    createRecycleBin(pipeView);
                    break;
                }
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
                    pipeView.getGrid().setGridValue(view.getGridX(), view.getGridY(), false);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    cleanup(pipeView, event);
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                {
//                    //@todo add scroll behaviour to drag and drop
//                    HorizontalScrollView horizontalScrollView = (HorizontalScrollView) pipeView.getParent();
//                    if (horizontalScrollView != null)
//                    {
//                        ScrollView scrollView = (ScrollView) horizontalScrollView.getParent();
//                        if (scrollView != null)
//                        {
//                            //way one
//                            int y = Math.round(event.getY());
//                            int translatedY = y - scrollView.getScrollY();
//                            int threshold = 50;
//                            // make a scrolling up due the y has passed the threshold
//                            if (translatedY < threshold) {
//                                // make a scroll up by 30 px
//                                scrollView.smoothScrollBy(0, -30);
//                            }
//                            // make a autoscrolling down due y has passed the 500 px border
//                            if (translatedY + threshold > 500) {
//                                // make a scroll down by 30 px
//                                scrollView.smoothScrollBy(0, 30);
//                            }
//                            //way two
//                            int topOfDropZone = pipeView.getTop();
//                            int bottomOfDropZone = pipeView.getBottom();
//                            int scrollY = scrollView.getScrollY();
//                            int scrollViewHeight = scrollView.getMeasuredHeight();
//                            Log.d("location: Scroll Y: " + scrollY + " Scroll Y+Height: " + (scrollY + scrollViewHeight));
//                            Log.d(" top: " + topOfDropZone + " bottom: " + bottomOfDropZone);
//                            if (bottomOfDropZone > (scrollY + scrollViewHeight - 100))
//                            {
//                                scrollView.smoothScrollBy(0, 30);
//                            }
//                            if (topOfDropZone < (scrollY + 100))
//                            {
//                                scrollView.smoothScrollBy(0, -30);
//                            }
//                        }
//                    }
                    break;
                }
                default:
                    break;
            }
            return true;
        }
        return false;
    }
}
