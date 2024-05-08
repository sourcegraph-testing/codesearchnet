/*
 * aocode-public - Reusable Java library of general tools with minimal external dependencies.
 * Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018, 2019  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of aocode-public.
 *
 * aocode-public is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aocode-public is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with aocode-public.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.util;

import com.aoindustries.sql.SQLUtility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author  AO Industries, Inc.
 */
public final class StringUtility {

	private static final String[] MONTHS = {
		"Jan",
		"Feb",
		"Mar",
		"Apr",
		"May",
		"Jun",
		"Jul",
		"Aug",
		"Sep",
		"Oct",
		"Nov",
		"Dec"
	};

	/**
	 * @deprecated  This method is not locale-aware, is no longer used, and will be removed.
	 */
	@Deprecated
	public static String getMonth(int month) {
		return MONTHS[month];
	}

	private static final Calendar calendar = Calendar.getInstance();

	private static final char[] wordWrapChars = { ' ', '\t', '-', '=', ',', ';' };

	private static final String lineSeparator = System.getProperty("line.separator");

	/**
	 * StringUtilitly constructor comment.
	 */
	private StringUtility() {
	}

	/**
	 * Constructs a comma separated list from a <code>String[]</code>.
	 *
	 * @deprecated  This method is no longer used and will be removed.
	 */
	@Deprecated
	public static String buildEmailList(String[] list) {
		StringBuilder SB=new StringBuilder();
		int len=list.length;
		for(int c=0;c<len;c++) {
			if(c==0) SB.append('<'); else SB.append(",<");
			SB.append(list[c]).append('>');
		}
		return SB.toString();
	}

	/**
	 * Constructs a comma separated list from a <code>String[]</code>.
	 *
	 * @deprecated Please use <code>join(objects, ", ")</code> instead.
	 *
	 * @see #join(java.lang.Object[], java.lang.String)
	 */
	@Deprecated
	public static String buildList(String[] list) {
		return join(list, ", ");
	}

	/**
	 * Constructs a comma separated list from an <code>Object[]</code>.
	 *
	 * @deprecated Please use <code>join(objects, ", ")</code> instead.
	 *
	 * @see #join(java.lang.Object[], java.lang.String)
	 */
	@Deprecated
	public static String buildList(Object[] objects) {
		return join(objects, ", ");
	}

	/**
	 * Constructs a comma separated list from an <code>Iterable</code>.
	 *
	 * @deprecated Please use <code>join(objects, ", ")</code> instead.
	 *
	 * @see #join(java.lang.Iterable, java.lang.String)
	 */
	@Deprecated
	public static String buildList(Iterable<?> objects) {
		return join(objects, ", ");
	}

	/**
	 * Joins the string representation of objects on the provided delimiter.
	 * The iteration will be performed twice.  Once to compute the total length
	 * of the resulting string, and the second to build the result.
	 *
	 * @throws ConcurrentModificationException if iteration is not consistent between passes
	 *
	 * @see  #join(java.lang.Iterable, java.lang.String, java.lang.Appendable)
	 * @see  #join(java.lang.Object[], java.lang.String)
	 */
	public static String join(Iterable<?> objects, String delimiter) throws ConcurrentModificationException {
		int delimiterLength = delimiter.length();
		// Find total length
		int totalLength = 0;
		boolean didOne = false;
		for(Object obj : objects) {
			if(didOne) totalLength += delimiterLength;
			else didOne = true;
			totalLength += String.valueOf(obj).length();
		}
		// Build result
		StringBuilder sb = new StringBuilder(totalLength);
		didOne = false;
		for(Object obj : objects) {
			if(didOne) sb.append(delimiter);
			else didOne = true;
			sb.append(obj);
		}
		if(totalLength!=sb.length()) throw new ConcurrentModificationException();
		return sb.toString();
	}

	/**
	 * Joins the string representation of objects on the provided delimiter.
	 *
	 * @see  #join(java.lang.Iterable, java.lang.String)
	 * @see  #join(java.lang.Object[], java.lang.String, java.lang.Appendable)
	 */
	public static <A extends Appendable> A join(Iterable<?> objects, String delimiter, A out) throws IOException {
		boolean didOne = false;
		for(Object obj : objects) {
			if(didOne) out.append(delimiter);
			else didOne = true;
			out.append(String.valueOf(obj));
		}
		return out;
	}

	/**
	 * Joins the string representation of objects on the provided delimiter.
	 * The iteration will be performed twice.  Once to compute the total length
	 * of the resulting string, and the second to build the result.
	 *
	 * @throws ConcurrentModificationException if iteration is not consistent between passes
	 *
	 * @see  #join(java.lang.Object[], java.lang.String, java.lang.Appendable)
	 * @see  #join(java.lang.Iterable, java.lang.String)
	 */
	public static String join(Object[] objects, String delimiter) throws ConcurrentModificationException {
		int delimiterLength = delimiter.length();
		// Find total length
		int totalLength = 0;
		boolean didOne = false;
		for(Object obj : objects) {
			if(didOne) totalLength += delimiterLength;
			else didOne = true;
			totalLength += String.valueOf(obj).length();
		}
		// Build result
		StringBuilder sb = new StringBuilder(totalLength);
		didOne = false;
		for(Object obj : objects) {
			if(didOne) sb.append(delimiter);
			else didOne = true;
			sb.append(obj);
		}
		if(totalLength!=sb.length()) throw new ConcurrentModificationException();
		return sb.toString();
	}

	/**
	 * Joins the string representation of objects on the provided delimiter.
	 *
	 * @see  #join(java.lang.Object[], java.lang.String)
	 * @see  #join(java.lang.Iterable, java.lang.String, java.lang.Appendable)
	 */
	public static <A extends Appendable> A join(Object[] objects, String delimiter, A out) throws IOException {
		boolean didOne = false;
		for(Object obj : objects) {
			if(didOne) out.append(delimiter);
			else didOne = true;
			out.append(String.valueOf(obj));
		}
		return out;
	}

	/**
	 * Compare one date to another, must be in the DDMMYYYY format.
	 *
	 * @return  <0  if the first date is before the second<br>
	 *          0   if the dates are the same or the format is invalid<br>
	 *          >0  if the first date is after the second
	 */
	public static int compareToDDMMYYYY(String date1, String date2) {
		if(date1.length()!=8 || date2.length()!=8) return 0;
		return compareToDDMMYYYY0(date1)-compareToDDMMYYYY0(date2);
	}

	private static int compareToDDMMYYYY0(String date) {
		return
			(date.charAt(4)-'0')*10000000
			+(date.charAt(5)-'0')*1000000
			+(date.charAt(6)-'0')*100000
			+(date.charAt(7)-'0')*10000
			+(date.charAt(0)-'0')*1000
			+(date.charAt(1)-'0')*100
			+(date.charAt(2)-'0')*10
			+(date.charAt(3)-'0')
		;
	}

	public static boolean containsIgnoreCase(String line, String word) {
		int word_len=word.length();
		int line_len=line.length();
		int end_pos=line_len-word_len;
		Loop:
		for(int c=0;c<=end_pos;c++) {
			for(int d=0;d<word_len;d++) {
				char ch1=line.charAt(c+d);
				char ch2=word.charAt(d);
				if(ch1>='A'&&ch1<='Z') ch1+='a'-'A';
				if(ch2>='A'&&ch2<='Z') ch2+='a'-'A';
				if(ch1!=ch2) continue Loop;
			}
			return true;
		}
		return false;
	}

	public static long convertStringDateToTime(String date) throws IllegalArgumentException {
		synchronized(StringUtility.class) {
			if(date.length()<9) throw new IllegalArgumentException("Invalid date");
			int day=Integer.parseInt(date.substring(0,2));
			if(day<0||day>31) throw new IllegalArgumentException("Invalid date");
			String monthString=date.substring(2,5);
			int month=-1;
			for(int c=0;c<MONTHS.length;c++) {
				if(MONTHS[c].equalsIgnoreCase(monthString)) {
					month=c;
					break;
				}
			}
			if(month==-1) throw new IllegalArgumentException("Invalid month: "+monthString);
			if(day>30 && (month==1||month==3||month==5||month==8||month==10))
				throw new IllegalArgumentException("Invalid date");
			int year=Integer.parseInt(date.substring(5,9));
			if(month==1) {
				if(day>29) throw new IllegalArgumentException("Invalid date");	
				if(day==29 && !leapYear(year)) throw new IllegalArgumentException("Invalid date");
			}	
			calendar.set(Calendar.DAY_OF_MONTH, day);
			calendar.set(Calendar.MONTH, month);
			calendar.set(Calendar.YEAR, year);
			return calendar.getTime().getTime();
		}
	}

	/**
	 * Counts how many times a word appears in a line.  Case insensitive matching.
	 *
	 * @deprecated Corrected spelling
	 */
	@Deprecated
	public static int countOccurances(byte[] buff, int len, String word) {
		return countOccurrences(buff, len, word);
	}

	/**
	 * Counts how many times a word appears in a line.  Case insensitive matching.
	 */
	public static int countOccurrences(byte[] buff, int len, String word) {
		int wordlen=word.length();
		int end=len-wordlen;
		int count=0;
		Loop:
		for(int c=0;c<=end;c++) {
			for(int d=0;d<wordlen;d++) {
				char ch1=(char)buff[c+d];
				if(ch1<='Z' && ch1>='A') ch1+='a'-'A';
				char ch2=word.charAt(d);
				if(ch2<='Z' && ch2>='A') ch2+='a'-'A';
				if(ch1!=ch2) continue Loop;
			}
			c+=wordlen-1;
			count++;
		}
		return count;
	}

	/**
	 * Counts how many times a word appears in a line.  Case insensitive matching.
	 *
	 * @deprecated Corrected spelling
	 */
	@Deprecated
	public static int countOccurances(byte[] buff, String word) {
		return countOccurrences(buff, word);
	}

	/**
	 * Counts how many times a word appears in a line.  Case insensitive matching.
	 */
	public static int countOccurrences(byte[] buff, String word) {
		int wordlen=word.length();
		int end=buff.length-wordlen;
		int count=0;
		Loop:
		for(int c=0;c<=end;c++) {
			for(int d=0;d<wordlen;d++) {
				char ch1=(char)buff[c+d];
				if(ch1<='Z' && ch1>='A') ch1+='a'-'A';
				char ch2=word.charAt(d);
				if(ch2<='Z' && ch2>='A') ch2+='a'-'A';
				if(ch1!=ch2) continue Loop;
			}
			c+=wordlen-1;
			count++;
		}
		return count;
	}

	/**
	 * Counts how many times a word appears in a line.  Case insensitive matching.
	 *
	 * @deprecated Corrected spelling
	 */
	@Deprecated
	public static int countOccurances(String line, String word) {
		return countOccurrences(line, word);
	}

	/**
	 * Counts how many times a word appears in a line.  Case insensitive matching.
	 */
	public static int countOccurrences(String line, String word) {
		int wordlen=word.length();
		int end=line.length()-wordlen;
		int count=0;
		Loop:
		for(int c=0;c<=end;c++) {
			for(int d=0;d<wordlen;d++) {
				char ch1=line.charAt(c+d);
				if(ch1<='Z' && ch1>='A') ch1+='a'-'A';
				char ch2=word.charAt(d);
				if(ch2<='Z' && ch2>='A') ch2+='a'-'A';
				if(ch1!=ch2) continue Loop;
			}
			c+=wordlen-1;
			count++;
		}
		return count;
	}

	/**
	 * @deprecated  Please use SQLUtility.escapeSQL(s.replace('*', '%'))
	 *
	 * @see  SQLUtility#escapeSQL(String)
	 */
	@Deprecated
	public static String escapeSQL(String s) {
		return SQLUtility.escapeSQL(s.replace('*', '%'));
	}

	/**
	 * Converts a date in a the format MMDDYYYY to a <code>Date</code>.
	 *
	 * @param  date  a <code>String</code> containing the date in MMDDYYYY format.
	 *
	 * @return  <code>null</code> if <code>date</code> is <code>null</code>, a <code>java.sql.Date</code>
	 *          otherwise
	 */
	public static java.sql.Date getDateMMDDYYYY(String date) throws NumberFormatException, IllegalArgumentException {
		synchronized(StringUtility.class) {
			int len = date.length();
			if (len == 0) return null;
			if (len != 8) throw new IllegalArgumentException("Date must be in MMDDYYYY format: " + date);
			return new java.sql.Date(
				new GregorianCalendar(
					Integer.parseInt(date.substring(4, 8)),
					Integer.parseInt(date.substring(0, 2))-1,
					Integer.parseInt(date.substring(2, 4))
				).getTime().getTime()
			);
		}
	}

	/**
	 * @deprecated  Please use SQLUtility.getDate(long)
	 *
	 * @see  SQLUtility#getDate(long)
	 */
	@Deprecated
	public static String getDateString(long time) {
		return getDateString(new Date(time));
	}

	/**
	 * @deprecated  Please use SQLUtility.getDate(date.getTime())
	 *
	 * @see  SQLUtility#getDate(long)
	 */
	@Deprecated
	public static String getDateString(Date date) {
		synchronized(StringUtility.class) {
			calendar.setTime(date);
			int day=calendar.get(Calendar.DATE);
			return (day>=0 && day<=9 ? "0":"")+String.valueOf(calendar.get(Calendar.DATE))+MONTHS[calendar.get(Calendar.MONTH)]+calendar.get(Calendar.YEAR);
		}
	}

	/**
	 * @deprecated  Please use SQLUtility.getDate(date.getTime())
	 *
	 * @see  SQLUtility#getDate(long)
	 */
	@Deprecated
	public static String getDateStringMMDDYYYY(Date date) {
		if(date==null) return "";
		Calendar C=Calendar.getInstance();
		C.setTime(date);
		int day=C.get(Calendar.DATE);
		int month=C.get(Calendar.MONTH)+1;
		return
			(month>=0 && month<=9 ? "0":"")
			+month
			+(day>=0 && day<=9 ? "0":"")
			+day
			+C.get(Calendar.YEAR)
		;
	}

	/**
	 * @deprecated  Please use SQLUtility.getDateTime(long)
	 *
	 * @see  SQLUtility#getDateTime(long)
	 */
	@Deprecated
	public static String getDateStringSecond(long time) {
		Date date=new Date(time);
		Calendar C=Calendar.getInstance();
		C.setTime(date);
		int day=C.get(Calendar.DATE);
		int hour=C.get(Calendar.HOUR_OF_DAY);
		int minute=C.get(Calendar.MINUTE);
		int second=C.get(Calendar.SECOND);
		return
			(day>=0 && day<=9 ? "0":"")
			+day
			+MONTHS[C.get(Calendar.MONTH)]
			+C.get(Calendar.YEAR)
			+' '
			+(hour>=0 && hour<=9 ? "0":"")
			+hour
			+':'
			+(minute>=0 && minute<=9 ? "0":"")
			+minute
			+':'		
			+(second>=0 && second<=9 ? "0":"")
			+second
		;
	}

	/**
	 * @deprecated  Please use SQLUtility.getDateTime(long)
	 *
	 * @see  SQLUtility#getDateTime(long)
	 */
	@Deprecated
	public static String getDateStringSecond(String time) {
		return
			time.substring(6,8)
			+MONTHS[Integer.parseInt(time.substring(4,6))]
			+time.substring(0,4)
			+' '
			+time.substring(8,10)
			+':'
			+time.substring(10,12)
			+':'
			+time.substring(12,14)
		;        
	}

	/**
	 * Creates a <code>String[]</code> by calling the toString() method of each object in a list.
	 *
	 * @deprecated  Please use List.toArray(Object[])
	 *
	 * @see  List#toArray(Object[])
	 */
	@Deprecated
	public static String[] getStringArray(List<?> V) {
		if(V==null) return null;
		int len = V.size();
		String[] SA = new String[len];
		for (int c = 0; c < len; c++) {
			Object O=V.get(c);
			SA[c]=O==null?null:O.toString();
		}
		return SA;
	}

	public static String getTimeLengthString(long time) {
		StringBuilder SB=new StringBuilder();
		if(time<0) {
			SB.append('-');
			time=-time;
		}

		long days=time/86400000;
		time-=days*86400000;
		int hours=(int)(time/3600000);
		time-=hours*3600000;
		int minutes=(int)(time/60000);
		time-=minutes*60000;
		int seconds=(int)(time/1000);
		time-=seconds*1000;
		if(days==0) {
			if(hours==0) {
				if(minutes==0) {
					if(seconds==0) {
						if(time==0) SB.append("0 minutes");
						else SB.append(time).append(time==1?" millisecond":" milliseconds");
					} else SB.append(seconds).append(seconds==1?" second":" seconds");
				} else SB.append(minutes).append(minutes==1?" minute":" minutes");
			} else {
				if(minutes==0) SB.append(hours).append(hours==1?" hour":" hours");
				else SB.append(hours).append(hours==1?" hour and ":" hours and ").append(minutes).append(minutes==1?" minute":" minutes");
			}
		} else {
			if(hours==0) {
				if(minutes==0) SB.append(days).append(days==1?" day":" days");
				else SB.append(days).append(days==1?" day and ":" days and ").append(minutes).append(minutes==1?" minute":" minutes");
			} else {
				if(minutes==0) SB.append(days).append(days==1?" day and ":" days and ").append(hours).append(hours==1?" hour":" hours");
				else SB.append(days).append(days==1?" day, ":" days, ").append(hours).append(hours==1?" hour and ":" hours and ").append(minutes).append(minutes==1?" minute":" minutes");
			}
		}
		return SB.toString();
	}

	public static String getDecimalTimeLengthString(long time) {
		return getDecimalTimeLengthString(time, true);
	}

	public static String getDecimalTimeLengthString(long time, boolean alwaysShowMillis) {
		StringBuilder SB=new StringBuilder();
		if(time<0) {
			SB.append('-');
			time=-time;
		}

		long days=time/86400000;
		time-=days*86400000;
		int hours=(int)(time/3600000);
		time-=hours*3600000;
		int minutes=(int)(time/60000);
		time-=minutes*60000;
		int seconds=(int)(time/1000);
		time-=seconds*1000;

		if(days>0) SB.append(days).append(days==1?" day, ":" days, ");
		SB.append(hours).append(':');
		if(minutes<10) SB.append('0');
		SB.append(minutes).append(':');
		if(seconds<10) SB.append('0');
		SB.append(seconds);
		if(alwaysShowMillis || time != 0) {
			SB.append('.');
			if(time<10) SB.append("00");
			else if(time<100) SB.append('0');
			SB.append(time);
		}
		return SB.toString();
	}

	/**
	 * Finds the first occurrence of any of the supplied characters
	 *
	 * @param  S  the <code>String</code> to search
	 * @param  chars  the characters to look for
	 *
	 * @return  the index of the first occurrence of <code>-1</code> if none found
	 */
	public static int indexOf(String S, char[] chars) {
		return indexOf(S, chars, 0);
	}

	/**
	 * Finds the first occurrence of any of the supplied characters starting at the specified index.
	 *
	 * @param  S  the <code>String</code> to search
	 * @param  chars  the characters to look for
	 * @param  start  the starting index.
	 *
	 * @return  the index of the first occurrence of <code>-1</code> if none found
	 */
	public static int indexOf(String S, char[] chars, int start) {
		int Slen=S.length();
		int clen=chars.length;
		for(int c=start;c<Slen;c++) {
			char ch=S.charAt(c);
			for(int d=0;d<clen;d++) if(ch==chars[d]) return c;
		}
		return -1;
	}

	/**
	 * @deprecated  Please use Calendar class instead.
	 *
	 * @see  Calendar
	 */
	@Deprecated
	public static boolean isValidDate(String date) {
		try {
			convertStringDateToTime(date);
			return true;
		} catch (IllegalArgumentException err) {
			return false;
		}
	}

	/**
	 * @deprecated  Please use Calendar class instead.
	 *
	 * @see  Calendar
	 */
	@Deprecated
	public static boolean leapYear(int year) {
		return year%4==0 && year%400==0;
	}

	/**
	 * Removes all occurrences of a <code>char</code> from a <code>String</code>
	 *
	 * @deprecated  this method is slow and no longer supported
	 */
	@Deprecated
	public static String removeChars(String S, char[] chars) {
		int pos;
		while((pos=indexOf(S, chars))!=-1) S=S.substring(0,pos)+S.substring(pos+1);
		return S;
	}

	/**
	 * Removes all occurrences of a <code>char</code> from a <code>String</code>
	 *
	 * @deprecated  this method is slow and no longer supported
	 */
	@Deprecated
	public static String removeChars(String S, char ch) {
		int pos;
		while((pos=S.indexOf(ch))!=-1) S=S.substring(0,pos)+S.substring(pos+1);
		return S;
	}

	/**
	 * Replaces all occurrences of a character with a String
	 */
	public static String replace(String string, char ch, String replacement) {
		int pos = string.indexOf(ch);
		if (pos == -1) return string;
		StringBuilder SB = new StringBuilder();
		int lastpos = 0;
		do {
			SB.append(string, lastpos, pos).append(replacement);
			lastpos = pos + 1;
			pos = string.indexOf(ch, lastpos);
		} while (pos != -1);
		int len = string.length();
		if(lastpos<len) SB.append(string, lastpos, len);
		return SB.toString();
	}

	/**
	 * Replaces all occurrences of a String with a String
	 * Please consider the variant with the Appendable for higher performance.
	 */
	public static String replace(final String string, final String find, final String replacement) {
		int pos = string.indexOf(find);
		//System.out.println(string+": "+find+" at "+pos);
		if (pos == -1) return string;
		StringBuilder SB = new StringBuilder();
		int lastpos = 0;
		final int findLen = find.length();
		do {
			SB.append(string, lastpos, pos).append(replacement);
			lastpos = pos + findLen;
			pos = string.indexOf(find, lastpos);
		} while (pos != -1);
		int len = string.length();
		if(lastpos<len) SB.append(string, lastpos, len);
		return SB.toString();
	}

	/**
	 * Replaces all occurrences of a String with a String, appends the replacement
	 * to <code>out</code>.
	 */
	public static void replace(final String string, final String find, final String replacement, final Appendable out) throws IOException {
		int pos = string.indexOf(find);
		//System.out.println(string+": "+find+" at "+pos);
		if (pos == -1) {
			out.append(string);
		} else {
			int lastpos = 0;
			final int findLen = find.length();
			do {
				out.append(string, lastpos, pos).append(replacement);
				lastpos = pos + findLen;
				pos = string.indexOf(find, lastpos);
			} while (pos != -1);
			int len = string.length();
			if(lastpos<len) out.append(string, lastpos, len);
		}
	}

	/**
	 * Replaces all occurrences of a String with a String.
	 */
	public static void replace(final StringBuffer sb, final String find, final String replacement) {
		int pos = 0;
		while(pos<sb.length()) {
			pos = sb.indexOf(find, pos);
			if(pos==-1) break;
			sb.replace(pos, pos+find.length(), replacement);
			pos += replacement.length();
		}
	}

	/**
	 * Replaces all occurrences of a String with a String.
	 */
	public static void replace(final StringBuilder sb, final String find, final String replacement) {
		int pos = 0;
		while(pos<sb.length()) {
			pos = sb.indexOf(find, pos);
			if(pos==-1) break;
			sb.replace(pos, pos+find.length(), replacement);
			pos += replacement.length();
		}
	}

	/**
	 * Splits a String into lines on any '\n' characters.  Also removes any ending '\r' characters if present
	 */
	public static List<String> splitLines(String S) {
		List<String> V=new ArrayList<>();
		int start=0;
		int pos;
		while((pos=S.indexOf('\n', start))!=-1) {
			String line;
			if(pos>start && S.charAt(pos-1)=='\r') line = S.substring(start, pos-1);
			else line = S.substring(start, pos);
			V.add(line);
			start=pos+1;
		}
		int slen = S.length();
		if(start<slen) {
			// Ignore any trailing '\r'
			if(S.charAt(slen-1)=='\r') slen--;
			String line = S.substring(start, slen);
			V.add(line);
		}
		return V;
	}

	/**
	 * Splits a <code>String</code> into a <code>String[]</code>.
	 */
	public static String[] splitString(String line) {
		int len=line.length();
		int wordCount=0;
		int pos=0;
		while(pos<len) {
			// Skip past blank space
			while(pos<len&&line.charAt(pos)<=' ') pos++;
			int start=pos;
			// Skip to the next blank space
			while(pos<len&&line.charAt(pos)>' ') pos++;
			if(pos>start) wordCount++;
		}

		String[] words=new String[wordCount];

		int wordPos=0;
		pos=0;
		while(pos<len) {
			// Skip past blank space
			while(pos<len&&line.charAt(pos)<=' ') pos++;
			int start=pos;
			// Skip to the next blank space
			while(pos<len&&line.charAt(pos)>' ') pos++;
			if(pos>start) words[wordPos++]=line.substring(start,pos);
		}

		return words;
	}

	/**
	 * Splits a <code>String</code> into a <code>String[]</code>.
	 */
	public static int splitString(String line, char[][][] buff) {
		int len=line.length();
		int wordCount=0;
		int pos=0;
		while(pos<len) {
			// Skip past blank space
			while(pos<len&&line.charAt(pos)<=' ') pos++;
			int start=pos;
			// Skip to the next blank space
			while(pos<len&&line.charAt(pos)>' ') pos++;
			if(pos>start) wordCount++;
		}

		char[][] words=buff[0];
		if(words==null || words.length<wordCount) buff[0]=words=new char[wordCount][];

		int wordPos=0;
		pos=0;
		while(pos<len) {
			// Skip past blank space
			while(pos<len&&line.charAt(pos)<=' ') pos++;
			int start=pos;
			// Skip to the next blank space
			while(pos<len&&line.charAt(pos)>' ') pos++;
			if(pos>start) {
				int chlen=pos-start;
				char[] tch=words[wordPos++]=new char[chlen];
				System.arraycopy(line.toCharArray(), start, tch, 0, chlen);
			}
		}

		return wordCount;
	}

	/**
	 * Splits a <code>String</code> into a <code>String[]</code>.
	 */
	public static int splitString(String line, String[][] buff) {
		int len=line.length();
		int wordCount=0;
		int pos=0;
		while(pos<len) {
			// Skip past blank space
			while(pos<len&&line.charAt(pos)<=' ') pos++;
			int start=pos;
			// Skip to the next blank space
			while(pos<len&&line.charAt(pos)>' ') pos++;
			if(pos>start) wordCount++;
		}

		String[] words=buff[0];
		if(words==null || words.length<wordCount) buff[0]=words=new String[wordCount];

		int wordPos=0;
		pos=0;
		while(pos<len) {
			// Skip past blank space
			while(pos<len&&line.charAt(pos)<=' ') pos++;
			int start=pos;
			// Skip to the next blank space
			while(pos<len&&line.charAt(pos)>' ') pos++;
			if(pos>start) words[wordPos++]=line.substring(start, pos);
		}

		return wordCount;
	}

	/**
	 * Splits a string on the given delimiter.
	 * Does include all empty elements on the split.
	 *
	 * @return  the modifiable list from the split
	 */
	public static List<String> splitString(String line, char delim) {
		return splitString(line, 0, line.length(), delim, new ArrayList<String>());
	}

	/**
	 * Splits a string on the given delimiter.
	 * Does include all empty elements on the split.
	 *
	 * @param  words  the words will be added to this collection.
	 *
	 * @return  the collection provided in words parameter
	 */
	public static <C extends Collection<String>> C splitString(String line, char delim, C words) {
		return splitString(line, 0, line.length(), delim, words);
	}

	/**
	 * Splits a string on the given delimiter over the given range.
	 * Does include all empty elements on the split.
	 *
	 * @return  the modifiable list from the split
	 */
	public static List<String> splitString(String line, int begin, int end, char delim) {
		return splitString(line, begin, end, delim, new ArrayList<String>());
	}

	/**
	 * Splits a string on the given delimiter over the given range.
	 * Does include all empty elements on the split.
	 *
	 * @param  words  the words will be added to this collection.
	 *
	 * @return  the collection provided in words parameter
	 */
	public static <C extends Collection<String>> C splitString(String line, int begin, int end, char delim, C words) {
		int pos = begin;
		while (pos < end) {
			int start = pos;
			pos = line.indexOf(delim, pos);
			if(pos == -1 || pos > end) pos = end;
			words.add(line.substring(start, pos));
			pos++;
		}
		// If ending in a delimeter, add the empty string
		if(end>begin && line.charAt(end-1)==delim) words.add("");
		return words;
	}

	public static List<String> splitString(String line, String delim) {
		int delimLen = delim.length();
		if(delimLen==0) throw new IllegalArgumentException("Delimiter may not be empty");
		List<String> words = new ArrayList<>();
		int len = line.length();
		int pos = 0;
		while (pos < len) {
			int start = pos;
			pos = line.indexOf(delim, pos);
			if (pos == -1) {
				words.add(line.substring(start, len));
				pos = len;
			} else {
				words.add(line.substring(start, pos));
				pos += delimLen;
			}
		}
		// If ending in a delimeter, add the empty string
		if(len>=delimLen && line.endsWith(delim)) words.add("");

		return words;
	}

	/**
	 * Splits a string into multiple words on either whitespace or commas
	 * @return java.lang.String[]
	 * @param line java.lang.String
	 */
	public static List<String> splitStringCommaSpace(String line) {
		List<String> words=new ArrayList<>();
		int len=line.length();
		int pos=0;
		while(pos<len) {
			// Skip past blank space
			char ch;
			while(pos<len && ((ch=line.charAt(pos))<=' ' || ch==',')) pos++;
			int start=pos;
			// Skip to the next blank space
			while(pos<len && (ch=line.charAt(pos))>' ' && ch!=',') pos++;
			if(pos>start) words.add(line.substring(start,pos));
		}
		return words;
	}

	/**
	 * Word wraps a <code>String</code> to be no longer than the provided number of characters wide.
	 *
	 * @deprecated  Use new version with Appendable for higher performance
	 */
	@Deprecated
	public static String wordWrap(String string, int width) {
		// Leave room for two word wrap characters every width / 2 characters, on average.
		int inputLength = string.length();
		int estimatedLines = 2 * inputLength / width;
		int initialLength = inputLength + estimatedLines * 2;
		try {
			StringBuilder buffer = new StringBuilder(initialLength);
			wordWrap(string, width, buffer);
			return buffer.toString();
		} catch(IOException e) {
			throw new AssertionError("Should not get IOException from StringBuilder", e);
		}
	}

	/**
	 * Word wraps a <code>String</code> to be no longer than the provided number of characters wide.
	 *
	 * TODO: Make this more efficient by eliminating the internal use of substring.
	 */
	public static void wordWrap(String string, int width, Appendable out) throws IOException {
		width++;
		boolean useCR = false;
		do {
			int pos = string.indexOf('\n');
			if (!useCR && pos > 0 && string.charAt(pos - 1) == '\r') useCR = true;
			int linelength = pos == -1 ? string.length() : pos + 1;
			if ((pos==-1?linelength-1:pos) <= width) {
				// No wrap required
				out.append(string, 0, linelength);
				string = string.substring(linelength);
			} else {
				// Word wrap required

				// Search for the beginning of the first word that is past the <code>width</code> column
				// The wrap character must be on the same line as the outputted line.
				int lastBreakChar = 0;

				for (int c = 0; c < width; c++) {
					// Check to see if it is a break character
					char ch = string.charAt(c);
					boolean isBreak = false;
					for (int d = 0; d < wordWrapChars.length; d++) {
						if (ch == wordWrapChars[d]) {
							isBreak = true;
							break;
						}
					}
					if (isBreak) lastBreakChar = c + 1;
				}

				// If no break has been found, keep searching until a break is found
				if (lastBreakChar == 0) {
					for (int c = width; c < linelength; c++) {
						char ch = string.charAt(c);
						boolean isBreak = false;
						for (int d = 0; d < wordWrapChars.length; d++) {
							if (ch == wordWrapChars[d]) {
								isBreak = true;
								break;
							}
						}
						if (isBreak) {
							lastBreakChar = c + 1;
							break;
						}
					}
				}

				if (lastBreakChar == 0) {
					// Take the whole line
					out.append(string, 0, linelength);
					string = string.substring(linelength);
				} else {
					// Break out the section
					out.append(string, 0, lastBreakChar);
					if(useCR) out.append("\r\n");
					else out.append('\n');
					string = string.substring(lastBreakChar);
				}
			}
		} while (string.length() > 0);
	}

	private static final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	/**
	 * Gets the hexadecimal character for the low-order four bits of the provided int.
	 */
	public static char getHexChar(int v) {
		return hexChars[v & 0xf];
	}

	/**
	 * Converts one hex digit to an integer
	 */
	public static int getHex(char ch) throws IllegalArgumentException {
		switch(ch) {
			case '0': return 0x00;
			case '1': return 0x01;
			case '2': return 0x02;
			case '3': return 0x03;
			case '4': return 0x04;
			case '5': return 0x05;
			case '6': return 0x06;
			case '7': return 0x07;
			case '8': return 0x08;
			case '9': return 0x09;
			case 'a': case 'A': return 0x0a;
			case 'b': case 'B': return 0x0b;
			case 'c': case 'C': return 0x0c;
			case 'd': case 'D': return 0x0d;
			case 'e': case 'E': return 0x0e;
			case 'f': case 'F': return 0x0f;
			default: throw new IllegalArgumentException("Invalid hex character: "+ch);
		}
	}

	public static void convertToHex(byte[] bytes, Appendable out) throws IOException {
		if(bytes != null) {
			int len = bytes.length;
			for(int c = 0; c < len; c++) {
				int b = bytes[c];
				out.append(getHexChar(b >> 4));
				out.append(getHexChar(b));
			}
		}
	}

	public static String convertToHex(byte[] bytes) {
		if(bytes == null) return null;
		int len = bytes.length;
		StringBuilder SB = new StringBuilder(len * 2);
		try {
			convertToHex(bytes, SB);
		} catch(IOException e) {
			throw new AssertionError(e);
		}
		return SB.toString();
	}

	public static byte[] convertByteArrayFromHex(char[] hex) {
		int hexLen = hex.length;
		if((hexLen&1) != 0) throw new IllegalArgumentException("Uneven number of characters: " + hexLen);
		byte[] result = new byte[hexLen / 2];
		int resultPos = 0;
		int hexPos = 0;
		while(hexPos < hexLen) {
			int h = getHex(hexChars[hexPos++]);
			int l = getHex(hexChars[hexPos++]);
			result[resultPos++] = (byte)(
				(h<<4) | l
			);
		}
		return result;
	}

	/**
	 * Converts an int to a full 8-character hex code.
	 */
	public static void convertToHex(int value, Appendable out) throws IOException {
		out.append(getHexChar(value >>> 28));
		out.append(getHexChar(value >>> 24));
		out.append(getHexChar(value >>> 20));
		out.append(getHexChar(value >>> 16));
		out.append(getHexChar(value >>> 12));
		out.append(getHexChar(value >>> 8));
		out.append(getHexChar(value >>> 4));
		out.append(getHexChar(value));
	}

	/**
	 * Converts an int to a full 8-character hex code.
	 */
	public static String convertToHex(int value) {
		StringBuilder SB = new StringBuilder(8);
		try {
			convertToHex(value, SB);
		} catch(IOException e) {
			throw new AssertionError(e);
		}
		return SB.toString();
	}

	public static int convertIntArrayFromHex(char[] hex) {
		int hexLen = hex.length;
		if(hexLen < 8) throw new IllegalArgumentException("Too few characters: " + hexLen);
		return
			(getHex(hex[0]) << 28)
			| (getHex(hex[1]) << 24)
			| (getHex(hex[2]) << 20)
			| (getHex(hex[3]) << 16)
			| (getHex(hex[4]) << 12)
			| (getHex(hex[5]) << 8)
			| (getHex(hex[6]) << 4)
			| (getHex(hex[7]))
		;
	}

	/**
	 * Converts a long integer to a full 16-character hex code.
	 */
	public static void convertToHex(long value, Appendable out) throws IOException {
		convertToHex((int)(value >>> 32), out);
		convertToHex((int)value, out);
	}

	/**
	 * Converts a long integer to a full 16-character hex code.
	 */
	public static String convertToHex(long value) {
		StringBuilder SB = new StringBuilder(16);
		try {
			convertToHex(value, SB);
		} catch(IOException e) {
			throw new AssertionError(e);
		}
		return SB.toString();
	}

	public static long convertLongArrayFromHex(char[] hex) {
		int hexLen = hex.length;
		if(hexLen < 16) throw new IllegalArgumentException("Too few characters: " + hexLen);
		int h = (getHex(hex[0]) << 28)
			| (getHex(hex[1]) << 24)
			| (getHex(hex[2]) << 20)
			| (getHex(hex[3]) << 16)
			| (getHex(hex[4]) << 12)
			| (getHex(hex[5]) << 8)
			| (getHex(hex[6]) << 4)
			| (getHex(hex[7]))
		;
		int l = (getHex(hex[8]) << 28)
			| (getHex(hex[9]) << 24)
			| (getHex(hex[10]) << 20)
			| (getHex(hex[11]) << 16)
			| (getHex(hex[12]) << 12)
			| (getHex(hex[13]) << 8)
			| (getHex(hex[14]) << 4)
			| (getHex(hex[15]))
		;
		return (((long)h) << 32) | (l & 0xffffffffL);
	}

	/**
	 * Gets the approximate size (where k=1024) of a file in this format:
	 *
	 * x byte(s)
	 * xx bytes
	 * xxx bytes
	 * x.x k
	 * xx.x k
	 * xxx k
	 * x.x M
	 * xx.x M
	 * xxx M
	 * x.x G
	 * xx.x G
	 * xxx G
	 * x.x T
	 * xx.x T
	 * xxx T
	 * xxx... T
	 */
	public static String getApproximateSize(long size) {
		if(size==1) return "1 byte";
		if(size<1024) return new StringBuilder().append((int)size).append(" bytes").toString();
		String unitName;
		long unitSize;
		if(size<(1024*1024)) {
			unitName=" k";
			unitSize=1024;
		} else if(size<((long)1024*1024*1024)) {
			unitName=" M";
			unitSize=1024*1024;
		} else if(size<((long)1024*1024*1024*1024)) {
			unitName=" G";
			unitSize=(long)1024*1024*1024;
		} else {
			unitName=" T";
			unitSize=(long)1024*1024*1024*1024;
		}
		long whole=size/unitSize;
		if(whole<100) {
			int fraction=(int)(((size%unitSize)*10)/unitSize);
			return new StringBuilder().append(whole).append('.').append(fraction).append(unitName).toString();
		} else return new StringBuilder().append(whole).append(unitName).toString();
	}

	/**
	 * Gets the approximate bit rate (where k=1000) in this format:
	 *
	 * x
	 * xx
	 * xxx
	 * x.x k
	 * xx.x k
	 * xxx k
	 * x.x M
	 * xx.x M
	 * xxx M
	 * x.x G
	 * xx.x G
	 * xxx G
	 * x.x T
	 * xx.x T
	 * xxx T
	 * xxx... T
	 */
	public static String getApproximateBitRate(long bit_rate) {
		if(bit_rate<1000) return Integer.toString((int)bit_rate);
		String unitName;
		long unitSize;
		if(bit_rate<(1000*1000)) {
			unitName=" k";
			unitSize=1000;
		} else if(bit_rate<((long)1000*1000*1000)) {
			unitName=" M";
			unitSize=1000*1000;
		} else if(bit_rate<((long)1000*1000*1000*1000)) {
			unitName=" G";
			unitSize=(long)1000*1000*1000;
		} else {
			unitName=" T";
			unitSize=(long)1000*1000*1000*1000;
		}
		long whole=bit_rate/unitSize;
		if(whole<100) {
			int fraction=(int)(((bit_rate%unitSize)*10)/unitSize);
			return new StringBuilder().append(whole).append('.').append(fraction).append(unitName).toString();
		} else return new StringBuilder().append(whole).append(unitName).toString();
	}

	/**
	 * Compares two strings in a case insensitive manner.  However, if they are considered equals in the
	 * case-insensitive manner, the case sensitive comparison is done.
	 */
	public static int compareToIgnoreCaseCarefulEquals(String S1, String S2) {
		int diff=S1.compareToIgnoreCase(S2);
		if(diff==0) diff=S1.compareTo(S2);
		return diff;
	}

	/**
	 * Null-safe intern: interns a String if it is not null, returns null if parameter is null.
	 *
	 * @deprecated  Use InternUtils instead.
	 */
	@Deprecated
	public static String intern(String S) {
		return InternUtils.intern(S);
	}

	/**
	 * Finds the next of a substring like regular String.indexOf, but stops at a certain maximum index.
	 * Like substring, will look up to the character one before toIndex.
	 */
	public static int indexOf(String source, String target, int fromIndex, int toIndex) {
		if(fromIndex>toIndex) throw new IllegalArgumentException("fromIndex>toIndex: fromIndex="+fromIndex+", toIndex="+toIndex);

		int sourceCount = source.length();

		// This line makes it different than regular String indexOf method.
		if(toIndex<sourceCount) sourceCount = toIndex;

		int targetCount = target.length();

		if (fromIndex >= sourceCount) {
			return (targetCount == 0 ? sourceCount : -1);
	}
		if (fromIndex < 0) {
			fromIndex = 0;
		}
	if (targetCount == 0) {
		return fromIndex;
	}

		char first  = target.charAt(0);
		int max = sourceCount - targetCount;

		for (int i = fromIndex; i <= max; i++) {
			/* Look for first character. */
			if (source.charAt(i) != first) {
				while (++i <= max && source.charAt(i) != first) {
					// Intentionally empty
				}
			}

			/* Found first character, now look at the rest of v2 */
			if (i <= max) {
				int j = i + 1;
				int end = j + targetCount - 1;
				for (int k = 1; j < end && source.charAt(j) == target.charAt(k); j++, k++) {
					// Intentionally empty
				}

				if (j == end) {
					/* Found whole string. */
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the first line only, and only up to the maximum number of characters.  If the
	 * value is modified, will append a horizontal ellipsis (Unicode 0x2026).
	 */
	public static String firstLineOnly(String value, int maxCharacters) {
		if(value==null) return value;
		int pos = value.indexOf(lineSeparator);
		if(pos==-1) pos = value.length();
		if(pos>maxCharacters) pos = maxCharacters;
		return pos==value.length() ? value : (value.substring(0, pos) + '\u2026');
	}

	/**
	 * Returns null if the string is null or empty.
	 */
	public static String nullIfEmpty(String value) {
		return value==null || value.isEmpty() ? null : value;
	}
}
