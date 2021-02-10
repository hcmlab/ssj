/*
 * TabHandler.java
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

package hcm.ssj.creator.main;

import android.app.Activity;
import androidx.core.content.ContextCompat;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TabHost;
import android.widget.TableLayout;

import com.jjoe64.graphview.GraphView;

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
import hcm.ssj.feedback.FeedbackCollection;
import hcm.ssj.feedback.VisualFeedback;
import hcm.ssj.file.IFileWriter;
import hcm.ssj.landmark.LandmarkPainter;
import hcm.ssj.graphic.GridPainter;
import hcm.ssj.graphic.SignalPainter;
import hcm.ssj.ml.Trainer;


public class TabHandler
{
	private Activity activity;
	//tabs
	private TabHost tabHost = null;
	private LinkedHashMap<ITab, TabHost.TabSpec> firstTabs = new LinkedHashMap<>();
	private LinkedHashMap<Component, TabHost.TabSpec> additionalTabs = new LinkedHashMap<>();

	private Canvas canvas;
	private Console console;

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

	private void checkAdditionalTabs()
	{
		checkAnnotationTabs();
		checkSignalPainterTabs();
		checkCameraPainterTabs();
		checkLandmarkPainterTabs();
		checkGridPainterTabs();
		checkFeedbackCollectionTabs();
		checkVisualFeedbackTabs();

		buildTabs();
	}

	private void checkVisualFeedbackTabs()
	{
		List<Component> visualFeedbacks = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.EventHandler, VisualFeedback.class);
		removeComponentsOfClass(additionalTabs, VisualFeedback.class);
		if (!visualFeedbacks.isEmpty())
		{
			boolean anyUnmanaged = false;
			TableLayout visualFeedbackLayout = getTableLayoutForVisualFeedback(visualFeedbacks);
			TabHost.TabSpec newTabSpec = getNewTabSpec(visualFeedbackLayout, visualFeedbacks.get(0).getComponentName(), android.R.drawable.ic_menu_compass); // TODO: Change icon.
			for (Component visualFeedback : visualFeedbacks)
			{
				boolean isManaged = PipelineBuilder.getInstance().isManagedFeedback(visualFeedback);

				if(! isManaged)
				{
					anyUnmanaged = true;
					((VisualFeedback) visualFeedback).options.layout.set(visualFeedbackLayout);
				}
			}
			if(anyUnmanaged)
				additionalTabs.put(visualFeedbacks.get(0), newTabSpec);
		}
	}

	private void checkFeedbackCollectionTabs()
	{
		List<Component> feedbackCollections = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.EventHandler, FeedbackCollection.class);
		removeObsoleteComponentsOfClass(additionalTabs, feedbackCollections, FeedbackCollection.class);
		for (Component feedbackCollection : feedbackCollections)
		{
			if (additionalTabs.containsKey(feedbackCollection))
			{
				continue;
			}

			TableLayout tableLayout = ((FeedbackCollection) feedbackCollection).options.layout.get();
			if (tableLayout == null)
			{
				tableLayout = new TableLayout(activity);
				((FeedbackCollection) feedbackCollection).options.layout.set(tableLayout);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(tableLayout, feedbackCollection.getComponentName(), android.R.drawable.ic_menu_compass); // TODO: Change icon.
			additionalTabs.put(feedbackCollection, tabSpec);
		}
	}

	private void checkCameraPainterTabs()
	{
		List<Component> cameraPainters = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, CameraPainter.class);
		removeObsoleteComponentsOfClass(additionalTabs, cameraPainters, CameraPainter.class);
		for (Component cameraPainter : cameraPainters)
		{
			if (additionalTabs.containsKey(cameraPainter))
			{
				continue;
			}

			SurfaceView surfaceView = ((CameraPainter) cameraPainter).options.surfaceView.get();
			if (surfaceView == null)
			{
				surfaceView = new SurfaceView(activity);
				((CameraPainter) cameraPainter).options.surfaceView.set(surfaceView);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(surfaceView, cameraPainter.getComponentName(), android.R.drawable.ic_menu_camera);
			additionalTabs.put(cameraPainter, tabSpec);
		}
	}

	private void checkLandmarkPainterTabs()
	{
		List<Component> landmarkPainters = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, LandmarkPainter.class);
		removeObsoleteComponentsOfClass(additionalTabs, landmarkPainters, LandmarkPainter.class);
		for (Component landmarkPainter : landmarkPainters)
		{
			if (additionalTabs.containsKey(landmarkPainter))
			{
				continue;
			}

			SurfaceView surfaceView = ((LandmarkPainter) landmarkPainter).options.surfaceView.get();
			if (surfaceView == null)
			{
				surfaceView = new SurfaceView(activity);
				((LandmarkPainter) landmarkPainter).options.surfaceView.set(surfaceView);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(surfaceView, landmarkPainter.getComponentName(), android.R.drawable.ic_menu_camera);
			additionalTabs.put(landmarkPainter, tabSpec);
		}
	}

	private void checkGridPainterTabs()
	{
		List<Component> gridPainters = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, GridPainter.class);
		removeObsoleteComponentsOfClass(additionalTabs, gridPainters, GridPainter.class);
		for (Component gridPainter : gridPainters)
		{
			if (additionalTabs.containsKey(gridPainter))
			{
				continue;
			}

			SurfaceView surfaceView = ((GridPainter) gridPainter).options.surfaceView.get();
			if (surfaceView == null)
			{
				surfaceView = new SurfaceView(activity);
				((GridPainter) gridPainter).options.surfaceView.set(surfaceView);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(surfaceView, gridPainter.getComponentName(), android.R.drawable.ic_menu_view);
			additionalTabs.put(gridPainter, tabSpec);
		}
	}

	private void checkSignalPainterTabs()
	{
		List<Component> signalPainters = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, SignalPainter.class);
		removeObsoleteComponentsOfClass(additionalTabs, signalPainters, SignalPainter.class);
		for (Component signalPainter : signalPainters)
		{
			if (additionalTabs.containsKey(signalPainter))
			{
				continue;
			}

			GraphView graphView = ((SignalPainter) signalPainter).options.graphView.get();
			if (graphView == null)
			{
				graphView = new GraphView(activity);
				((SignalPainter) signalPainter).options.graphView.set(graphView);
			}
			TabHost.TabSpec tabSpec = getNewTabSpec(graphView, signalPainter.getComponentName(), android.R.drawable.ic_menu_view);
			additionalTabs.put(signalPainter, tabSpec);
		}
	}

	private void checkAnnotationTabs()
	{
		List<Component> iFileWriters = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, IFileWriter.class);
		List<Component> trainers = PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, Trainer.class);

		if (iFileWriters.isEmpty() && trainers.isEmpty())
		{
			removeComponentsOfClass(firstTabs, AnnotationTab.class);
		}
		else if (!containsOfClass(firstTabs, AnnotationTab.class))
		{
			AnnotationTab annotationTab = new AnnotationTab(activity);
			TabHost.TabSpec annotationTabSpec = getTabSpecForITab(annotationTab);
			firstTabs.put(annotationTab, annotationTabSpec);
		}
		else //Annotation tab is already here, refresh it
		{
			getAnnotation().syncWithModel();
		}
	}

	private TableLayout getTableLayoutForVisualFeedback(List<Component> visualFeedbackComponents)
	{
		List<TableLayout> layouts = new ArrayList<>();
		for (Object visualFeedback : visualFeedbackComponents)
		{
			TableLayout currentLayout = ((VisualFeedback) visualFeedback).options.layout.get();
			if (currentLayout != null && !layouts.contains(currentLayout))
			{
				layouts.add(currentLayout);
			}

		}
		return (layouts.size() == 1) ? layouts.get(0) : new TableLayout(activity);
	}

	private void removeComponentsOfClass(Map map, Class objectClass)
	{
		removeObsoleteComponentsOfClass(map, new ArrayList<Component>(), objectClass);
	}

	private void removeObsoleteComponentsOfClass(Map map, List<Component> retainableObjects, Class objectClass)
	{
		Iterator<Object> iterator = map.keySet().iterator();
		while (iterator.hasNext())
		{
			Object additionalTabObject = iterator.next();
			if (!retainableObjects.contains(additionalTabObject) && objectClass.isInstance(additionalTabObject))
			{
				iterator.remove();
			}
		}
	}

	private <T, S> boolean containsOfClass(Map<T, S> map, Class objectClass)
	{
		for (Map.Entry entry : map.entrySet())
		{
			if (objectClass.isInstance(entry.getKey()))
			{
				return true;
			}
		}
		return false;
	}

	private void buildTabs()
	{
		tabHost.setCurrentTab(0);
		tabHost.clearAllTabs();

		for (TabHost.TabSpec spec : firstTabs.values())
		{
			tabHost.addTab(spec);
		}

		for (TabHost.TabSpec spec : additionalTabs.values())
		{
			tabHost.addTab(spec);
		}
	}

	private TabHost.TabSpec getTabSpecForITab(ITab iTab)
	{
		return getNewTabSpec(iTab.getView(), iTab.getTitle(), iTab.getIcon());
	}

	private TabHost.TabSpec getNewTabSpec(final View view, String title, int icon)
	{
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
		AnnotationTab annotationTab = getAnnotation();
		if (annotationTab != null)
		{
			annotationTab.startAnnotation();
		}
	}

	public void preStop()
	{
		AnnotationTab annotationTab = getAnnotation();
		if (annotationTab != null)
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
		for (ITab iTab : firstTabs.keySet())
		{
			if (iTab instanceof AnnotationTab)
			{
				return (AnnotationTab) iTab;
			}
		}
		return null;
	}

}