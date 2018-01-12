/*
 * Option.java
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

package hcm.ssj.core.option;

import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Util;

/**
 * Standard option for SSJ.<br>
 * Created by Frank Gaibler on 04.03.2016.
 */
public class Option<T>
{
	private final String name;
	private T value;
	private final Class<T> type;
	private final String help;

	/**
	 * @param name  String
	 * @param value T
	 * @param type  Class
	 * @param help  String
	 */
	public Option(String name, T value, Class<T> type, String help)
	{
		this.name = name;
		this.value = value;
		this.type = type;
		this.help = help;
	}

	/**
	 * @return String
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * @return T
	 */
	public final T get()
	{
		return value;
	}

	/**
	 * @return T
	 */
	public String parseWildcards()
	{
		if (value != null && (type == String.class || type == FilePath.class || type == FolderPath.class))
		{
			String str = "";
			if (type == String.class)
				str = (String) value;
			else if (type == FilePath.class)
				str = ((FilePath) value).value;
			else if (type == FolderPath.class)
				str = ((FolderPath) value).value;

			if (str.contains("[time]"))
			{
				return str.replace("[time]", Util.getTimestamp(Pipeline.getInstance().getCreateTimeMs()));
			}
			else
			{
				return str;
			}
		}
		else
		{
			return null;
		}
	}

	/**
	 * @param value T
	 */
	public final void set(T value)
	{
		this.value = value;
	}

	/**
	 * @return Class
	 */
	public final Class<T> getType()
	{
		return type;
	}

	/**
	 * @return String
	 */
	public final String getHelp()
	{
		return help;
	}

	/**
	 * Tries to set the value by parsing the String parameter. <br>
	 * This method will only work with primitives, strings, arrays and enums.
	 *
	 * @param value String
	 * @return boolean
	 */
	public final boolean setValue(String value)
	{
		if (value == null || value.isEmpty())
		{
			set(null);
			return true;
		}
		else
		{
			if (!value.equals("-"))
			{
				//number primitives
				if (type == Byte.class)
				{
					return setValue(Byte.valueOf(value));
				}
				if (type == Short.class)
				{
					return setValue(Short.valueOf(value));
				}
				if (type == Integer.class)
				{
					return setValue(Integer.valueOf(value));
				}
				if (type == Long.class)
				{
					return setValue(Long.valueOf(value));
				}
				if (type == Float.class)
				{
					return setValue(Float.valueOf(value));
				}
				if (type == Double.class)
				{
					return setValue(Double.valueOf(value));
				}
				//arrays
				if (type.isArray())
				{
					String[] strings = value.replace("[", "").replace("]", "").split("\\s*,\\s*");
					//check strings for plausibility
					if (strings.length <= 0)
					{
						return false;
					}
					Class<?> componentType = type.getComponentType();
					if (componentType.isPrimitive())
					{
						//check strings for plausibility
						for (int i = 0; i < strings.length; i++)
						{
							strings[i] = strings[i].replace(",", "");
							if (strings[i].isEmpty())
							{
								return false;
							}
						}
						if (char.class.isAssignableFrom(componentType))
						{
							char[] ar = new char[strings.length];
							for (int i = 0; i < strings.length; i++)
							{
								ar[i] = strings[i].charAt(0);
							}
							set((T) ar);
							return true;
						}
						//check strings for plausibility
						for (String string : strings)
						{
							if (string.equals("-"))
							{
								return false;
							}
						}
						if (boolean.class.isAssignableFrom(componentType))
						{
							boolean[] ar = new boolean[strings.length];
							for (int i = 0; i < strings.length; i++)
							{
								ar[i] = Boolean.parseBoolean(strings[i]);
							}
							set((T) ar);
							return true;
						}
						if (byte.class.isAssignableFrom(componentType))
						{
							byte[] ar = new byte[strings.length];
							for (int i = 0; i < strings.length; i++)
							{
								ar[i] = Byte.parseByte(strings[i]);
							}
							set((T) ar);
							return true;
						}
						if (double.class.isAssignableFrom(componentType))
						{
							double[] ar = new double[strings.length];
							for (int i = 0; i < strings.length; i++)
							{
								ar[i] = Double.parseDouble(strings[i]);
							}
							set((T) ar);
							return true;
						}
						if (float.class.isAssignableFrom(componentType))
						{
							float[] ar = new float[strings.length];
							for (int i = 0; i < strings.length; i++)
							{
								ar[i] = Float.parseFloat(strings[i]);
							}
							set((T) ar);
							return true;
						}
						if (int.class.isAssignableFrom(componentType))
						{
							int[] ar = new int[strings.length];
							for (int i = 0; i < strings.length; i++)
							{
								ar[i] = Integer.parseInt(strings[i]);
							}
							set((T) ar);
							return true;
						}
						if (long.class.isAssignableFrom(componentType))
						{
							long[] ar = new long[strings.length];
							for (int i = 0; i < strings.length; i++)
							{
								ar[i] = Long.parseLong(strings[i]);
							}
							set((T) ar);
							return true;
						}
						if (short.class.isAssignableFrom(componentType))
						{
							short[] ar = new short[strings.length];
							for (int i = 0; i < strings.length; i++)
							{
								ar[i] = Short.parseShort(strings[i]);
							}
							set((T) ar);
							return true;
						}
					}
					else if (FilePath.class.isAssignableFrom(componentType))
					{
						FilePath[] ar = new FilePath[strings.length];
						for (int i = 0; i < strings.length; i++)
						{
							ar[i] = new FilePath(strings[i]);
						}
						set((T) ar);
						return true;
					}
					else if (FolderPath.class.isAssignableFrom(componentType))
					{
						FolderPath[] ar = new FolderPath[strings.length];
						for (int i = 0; i < strings.length; i++)
						{
							ar[i] = new FolderPath(strings[i]);
						}
						set((T) ar);
						return true;
					}
					else if (String.class.isAssignableFrom(componentType))
					{
						return setValue(strings);
					}
				}
			}
			//enums
			if (type.isEnum())
			{
				return setValue(Enum.valueOf((Class<Enum>) type, value));
			}
			//other primitives
			if (type == Character.class)
			{
				return setValue(value.charAt(0));
			}
			if (type == String.class)
			{
				set((T) value);
				return true;
			}
			if (type == FilePath.class)
			{
				return setValue(new FilePath(value));
			}
			if (type == FolderPath.class)
			{
				return setValue(new FolderPath(value));
			}
			if (type == Boolean.class)
			{
				return setValue(Boolean.valueOf(value));
			}
		}
		return false;
	}

	/**
	 * @param o Object
	 * @return boolean
	 */
	private boolean setValue(Object o)
	{
		try
		{
			set((T) o);
		}
		catch (ClassCastException ex)
		{
			set(null);
			return false;
		}
		return true;
	}

	/**
	 * Verifies if the option is assignable by {@link #setValue(String) setValue}
	 *
	 * @return boolean
	 */
	public boolean isAssignableByString()
	{
		return (type.isEnum()
				|| type.isArray()
				|| type == Boolean.class
				|| type == Character.class
				|| type == Byte.class
				|| type == Short.class
				|| type == Integer.class
				|| type == Long.class
				|| type == Float.class
				|| type == Double.class
				|| type == String.class
				|| type == FilePath.class
				|| type == FolderPath.class);
	}
}
