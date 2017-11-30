/*
 * OnlineNaiveBayes.java
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

package hcm.ssj.ml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.SimpleXmlParser;

/**
 * Created by Michael Dietz on 06.11.2017.
 */

public class OnlineNaiveBayes extends Model
{
	/**
	 * All options for OnlineNaiveBayes
	 */
	public class Options extends OptionList
	{
		/**
		 *
		 */
		private Options()
		{
			addOptions();
		}
	}

	private static final double NORMAL_CONSTANT = Math.sqrt(2 * Math.PI);
	private static final double DEFAULT_SAMPLE_WEIGHT = 1.0;
	private static final double INITIAL_MODEL_WEIGHT = 2.0;
	private static final int SSI_FORMAT_LENGTH = 2;
	private static final int SSJ_FORMAT_LENGTH = 4;

	// Options loaded from file
	private boolean logNormalDistribution = true;
	private boolean usePriorProbability = true;

	// Helper variables
	private Map<String, Integer> classNameIndices = null;
	private double[] featureValues = null;

	private int classCount;
	private int featureCount;
	private float[] classProbabilities;

	// Model values per class and per feature
	private double[][] mean = null;
	private double[][] varianceSum = null;
	private double[][] weightSum = null;
	private double[] classDistribution = null;

	// Options instance
	public final Options options = new Options();

	public OnlineNaiveBayes()
	{
		_name = this.getClass().getSimpleName();
	}

	/**
	 * Calculates the standard deviation for a feature dimension from a certain class
	 */
	private double getStdDev(int classIndex, int featureIndex)
	{
		return Math.sqrt(getVariance(classIndex, featureIndex));
	}

	/**
	 * Variance is calculated between last and current mean value.
	 * Therefore only x-1 variance values exist between x mean values.
	 */
	private double getVariance(int classIndex, int featureIndex)
	{
		return weightSum[classIndex][featureIndex] > 1.0 ? varianceSum[classIndex][featureIndex] / (weightSum[classIndex][featureIndex] - 1.0) : 0.0;
	}

	@Override
	public float[] forward(Stream stream)
	{
		if (!_isTrained)
		{
			Log.w("Not trained");
			return null;
		}
		if (stream.dim != featureCount)
		{
			Log.w("Feature dimension (" + featureCount + ") differs from input stream dimension (" + stream.dim + ")");
			return null;
		}

		// Convert stream to double values
		featureValues = getValuesAsDouble(stream, featureValues);

		double classDistributionSum = getClassDistributionSum();

		double probabilitySum = 0;

		// Do prediction
		if (logNormalDistribution)
		{
			for (int classIndex = 0; classIndex < classCount; classIndex++)
			{
				double probability = usePriorProbability ? naiveBayesLog(classDistribution[classIndex] / classDistributionSum) : 0;

				for (int featureIndex = 0; featureIndex < featureCount; featureIndex++)
				{
					double stdDev = getStdDev(classIndex, featureIndex);

					if (stdDev != 0)
					{
						probability += getProbabilityLog(featureValues[featureIndex], stdDev, mean[classIndex][featureIndex], weightSum[classIndex][featureIndex]);
					}
				}

				classProbabilities[classIndex] = (float) Math.exp(probability / featureCount);
				probabilitySum += classProbabilities[classIndex];
			}
		}
		else
		{
			for (int classIndex = 0; classIndex < classCount; classIndex++)
			{
				double probability = usePriorProbability ? classDistribution[classIndex] / classDistributionSum : 0;

				for (int featureIndex = 0; featureIndex < featureCount; featureIndex++)
				{
					probability *= getProbability(featureValues[featureIndex], getStdDev(classIndex, featureIndex), mean[classIndex][featureIndex], weightSum[classIndex][featureIndex]);
				}

				classProbabilities[classIndex] = (float) probability;
				probabilitySum += classProbabilities[classIndex];
			}
		}

		// Normalization
		if (probabilitySum == 0)
		{
			Log.w("Probability sum == 0");

			for (int i = 0; i < classCount; i++)
			{
				classProbabilities[i] = 1.0f / classCount;
			}
		}
		else
		{
			for (int i = 0; i < classCount; i++)
			{
				classProbabilities[i] /= probabilitySum;
			}
		}

		return classProbabilities;
	}

	private double getProbability(double featureValue, double stdDev, double mean, double weightSum)
	{
		double probability = 0;

		if (weightSum > 0.0)
		{
			if (stdDev > 0.0)
			{
				double diff = featureValue - mean;

				probability = (1.0 / (NORMAL_CONSTANT * stdDev)) * Math.exp(-(diff * diff / (2.0 * stdDev * stdDev)));
			}
			else
			{
				probability = featureValue == mean ? 1.0 : 0.0;
			}
		}

		return probability;
	}

	private double getProbabilityLog(double featureValue, double stdDev, double mean, double weightSum)
	{
		double probability = 0;

		if (stdDev != 0.0)
		{
			double diff = featureValue - mean;

			probability = -naiveBayesLog(stdDev) - (diff * diff) / (2 * stdDev * stdDev);
		}

		return probability;
	}

	private double getClassDistributionSum()
	{
		double sum = 0;

		for (double distributionValue : classDistribution)
		{
			sum += distributionValue;
		}

		return sum;
	}

	@Override
	public void train(Stream stream, String label)
	{
		if (classDistribution == null || classDistribution.length <= 0)
		{
			Log.w("Base model not loaded");
			return;
		}
		if (stream.dim != featureCount)
		{
			Log.w("Feature dimension differs");
			return;
		}
		if (!classNameIndices.containsKey(label))
		{
			Log.w("Class name (" + label + ") not found, data ignored!");
			return;
		}

		int classIndex = classNameIndices.get(label);

		double weight = DEFAULT_SAMPLE_WEIGHT;

		// Add to class distribution
		classDistribution[classIndex] += weight;

		// Convert stream to double values
		featureValues = getValuesAsDouble(stream, featureValues);

		// Train model for each feature dimension independently
		for (int featureIndex = 0; featureIndex < stream.dim; featureIndex++)
		{
			trainOnSample(featureValues[featureIndex], featureIndex, classIndex, weight);
		}
	}

	private void trainOnSample(double featureValue, int featureIndex, int classIndex, double weight)
	{
		if (Double.isInfinite(featureValue) || Double.isNaN(featureValue))
		{
			Log.w("Invalid featureValue[" + featureIndex + "] = " + featureValue + " ignored for training");
			return;
		}

		if (weightSum[classIndex][featureIndex] > 0.0)
		{
			weightSum[classIndex][featureIndex] += weight;

			double lastMean = mean[classIndex][featureIndex];

			mean[classIndex][featureIndex] += weight * (featureValue - lastMean) / weightSum[classIndex][featureIndex];
			varianceSum[classIndex][featureIndex] += weight * (featureValue - lastMean) * (featureValue - mean[classIndex][featureIndex]);
		}
		else
		{
			mean[classIndex][featureIndex] = featureValue;
			weightSum[classIndex][featureIndex] = weight;
		}
	}

	public void init(String[] classes, int n_features)
	{
		classCount = classes.length;
		featureCount = n_features;

		// Initialize model variables
		class_names = classes.clone();
		mean = new double[classCount][];
		varianceSum = new double[classCount][];
		weightSum = new double[classCount][];
		classDistribution = new double[classCount];

		for (int classIndex = 0; classIndex < classCount; classIndex++)
		{
			// Create arrays
			mean[classIndex] = new double[featureCount];
			varianceSum[classIndex] = new double[featureCount];
			weightSum[classIndex] = new double[featureCount];
		}

		classProbabilities = new float[classCount];

		// Store class indices for reverse lookup used in online learning
		classNameIndices = new HashMap<>();

		for (int i = 0; i < class_names.length; i++)
		{
			classNameIndices.put(class_names[i], i);
		}
	}

	@Override
	public void load(File file)
	{
		BufferedReader reader;
		try
		{
			InputStream inputStream = new FileInputStream(file);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			reader = new BufferedReader(inputStreamReader);
		}
		catch (FileNotFoundException e)
		{
			Log.e("File not found");
			return;
		}

		// Skip first comments
		String line;
		do
		{
			line = readLine(reader);
		}
		while (line.startsWith("#"));

		// Parse number of classes
		String token[] = line.split("\t");
		if (token.length > 0)
		{
			int classNum = Integer.valueOf(token[0]);

			if (classNum != n_classes)
			{
				Log.w("Model definition (n_classes) mismatch between trainer and model file: " + classNum + " != " + n_classes);
			}

			classCount = n_classes;
		}
		else
		{
			Log.w("Can't read number of classes from classifier file " + file.getName() + "!");
			return;
		}

		// Parse number of features
		if (token.length > 1)
		{
			featureCount = Integer.valueOf(token[1]);
		}
		else
		{
			Log.w("Can't read feature dimension from classifier file " + file.getName() + "'!");
			return;
		}

		// Initialize model variables
		mean = new double[classCount][];
		varianceSum = new double[classCount][];
		weightSum = new double[classCount][];
		classDistribution = new double[classCount];

		for (int classIndex = 0; classIndex < classCount; classIndex++)
		{
			// Create arrays
			mean[classIndex] = new double[featureCount];
			varianceSum[classIndex] = new double[featureCount];
			weightSum[classIndex] = new double[featureCount];

			// Load model values
			do
			{
				line = readLine(reader);
			}
			while (line.isEmpty() || line.startsWith("#"));
			token = line.split("\t");

			// Get class name
			String name = token[0];
			if (!name.equalsIgnoreCase(class_names[classIndex]))
			{
				Log.w("Model definition (name of class " + classIndex + ") mismatch between trainer and model file:" + name + " != " + class_names[classIndex]);

				// Overwrite class name
				class_names[classIndex] = name;
			}
			// Get class distribution
			classDistribution[classIndex] = Double.valueOf(token[1]);

			// Load feature values
			for (int featureIndex = 0; featureIndex < featureCount; featureIndex++)
			{
				line = readLine(reader);
				token = line.split("\t");

				if (token.length == SSI_FORMAT_LENGTH)
				{
					// Model is in offline (SSI) format with mean and std deviation
					mean[classIndex][featureIndex] = Double.valueOf(token[0]);
					varianceSum[classIndex][featureIndex] = Math.pow(Double.valueOf(token[1]), 2) * (INITIAL_MODEL_WEIGHT - 1);
					weightSum[classIndex][featureIndex] = INITIAL_MODEL_WEIGHT;
				}
				else if (token.length == SSJ_FORMAT_LENGTH)
				{
					// Model is in online format with mean, variance sum and weight sum
					mean[classIndex][featureIndex] = Double.valueOf(token[0]);
					varianceSum[classIndex][featureIndex] = Double.valueOf(token[2]);
					weightSum[classIndex][featureIndex] = Double.valueOf(token[3]);
				}
				else
				{
					Log.e("Unknown model format");
				}
			}
		}

		// Close model file
		try
		{
			reader.close();
		}
		catch (IOException e)
		{
			Log.e("Could not close reader");
		}

		classProbabilities = new float[classCount];

		// Store class indices for reverse lookup used in online learning
		classNameIndices = new HashMap<>();

		for (int i = 0; i < class_names.length; i++)
		{
			classNameIndices.put(class_names[i], i);
		}

		_isTrained = true;
	}

	/**
	 * Load data from option file
	 */
	@Override
	public void loadOption(File file)
	{
		if (file == null)
		{
			Log.w("Option file not set in options");
			return;
		}

		SimpleXmlParser simpleXmlParser = new SimpleXmlParser();
		try
		{
			SimpleXmlParser.XmlValues xmlValues = simpleXmlParser.parse(
					new FileInputStream(file),
					new String[]{"options", "item"},
					new String[]{"name", "value"}
			);
			ArrayList<String[]> foundAttributes = xmlValues.foundAttributes;
			for (String[] strings : foundAttributes)
			{
				if (strings[0].equals("log"))
				{
					logNormalDistribution = strings[1].equals("true");
				}
				else if (strings[0].equals("prior"))
				{
					usePriorProbability = strings[1].equals("true");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.e("File could not be parsed", e);
		}
	}

	@Override
	public void save(File file)
	{
		if (file == null)
		{
			Log.e("Model file not set in options");
			return;
		}

		BufferedWriter writer;
		try
		{
			OutputStream outputStream = new FileOutputStream(file);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
			writer = new BufferedWriter(outputStreamWriter);
		}
		catch (FileNotFoundException e)
		{
			Log.e("File not found");
			return;
		}

		try
		{
			// Write model header
			String header = "# Classifier type:\tnaive_bayes\n# number of classes\tfeature space dimension\n" + classCount + "\t" + featureCount + "\n\n# class\tprior class probability\n# mean\tstandard deviation\tvariance sum\tweight sum\n";
			writer.write(header);

			double classDistributionSum = getClassDistributionSum();

			for (int classIndex = 0; classIndex < classCount; classIndex++)
			{
				// Write class name and distribution
				writer.write(class_names[classIndex] + "\t" + classDistribution[classIndex] / classDistributionSum + "\n");

				// Write feature values
				for (int featureIndex = 0; featureIndex < featureCount; featureIndex++)
				{
					writer.write(mean[classIndex][featureIndex] + "\t" + getStdDev(classIndex, featureIndex) + "\t" + varianceSum[classIndex][featureIndex] + "\t" + weightSum[classIndex][featureIndex] + "\n");
				}

				writer.write("\n");
			}
		}
		catch (IOException e)
		{
			Log.e("Error while writing to file " + file.getAbsolutePath());
		}

		// Close model file
		try
		{
			writer.close();
		}
		catch (IOException e)
		{
			Log.e("Could not close reader");
		}
	}

	/**
	 * Helper function to read a line from the model file
	 */
	private String readLine(BufferedReader reader)
	{
		String line = null;
		if (reader != null)
		{
			try
			{
				line = reader.readLine();
			}
			catch (IOException e)
			{
				Log.e("could not read line");
			}
		}
		return line;
	}

	/**
	 * Calculates the log value
	 */
	private double naiveBayesLog(double x)
	{
		return x > (1e-20) ? Math.log(x) : -46; // Smallest log value
	}

	/**
	 * Transforms an input array of a stream into a double array and returns it.
	 *
	 * @param stream Stream
	 */
	private double[] getValuesAsDouble(Stream stream, double[] data)
	{
		if (data == null)
		{
			data = new double[stream.num * stream.dim];
		}

		switch (stream.type)
		{
			case CHAR:
				char[] chars = stream.ptrC();
				for (int i = 0; i < data.length; i++)
				{
					data[i] = (double) chars[i];
				}
				break;
			case SHORT:
				short[] shorts = stream.ptrS();
				for (int i = 0; i < data.length; i++)
				{
					data[i] = (double) shorts[i];
				}
				break;
			case INT:
				int[] ints = stream.ptrI();
				for (int i = 0; i < data.length; i++)
				{
					data[i] = (double) ints[i];
				}
				break;
			case LONG:
				long[] longs = stream.ptrL();
				for (int i = 0; i < data.length; i++)
				{
					data[i] = (double) longs[i];
				}
				break;
			case FLOAT:
				float[] floats = stream.ptrF();
				for (int i = 0; i < data.length; i++)
				{
					data[i] = (double) floats[i];
				}
				break;
			case DOUBLE:
				data = stream.ptrD();
				break;
			default:
				Log.e("invalid input stream type");
				break;
		}

		return data;
	}
}
