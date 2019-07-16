/*
 * TutorialActivity.java
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

package hcm.ssj.creator.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import hcm.ssj.creator.R;

/**
 * Created by Johnny on 22.08.2016.
 */
public class TutorialActivity extends AppIntro
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide1_title), getResources().getString(R.string.slide1_text), R.drawable.logo, Color.parseColor("#0099CC")));
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide2_title), getResources().getString(R.string.slide2_text), R.drawable.file, Color.parseColor("#BB8930")));
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide3_title), getResources().getString(R.string.slide3_text), R.drawable.edit, Color.parseColor("#979797")));
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide4_title), getResources().getString(R.string.slide4_text), R.drawable.eventconnections, Color.parseColor("#0F9D58")));
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide5_title), getResources().getString(R.string.slide5_text), R.drawable.start, Color.parseColor("#4A82AE")));
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.slide6_title), getResources().getString(R.string.slide6_text), R.drawable.tabs, Color.parseColor("#963aff")));

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
