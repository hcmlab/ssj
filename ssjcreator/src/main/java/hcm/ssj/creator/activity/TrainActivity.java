/*
 * TrainActivity.java
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

package hcm.ssj.creator.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import hcm.ssj.core.Annotation;
import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.SSJDescriptor;
import hcm.ssj.file.FileCons;
import hcm.ssj.ml.Model;
import hcm.ssj.ml.ModelDescriptor;
import hcm.ssj.ml.Session;

/**
 * Visualize user-saved stream file data with the GraphView.
 */
public class TrainActivity extends AppCompatActivity
{
	private ChooserDialog chooserDialog;
	private Activity activity = this;

	ArrayList<Session> sessions = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.train_layout);

		//setup session list
		Button btn_add_session = (Button) findViewById(R.id.button_session_list_add);
		btn_add_session.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				LinearLayout sessionList = (LinearLayout) findViewById(R.id.session_list);

				int id = sessions.size();
				Session session = new Session();
				session.name = "Session " + id;
				sessions.add(session);
				sessionList.addView(createSessionView(session), id +1);
			}
		});

//		CheckBox checkBox = (CheckBox) findViewById(R.id.train_anno_convert);
//		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
//			{
//				int visibility = (isChecked) ? View.VISIBLE : View.GONE;
//				findViewById(R.id.train_anno_convert_frame_layout).setVisibility(visibility);
//				findViewById(R.id.train_anno_convert_fill_layout).setVisibility(visibility);
//				findViewById(R.id.train_anno_convert_label_layout).setVisibility(visibility);
//
//				findViewById(R.id.train_anno_convert_frame_layout).setEnabled(isChecked);
//				findViewById(R.id.train_anno_convert_fill_layout).setEnabled(isChecked);
//				findViewById(R.id.train_anno_convert_label_layout).setEnabled(isChecked);
//			}
//		});
//
//		findViewById(R.id.train_anno_convert_frame_layout).setVisibility(View.GONE);
//		findViewById(R.id.train_anno_convert_fill_layout).setVisibility(View.GONE);
//		findViewById(R.id.train_anno_convert_label_layout).setVisibility(View.GONE);
//		findViewById(R.id.train_anno_convert_frame_layout).setEnabled(false);
//		findViewById(R.id.train_anno_convert_fill_layout).setEnabled(false);
//		findViewById(R.id.train_anno_convert_label_layout).setEnabled(false);

		final EditText textModelPath = (EditText) findViewById(R.id.model_filepath);
		textModelPath.setText(FileCons.MODELS_DIR);

		ImageButton butModelPath = (ImageButton) findViewById(R.id.model_filepath_button);
		butModelPath.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				chooserDialog = new ChooserDialog().with(TrainActivity.this);
				chooserDialog.withFilter(true, false, "");

				if(textModelPath.getText().toString().isEmpty())
					chooserDialog.withStartFile(FileCons.SSJ_EXTERNAL_STORAGE);
				else
					chooserDialog.withStartFile(textModelPath.getText().toString());

				chooserDialog.withChosenListener(new ChooserDialog.Result()
				{
					@Override
					public void onChoosePath(String path, File pathFile)
					{
						EditText textview = (EditText) findViewById(R.id.model_filepath);
						textview.setText(path);
					}
				}).build();
				chooserDialog.show();
			}
		});

		//populate Model list
		ArrayList<String> items = new ArrayList<>();
		for(Class<?> model : SSJDescriptor.getInstance().models)
		{
			try
			{
				if(model.getMethod("train", Stream.class, String.class).getDeclaringClass() == model)
					items.add(model.getSimpleName());
			}
			catch (NoSuchMethodException e)
			{
				//do nothing, method is not implemented for model, thus ignore
			}
		}

		((Spinner) findViewById(R.id.model_selector)).setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items));

		Button butTrain = (Button) findViewById(R.id.train_button);
		butTrain.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				view.setEnabled(false);

				new Thread(new Runnable() {
					@Override
					public void run()
					{
						trainModel();
					}
				}).start();
			}
		});
	}

	private View createSessionView(final Session session_obj)
	{
		LinearLayout sessionLayout = new LinearLayout(this);
		sessionLayout.setBackgroundColor(Color.parseColor("#EEEEEE"));

		final TextView textView = new TextView(this);
		textView.setText(session_obj.name);
		textView.setTextSize(textView.getTextSize() * 0.5f);

		int dpValue = 8; // margin in dips
		float d = getResources().getDisplayMetrics().density;
		int margin = (int) (dpValue * d); // margin in pixels

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(margin, margin, margin, 0);
		sessionLayout.setLayoutParams(params);
		sessionLayout.addView(textView);

		//define popup for clicking on a session
		textView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				//content
				LinearLayout linearLayout = new LinearLayout(activity);
				linearLayout.setOrientation(LinearLayout.VERTICAL);

				final EditText nameText = new EditText(activity);
				nameText.setInputType(InputType.TYPE_CLASS_TEXT);
				nameText.setText(((TextView) v).getText(), TextView.BufferType.NORMAL);
				linearLayout.addView(nameText);

				LinearLayout.LayoutParams layout_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				LinearLayout.LayoutParams text_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
				LinearLayout.LayoutParams button_params = new LinearLayout.LayoutParams((int)(50 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.MATCH_PARENT);

				//stream file
				LinearLayout streamLayout = new LinearLayout(activity);
				streamLayout.setOrientation(LinearLayout.HORIZONTAL);
				streamLayout.setLayoutParams(layout_params);

				final EditText streamFile = new EditText(activity);
				streamFile.setInputType(InputType.TYPE_CLASS_TEXT);
				streamFile.setHint(R.string.train_load_stream_hint);
				streamFile.setText(session_obj.stream_path);
				streamFile.setLayoutParams(text_params);
				streamLayout.addView(streamFile);

				ImageButton streamLoadButton = new ImageButton(activity);
				streamLoadButton.setLayoutParams(button_params);
				streamLoadButton.setPadding(0, 10, 0, 0);
				streamLoadButton.setImageResource(R.drawable.ic_insert_drive_file_black_24dp);
				streamLoadButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						chooserDialog = new ChooserDialog().with(TrainActivity.this);
						chooserDialog.withFilter(false, "stream");

						if(streamFile.getText().toString().isEmpty())
							chooserDialog.withStartFile(FileCons.SSJ_EXTERNAL_STORAGE);
						else
							chooserDialog.withStartFile(streamFile.getText().toString());

						chooserDialog.withChosenListener(new ChooserDialog.Result() {
							@Override
							public void onChoosePath(String path, File pathFile) {
								streamFile.setText(path);
							}
						}).build();
						chooserDialog.show();
					}
				});
				streamLayout.addView(streamLoadButton);
				linearLayout.addView(streamLayout);

				//anno file
				LinearLayout annoLayout = new LinearLayout(activity);
				streamLayout.setOrientation(LinearLayout.HORIZONTAL);
				annoLayout.setLayoutParams(layout_params);

				final EditText annoFile = new EditText(activity);
				annoFile.setHint(R.string.train_load_anno_hint);
				annoFile.setText(session_obj.anno_path);
				annoFile.setInputType(InputType.TYPE_CLASS_TEXT);
				annoFile.setLayoutParams(text_params);
				annoLayout.addView(annoFile);

				ImageButton annoLoadButton = new ImageButton(activity);
				annoLoadButton.setLayoutParams(button_params);
				annoLoadButton.setPadding(0, 10, 0, 0);
				annoLoadButton.setImageResource(R.drawable.ic_insert_drive_file_black_24dp);
				annoLoadButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						chooserDialog = new ChooserDialog().with(TrainActivity.this);
						chooserDialog.withFilter(false, "annotation");

						if(annoFile.getText().toString().isEmpty() && streamFile.getText().toString().isEmpty())
							chooserDialog.withStartFile(FileCons.SSJ_EXTERNAL_STORAGE);
						else if(!annoFile.getText().toString().isEmpty())
							chooserDialog.withStartFile(annoFile.getText().toString());
						else if(!streamFile.getText().toString().isEmpty())
							chooserDialog.withStartFile(streamFile.getText().toString());

						chooserDialog.withChosenListener(new ChooserDialog.Result() {
							@Override
							public void onChoosePath(String path, File pathFile) {
								annoFile.setText(path);
							}
						}).build();
						chooserDialog.show();
					}
				});
				annoLayout.addView(annoLoadButton);
				linearLayout.addView(annoLayout);

				//dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle(R.string.str_session);
				builder.setView(linearLayout);
				builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						ViewGroup viewGroup = (ViewGroup) v.getParent();
						String name = nameText.getText().toString().trim();
						((TextView) viewGroup.getChildAt(0)).setText(name);

						session_obj.anno_path = annoFile.getText().toString();
						session_obj.stream_path = streamFile.getText().toString();
					}
				});
				builder.setNegativeButton(R.string.str_cancel, null);
				builder.setNeutralButton(R.string.str_delete, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						ViewGroup viewGroup = (ViewGroup) v.getParent();
						if (viewGroup != null)
						{
							sessions.remove(session_obj);
							((ViewGroup) viewGroup.getParent()).removeView(viewGroup);
						}
						v.invalidate();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});

		return sessionLayout;
	}

	private void trainModel()
	{
		if(sessions.isEmpty())
		{
			showToast("no data sources provided", Toast.LENGTH_SHORT);
			return;
		}

		SparseArray<String> classes = new SparseArray<>();
		for(int i = 0; i< sessions.size(); i++)
		{
			Session session = sessions.get(i);

			//load stream
			try
			{
				session.stream = Stream.load(session.stream_path);
			}
			catch (Exception e)
			{
				Log.e("error loading stream file", e);
				showToast("error loading stream file", Toast.LENGTH_SHORT);
				return;
			}

			//check if streams match
			if(i != 0)
			{
				if(session.stream.dim != sessions.get(0).stream.dim)
				{
					String msg = "stream dimension mismatch: " + session.stream.dim  +"!=" + sessions.get(0).stream.dim;
					Log.e(msg);
					showToast(msg, Toast.LENGTH_SHORT);
					return;
				}

				if(session.stream.sr != sessions.get(0).stream.sr)
				{
					String msg = "stream sr mismatch: " + session.stream.sr  +"!=" + sessions.get(0).stream.sr;
					Log.e(msg);
					showToast(msg, Toast.LENGTH_SHORT);
					return;
				}

				if(session.stream.type != sessions.get(0).stream.type)
				{
					String msg = "stream type mismatch: " + session.stream.type  +"!=" + sessions.get(0).stream.type;
					Log.e(msg);
					showToast(msg, Toast.LENGTH_SHORT);
					return;
				}
			}

			//load anno
			try
			{
				session.anno = new Annotation();
				session.anno.load(session.anno_path);
			}
			catch (IOException | XmlPullParserException e)
			{
				Log.e("error loading anno file", e);
				Toast.makeText(this, "error loading anno file", Toast.LENGTH_SHORT).show();
				return;
			}

			//update known classes
			for (int j = 0; j < session.anno.getClasses().size(); j++)
			{
				classes.put(session.anno.getClasses().keyAt(i), session.anno.getClasses().valueAt(i));
			}

			String emptyClass = null;

			CheckBox checkBox = (CheckBox) findViewById(R.id.train_anno_garbage);
			if (checkBox.isChecked())
			{
				emptyClass = "GARBAGE";
			}

//			CheckBox checkBox = (CheckBox) findViewById(R.id.train_anno_convert);
//			if (checkBox.isChecked())
//			{
//				String str_frame = ((EditText) findViewById(R.id.train_anno_convert_frame)).getText().toString();
//				String str_fill = ((EditText) findViewById(R.id.train_anno_convert_fill)).getText().toString();
//
//				String str_label = ((EditText) findViewById(R.id.train_anno_convert_label)).getText().toString();
//				if (str_label.isEmpty())
//					str_label = null;
//
//				if (!str_frame.isEmpty() && !str_fill.isEmpty())
//				{
//					float frame = Float.valueOf(str_frame);
//					if(frame < 1.0 / session.stream.sr)
//					{
//						String msg = "frame value too small for stream: min frame = " + 1.0 / session.stream.sr;
//						Log.e(msg);
//						showToast(msg, Toast.LENGTH_SHORT);
//						return;
//					}
//
//					session.anno.convertToFrames(frame, str_label, 0, Float.valueOf(str_fill));
//				}
//				else
//				{
//					Log.e("error converting anno to frames");
//					showToast("error converting anno to frames", Toast.LENGTH_SHORT);
//					return;
//				}
//			}
			session.anno.convertToFrames(1.0 / session.stream.sr, emptyClass, 0, 0.5f);
		}

		String str_model = ((Spinner) findViewById(R.id.model_selector)).getSelectedItem().toString();
		Model model = Model.create(str_model);
		if (model == null)
		{
			showToast("unknown model", Toast.LENGTH_SHORT);
			return;
		}

		//todo merge multiple streams
		showToast("model training started", Toast.LENGTH_SHORT);

		//init model
		String classes_array[] = new String[classes.size()];
		for (int i = 0; i < classes.size(); i++)
		{
			classes_array[i] = classes.valueAt(i);
		}
		model.init(classes_array, sessions.get(0).stream.dim);

		//train
		for(Session session : sessions)
			model.train(session.stream, session.anno, session.name);

		// save model
		Stream stream = sessions.get(0).stream;
		ModelDescriptor desc = new ModelDescriptor(model, stream.bytes, stream.dim, stream.sr, stream.type);

		String str_path = ((EditText) findViewById(R.id.model_filepath)).getText().toString();
		String str_name = ((EditText) findViewById(R.id.model_filename)).getText().toString();
		try
		{
			desc.save(str_path, str_name);
			showToast("model training finished", Toast.LENGTH_SHORT);
		}
		catch (IOException e)
		{
			Log.e("error writing model file", e);
			showToast("error writing model file", Toast.LENGTH_SHORT);
		}

		//enable button
		this.runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
				findViewById(R.id.train_button).setEnabled(true);
			}
		});
	}

	private void showToast(final String text, final int duration)
	{
		final Activity act = this;
		this.runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
				Toast.makeText(act, text, duration).show();
			}
		});
	}
}
