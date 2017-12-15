/*
 * SaveLoad.java
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

package hcm.ssj.creator.core;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hcm.ssj.core.Annotation;
import hcm.ssj.core.Component;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.creator.core.container.ContainerElement;
import hcm.ssj.creator.util.ConnectionType;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;
import hcm.ssj.ml.IModelHandler;
import hcm.ssj.ml.Model;

/**
 * Save and load files in a {@link PipelineBuilder} friendly format.<br>
 * Created by Frank Gaibler on 28.06.2016.
 */
public abstract class SaveLoad
{
	private final static String ROOT = "ssjSaveFile";
	private final static String VERSION = "version";
	private final static String VERSION_NUMBER = "5";
	private final static String FRAMEWORK = "framework";
	private final static String SENSOR_CHANNEL_LIST = "sensorChannelList";
	private final static String SENSOR_LIST = "sensorList";
	private final static String TRANSFORMER_LIST = "transformerList";
	private final static String CONSUMER_LIST = "consumerList";
	private final static String EVENT_HANDLER_LIST = "eventHandlerList";
	private final static String SENSOR_CHANNEL = "sensorChannel";
	private final static String SENSOR = "sensor";
	private final static String TRANSFORMER = "transformer";
	private final static String CONSUMER = "consumer";
	private final static String EVENT_HANDLER = "eventHandler";
	private final static String MODEL = "model";
	private static final String MODEL_LIST = "modelList";
	private final static String CLASS = "class";
	private final static String ID = "id";
	private final static String OPTIONS = "options";
	private final static String OPTION = "option";
	private final static String NAME = "name";
	private final static String VALUE = "value";
	private final static String CHANNEL_ID = "providerId";
	private final static String CHANNEL_LIST = "providerList";
	private final static String EVENT_CHANNEL_ID = "eventProviderId";
	private final static String EVENT_CHANNEL_LIST = "eventProviderList";
	private final static String MODEL_HANDLER_ID = "modelHandlerId";
	private static final String MODEL_HANDLER_LIST = "modelHandlerList";
	private final static String FRAME_SIZE = "frameSize";
	private final static String DELTA = "delta";
	private final static String EVENT_TRIGGER = "eventTrigger";

	private static final String FEEDBACK_LEVEL_LIST = "feedbackLevelList";
	private static final String FEEDBACK_LEVEL = "feedbackLevel";
	private static final String FEEDBACK = "feedback";
	private static final String LEVEL = "level";
	private static final String FEEDBACK_BEHAVIOUR = "feedbackBehaviour";

	private final static String ANNOTATION = "annotation";
	private final static String ANNOTATION_CLASS = "class";
	private final static String FILE_NAME = "fileName";
	private final static String FILE_PATH = "filePath";

	/**
	 * Saves the values in {@link PipelineBuilder}
	 *
	 * @param file File
	 * @return boolean
	 */
	public static boolean save(File file)
	{
		//open stream
		FileOutputStream fileOutputStream;
		try
		{
			fileOutputStream = new FileOutputStream(file);

		}
		catch (FileNotFoundException e)
		{
			Log.e("file not found");
			return false;
		}
		XmlSerializer serializer = Xml.newSerializer();
		try
		{
			//start document
			serializer.setOutput(fileOutputStream, "UTF-8");
			serializer.startDocument(null, true);
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serializer.startTag(null, ROOT);
			serializer.attribute(null, VERSION, VERSION_NUMBER);
			//framework
			serializer.startTag(null, FRAMEWORK);
			addOptions(serializer, Pipeline.getInstance());
			serializer.endTag(null, FRAMEWORK);
			//sensorChannels
			serializer.startTag(null, SENSOR_CHANNEL_LIST);
			for (ContainerElement<SensorChannel> containerElement : PipelineBuilder.getInstance().hsSensorChannelElements)
			{
				addContainerElement(serializer, SENSOR_CHANNEL, containerElement, false);
			}
			serializer.endTag(null, SENSOR_CHANNEL_LIST);
			//sensors
			serializer.startTag(null, SENSOR_LIST);
			for (ContainerElement<Sensor> containerElement : PipelineBuilder.getInstance().hsSensorElements)
			{
				addContainerElement(serializer, SENSOR, containerElement, false);
			}
			serializer.endTag(null, SENSOR_LIST);
			//transformers
			serializer.startTag(null, TRANSFORMER_LIST);
			for (ContainerElement<Transformer> containerElement : PipelineBuilder.getInstance().hsTransformerElements)
			{
				addContainerElement(serializer, TRANSFORMER, containerElement, true);
			}
			serializer.endTag(null, TRANSFORMER_LIST);
			//consumers
			serializer.startTag(null, CONSUMER_LIST);
			for (ContainerElement<Consumer> containerElement : PipelineBuilder.getInstance().hsConsumerElements)
			{
				addContainerElement(serializer, CONSUMER, containerElement, true);
			}
			serializer.endTag(null, CONSUMER_LIST);
			//eventhandler
			serializer.startTag(null, EVENT_HANDLER_LIST);
			for (ContainerElement<EventHandler> containerElement : PipelineBuilder.getInstance().hsEventHandlerElements)
			{
				addContainerElement(serializer, EVENT_HANDLER, containerElement, true);
			}
			serializer.endTag(null, EVENT_HANDLER_LIST);
			//models
			serializer.startTag(null, MODEL_LIST);
			for (ContainerElement<Model> containerElement : PipelineBuilder.getInstance().hsModelElements)
			{
				addContainerElement(serializer, MODEL, containerElement, false);
			}
			serializer.endTag(null, MODEL_LIST);
			//annotation
			if(PipelineBuilder.getInstance().annotationExists())
			{
				addAnnotation(serializer, PipelineBuilder.getInstance().getAnnotation());
			}
			//finish document
			serializer.endTag(null, ROOT);
			serializer.endDocument();
			serializer.flush();
		}
		catch (IOException ex)
		{
			Log.e("could not save file");
			return false;
		}
		finally
		{
			try
			{
				fileOutputStream.close();
			}
			catch (IOException ex)
			{
				Log.e("could not close stream");
			}
		}
		return true;
	}

	/**
	 * @param File file
	 * @return boolean
	 */
	public static boolean load(File file)
	{
		InputStream inputStream = null;

		try
		{
			inputStream = new FileInputStream(file);

			//check file version
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(inputStream, null);
			parser.nextTag();
			if (parser.getName().equals(ROOT))
			{
				String value = parser.getAttributeValue(null, VERSION);
				float versionFile = Float.parseFloat(value);
				float versionCurrent = Float.parseFloat(VERSION_NUMBER);
				if (versionFile < versionCurrent)
				{
					Log.i("old file version detected, converting from v" + versionFile + " to v" + versionCurrent);
					String text = convertOldVersion(file, versionFile);
					inputStream.close();
					inputStream = new ByteArrayInputStream(text.getBytes());

                    //reset stream
                    parser.setInput(inputStream, null);
                    parser.nextTag();
                }
            } else
            {
                return false;
            }
            //clear previous content
            Pipeline.getInstance().clear();
            PipelineBuilder.getInstance().clear();

			//load classes
			parser.nextTag();
			String tag;
			Object context = null;
			Option[] options = null;
			LinkedHashMap<Object, LinkContainer> connectionMap = new LinkedHashMap<>();
			LinkedHashMap<Object, LinkFeedbackCollection> feedbackCollectionMap = new LinkedHashMap<>();
			while (!(tag = parser.getName()).equals(ROOT))
			{
				if (parser.getEventType() == XmlPullParser.START_TAG)
				{
					switch (tag)
					{
						case FRAMEWORK:
						{
							context = Pipeline.getInstance();
							break;
						}
						case OPTIONS:
						{
							options = PipelineBuilder.getOptionList(context);
							break;
						}
						case OPTION:
						{
							if (options != null)
							{
								String name = parser.getAttributeValue(null, NAME);
								String value = parser.getAttributeValue(null, VALUE);
								for (Option option : options)
								{
									if (option.getName().equals(name))
									{
										option.setValue(value);
										break;
									}
								}
							}
							break;
						}
						case SENSOR_CHANNEL:
						case SENSOR:
						case MODEL:
						{
							String clazz = parser.getAttributeValue(null, CLASS);
							context = Class.forName(clazz).newInstance();
							PipelineBuilder.getInstance().add(context);
							String hash = parser.getAttributeValue(null, ID);
							LinkContainer container = new LinkContainer();
							container.hash = Integer.parseInt(hash);
							connectionMap.put(context, container);
							break;
						}
						case TRANSFORMER:
						case CONSUMER:
						case EVENT_HANDLER:
						{
							String clazz = parser.getAttributeValue(null, CLASS);
							context = Class.forName(clazz).newInstance();
							PipelineBuilder.getInstance().add(context);
							Double frame = (parser.getAttributeValue(null, FRAME_SIZE) != null) ? Double.valueOf(parser.getAttributeValue(null, FRAME_SIZE)) : null;
							PipelineBuilder.getInstance().setFrameSize(context, frame);
							PipelineBuilder.getInstance().setDelta(context, Double.valueOf(parser.getAttributeValue(null, DELTA)));

							String hash = parser.getAttributeValue(null, ID);
							LinkContainer container = new LinkContainer();
							container.hash = Integer.parseInt(hash);
							connectionMap.put(context, container);

							String trigger_hash = parser.getAttributeValue(null, EVENT_TRIGGER);
							if(trigger_hash != null)
								connectionMap.get(context).typedHashes.put(Integer.parseInt(trigger_hash), ConnectionType.EVENTTRIGGERCONNECTION);

							if (context instanceof FeedbackCollection)
							{
								LinkFeedbackCollection linkFeedbackCollection  = new LinkFeedbackCollection();
								linkFeedbackCollection.hash = Integer.parseInt(hash);
								feedbackCollectionMap.put(context, linkFeedbackCollection);
							}
							break;
						}
						case CHANNEL_ID:
						{
							String hash = parser.getAttributeValue(null, ID);
							connectionMap.get(context).typedHashes.put(Integer.parseInt(hash), ConnectionType.STREAMCONNECTION);
							break;
						}
						case EVENT_CHANNEL_ID:
						{
							String hash = parser.getAttributeValue(null, ID);
							connectionMap.get(context).typedHashes.put(Integer.parseInt(hash), ConnectionType.EVENTCONNECTION);
							break;
						}
						case MODEL_HANDLER_ID:
						{
							String hash = parser.getAttributeValue(null, ID);
							connectionMap.get(context).typedHashes.put(Integer.parseInt(hash), ConnectionType.MODELCONNECTION);
							break;
						}
						case FEEDBACK_LEVEL:
						{
							int level = Integer.parseInt(parser.getAttributeValue(null, LEVEL));
							LinkFeedbackCollection contextLinkFeedbackCollection = feedbackCollectionMap.get(context);
							while (contextLinkFeedbackCollection.typedHashes.size() <= level)
							{
								contextLinkFeedbackCollection.typedHashes.add(new LinkedHashMap<Integer, FeedbackCollection.LevelBehaviour>());
							}

							Map<Integer, FeedbackCollection.LevelBehaviour> levelLinkMap = contextLinkFeedbackCollection.typedHashes.get(level);
							parser.nextTag();
							while (parser.getName().equals(FEEDBACK))
							{
								if(parser.getEventType() == XmlPullParser.START_TAG)
								{
									int hash = Integer.parseInt(parser.getAttributeValue(null, ID));
									FeedbackCollection.LevelBehaviour behaviour = FeedbackCollection.LevelBehaviour.valueOf(parser.getAttributeValue(null, FEEDBACK_BEHAVIOUR));
									levelLinkMap.put(hash, behaviour);
								}
								parser.nextTag();
							}

							break;
						}
						case ANNOTATION:
						{
							String hash = parser.getAttributeValue(null, ID);
							LinkContainer container = new LinkContainer();
							container.hash = Integer.parseInt(hash);
							container.typedHashes.put(Integer.parseInt(hash), ConnectionType.EVENTTRIGGERCONNECTION);
							connectionMap.put(PipelineBuilder.getInstance().getAnnotation(), container);

							String filename = parser.getAttributeValue(null, FILE_NAME);
							if(filename != null && !filename.isEmpty())
								PipelineBuilder.getInstance().getAnnotation().setFileName(filename);

							String filepath = parser.getAttributeValue(null, FILE_PATH);
							if(filepath != null && !filepath.isEmpty())
								PipelineBuilder.getInstance().getAnnotation().setFilePath(filepath);

							break;
						}
						case ANNOTATION_CLASS:
						{
							String annoClass = parser.getAttributeValue(null, NAME);
							PipelineBuilder.getInstance().getAnnotation().appendClass(annoClass);
							break;
						}
					}
				}
				parser.nextTag();
			}

			setConnections(connectionMap);

			setInnerFeedbackCollectionComponents(feedbackCollectionMap, connectionMap);

			return true;
		}


		catch (IOException | XmlPullParserException ex)
		{
			Log.e("could not parse file", ex);
			return false;
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex)
		{
			Log.e("could not create class", ex);
			return false;
		}
		finally
		{
			try
			{
				if (inputStream != null)
				{
					inputStream.close();
				}
			}
			catch (IOException ex)
			{
				Log.e("could not close stream", ex);
			}
		}
	}

	private static void setConnections(LinkedHashMap<Object, LinkContainer> connectionMap)
	{
		for (Map.Entry<Object, LinkContainer> entry : connectionMap.entrySet())
		{
			Object key = entry.getKey();
			LinkContainer value = entry.getValue();

			for (int provider : value.typedHashes.keySet())
			{
				for (Map.Entry<Object, LinkContainer> candidate : connectionMap.entrySet())
				{
					Object candidateKey = candidate.getKey();
					LinkContainer candidateValue = candidate.getValue();
					if (candidateValue.hash == provider)
					{
						if (value.typedHashes.get(provider).equals(ConnectionType.STREAMCONNECTION))
						{
							PipelineBuilder.getInstance().addStreamProvider(key, (Provider) candidateKey);
						}
						else if (value.typedHashes.get(provider).equals(ConnectionType.EVENTCONNECTION))
						{
							PipelineBuilder.getInstance().addEventProvider(key, (Component) candidateKey);
						}
						else if (value.typedHashes.get(provider).equals(ConnectionType.EVENTTRIGGERCONNECTION))
						{
							PipelineBuilder.getInstance().setEventTrigger(key, candidateKey);
						}
						else if (value.typedHashes.get(provider).equals(ConnectionType.MODELCONNECTION))
						{
							PipelineBuilder.getInstance().addModelConnection((Component) key, (Component) candidateKey);
						}
					}
				}
			}
		}
	}

	private static void setInnerFeedbackCollectionComponents(Map<Object, LinkFeedbackCollection> feedbackCollectionMap, Map<Object, LinkContainer> connectionMap)
	{
		//set inner components of FeedbackCollection
		for (Map.Entry<Object, LinkFeedbackCollection> entry : feedbackCollectionMap.entrySet())
		{
			if (!(entry.getKey() instanceof FeedbackCollection))
			{
				continue;
			}

			FeedbackCollection feedbackCollection = (FeedbackCollection) entry.getKey();
			LinkFeedbackCollection linkFeedbackCollection = entry.getValue();

			for (int level = 0; level < linkFeedbackCollection.typedHashes.size(); level++)
			{
				for (Map.Entry<Integer, FeedbackCollection.LevelBehaviour> feedbackEntry : linkFeedbackCollection.typedHashes.get(level).entrySet())
				{
					for (Map.Entry<Object, LinkContainer> candidate : connectionMap.entrySet())
					{
						if (candidate.getValue().hash == feedbackEntry.getKey())
						{
							Feedback feedback = (Feedback) candidate.getKey();
							PipelineBuilder.getInstance().addFeedbackToCollectionContainer(feedbackCollection, feedback, level, feedbackEntry.getValue());
						}
					}

				}
			}
		}
	}

	/**
	 * @param serializer XmlSerializer
	 * @param object     Object
	 * @throws IOException
	 */
	private static void addStandard(XmlSerializer serializer, Object object) throws IOException
	{
		serializer.attribute(null, CLASS, object.getClass().getName());
		serializer.attribute(null, ID, String.valueOf(object.hashCode()));
	}

	/**
	 * @param serializer XmlSerializer
	 * @param object     Object
	 * @throws IOException
	 */
	private static void addOptions(XmlSerializer serializer, Object object) throws IOException
	{
		serializer.startTag(null, OPTIONS);
		Option[] options = PipelineBuilder.getOptionList(object);
		if (options != null)
		{
			for (Option option : options)
			{
				if (option.isAssignableByString() && option.get() != null)
				{
					serializer.startTag(null, OPTION);
					serializer.attribute(null, NAME, option.getName());
					if (option.getType().isArray())
					{
						Object value = option.get();
						List ar = new ArrayList();
						int length = Array.getLength(value);
						for (int i = 0; i < length; i++)
						{
							ar.add(Array.get(value, i));
						}
						Object[] objects = ar.toArray();
						serializer.attribute(null, VALUE, Arrays.toString(objects));
					}
					else
					{
						serializer.attribute(null, VALUE, String.valueOf(option.get()));
					}
					serializer.endTag(null, OPTION);
				}
			}
		}
		serializer.endTag(null, OPTIONS);
	}

	/**
	 * @param serializer       XmlSerializer
	 * @param tag              String
	 * @param containerElement ContainerElement
	 * @param withAttributes   boolean
	 */
	private static void addContainerElement(XmlSerializer serializer, String tag, ContainerElement<?> containerElement, boolean withAttributes) throws IOException
	{
		serializer.startTag(null, tag);
		addStandard(serializer, containerElement.getElement());
		if (withAttributes)
		{
			if (containerElement.getFrameSize() != null)
			{
				serializer.attribute(null, FRAME_SIZE, String.valueOf(containerElement.getFrameSize()));
			}
			serializer.attribute(null, DELTA, String.valueOf(containerElement.getDelta()));

			if(containerElement.getEventTrigger() != null)
			{
				serializer.attribute(null, EVENT_TRIGGER, String.valueOf(containerElement.getEventTrigger().hashCode()));
			}
		}
		addOptions(serializer, containerElement.getElement());

		HashMap<Provider, Boolean> streamHashMap = containerElement.getHmStreamProviders();
		if(streamHashMap.size() > 0)
		{
			serializer.startTag(null, CHANNEL_LIST);
			for (Map.Entry<Provider, Boolean> element : streamHashMap.entrySet())
			{
				serializer.startTag(null, CHANNEL_ID);
				serializer.attribute(null, ID, String.valueOf(element.getKey().hashCode()));
				serializer.endTag(null, CHANNEL_ID);
			}
			serializer.endTag(null, CHANNEL_LIST);
		}

		HashMap<Component, Boolean> eventHashMap = containerElement.getHmEventProviders();
		if(eventHashMap.size() > 0)
		{
			serializer.startTag(null, EVENT_CHANNEL_LIST);
			for (Map.Entry<Component, Boolean> element : eventHashMap.entrySet())
			{
				serializer.startTag(null, EVENT_CHANNEL_ID);
				serializer.attribute(null, ID, String.valueOf(element.getKey().hashCode()));
				serializer.endTag(null, EVENT_CHANNEL_ID);
			}
			serializer.endTag(null, EVENT_CHANNEL_LIST);
		}

		if (containerElement.getElement() instanceof FeedbackCollection)
		{
			addFeedbackCollectionTag(serializer, containerElement);
		}

		HashMap<IModelHandler, Boolean> modelMap = containerElement.getHmModelHandlers();
		if(modelMap.size() > 0)
		{
			serializer.startTag(null, MODEL_HANDLER_LIST);
			for (Map.Entry<IModelHandler, Boolean> element : modelMap.entrySet())
			{
				serializer.startTag(null, MODEL_HANDLER_ID);
				serializer.attribute(null, ID, String.valueOf(element.getKey().hashCode()));
				serializer.endTag(null, MODEL_HANDLER_ID);
			}
			serializer.endTag(null, MODEL_HANDLER_LIST);
		}

		serializer.endTag(null, tag);
	}

	private static void addFeedbackCollectionTag(XmlSerializer serializer, ContainerElement<?> containerElement) throws IOException
	{
		serializer.startTag(null, FEEDBACK_LEVEL_LIST);

		FeedbackCollection feedbackCollection = (FeedbackCollection) containerElement.getElement();
		List<Map<Feedback, FeedbackCollection.LevelBehaviour>> feedbackLevelList = feedbackCollection.getFeedbackList();

		for (int i = 0; i < feedbackLevelList.size(); i++)
		{
			serializer.startTag(null, FEEDBACK_LEVEL);
			serializer.attribute(null, LEVEL, String.valueOf(i));

			for (Map.Entry<Feedback, FeedbackCollection.LevelBehaviour> entry : feedbackLevelList.get(i).entrySet())
			{
				serializer.startTag(null, FEEDBACK);
				serializer.attribute(null, FEEDBACK_BEHAVIOUR, entry.getValue().toString());
				serializer.attribute(null, ID, String.valueOf(entry.getKey().hashCode()));
				serializer.endTag(null, FEEDBACK);
			}

			serializer.endTag(null, FEEDBACK_LEVEL);
		}

		serializer.endTag(null, FEEDBACK_LEVEL_LIST);
	}

	/**
	 * @param serializer       XmlSerializer
	 * @param anno Annotation
	 */
	private static void addAnnotation(XmlSerializer serializer, Annotation anno) throws IOException
	{
		serializer.startTag(null, ANNOTATION);
		addStandard(serializer, anno);
		serializer.attribute(null, FILE_NAME, anno.getFileName());
		serializer.attribute(null, FILE_PATH, anno.getFilePath());

		String[] anno_classes = anno.getClassArray();
		for (String anno_class : anno_classes)
		{
			serializer.startTag(null, ANNOTATION_CLASS);
			serializer.attribute(null, NAME, anno_class);
			serializer.endTag(null, ANNOTATION_CLASS);
		}
		serializer.endTag(null, ANNOTATION);
	}

	private static String convertOldVersion(File file, float from_version) throws IOException
	{
		int bufferSize = 10240;
		char[] buffer = new char[bufferSize];

		FileReader reader = new FileReader(file);
		int len = reader.read(buffer);
		buffer[len] = '\0';

		String text = new String(buffer, 0, len);

		//from v0.2 to v3
		if(from_version == 0.2)
		{
			text = text.replace("Provider", "Channel");
			text = text.replace("SimpleFile", "File");
			text = text.replace("Classifier", "ClassifierT");
			text = text.replace("option name=\"timeoutThread\"", "option name=\"waitThreadKill\"");
			text = text.replaceFirst(ROOT + " version=\".+\"", ROOT + " version=\"" + VERSION_NUMBER + "\"");
		}

		if(from_version == 3)
		{
			text = text.replaceAll("eventTrigger=\"(true|false)\"", "");
			text = text.replaceFirst(ROOT + " version=\".+\"", ROOT + " version=\"" + VERSION_NUMBER + "\"");
		}

		java.io.FileWriter writer = new java.io.FileWriter(file);
		writer.write(text);
		writer.flush();

		return text;
	}

	/**
	 * Used to add connections.
	 */
	private static class LinkContainer
	{
		int hash;
		LinkedHashMap<Integer, ConnectionType> typedHashes = new LinkedHashMap<>();
	}

	/**
	 * Used to add inner feedback components for FeedbackCollection.
	 */
	private static class LinkFeedbackCollection
	{
		int hash;
		List<Map<Integer, FeedbackCollection.LevelBehaviour>> typedHashes = new ArrayList<>();
	}
}
