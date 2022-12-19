/*
 * MainActivity.java
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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.Toast;

import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.TileEvent;

import java.util.ArrayList;
import java.util.List;

import hcm.ssj.core.ExceptionHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.Monitor;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.PipelineStateListener;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.BandComm;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.core.SSJDescriptor;
import hcm.ssj.creator.dialogs.AddDialog;
import hcm.ssj.creator.dialogs.FileDialog;
import hcm.ssj.creator.dialogs.Listener;
import hcm.ssj.creator.main.AnnotationTab;
import hcm.ssj.creator.main.TabHandler;
import hcm.ssj.creator.util.DemoHandler;
import hcm.ssj.creator.util.Util;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, PipelineStateListener, ActivityCompat.OnRequestPermissionsResultCallback
{
	private static final int REQUEST_DANGEROUS_PERMISSIONS = 108;
	private static boolean ready = true;
	private boolean firstStart = false;
	//tabs
	private TabHandler tabHandler;

	private boolean actionButtonsVisible = false;

	private LinearLayout sensorLayout;
	private LinearLayout sensorChannelLayout;
	private LinearLayout transformerLayout;
	private LinearLayout consumerLayout;
	private LinearLayout eventHandlerLayout;
	private LinearLayout modelLayout;

	private Animation showButton;
	private Animation hideButton;
	private Animation showLayout;
	private Animation hideLayout;

	private FloatingActionButton fab;

	private BroadcastReceiver msBandReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{

			android.util.Log.i("SSJCreator", "received tile event");
			TileButtonEvent data = intent.getParcelableExtra(TileEvent.TILE_EVENT_DATA);

			if (!data.getPageID().equals(BandComm.pageId))
			{
				return;
			}

			//toggle button
			AnnotationTab anno = tabHandler.getAnnotation();
			if (anno == null)
			{
				return;
			}

			anno.toggleAnnoButton(anno.getBandAnnoButton(), data.getElementID() == BandComm.BTN_YES);
		}
	};

	/**
	 *
	 */
	private void init()
	{
		// Initialize action button layouts with their corresponding event listeners.
		initAddComponentButtons();
		initActionButtonLayouts();
		initFloatingActionButton();

		// Init tabs
		tabHandler = new TabHandler(MainActivity.this);

		// Display location popup
		showLocationDialog();

		// Handle permissions
		// checkPermissions();

		// Set exception handler
		setExceptionHandler();

		// Register receiver for ms band events
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.microsoft.band.action.ACTION_TILE_BUTTON_PRESSED");
		registerReceiver(msBandReceiver, filter);
	}

	private void showLocationDialog()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle(R.string.str_location_title)
					.setMessage(R.string.str_location_message)
					.setPositiveButton(R.string.str_location_button, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							checkPermissions();
						}
					})
					.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog)
						{
							checkPermissions();
						}
					})
					.show();
		}
		else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
				!= PackageManager.PERMISSION_GRANTED
				|| ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED
				|| ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
				!= PackageManager.PERMISSION_GRANTED
				|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED)
		{
			checkPermissions();
		}
	}

	/**
	 *
	 */
	private void setExceptionHandler()
	{
		ExceptionHandler exceptionHandler = new ExceptionHandler()
		{
			@Override
			public void handle(final String location, final String msg, final Throwable t)
			{
				Monitor.notifyMonitor();
				Handler handler = new Handler(Looper.getMainLooper());
				Runnable runnable = new Runnable()
				{
					public void run()
					{

						String text = location + ": " + msg;

						Throwable cause = t;
						while (cause != null)
						{
							text += "\ncaused by: " + cause.getMessage();
							cause = cause.getCause();
						}

						new AlertDialog.Builder(MainActivity.this)
								.setTitle(R.string.str_error)
								.setMessage(text)
								.setPositiveButton(R.string.str_ok, null)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					}
				};
				handler.post(runnable);
			}
		};
		Pipeline.getInstance().setExceptionHandler(exceptionHandler);
	}

	/**
	 * Request permissions through system dialog
	 */
	private void checkPermissions()
	{
		if (Build.VERSION.SDK_INT >= 23)
		{
			String[] permissions = new String[]{
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.BODY_SENSORS,
					Manifest.permission.CAMERA,
					Manifest.permission.RECORD_AUDIO
			};

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
			{
				permissions = new String[]{
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_BACKGROUND_LOCATION,
						Manifest.permission.BODY_SENSORS,
						Manifest.permission.CAMERA,
						Manifest.permission.RECORD_AUDIO
				};
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			{
				permissions = new String[]{
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.BODY_SENSORS,
						Manifest.permission.CAMERA,
						Manifest.permission.RECORD_AUDIO
				};
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			{
				permissions = new String[]{
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.BODY_SENSORS,
						Manifest.permission.CAMERA,
						Manifest.permission.RECORD_AUDIO,
						Manifest.permission.BLUETOOTH_SCAN,
						Manifest.permission.BLUETOOTH_CONNECT,
				};
			}

			// Dangerous permissions
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED
					|| ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
					!= PackageManager.PERMISSION_GRANTED
					|| ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
					!= PackageManager.PERMISSION_GRANTED
					|| ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
					!= PackageManager.PERMISSION_GRANTED
					|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED)
			{
				ActivityCompat.requestPermissions(this, permissions, REQUEST_DANGEROUS_PERMISSIONS);
			}
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		if (requestCode == REQUEST_DANGEROUS_PERMISSIONS)
		{
			List<String> permissionList = new ArrayList<>();

			for (int i = 0; i < grantResults.length; i++)
			{
				if (grantResults[i] != PackageManager.PERMISSION_GRANTED
						&& !permissions[i].equalsIgnoreCase("android.permission.FOREGROUND_SERVICE")
						&& !permissions[i].equalsIgnoreCase("android.permission.MANAGE_EXTERNAL_STORAGE")
						&& !permissions[i].equalsIgnoreCase("com.samsung.WATCH_APP_TYPE.Companion")
						&& !permissions[i].equalsIgnoreCase("com.samsung.accessory.permission.ACCESSORY_FRAMEWORK"))
				{
					permissionList.add(permissions[i]);

					Log.i("Missing permission: " + permissions[i]);
				}
			}

			// Request all file access
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			{
				if (!Environment.isExternalStorageManager())
				{
					Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
					Uri uri = Uri.fromParts("package", getPackageName(), null);
					intent.setData(uri);
					startActivity(intent);
				}
			}
		}
	}

	/**
	 * @param view View
	 */
	public void buttonPressed(View view)
	{
		switch (view.getId())
		{
			case R.id.id_imageButton:
			{
				handlePipe();
				break;
			}
		}
	}

	/**
	 * Start or stop pipe
	 */
	private void handlePipeOld()
	{
		if (ready)
		{
			ready = false;
			new Thread()
			{
				@Override
				public void run()
				{
					//change button text
					changeImageButton(android.R.drawable.ic_popup_sync, false);
					//save framework options
					Pipeline pipeline = Pipeline.getInstance();
					//remove old content
					pipeline.clear();
					pipeline.resetCreateTime();
					//add components
					try
					{
						PipelineBuilder.getInstance().buildPipe();
					}
					catch (Exception e)
					{
						Log.e(getString(R.string.err_buildPipe), e);
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								Toast.makeText(getApplicationContext(), R.string.err_buildPipe, Toast.LENGTH_LONG).show();
							}
						});
						ready = true;
						changeImageButton(android.R.drawable.ic_media_play, true);
						return;
					}
					//change button text
					changeImageButton(android.R.drawable.ic_media_pause, true);
					//notify tabs
					tabHandler.preStart();
					//start framework
					pipeline.start();

					//run
					if (pipeline.isRunning())
					{
						Monitor.waitMonitor();
					}

					//stop framework
					try
					{
						tabHandler.preStop();
						pipeline.stop();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					ready = true;
					//change button text
					changeImageButton(android.R.drawable.ic_media_play, true);
				}
			}.start();
		}
		else
		{
			Monitor.notifyMonitor();
		}
	}

	/**
	 * Start or stop pipe
	 */
	private void handlePipe()
	{
		if (ready)
		{
			ready = false;
			new Thread()
			{
				@Override
				public void run()
				{
					// Change button text
					changeImageButton(android.R.drawable.ic_popup_sync, false);

					// Save framework options
					Pipeline pipeline = Pipeline.getInstance();

					// Remove old content
					pipeline.clear();
					pipeline.resetCreateTime();
					pipeline.registerStateListener(MainActivity.this);

					// Add components
					try
					{
						PipelineBuilder.getInstance().buildPipe();
					}
					catch (Exception e)
					{
						Log.e(getString(R.string.err_buildPipe), e);
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								Toast.makeText(getApplicationContext(), R.string.err_buildPipe, Toast.LENGTH_LONG).show();
							}
						});
						ready = true;
						changeImageButton(android.R.drawable.ic_media_play, true);
						return;
					}
					// Change button text
					changeImageButton(android.R.drawable.ic_media_pause, true);

					//Notify tabs
					tabHandler.preStart();

					// Start framework
					startPipelineService();

					// Run
					Monitor.waitMonitor();

					// Stop framework
					try
					{
						stopPipelineService();
						tabHandler.preStop();
						pipeline.stop();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					ready = true;
					//change button text
					changeImageButton(android.R.drawable.ic_media_play, true);
				}
			}.start();
		}
		else
		{
			Monitor.notifyMonitor();
		}
	}

	@Override
	public void stateUpdated(Pipeline.State state)
	{
		if (!ready)
		{
			switch (state)
			{
				case STARTING:
					break;
				case RUNNING:
					break;
				case INACTIVE:
				case STOPPING:
					Monitor.notifyMonitor();
					break;
			}
		}
	}

	private void startPipelineService()
	{
		Intent serviceIntent = new Intent(this, PipelineService.class);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			startForegroundService(serviceIntent);
		}
		else
		{
			startService(serviceIntent);
		}
	}

	private void stopPipelineService()
	{
		Intent serviceIntent = new Intent(this, PipelineService.class);

		stopService(serviceIntent);
	}

	/**
	 * @param idImage int
	 */
	private void changeImageButton(final int idImage, final boolean enabled)
	{
		final ImageButton imageButton = (ImageButton) findViewById(R.id.id_imageButton);
		if (imageButton != null)
		{
			imageButton.post(new Runnable()
			{
				public void run()
				{
					imageButton.setImageResource(idImage);
					imageButton.setEnabled(enabled);
				}
			});
		}
	}


	/**
	 * @param resource int
	 * @param list     ArrayList
	 */
	private void showAddDialog(int resource, ArrayList<Class> list)
	{
		final AddDialog addDialog = new AddDialog();
		addDialog.setTitleMessage(resource);
		addDialog.setOption(list);
		Listener listener = new Listener()
		{
			@Override
			public void onPositiveEvent(Object[] o)
			{
				addDialog.removeListener(this);
				actualizeContent(Util.AppAction.ADD, o != null ? o[0] : null);
			}

			@Override
			public void onNegativeEvent(Object[] o)
			{
				addDialog.removeListener(this);
			}
		};
		addDialog.addListener(listener);
		addDialog.show(getSupportFragmentManager(), MainActivity.this.getClass().getSimpleName());
	}

	/**
	 * @param title   int
	 * @param type    FileDialog.Type
	 * @param message int
	 */
	private void showFileDialog(final int title, final FileDialog.Type type, final int message)
	{
		if (firstStart)
		{
			DemoHandler.copyFiles(MainActivity.this);
		}

		final FileDialog fileDialog = new FileDialog();
		fileDialog.setTitleMessage(title);
		fileDialog.setType(type);
		fileDialog.show(getSupportFragmentManager(), MainActivity.this.getClass().getSimpleName());
		Listener listener = new Listener()
		{
			@Override
			public void onPositiveEvent(Object[] o)
			{
				fileDialog.removeListener(this);
				if (type == FileDialog.Type.LOAD_PIPELINE || type == FileDialog.Type.LOAD_STRATEGY)
				{
					actualizeContent(Util.AppAction.LOAD, o != null ? o[0] : null);
				}
				else if (type == FileDialog.Type.SAVE)
				{
					actualizeContent(Util.AppAction.SAVE, o != null ? o[0] : null);
				}
			}

			@Override
			public void onNegativeEvent(Object[] o)
			{
				if (o != null)
				{
					Log.e(getResources().getString(message));
					new AlertDialog.Builder(MainActivity.this)
							.setTitle(message)
							.setPositiveButton(R.string.str_ok, null)
							.setIcon(android.R.drawable.ic_dialog_alert)
							.show();
				}
				fileDialog.removeListener(this);
			}
		};
		fileDialog.addListener(listener);
	}

	/**
	 * @param appAction Util.AppAction
	 * @param o         Object
	 */
	private void actualizeContent(Util.AppAction appAction, Object o)
	{
		tabHandler.actualizeContent(appAction, o);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		actualizeContent(Util.AppAction.DISPLAYED, null);
		if (!ready)
		{
			changeImageButton(android.R.drawable.ic_media_pause, true);
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		unregisterReceiver(msBandReceiver);

		tabHandler.cleanUp();
		Pipeline framework = Pipeline.getInstance();
		if (framework.isRunning())
		{
			framework.stop();
			stopPipelineService();
		}
		PipelineBuilder.getInstance().clear();
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		//  check if the activity has started before on this app version...
		String name = "LAST_VERSION";
		String ssjVersion = Pipeline.getVersion();

		SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		firstStart = !getPrefs.getString(name, "").equalsIgnoreCase(ssjVersion);

		//save current version in preferences so the next time this won't run again
		SharedPreferences.Editor e = getPrefs.edit();
		e.putString(name, ssjVersion);
		e.apply();

		if(firstStart)
			startTutorial();

		setContentView(R.layout.activity_main);

		loadAnimations();
		init();

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		final TabHost tabHost = (TabHost) findViewById(R.id.id_tabHost);
		tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener()
		{
			@Override
			public void onTabChanged(String s)
			{
				int currentTabId = tabHost.getCurrentTab();
				FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

				// Show floating action button only if canvas tab is selected.
				if (currentTabId == 0)
				{
					fab.show();
					fab.setEnabled(true);
				}
				else
				{
					if (actionButtonsVisible)
					{
						toggleActionButtons(false);
					}
					fab.hide();
					fab.setEnabled(false);
				}
			}
		});
	}

	/**
	 * Close drawer if open otherwise go to app home screen.
	 */
	@Override
	public void onBackPressed()
	{
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START))
		{
			drawer.closeDrawer(GravityCompat.START);
		}
		else
		{
			moveTaskToBack(true);
		}
	}

	private void startTutorial()
	{
		//launch app intro
		Intent i = new Intent(MainActivity.this, TutorialActivity.class);
		startActivity(i);
	}

	/**
	 * @param menu Menu
	 * @return boolean
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	/**
	 * @param item MenuItem
	 * @return boolean
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_framework:
			{
				Intent intent = new Intent(getApplicationContext(), OptionsActivity.class);
				startActivity(intent);
				return true;
			}
			case R.id.action_save:
			{
				showFileDialog(R.string.str_save, FileDialog.Type.SAVE, R.string.str_saveError);
				return true;
			}
			case R.id.action_load_pipeline:
			{
				showFileDialog(R.string.str_load_pipeline, FileDialog.Type.LOAD_PIPELINE, R.string.str_loadError);
				return true;
			}
			case R.id.action_load_strategy:
			{
				showFileDialog(R.string.str_load_strategy, FileDialog.Type.LOAD_STRATEGY, R.string.str_loadError);
				return true;
			}
			case R.id.action_delete_pipeline:
			{
				showFileDialog(R.string.str_delete, FileDialog.Type.DELETE_PIPELINE, R.string.str_deleteError);
				return true;
			}
			case R.id.action_clear:
			{
				PipelineBuilder.getInstance().clear();
				actualizeContent(Util.AppAction.CLEAR, null);
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item)
	{
		int itemId = item.getItemId();

//		if (itemId == R.id.action_graph)
//		{
//			Intent intent = new Intent(getApplicationContext(), GraphActivity.class);
//			startActivity(intent);
//		}
//		else
		if (itemId == R.id.action_train_model)
		{
			Intent intent = new Intent(getApplicationContext(), TrainActivity.class);
			startActivity(intent);
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);

		return false;
	}

	/**
	 * Initialize floating action button to show/hide SSJ component selection buttons.
	 */
	private void initFloatingActionButton()
	{
		fab = (FloatingActionButton) findViewById(R.id.fab);

		fab.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (actionButtonsVisible)
				{
					toggleActionButtons(false);
				}
				else
				{
					toggleActionButtons(true);
				}
			}
		});
	}

	/**
	 * Initialize all linear layouts that contain action buttons for SSJ component selection
	 * and their corresponding text labels.
	 */
	private void initActionButtonLayouts()
	{
		sensorLayout = (LinearLayout) findViewById(R.id.sensor_layout);
		sensorChannelLayout = (LinearLayout) findViewById(R.id.sensor_channel_layout);
		transformerLayout = (LinearLayout) findViewById(R.id.transformers_layout);
		consumerLayout = (LinearLayout) findViewById(R.id.consumer_layout);
		eventHandlerLayout = (LinearLayout) findViewById(R.id.event_handler_layout);
		modelLayout = (LinearLayout) findViewById(R.id.model_layout);
	}

	/**
	 * Initialize all action buttons for SSJ component selection and add corresponding event
	 * listeners.
	 */
	private void initAddComponentButtons()
	{
		FloatingActionButton addSensor = (FloatingActionButton) findViewById(R.id.action_sensors);
		addSensor.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_sensors, SSJDescriptor.getInstance().sensors);
			}
		});

		FloatingActionButton addProvider = (FloatingActionButton) findViewById(R.id.action_providers);
		addProvider.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_sensor_channels, SSJDescriptor.getInstance().sensorChannels);
			}
		});

		FloatingActionButton addTransformer = (FloatingActionButton) findViewById(R.id.action_transformers);
		addTransformer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_transformers, SSJDescriptor.getInstance().transformers);
			}
		});

		FloatingActionButton addConsumer = (FloatingActionButton) findViewById(R.id.action_consumers);
		addConsumer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_consumers, SSJDescriptor.getInstance().consumers);
			}
		});

		FloatingActionButton addEventHandler = (FloatingActionButton) findViewById(R.id.action_eventhandlers);
		addEventHandler.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_eventhandlers, SSJDescriptor.getInstance().eventHandlers);
			}
		});

		FloatingActionButton addModel = (FloatingActionButton) findViewById(R.id.action_models);
		addModel.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_models, SSJDescriptor.getInstance().models);
			}
		});
	}

	/**
	 * Load animations that toggle visibility of action buttons.
	 */
	private void loadAnimations()
	{
		showButton = AnimationUtils.loadAnimation(MainActivity.this, R.anim.show_button);
		hideButton = AnimationUtils.loadAnimation(MainActivity.this, R.anim.hide_button);
		showLayout = AnimationUtils.loadAnimation(MainActivity.this, R.anim.show_layout);
		hideLayout = AnimationUtils.loadAnimation(MainActivity.this, R.anim.hide_layout);
	}

	/**
	 * Toggle visibility of all floating action buttons.
	 * @param enable True to enable visibility, false otherwise.
	 */
	private void toggleActionButtons(boolean enable)
	{
		toggleLayout(sensorLayout, enable);
		toggleLayout(sensorChannelLayout, enable);
		toggleLayout(transformerLayout, enable);
		toggleLayout(consumerLayout, enable);
		toggleLayout(eventHandlerLayout, enable);
		toggleLayout(modelLayout, enable);
	}

	/**
	 * Toggle visibility of a linear layout and all of it's children.
	 * @param layout LinearLayout to show/hide.
	 * @param enable True to enable visibility, false otherwise.
	 */
	private void toggleLayout(LinearLayout layout, boolean enable)
	{
		actionButtonsVisible = enable;
		if (enable)
		{
			layout.setVisibility(View.VISIBLE);
			layout.startAnimation(showLayout);
			fab.startAnimation(showButton);
		}
		else
		{
			layout.setVisibility(View.GONE);
			layout.startAnimation(hideLayout);
			fab.startAnimation(hideButton);
		}
		for (int i = 0; i < layout.getChildCount(); i++)
		{
			layout.getChildAt(i).setEnabled(enable);
		}
	}
}
