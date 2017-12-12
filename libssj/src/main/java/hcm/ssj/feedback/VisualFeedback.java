/*
 * VisualFeedback.java
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

package hcm.ssj.feedback;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.List;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.FilePath;
import hcm.ssj.core.option.Option;

/**
 * Created by Antonio Grieco on 06.09.2017.
 */

public class VisualFeedback extends Feedback
{
	public final Options options = new Options();
	private final int TIMEOUT_CHECK_DELAY = 100;
	private List<Drawable> iconList;
	private List<ImageSwitcher> imageSwitcherList;
	private Activity activity = null;
	private long timeout = 0;
	private float defaultBrightness;

	public VisualFeedback()
	{
		_name = "VisualFeedback";
		Log.d("Instantiated VisualFeedback " + this.hashCode());
	}

	@Override
	public Feedback.Options getOptions()
	{
		return options;
	}

	@Override
	public void enterFeedback() throws SSJFatalException
	{
		if (_evchannel_in == null || _evchannel_in.size() == 0)
		{
			throw new SSJFatalException("no input channels");
		}

		if (options.layout.get() == null)
		{
			throw new SSJFatalException("layout not set, cannot render visual feedback");
		}

		activity = getActivity(options.layout.get());
		if (activity == null)
		{
			throw new SSJFatalException("unable to get activity from layout");
		}

		imageSwitcherList = new ArrayList<>();

		getDefaultBrightness();
		loadIcons();
		buildLayout();
		clearIcons();
	}

	private void loadIcons()
	{
		iconList = new ArrayList<>();

		Drawable feedbackDrawable = Drawable.createFromPath(options.feedbackIcon.get().value);
		if (feedbackDrawable != null)
		{
			iconList.add(feedbackDrawable);
		}

		Drawable qualityDrawable = Drawable.createFromPath(options.qualityIcon.get().value);
		if (qualityDrawable != null)
		{
			iconList.add(qualityDrawable);
		}
	}

	@Override
	public void notifyFeedback(Event event)
	{
		if (options.duration.get() > 0)
		{
			timeout = System.currentTimeMillis() + options.duration.get();
		}
		updateIcons();
		updateBrightness(options.brightness.get());
	}

	@Override
	public void process() throws SSJFatalException
	{
		try
		{
			Thread.sleep(TIMEOUT_CHECK_DELAY);
		}
		catch (InterruptedException e)
		{
			throw new SSJFatalException("timeout interrupted", e);
		}

		if (timeout == 0 || System.currentTimeMillis() < timeout)
		{
			return;
		}

		if (isActive())
		{
			Log.i("clearing icons");
			clearIcons();
			updateBrightness(defaultBrightness);
			timeout = 0;
		}
	}

	@Override
	public void flush() throws SSJFatalException
	{
		clearIcons();
	}

	private void clearIcons()
	{
		for (ImageSwitcher imageSwitcher : imageSwitcherList)
		{
			updateImageSwitcher(imageSwitcher, null);
		}
	}

	private void updateIcons()
	{
		int minimal = Math.min(imageSwitcherList.size(), iconList.size());

		for (int i = 0; i < minimal; i++)
		{
			updateImageSwitcher(imageSwitcherList.get(i), iconList.get(i));
		}
	}

	private void updateImageSwitcher(final ImageSwitcher imageSwitcher, final Drawable drawable)
	{
		imageSwitcher.post(new Runnable()
		{
			public void run()
			{
				imageSwitcher.setImageDrawable(drawable);
			}
		});
	}

	private void getDefaultBrightness()
	{
		WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
		defaultBrightness = lp.screenBrightness;
	}

	private void updateBrightness(final float brightness)
	{
		activity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
				lp.screenBrightness = brightness;
				activity.getWindow().setAttributes(lp);
			}
		});
	}

	private Activity getActivity(View view)
	{
		Context context = view.getContext();
		while (context instanceof ContextWrapper)
		{
			if (context instanceof Activity)
			{
				return (Activity) context;
			}
			context = ((ContextWrapper) context).getBaseContext();
		}

		//alternative method
		View content = view.findViewById(android.R.id.content);
		if (content != null)
		{
			return (Activity) content.getContext();
		}
		else
		{
			return null;
		}
	}

	private void buildLayout()
	{
		if (options.layout.get() == null)
		{
			Log.e("layout not set, cannot render visual feedback");
			return;
		}

		Handler handler = new Handler(activity.getMainLooper());
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				TableLayout table = options.layout.get();
				table.setStretchAllColumns(true);

				if (table.getChildCount() < iconList.size())
				{
					for (int i = table.getChildCount(); i < iconList.size(); ++i)
					{
						table.addView(new TableRow(activity), i);
					}
				}

				for (int i = 0; i < iconList.size(); ++i)
				{
					TableRow tr = (TableRow) table.getChildAt(i);
					tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1f));

					//if the image switcher has already been initialized by a class on previous level
					if (tr.getChildAt(options.position.get()) instanceof ImageSwitcher)
					{
						imageSwitcherList.add((ImageSwitcher) tr.getChildAt(options.position.get()));
					}
					else
					{
						ImageSwitcher imageSwitcher = new ImageSwitcher(activity);
						imageSwitcher.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1f));

						Animation in = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
						in.setDuration(options.fade.get());
						Animation out = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out);
						out.setDuration(options.fade.get());

						imageSwitcher.setInAnimation(in);
						imageSwitcher.setOutAnimation(out);

						imageSwitcher.setFactory(new ViewSwitcher.ViewFactory()
						{
							@Override
							public View makeView()
							{
								ImageView imageView = new ImageView(activity);
								imageView.setLayoutParams(new ImageSwitcher.LayoutParams(ImageSwitcher.LayoutParams.MATCH_PARENT, ImageSwitcher.LayoutParams.MATCH_PARENT));
								return imageView;
							}
						});

						// Fill with empty views to match position
						for (int columnCount = 0; columnCount <= options.position.get(); columnCount++)
						{
							if (tr.getChildAt(columnCount) == null)
							{
								tr.addView(new View(activity), columnCount);
							}
						}
						tr.removeViewAt(options.position.get());
						tr.addView(imageSwitcher, options.position.get());
						imageSwitcherList.add(imageSwitcher);
					}
				}

			}
		});
	}

	public class Options extends Feedback.Options
	{
		public final Option<FilePath> feedbackIcon = new Option<>("feedbackIcon", null, FilePath.class, "feedback icon file");
		public final Option<FilePath> qualityIcon = new Option<>("qualityIcon", null, FilePath.class, "quality icon file");

		public final Option<Float> brightness = new Option<>("brightness", 1f, Float.class, "screen brightness");
		public final Option<Integer> duration = new Option<>("duration", 0, Integer.class, "duration until icons disappear");
		public final Option<Integer> fade = new Option<>("fade", 0, Integer.class, "fade duration");
		public final Option<Integer> position = new Option<>("position", 0, Integer.class, "position of the icons");
		public final Option<TableLayout> layout = new Option<>("layout", null, TableLayout.class, "TableLayout in which to render visual feedback");

		private Options()
		{
			super();
			addOptions();
		}

	}
}
