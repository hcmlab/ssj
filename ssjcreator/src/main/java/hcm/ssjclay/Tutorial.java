/*
 * Tutorial.java
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

package hcm.ssjclay;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

/**
 * Created by Johnny on 22.08.2016.
 */
public class Tutorial extends AppIntro
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance("Welcome", "SSJ Creator allows you to perform signal processing activities by creating, editing and executing SSJ pipelines", R.drawable.logo, Color.parseColor("#0099CC")));
        addSlide(AppIntroFragment.newInstance("Load Standard Pipeline", "To start, simply load one of the standard pipelines from the file menu", R.drawable.file, Color.parseColor("#BB8930")));
        addSlide(AppIntroFragment.newInstance("Create Your Own Pipeline", "You can also create your own pipeline form scratch (or edit existing ones) by adding components from the paint menu", R.drawable.edit, Color.parseColor("#979797")));
        addSlide(AppIntroFragment.newInstance("Start the Pipeline", "Once you have a pipeline, press the play button to start it", R.drawable.start, Color.parseColor("#4A82AE")));
        addSlide(AppIntroFragment.newInstance("Enjoy", "Now check out your pipeline in action using the different views", R.drawable.tabs, Color.parseColor("#4CAF50")));
//        addSlide(AppIntroFragment.newInstance("Enjoy", "Now, you just need to allow SSJ Creator to access your Phones various sensors and than you are all set", R.drawable.ic_done_white_24px, Color.parseColor("#4CAF50")));

        // OPTIONAL METHODS
        // Override bar/separator color.
//        setBarColor(Color.parseColor("#4A82AE"));
//        setSeparatorColor(Color.parseColor("#4A82AE"));

        // Hide Skip/Done button.
        showSkipButton(true);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }

    void loadMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
