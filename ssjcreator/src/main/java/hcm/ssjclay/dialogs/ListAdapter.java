/*
 * ListAdapter.java
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

package hcm.ssjclay.dialogs;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Adapter for expandable list view. <br>
 * Created by Frank Gaibler on 02.08.2016.
 */
class ListAdapter extends BaseExpandableListAdapter
{
    private Context context;
    private String[] listDataHeader;
    private LinkedHashMap<String, ArrayList<Class>> mapDataChild;
    private static final int PADDING = 25;

    /**
     * @param context      Context
     * @param mapDataChild LinkedHashMap
     */
    public ListAdapter(Context context, LinkedHashMap<String, ArrayList<Class>> mapDataChild)
    {
        this.context = context;
        this.listDataHeader = mapDataChild.keySet().toArray(new String[mapDataChild.size()]);
        this.mapDataChild = mapDataChild;
    }

    /**
     * @param groupPosition int
     * @param childPosition int
     * @return Object
     */
    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        return this.mapDataChild.get(this.listDataHeader[groupPosition]).get(childPosition).getSimpleName();
    }

    /**
     * @param groupPosition int
     * @param childPosition int
     * @return long
     */
    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return childPosition;
    }

    /**
     * @param groupPosition int
     * @param childPosition int
     * @param isLastChild   boolean
     * @param convertView   View
     * @param parent        ViewGroup
     * @return View
     */
    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        final String childText = (String) getChild(groupPosition, childPosition);
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, null);
        }
        CheckedTextView txtListChild = (CheckedTextView) convertView;
        txtListChild.setPadding(PADDING, PADDING, PADDING, PADDING);
        txtListChild.setText(childText);
        return convertView;
    }

    /**
     * @param groupPosition int
     * @return int
     */
    @Override
    public int getChildrenCount(int groupPosition)
    {
        return this.mapDataChild.get(this.listDataHeader[groupPosition]).size();
    }

    /**
     * @param groupPosition int
     * @return Object
     */
    @Override
    public Object getGroup(int groupPosition)
    {
        return this.listDataHeader[groupPosition];
    }

    /**
     * @return int
     */
    @Override
    public int getGroupCount()
    {
        return this.listDataHeader.length;
    }

    /**
     * @param groupPosition int
     * @return long
     */
    @Override
    public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    /**
     * @param groupPosition int
     * @param isExpanded    boolean
     * @param convertView   View
     * @param parent        ViewGroup
     * @return View
     */
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
    {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
        }
        TextView lblListHeader = (TextView) convertView;
        lblListHeader.setPadding(lblListHeader.getPaddingLeft(), PADDING,
                lblListHeader.getPaddingRight(), PADDING);
        lblListHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        lblListHeader.setTypeface(null, Typeface.ITALIC);
        lblListHeader.setText(headerTitle);
        return convertView;
    }

    /**
     * @return boolean
     */
    @Override
    public boolean hasStableIds()
    {
        return false;
    }

    /**
     * @param groupPosition int
     * @param childPosition int
     * @return boolean
     */
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }
}
