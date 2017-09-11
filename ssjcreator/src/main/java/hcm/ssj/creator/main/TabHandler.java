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

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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


public class TabHandler
{
	private Activity activity;
	//tabs
	private TabHost tabHost = null;
	private LinkedHashMap<ITab, TabHost.TabSpec> firstTabs = new LinkedHashMap<>();
	private LinkedHashMap<Object, TabHost.TabSpec> additionalTabs = new LinkedHashMap<>();

	private Canvas canvas;
	private Console console;

	//REMOVE!
	private boolean annotationExists = false;
	private AnnotationTab annotationTab = null;


	public TabHandler(Activity activity)
	{
		this.activity = activity;
		tabHost = (TabHost) activity.findViewById(R.id.id_tabHost);
		if (tabHost != null)
		{
			tabHost.setup();
			//canvas
			canvas = new Canvas(this.activity);
			firstTabs.put(canvas, getTabSpecForITab(canvas));
			//console
			console = new Console(this.activity);
			firstTabs.put(console, getTabSpecForITab(console));
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

	private void checkAdditionalTabs() {

/*
		List<Component> iFileWriters = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, IFileWriter.class);
		removeTabsOfClass(IFileWriter.class);
		if(!containsOfClass(firstTabs, IFileWriter.class))
		{

		}
*/



		List<Component> signalPainters = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, SignalPainter.class);
		removeObsoleteTabsOfClass(signalPainters, SignalPainter.class);
		for(Component signalPainter : signalPainters)
		{
			if(additionalTabs.containsKey(signalPainter))
				continue;

			GraphView graphView = ((SignalPainter) signalPainter).options.graphView.get();
			if (graphView == null)
			{
				graphView = new GraphView(activity);
				((SignalPainter) signalPainter).options.graphView.set(graphView);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(graphView, signalPainter.getComponentName(), android.R.drawable.ic_menu_view);
			additionalTabs.put(signalPainter, tabSpec);
		}

		List<Component> cameraPainters = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, CameraPainter.class);
		removeObsoleteTabsOfClass(cameraPainters, CameraPainter.class);
		for(Component cameraPainter : cameraPainters)
		{
			if(additionalTabs.containsKey(cameraPainter))
				continue;

			SurfaceView surfaceView = ((CameraPainter) cameraPainter).options.surfaceView.get();
			if (surfaceView == null)
			{
				surfaceView = new SurfaceView(activity);
				((CameraPainter) cameraPainter).options.surfaceView.set(surfaceView);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(surfaceView, cameraPainter.getComponentName(), android.R.drawable.ic_menu_camera);
			additionalTabs.put(cameraPainter, tabSpec);
		}

		List<Component> feedbackManagers = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.EventHandler, FeedbackManager.class);
		removeObsoleteTabsOfClass(feedbackManagers, FeedbackManager.class);
		for(Component feedbackManager : feedbackManagers)
		{
			if(additionalTabs.containsKey(feedbackManager))
				continue;

			TableLayout tableLayout = ((FeedbackManager) feedbackManager).options.layout.get();
			if (tableLayout == null)
			{
				tableLayout = new TableLayout(activity);
				((FeedbackManager) feedbackManager).options.layout.set(tableLayout);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(tableLayout, feedbackManager.getComponentName(), android.R.drawable.ic_menu_compass); // TODO: Change icon.
			additionalTabs.put(feedbackManager, tabSpec);
		}

		List<Component> visualFeedbacks = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.EventHandler, VisualFeedback.class);
		removeTabsOfClass(VisualFeedback.class);
		if(!visualFeedbacks.isEmpty())
		{
			TableLayout visualFeedbackLayout = getTableLayoutForVisualFeedback(visualFeedbacks);
			for (Component visualFeedback : visualFeedbacks)
			{
				((VisualFeedback) visualFeedback).options.layout.set(visualFeedbackLayout);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(visualFeedbackLayout, visualFeedbacks.get(0).getComponentName(), android.R.drawable.ic_menu_compass); // TODO: Change icon.
			additionalTabs.put(visualFeedbacks.get(0), tabSpec);
		}

		buildTabs();
	}

	private TableLayout getTableLayoutForVisualFeedback(List<Component> visualFeedbackComponents) {
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

	private void removeTabsOfClass(Class componentClass)
	{
		removeObsoleteTabsOfClass(new ArrayList<Component>(), componentClass);
	}

	private void removeObsoleteTabsOfClass(List<Component> components, Class componentClass)
	{
		Iterator<Object> iterator = additionalTabs.keySet().iterator();
		while (iterator.hasNext())
		{
			Object additionalTabObject = iterator.next();
			if(!components.contains(additionalTabObject) && componentClass.isInstance(additionalTabObject))
			{
				iterator.remove();
			}
		}
	}

	private<T,S> boolean containsOfClass(Map<T,S> map, Class clazz)
	{
		for(Map.Entry<T,S> entry : map.entrySet())
		{
			if (clazz.isInstance(entry.getKey()))
				return true;
		}
		return false;
	}

	private void buildTabs() {
		tabHost.setCurrentTab(0);
		tabHost.clearAllTabs();

		for(TabHost.TabSpec spec : firstTabs.values())
		{
			tabHost.addTab(spec);
		}

		for(TabHost.TabSpec spec : additionalTabs.values())
		{
			tabHost.addTab(spec);
		}
	}

	private TabHost.TabSpec getTabSpecForITab(ITab iTab) {
		return getNewTabSpec(iTab.getView(), iTab.getTitle(), iTab.getIcon());
	}

	private TabHost.TabSpec getNewTabSpec(final View view, String title, int icon) {
		final TabHost.TabSpec tabSpec = tabHost.newTabSpec(title);
		tabSpec.setContent(new TabHost.TabContentFactory()
		{
			public View createTabContent(String tag)
			{
				return view;
			}
		});
		tabSpec.setIndicator("", ContextCompat.getDrawable(activity, icon));
		return tabSpec;
	}


	public void preStart()
	{
		console.clear();

		if (annotationExists && annotationTab != null)
		{
			annotationTab.startAnnotation();
		}
	}

	public void preStop()
	{
		if (annotationExists && annotationTab != null)
		{
			annotationTab.finishAnnotation();
		}
	}

	public void actualizeContent(Util.AppAction appAction, Object o)
	{
		canvas.actualizeContent(appAction, o);
	}

	public void cleanUp()
	{
		console.cleanUp();
		canvas.cleanUp();
	}

	public AnnotationTab getAnnotation()
	{
		return annotationTab;
	}

}