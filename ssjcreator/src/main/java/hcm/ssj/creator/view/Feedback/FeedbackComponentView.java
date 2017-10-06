/*
 * FeedbackComponentView.java
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

package hcm.ssj.creator.view.Feedback;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Map;

import hcm.ssj.creator.R;
import hcm.ssj.creator.activity.FeedbackContainerActivity;
import hcm.ssj.creator.view.ComponentView;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackContainer;

/**
 * Created by Antonio Grieco on 04.10.2017.
 */

public class FeedbackComponentView extends ComponentView
{
	private Map.Entry<Feedback, FeedbackContainer.LevelBehaviour> feedbackLevelBehaviourEntry;
	private final FeedbackContainerActivity feedbackContainerActivity;

	private Paint levelBehaviourPaint;
	private Paint dragBoxPaint;
	private boolean currentlyDraged = false;

	public FeedbackComponentView(FeedbackContainerActivity feedbackContainerActivity, Map.Entry<Feedback, FeedbackContainer.LevelBehaviour> feedbackLevelBehaviourEntry)
	{
		super(feedbackContainerActivity, feedbackLevelBehaviourEntry.getKey());
		this.feedbackContainerActivity = feedbackContainerActivity;
		this.feedbackLevelBehaviourEntry = feedbackLevelBehaviourEntry;

		OnLongClickListener onTouchListener = new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				ClipData.Item item = new ClipData.Item("DragEvent");
				ClipData dragData = new ClipData("DragEvent", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
				DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
				v.startDrag(dragData, shadowBuilder, v, 0);
				((FeedbackComponentView) v).setCurrentlyDraged(true);
				return true;
			}
		};
		this.setOnLongClickListener(onTouchListener);

		this.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				openLevelBehaviourDialog();
			}
		});

		initPaints();
	}

	private void initPaints()
	{
		dragBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		dragBoxPaint.setStyle(Paint.Style.FILL);
		dragBoxPaint.setColor(Color.DKGRAY);

		levelBehaviourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		levelBehaviourPaint.setStyle(Paint.Style.FILL);
		levelBehaviourPaint.setStrokeWidth(10);
	}

	public Map.Entry<Feedback, FeedbackContainer.LevelBehaviour> getFeedbackLevelBehaviourEntry()
	{
		return feedbackLevelBehaviourEntry;
	}

	public void setCurrentlyDraged(boolean currentlyDraged)
	{
		this.currentlyDraged = currentlyDraged;
	}

	@Override
	public void onDraw(Canvas canvas)
	{
		if (!currentlyDraged)
		{
			super.onDraw(canvas);
			canvas.save();
			if (feedbackLevelBehaviourEntry.getValue().equals(FeedbackContainer.LevelBehaviour.Progress))
			{
				// RED BOTTOM ARROW
				levelBehaviourPaint.setColor(Color.RED);
				Path path = new Path();
				path.moveTo(0, getHeight());
				path.lineTo(getWidth(), getHeight());
				path.lineTo(getWidth() / 2, getHeight() + (getHeight() / 8));
				path.offset(0, (getHeight() / 20));
				canvas.drawPath(path, levelBehaviourPaint);

			}
			else if (feedbackLevelBehaviourEntry.getValue().equals(FeedbackContainer.LevelBehaviour.Regress))
			{
				// GREEN TOP ARROW
				levelBehaviourPaint.setColor(Color.GREEN);
				Path path = new Path();
				path.moveTo(0, 0);
				path.lineTo(getWidth(), 0);
				path.lineTo(getWidth() / 2, -(getHeight() / 8));
				path.offset(0, -(getHeight() / 20));
				canvas.drawPath(path, levelBehaviourPaint);
			}
			canvas.restore();
		}
		else
		{
			canvas.save();
			canvas.drawRect(0, 0, getWidth(), getHeight(), dragBoxPaint);
			invalidate();
			canvas.restore();
		}
	}

	void openLevelBehaviourDialog(){

		AlertDialog.Builder alt_bld = new AlertDialog.Builder(feedbackContainerActivity);
		alt_bld.setTitle(feedbackLevelBehaviourEntry.getKey().getComponentName() + " - " + FeedbackContainer.LevelBehaviour.class.getSimpleName());
		alt_bld.setMessage(R.string.level_behaviour_message);
		final FeedbackContainer.LevelBehaviour oldLevelBehaviour = feedbackLevelBehaviourEntry.getValue();

		alt_bld.setView(getRadioGroup());

		// BUTTONS
		alt_bld.setNeutralButton(R.string.str_options, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				openOptions();
			}
		});
		alt_bld.setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				feedbackLevelBehaviourEntry.setValue(oldLevelBehaviour);
			}
		});
		alt_bld.setPositiveButton(R.string.str_ok, null);

		// DISPLAY
		AlertDialog alert = alt_bld.create();
		alert.show();
	}

	public RadioGroup getRadioGroup()
	{
		final String[] values = Arrays.toString(FeedbackContainer.LevelBehaviour.values()).replaceAll("^.|.$", "").split(", ");
		final RadioGroup radioGroup = new RadioGroup(feedbackContainerActivity);
		radioGroup.setPadding(20,20,20,20);
		for(String value : values)
		{
			CheckableListItem checkableListItem = new CheckableListItem(feedbackContainerActivity);
			checkableListItem.setText(value);
			radioGroup.addView(checkableListItem);
			if(value.equals(feedbackLevelBehaviourEntry.getValue().toString()))
			{
				radioGroup.check(checkableListItem.getId());
			}
		}

		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, @IdRes int checkedId)
			{
				RadioButton radioButton = (RadioButton) radioGroup.findViewById(checkedId);
				feedbackLevelBehaviourEntry.setValue(FeedbackContainer.LevelBehaviour.valueOf(radioButton.getText().toString()));
			}
		});

		return radioGroup;
	}

	private class CheckableListItem extends LinearLayout implements Checkable
	{
		TextView textView;
		RadioButton radioButton;

		public CheckableListItem(Context context)
		{
			super(context);
			this.setOrientation(HORIZONTAL);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.gravity = Gravity.CENTER_VERTICAL;
			radioButton = new RadioButton(context);
			radioButton.setChecked(false);
			radioButton.setLayoutParams(lp);
			textView = new TextView(context);
		}

		public String getText()
		{
			return textView.getText().toString();
		}

		public void setText(String text)
		{
			textView.setText(text);
		}

		@Override
		public void setChecked(boolean checked)
		{
			radioButton.setChecked(checked);
		}

		@Override
		public boolean isChecked()
		{
			return radioButton.isChecked();
		}

		@Override
		public void toggle()
		{
			setChecked(!isChecked());
		}
	}
}
