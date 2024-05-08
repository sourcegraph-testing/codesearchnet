/**
 * 
 */
package com.sporniket.libre.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading text in memory.
 * 
 * <p>
 * &copy; Copyright 2002-2019 David Sporn
 * </p>
 * <hr>
 * 
 * <p>
 * This file is part of <i>The Sporniket Core Library &#8211; io</i>.
 * 
 * <p>
 * <i>The Sporniket Core Library &#8211; io</i> is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * <p>
 * <i>The Sporniket Core Library &#8211; io</i> is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * <p>
 * You should have received a copy of the GNU Lesser General Public License along with <i>The Sporniket Core Library &#8211; io</i>.
 * If not, see <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>. 2
 * 
 * <hr>
 * 
 * @author David SPORN 
 * @version 19.04.00
 * @since 12.06.01
 */
// FIXME get an instance for iso-named encoding.
public class TextLoader
{
	/**
	 * Default buffer size for i/o operations.
	 */
	private static final int DEFAULT__BUFFER_SIZE = 4096;

	/**
	 * By default, use the system encoding.
	 */
	private static final Encoding DEFAULT__ENCODING = null;

	/**
	 * Default instance.
	 */
	private static final TextLoader INSTANCE = new TextLoader();

	/**
	 * Registry of instances by encoding.
	 */
	private static final Map<String, TextLoader> INSTANCE_BY_ENCODING = new HashMap<String, TextLoader>();

	/**
	 * Get the default instance.
	 * 
	 * @return the default instance.
	 */
	public static TextLoader getInstance()
	{
		return INSTANCE;
	}

	/**
	 * Get a default instance for the given encoding.
	 * 
	 * @param encoding the encoding to use.
	 * @return the instance for the encoding.
	 */
	public static TextLoader getInstance(Encoding encoding)
	{
		if (null == encoding)
		{
			return getInstance();
		}
		else
		{
			if (!INSTANCE_BY_ENCODING.containsKey(encoding.getIsoName()))
			{
				TextLoader _newInstance = new TextLoader(encoding);
				INSTANCE_BY_ENCODING.put(encoding.getIsoName(), _newInstance);
			}
			return INSTANCE_BY_ENCODING.get(encoding.getIsoName());
		}
	}

	private int myBufferSize;

	/**
	 * Encoding to use, none by default.
	 */
	private Encoding myEncoding;

	/**
	 * Instanciate a loader using a default buffer size and system encoding.
	 */
	public TextLoader()
	{
		this(DEFAULT__BUFFER_SIZE);
	}

	/**
	 * Instanciate a loader with the default buffer size and specified encoding.
	 * 
	 * @param encoding
	 *            encoding to use.
	 */
	public TextLoader(Encoding encoding)
	{
		this(DEFAULT__BUFFER_SIZE, encoding);
	}

	/**
	 * Instanciate a loader with the specified buffer size and system encoding.
	 * 
	 * @param bufferSize
	 *            buffer size in bytes.
	 */
	public TextLoader(int bufferSize)
	{
		this(bufferSize, DEFAULT__ENCODING);
	}

	/**
	 * Instanciate a loader with the specified buffer size and encoding.
	 * 
	 * @param bufferSize
	 *            buffer size in bytes.
	 * @param encoding
	 *            encoding to use.
	 */
	public TextLoader(int bufferSize, Encoding encoding)
	{
		setBufferSize(bufferSize);
		setEncoding(encoding);
	}

	/**
	 * Load a text from the specified file and put it in the provided StringBuffer.
	 * 
	 * @param source source file.
	 * @param buffer buffer to load text into.
	 * @return the buffer.
	 * @throws IOException if there is a problem to deal with.
	 */
	public StringBuffer append(File source, StringBuffer buffer) throws IOException
	{
		return append(new FileInputStream(source), buffer);
	}

	/**
	 * Load a text from the specified file and put it in the provided StringBuffer.
	 * 
	 * @param source source stream.
	 * @param buffer buffer to load text into.
	 * @return the buffer
	 * @throws IOException if there is a problem to deal with.
	 */
	public StringBuffer append(InputStream source, StringBuffer buffer) throws IOException
	{
		Reader _reader = FileTools.createReaderForInputStream(source, getEncoding());
		return append(_reader, buffer);
	}

	/**
	 * Load a text from the specified reader and put it in the provided StringBuffer.
	 * 
	 * @param source source reader.
	 * @param buffer buffer to load text into.
	 * @return the buffer
	 * @throws IOException if there is a problem to deal with.
	 */
	public StringBuffer append(Reader source, StringBuffer buffer) throws IOException
	{
		BufferedReader _bufferedReader = new BufferedReader(source);
		char[] _buffer = new char[getBufferSize()]; // load by chunk of 4 ko
		try
		{
			for (int _countReadChars = 0; _countReadChars >= 0;)
			{
				buffer.append(_buffer, 0, _countReadChars);
				_countReadChars = _bufferedReader.read(_buffer);
			}
		}
		finally
		{
			_bufferedReader.close();
		}
		return buffer;
	}

	/**
	 * @return the encoding
	 */
	public Encoding getEncoding()
	{
		return myEncoding;
	}

	/**
	 * Load a text from the specified File.
	 * 
	 * @param source source file.
	 * @return the text
	 * @throws IOException if there is a problem to deal with.
	 */
	public String load(File source) throws IOException
	{
		return append(source, new StringBuffer()).toString();
	}

	/**
	 * Load a text from the specified InputStream.
	 * 
	 * @param source source stream.
	 * @return the text
	 * @throws IOException if there is a problem to deal with.
	 */
	public String load(InputStream source) throws IOException
	{
		return append(source, new StringBuffer()).toString();
	}

	/**
	 * Load a text from the specified reader.
	 * 
	 * @param source source reader.
	 * @return the text
	 * @throws IOException if there is a problem to deal with.
	 */
	public String load(Reader source) throws IOException
	{
		return append(source, new StringBuffer()).toString();
	}

	/**
	 * Load a text file.
	 * 
	 * @param source source file.
	 * @return the text.
	 * @throws IOException if there is a problem to deal with.
	 */
	public String loadTextFile(File source) throws IOException
	{
		StringBuffer _result = new StringBuffer();
		append(source, _result);
		return _result.toString();
	}

	/**
	 * @param encoding
	 *            the encoding to set
	 */
	public void setEncoding(Encoding encoding)
	{
		myEncoding = encoding;
	}

	/**
	 * @return the bufferSize
	 */
	private int getBufferSize()
	{
		return myBufferSize;
	}

	/**
	 * @param bufferSize
	 *            the bufferSize to set
	 */
	private void setBufferSize(int bufferSize)
	{
		myBufferSize = bufferSize;
	}
}
