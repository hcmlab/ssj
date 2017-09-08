/*
 * TabHandler.java
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

package hcm.ssj.creator.main;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TabHost;
import android.widget.TableLayout;

import com.jjoe64.graphview.GraphView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hcm.ssj.camera.CameraPainter;
import hcm.ssj.core.Component;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.util.Util;
import hcm.ssj.creator.view.PipeListener;
import hcm.ssj.feedback.FeedbackManager;
import hcm.ssj.feedback.VisualFeedback;
import hcm.ssj.file.IFileWriter;
import hcm.ssj.graphic.SignalPainter;

/**
 * ITab handler for main activity.<br>
 * Created by Frank Gaibler on 23.09.2016.
 */
public class TabHandler
{
	private Activity activity;
	//tabs
	private TabHost tabHost = null;
	private Set<Object> alAdditionalTabs = new HashSet<>();
	private List<TabHost.TabSpec> alTabSpecs = new ArrayList<>();
	private final static int FIX_TAB_NUMBER = 2;
	//canvas
	private Canvas canvas;
	//console
	private Console console;
	//annotation
	private boolean annotationExists = false;

	//visual feedback tab index
	private int visualFeedbackTabIndex = Integer.MIN_VALUE;

	private AnnotationTab annotationTab = null;

	private Component[] visualFeedbackComponents = null;

	/**
	 * @param activity Activity
	 */
	public TabHandler(Activity activity)
	{
		this.activity = activity;
		tabHost = (TabHost) activity.findViewById(R.id.id_tabHost);
		if (tabHost != null)
		{
			tabHost.setup();
			//canvas
			canvas = new Canvas(this.activity);
			addTab(canvas.getView(), canvas.getTitle(), canvas.getIcon());
			//console
			console = new Console(this.activity);
			addTab(console.getView(), console.getTitle(), console.getIcon());
			//init tabs
			canvas.init(new PipeListener()
			{
				@Override
				public void viewChanged()
				{
					checkAdditionalTabs();
				}
			});
			console.init();
		}
	}

	/**
	 * @param view    View
	 * @param tabName String
	 * @param image   int
	 */
	private void addTab(final View view, final String tabName, int image)
	{
		final TabHost.TabSpec tabSpec = tabHost.newTabSpec(tabName);
		tabSpec.setContent(new TabHost.TabContentFactory()
		{
			/**
			 * @param tag String
			 * @return View
			 */
			public View createTabContent(String tag)
			{
				return view;
			}
		});
		tabSpec.setIndicator("", ContextCompat.getDrawable(activity, image));
		tabHost.addTab(tabSpec);
		//necessary to reset tab strip
		int tab = tabHost.getCurrentTab();
		tabHost.setCurrentTab(tabHost.getTabWidget().getTabCount() - 1);
		tabHost.setCurrentTab(tab);
		alTabSpecs.add(tabSpec);
	}

	/**
	 * Adds annotation tab after fixed tabs
	 *
	 * @param view    View
	 * @param tabName String
	 * @param image   int
	 */
	private void addTabAnno(final View view, final String tabName, int image)
	{
		final TabHost.TabSpec tabSpec = tabHost.newTabSpec(tabName);
		tabSpec.setContent(new TabHost.TabContentFactory()
		{
			/**
			 * @param tag String
			 * @return View
			 */
			public View createTabContent(String tag)
			{
				return view;
			}
		});
		tabSpec.setIndicator("", ContextCompat.getDrawable(activity, image));
		alTabSpecs.add(FIX_TAB_NUMBER, tabSpec);
		//
		int current = tabHost.getCurrentTab();
		tabHost.setCurrentTab(0);
		tabHost.clearAllTabs();
		for (TabHost.TabSpec tab : alTabSpecs)
		{
			tabHost.addTab(tab);
		}
		tabHost.setCurrentTab(current == 1 ? 1 : 0);
	}

	/**
	 * @param tab int
	 */
	private void removeTab(final int tab)
	{
		int current = tabHost.getCurrentTab();
		alTabSpecs.remove(tab);
		tabHost.setCurrentTab(0);
		tabHost.clearAllTabs();
		for (TabHost.TabSpec tabSpec : alTabSpecs)
		{
			tabHost.addTab(tabSpec);
		}
		tabHost.setCurrentTab(current == 1 ? 1 : 0);
	}

	/**
	 * Add or remove additional tabs
	 */
	private void checkAdditionalTabs()
	{
		List<Component> additionalTabComponents = new ArrayList<>();
		int counterAnno = 0;

		for(Component iFileWriter : PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, IFileWriter.class))
		{
			additionalTabComponents.add(iFileWriter);
			counterAnno++;
			if (!annotationExists)
			{
				annotationExists = true;
				annotationTab = new AnnotationTab(this.activity);
				addTabAnno(annotationTab.getView(), annotationTab.getTitle(), annotationTab.getIcon());
			}
		}

		for(Component signalPainter : PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, SignalPainter.class))
		{
			additionalTabComponents.add(signalPainter);
			GraphView graphView = ((SignalPainter) signalPainter).options.graphView.get();
			if (graphView == null)
			{
				graphView = new GraphView(activity);
				((SignalPainter) signalPainter).options.graphView.set(graphView);
				addTab(graphView, signalPainter.getComponentName(), android.R.drawable.ic_menu_view);
				alAdditionalTabs.add(signalPainter);
			}
		}

		for(Component cameraPainter : PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, CameraPainter.class))
		{
			additionalTabComponents.add(cameraPainter);
			SurfaceView surfaceView = ((CameraPainter) cameraPainter).options.surfaceView.get();
			if (surfaceView == null)
			{
				surfaceView = new SurfaceView(activity);
				((CameraPainter) cameraPainter).options.surfaceView.set(surfaceView);
				addTab(surfaceView, cameraPainter.getComponentName(), android.R.drawable.ic_menu_camera);
				alAdditionalTabs.add(cameraPainter);
			}
		}

		for(Component feedBackManager : PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.EventHandler, FeedbackManager.class))
		{
			additionalTabComponents.add(feedBackManager);
			TableLayout tableLayout = ((FeedbackManager) feedBackManager).options.layout.get();
			if (tableLayout == null)
			{
				tableLayout = new TableLayout(activity);
				((FeedbackManager) feedBackManager).options.layout.set(tableLayout);
				addTab(tableLayout, feedBackManager.getComponentName(), android.R.drawable.ic_menu_compass); // TODO: Change icon.
				alAdditionalTabs.add(feedBackManager);
			}
		}

		Component[] currentVisualFeedbackComponents = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.EventHandler, VisualFeedback.class);
		if(currentVisualFeedbackComponents.length > 0)
		{
			TableLayout newLayout = getTableLayoutForVisualFeedback(currentVisualFeedbackComponents);
			for(Component visualFeedback : currentVisualFeedbackComponents)
			{
				((VisualFeedback) visualFeedback).options.layout.set(newLayout);
			}
			Component addedComponent = addVisualFeedbackTab(newLayout, currentVisualFeedbackComponents, android.R.drawable.ic_menu_compass); // TODO: Change icon.
			additionalTabComponents.add(addedComponent);
		}
		else{
			visualFeedbackTabIndex = Integer.MIN_VALUE;
		}

		//remove annotation
		if (counterAnno <= 0 && annotationExists)
		{
			annotationExists = false;
			removeTab(FIX_TAB_NUMBER);
		}

		//remove obsolete tabs
		Object[] alAdditionalTabsList = alAdditionalTabs.toArray();
		for (int i = alAdditionalTabsList.length - 1; i >= 0; i--)
		{
			if (!additionalTabComponents.contains(alAdditionalTabsList[i]))
			{
				alAdditionalTabs.remove(i);
				removeTab(i + FIX_TAB_NUMBER + (annotationExists ? 1 : 0));
			}
		}
	}

	private Component addVisualFeedbackTab(TableLayout newLayout, Component[] newVisualFeedbackComponents, int drawable) {

		if(visualFeedbackComponents != null)
		{
			for (Component visualFeedBackComponent : visualFeedbackComponents)
			{
				alAdditionalTabs.remove(visualFeedBackComponent);
			}
		}
		VisualFeedback firstComponent = ((VisualFeedback) newVisualFeedbackComponents[0]);
		addTab(firstComponent.options.layout.get(), firstComponent.getComponentName(), drawable);
		visualFeedbackComponents = newVisualFeedbackComponents;

		return firstComponent;
	}

	// Check if any visual feedback has already an layout. if yes set all layouts to this one.
	// otherwise make a new one. if there are visual feedback components with different layouts set a new one to all.
	private TableLayout getTableLayoutForVisualFeedback(Component[] visualFeedbackComponents) {
		List<TableLayout> layouts = new ArrayList<>();
		for(Component visualFeedback : visualFeedbackComponents)
		{
			TableLayout currentLayout = ((VisualFeedback)visualFeedback).options.layout.get();
			if(currentLayout != null && !layouts.contains(currentLayout))
			{
				layouts.add(currentLayout);
			}

		}
		return (layouts.size() == 1) ? layouts.get(0) :  new TableLayout(activity);
	}

	/**
	 *
	 */
	public void preStart()
	{
		console.clear();

		if (annotationExists && annotationTab != null)
		{
			annotationTab.startAnnotation();
		}
	}

	/**
	 *
	 */
	public void preStop()
	{
		if (annotationExists && annotationTab != null)
		{
			annotationTab.finishAnnotation();
		}
	}

	/**
	 * @param appAction Util.AppAction
	 * @param o         Object
	 */
	public void actualizeContent(Util.AppAction appAction, Object o)
	{
		canvas.actualizeContent(appAction, o);
	}

	/**
	 *
	 */
	public void cleanUp()
	{
		console.cleanUp();
		canvas.cleanUp();
	}

	/**
	 * @return Annotation
	 */
	public AnnotationTab getAnnotation()
	{
		return annotationTab;
	}
}
