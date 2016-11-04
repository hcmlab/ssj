/*
 * OptionsActivity.java
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

package hcm.ssj.creator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TableLayout;

import hcm.ssj.core.TheFramework;
import hcm.ssj.core.option.Option;
import hcm.ssj.creator.core.Linker;
import hcm.ssj.creator.util.OptionTable;

public class OptionsActivity extends AppCompatActivity
{
    public static Object object;

    /**
     *
     */
    private void init()
    {
        Option[] options;
        if (object != null)
        {
            //change title
            setTitle(((hcm.ssj.core.Component) object).getComponentName());
            options = Linker.getOptionList(object);
        } else
        {
            //change title
            setTitle("SSJ_Framework");
            options = Linker.getOptionList(TheFramework.getFramework());
        }
        object = null;
        //stretch columns
        TableLayout tableLayout = (TableLayout) findViewById(R.id.id_tableLayout);
        tableLayout.setStretchAllColumns(true);
        //add options
        if (options != null && options.length > 0)
        {
            tableLayout.addView(OptionTable.createTable(this, options, false));
        }
    }

    /**
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
        init();
    }
}
