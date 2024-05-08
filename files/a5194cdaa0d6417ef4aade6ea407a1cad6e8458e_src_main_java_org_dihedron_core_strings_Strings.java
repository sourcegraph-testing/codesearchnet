/*
 * Copyright (c) 2012-2015, Andrea Funto'. All rights reserved. See LICENSE for details.
 */ 


package org.dihedron.core.strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;

import org.dihedron.core.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility package for string operations.
 * 
 * @author Andrea Funto'
 */
@License
public final class Strings {
	
	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(Strings.class);
	
	/**
	 * Whether the {@link #split(String, String)} operation should trim tokens 
	 * before returning them, by default.
	 */
	public static final boolean DEFAULT_TRIM = true;
	
	/**
	 * The default character sequence to use for telling tokens apart in the
	 * {@link #split(String)} method, and to join them in {@link #join(Object...)}.
	 */
	public static final String DEFAULT_SEPARATOR = ",";
	
	/**
	 * The default character used for padding.
	 */
	public static final char DEFAULT_PADDING = ' ';
	
	/**
	 * A textual representation of the null string.
	 */
	public static final String NULL = "<null>";

	/**
	 * Returns the given object as a string, if not null, otherwise
	 * returns null.
	 * 
	 * @param object
	 *   the object to be safely converted to a string.
	 * @return
	 *   the string representation of the object, or null.
	 */
	public static String asString(Object object) {
		return asString(object, null);
	}
	
	/**
	 * Returns the given object as a string, if not null, otherwise
	 * returns null.
	 * 
	 * @param object
	 *   the object to be safely converted to a string.
	 * @param otherwise
	 *   if the object is null, return this value instead.
	 * @return
	 *   the string representation of the object if not null, the default
	 *   value passed in otherwise.
	 */
	public static String asString(Object object, String otherwise) {
		return object != null ? object.toString() : otherwise;
	}	
	
	/**
	 * Checks whether the given string is neither null nor blank.
	 * 
	 * @param string
	 *   the string to be checked.
	 * @return
	 *   <code>true</code> if the string is not null and has some content besides 
	 *   blank spaces.
	 */
	public static boolean isValid(String string) {
		return (string != null && string.trim().length() > 0);
	}
	
	/**
	 * Checks whether all the given strings are neither null nor blank.
	 * 
	 * @param strings
	 *   the strings to be checked.
	 * @return
	 *   <code>true</code> if all the strings are not null and have some content 
	 *   besides blank spaces.
	 */
	public static boolean areValid(String... strings) {
		boolean result = false;
		if(strings != null) {
			result = true;
			for(String string : strings) {
				result = result && isValid(string);
			}
		}
		return result;
	}
	
	/**
	 * Trims the input string if it is not null.
	 * 
	 * @param string
	 *   the string to be trimmed if not null.
	 * @return
	 *   the trimmed string, or null.
	 */
	public static String trim(String string) {
		if(string != null) {
			return string.trim();
		}
		return string;
	}
	
	/**
	 * Trims the given string from blank characters at the beginning.
	 * 
	 * @param string
	 *   the string to be trimmed if not null.
	 * @return
	 *   the trimmed string, or null.
	 */
	public static String trimLeft(String string) {
		if(string != null) {
			return string.replaceAll("^\\s*", "");
		}
		return string;
	}
	
	/**
	 * Trims the given string from blank characters at the end.
	 * 
	 * @param string
	 *   the string to be trimmed if not null.
	 * @return
	 *   the trimmed string, or null.
	 */
	public static String trimRight(String string) {
		if(string != null) {
			return string.replaceAll("\\s*$", "");
		}
		return string;
	}	
	
	/**
	 * Returns a safe concatenation of the input strings, or null if all strings 
	 * are null.
	 * 
	 * @param strings
	 *   the set of string to concatenate.
	 * @return
	 *   the concatenation of the input strings, or null if all strings are null.
	 */
	public static String concatenate(String... strings) {
		return join("", strings);
	}

	
	/**
	 * Splits the input string using the default separator (',') to identify its
	 * tokens; it will also trim the tokens. 
	 *  
	 * @param strings
	 *   the string to split.
	 * @return
	 *   an array of tokens.
	 */
	public static String[] split(String strings) {
		return split(strings, DEFAULT_SEPARATOR);
	}
	
	/**
	 * Splits the input string using the given separator to identify its tokens; 
	 * it will also trim the tokens. 
	 *  
	 * @param strings
	 *   the string to split.
	 * @param separator
	 *   the character sequence to use as a token separator.
	 * @return
	 *   an array of tokens.
	 */
	public static String[] split(String strings, String separator) {
		return split(strings, separator, DEFAULT_TRIM);
	}
	
	/**
	 * Splits the input string using the given separator to identify its tokens; 
	 * it will also trim the tokens, depending on the trim parameter. 
	 *  
	 * @param strings
	 *   the string to split.
	 * @param separator
	 *   the character sequence to use as a token separator.
	 * @param trim
	 *   whether the tokens should be trimmed.
	 * @return
	 *   an array of tokens.
	 */
	public static String[] split(String strings, String separator, boolean trim) {
		StringTokeniser tokeniser = new StringTokeniser(separator);
		String [] tokens = tokeniser.tokenise(strings);
		if(trim) {
			for(int i = 0; i < tokens.length; ++i) {
				tokens[i] = tokens[i] != null ? tokens[i].trim() : tokens[i];
			}
		}
		return tokens;
	}
	
	public static String join(Object... objects) {
		return join(DEFAULT_SEPARATOR, DEFAULT_TRIM, objects);
	}

	public static String join(String separator, Object... objects) {
		return join(separator, DEFAULT_TRIM, objects);
	}
	
	public static String join(boolean trim, Object... objects) {
		return join(DEFAULT_SEPARATOR, trim, objects);
	}	
	
	public static String join(String separator, boolean trim, Object... objects) {
		StringBuffer buffer = new StringBuffer();
		if(objects != null) {		
			for(Object object : objects) {			
				if(object != null) {
					if(buffer.length() != 0) {
						buffer.append(separator);
					}
					buffer.append(trim ? object.toString().trim() : object);
				}
			}
		}
		return buffer.toString();
	}
	
	/**
	 * Joins the given set of strings using the provided separator.
	 * 
	 * @param separator
	 *   a character sequence used to separate strings.
	 * @param strings
	 *   the set of strings to be joined.
	 * @return
	 *   a string containing the list of input strings, or null if no valid input
	 *   was provided.
	 */
	public static String join(String separator, Collection<String> strings) {
		if(strings != null) {
			String[] array = new String[strings.size()];
			return join(separator, strings.toArray(array)); 
		}
		return null;			
	}
	
	/**
	 * Joins the given set of strings using the provided separator.
	 * 
	 * @param separator
	 *   a character sequence used to separate strings.
	 * @param strings
	 *   the set of strings to be joined.
	 * @return
	 *   a string containing the list of input strings, or null if no valid input
	 *   was provided.
	 */
	public static String join(String separator, String... strings) {
		StringBuilder builder = new StringBuilder();
		if(strings != null) {
			boolean first = true;
			for(String string : strings) {
				if(string != null) {				
					if(!first) {
						builder.append(separator);					
					}
					builder.append(string);
					first = false;
				}
			}
			if(builder.length() > 0) {
				return builder.toString();
			}
		}
		return null;
	}
	
	
	/**
	 * Formats an output string by centring the given input string and padding 
	 * it with blanks.
	 * 
	 * @param string
	 *   the string to be centred.
	 * @param size
	 *   the final size of the output string.
	 * @return
	 *   a formatted string, where the input string is centred and the remaining
	 *   space is filled with blanks. 
	 */
	public static String centre(String string, int size) {
		return centre(string, size, DEFAULT_PADDING);
	}
    
	/**
	 * Formats an output string by centering the given input string and padding 
	 * it with the given character.
	 * 
	 * @param string
	 *   the string to be centred.
	 * @param size
	 *   the final size of the output string.
	 * @param padding 
	 *   the character to be used as padding.  
	 * @return
	 *   a formatted string, where the input string is centred and the remaining
	 *   space is filled with blanks. 
	 */
	public static String centre(String string, int size, char padding) {
		if(string == null || size <= string.length()) {
			return string;
		}		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < (size - string.length()) / 2; i++) {
			sb.append(padding);
		}
		sb.append(string);
		while (sb.length() < size) {
			sb.append(padding);
		}
		return sb.toString();
    }	

	/**
	 * Formats a string by padding it with the default character to the left, until
	 * the given length is reached.
	 * 
	 * @param string
	 *   the string to pad.
	 * @param size
	 *   the final length of the resulting padded string.
	 * @return
	 *   the original string padded to the left.
	 */
	public static String padLeft(String string, int size) {
		return padLeft(string, size, DEFAULT_PADDING);
	}
	
	/**
	 * Formats a string by padding it with the given character to the left, until
	 * the given length is reached.
	 * 
	 * @param string
	 *   the string to pad.
	 * @param size
	 *   the final length of the resulting padded string.
	 * @param padding
	 *   the character to use as padding.
	 * @return
	 *   the original string padded to the left.
	 */
	public static String padLeft(String string, int size, char padding) {
		if(string == null || size <= string.length()) {
			return string;
		}		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < (size - string.length()); i++) {
			sb.append(padding);
		}
		sb.append(string);
		return sb.toString();		
	}

	/**
	 * Formats a string by padding it with the default character to the right, until
	 * the given length is reached.
	 * 
	 * @param string
	 *   the string to pad.
	 * @param size
	 *   the final length of the resulting padded string.
	 * @return
	 *   the original string padded to the right.
	 */
	public static String padRight(String string, int size) {
		return padRight(string, size, DEFAULT_PADDING);
	}
	
	/**
	 * Formats a string by padding it with the given character to the right, until
	 * the given length is reached.
	 * 
	 * @param string
	 *   the string to pad.
	 * @param size
	 *   the final length of the resulting padded string.
	 * @param padding
	 *   the character to use as padding.
	 * @return
	 *   the original string padded to the right.
	 */
	public static String padRight(String string, int size, char padding) {
		if(string == null || size <= string.length()) {
			return string;
		}		
		StringBuilder sb = new StringBuilder();
		sb.append(string);
		while (sb.length() < size) {
			sb.append(padding);
		}
		return sb.toString();		
	}
	
	/**
	 * Returns the first valid string in the given set.
	 * 
	 * @param strings
	 *   a set of strings.
	 * @return
	 *   the first valid string in the set, or null if none is valid.
	 */
	public static String firstValidOf(String... strings) {
		if(strings != null) {
			for(int i = 0; i < strings.length; ++i) {
				if(Strings.isValid(strings[i])) {
					return strings[i];
				}
			}
		}
		return null;	
	}
	
	/**
	 * Reverses the input string.
	 * 
	 * @param string
	 *   the string to be reversed.
	 * @return
	 *   the reversed string.
	 */
	public static String reverse(String string) {
		if(string != null) {
			return new StringBuilder(string).reverse().toString();
		}
		return null;
	}
	
	/**
	 * Reads from the given input stream into a String, and returns it.
	 * 
	 * @param stream
	 *   the input stream; when the method completes the stream will be closed.
	 * @return
	 *   the data read from the stream as a string, or {@code null}. if the input 
	 *   stream is not valid.
	 */
	public static String fromStream(InputStream stream) throws IOException {
		if (stream != null) {			
			try {
				Writer writer = new StringWriter();
				Reader reader = new BufferedReader(new InputStreamReader(stream));
				int read;
				while ((read = reader.read()) != -1) {
					writer.write(read);
				}
				return writer.toString();
			} finally {
				try {
					stream.close();
				} catch(IOException e) {
					logger.warn("error closing the input stream", e);
				}
			}			
		}
		return null;
	}		
	
	/**
	 * Private constructor to prevent utility class instantiation. 
	 */
	private Strings() {
	}	
}
