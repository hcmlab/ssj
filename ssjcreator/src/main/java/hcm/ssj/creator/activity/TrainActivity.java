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
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
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
import hcm.ssj.creator.util.Util;
import hcm.ssj.ml.Model;
import hcm.ssj.ml.ModelDescriptor;

/**
 * Visualize user-saved stream file data with the GraphView.
 */
public class TrainActivity extends AppCompatActivity
{
	private ChooserDialog chooserDialog;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.train_layout);

		ImageButton butLoadStream = (ImageButton) findViewById(R.id.load_stream);
		butLoadStream.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				chooserDialog = new ChooserDialog().with(TrainActivity.this);
				chooserDialog.withFilter(false, "stream");
				chooserDialog.withStartFile(Environment.getExternalStorageDirectory().getPath());
				chooserDialog.withChosenListener(new ChooserDialog.Result() {
					@Override
					public void onChoosePath(String path, File pathFile) {
						EditText textview = (EditText) findViewById(R.id.text_stream);
						textview.setText(path);
					}
				}).build();
				chooserDialog.show();
			}
		});

		ImageButton butLoadAnno = (ImageButton) findViewById(R.id.load_anno);
		butLoadAnno.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				chooserDialog = new ChooserDialog().with(TrainActivity.this);
				chooserDialog.withFilter(false, "annotation");
				chooserDialog.withStartFile(Environment.getExternalStorageDirectory().getPath() + File.separator + Util.SSJ);
				chooserDialog.withChosenListener(new ChooserDialog.Result() {
					@Override
					public void onChoosePath(String path, File pathFile) {
						EditText textview = (EditText) findViewById(R.id.text_anno);
						textview.setText(path);
					}
				}).build();
				chooserDialog.show();
			}
		});

		ImageButton butModelPath = (ImageButton) findViewById(R.id.model_filepath_button);
		butModelPath.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				chooserDialog = new ChooserDialog().with(TrainActivity.this);
				chooserDialog.withFilter(true, false, "");
				chooserDialog.withStartFile(Environment.getExternalStorageDirectory().getPath() + File.separator + Util.SSJ);
				chooserDialog.withChosenListener(new ChooserDialog.Result() {
					@Override
					public void onChoosePath(String path, File pathFile) {
						EditText textview = (EditText) findViewById(R.id.model_filepath);
						textview.setText(path);
					}
				}).build();
				chooserDialog.show();
			}
		});

		CheckBox checkBox = (CheckBox) findViewById(R.id.train_anno_convert);
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				int visibility = (isChecked) ? View.VISIBLE : View.GONE;
				findViewById(R.id.train_anno_convert_frame_layout).setVisibility(visibility);
				findViewById(R.id.train_anno_convert_fill_layout).setVisibility(visibility);
				findViewById(R.id.train_anno_convert_label_layout).setVisibility(visibility);

				findViewById(R.id.train_anno_convert_frame_layout).setEnabled(isChecked);
				findViewById(R.id.train_anno_convert_fill_layout).setEnabled(isChecked);
				findViewById(R.id.train_anno_convert_label_layout).setEnabled(isChecked);
			}
		});

		findViewById(R.id.train_anno_convert_frame_layout).setVisibility(View.GONE);
		findViewById(R.id.train_anno_convert_fill_layout).setVisibility(View.GONE);
		findViewById(R.id.train_anno_convert_label_layout).setVisibility(View.GONE);
		findViewById(R.id.train_anno_convert_frame_layout).setEnabled(false);
		findViewById(R.id.train_anno_convert_fill_layout).setEnabled(false);
		findViewById(R.id.train_anno_convert_label_layout).setEnabled(false);

		//populate Model list
		ArrayList<String> items = new ArrayList<>();
		for(Class model : SSJDescriptor.getInstance().models)
			items.add(model.getSimpleName());

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

	private void trainModel()
	{
		String streamFilePath = ((EditText) findViewById(R.id.text_stream)).getText().toString();
		if(streamFilePath.isEmpty())
		{
			showToast("invalid stream file(s)", Toast.LENGTH_SHORT);
			return;
		}

		String annoFilePath = ((EditText) findViewById(R.id.text_anno)).getText().toString();
		if(streamFilePath.isEmpty())
		{
			showToast("invalid anno file", Toast.LENGTH_SHORT);
			return;
		}

		//load stream
		Stream stream = null;
		try
		{
			stream = Stream.load(streamFilePath);
		}
		catch (Exception e)
		{
			Log.e("error loading stream file", e);
			showToast("error loading stream file", Toast.LENGTH_SHORT);
			return;
		}

		//load anno
		Annotation anno = new Annotation();
		try
		{
			anno.load(annoFilePath);
		}
		catch (IOException | XmlPullParserException e)
		{
			Log.e("error loading anno file", e);
			Toast.makeText(this, "error loading anno file", Toast.LENGTH_SHORT).show();
			return;
		}

		CheckBox checkBox = (CheckBox) findViewById(R.id.train_anno_convert);
		if(checkBox.isChecked())
		{
			String str_frame = ((EditText) findViewById(R.id.train_anno_convert_frame)).getText().toString();
			String str_fill = ((EditText) findViewById(R.id.train_anno_convert_fill)).getText().toString();

			String str_label = ((EditText) findViewById(R.id.train_anno_convert_label)).getText().toString();
			if(str_label.isEmpty())
				str_label = null;

			if(!str_frame.isEmpty() && !str_fill.isEmpty())
				anno.convertToFrames(Float.valueOf(str_frame), str_label, 0, Float.valueOf(str_fill));
			else
			{
				Log.e("error converting anno to frames");
				showToast("error converting anno to frames", Toast.LENGTH_SHORT);
				return;
			}
		}

		String str_model = ((Spinner) findViewById(R.id.model_selector)).getSelectedItem().toString();
		Model model = Model.create(str_model);

		//todo merge multiple streams
		showToast("model training started", Toast.LENGTH_SHORT);

		//train
		model.init(anno.getClasses().toArray(new String[]{}), stream.dim);
		model.train(stream, anno, "1");

		String str_path = ((EditText) findViewById(R.id.model_filepath)).getText().toString();
		String str_name = ((EditText) findViewById(R.id.model_filename)).getText().toString();
		ModelDescriptor desc = new ModelDescriptor(model, stream.bytes, stream.dim, stream.sr, stream.type);
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
