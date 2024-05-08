package org.codelogger.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.codelogger.utils.exceptions.DateException;
import org.codelogger.utils.lang.DatePeriod;

/**
 * A useful tools to handle dates, likes get date from string, date format, get
 * date back, calculate date between, get first/last day of month, get
 * year/month/week/day/hour/minute/second and so on...
 * 
 * @author DengDefei
 */
public class DateUtils {

  public static final int SUNDAY = 1;

  public static final int MONDAY = 2;

  public static final int TUESDAY = 3;

  public static final int WEDNESDAY = 4;

  public static final int THURSDAY = 5;

  public static final int FRIDAY = 6;

  public static final int SATURDAY = 7;

  public static final int JANUARY = 1;

  public static final int FEBRUARY = 2;

  public static final int MARCH = 3;

  public static final int APRIL = 4;

  public static final int MAY = 5;

  public static final int JUNE = 6;

  public static final int JULY = 7;

  public static final int AUGUST = 8;

  public static final int SEPTEMBER = 9;

  public static final int OCTOBER = 10;

  public static final int NOVEMBER = 11;

  public static final int DECEMBER = 12;

  private static final String DEFAULT_DATE_SIMPLE_PATTERN = "yyyy-MM-dd";

  private static final String DEFAULT_DATETIME_24HOUR_PATTERN = "yyyy-MM-dd HH:mm:ss";

  private static final ThreadLocal<SimpleDateFormat> simpleDateFormatCache = new ThreadLocal<SimpleDateFormat>();

  private static final ThreadLocal<Calendar> calendarCache = new ThreadLocal<Calendar>();

  private DateUtils() {

  }

  /**
   * The date format pattern is "yyyy-MM-dd HH:mm:ss", so you must put data like
   * this: '2012-12-21 00:00:00'
   * 
   * @param dateString date string to be handled.
   * @return a new Date object by given date string.
   * @throws DateException
   */
  public static Date getDateFromString(final String dateString) {

    return getDateFromString(dateString, DEFAULT_DATETIME_24HOUR_PATTERN);
  }

  /**
   * Get data from data string using the given pattern and the default date
   * format symbols for the default locale.
   * 
   * @param dateString date string to be handled.
   * @param pattern pattern to be formated.
   * @return a new Date object by given date string and pattern.
   * @throws DateException
   */
  public static Date getDateFromString(final String dateString, final String pattern) {

    try {
      SimpleDateFormat df = buildDateFormat(pattern);
      return df.parse(dateString);
    } catch (ParseException e) {
      throw new DateException(String.format("Could not parse %s with pattern %s.", dateString,
        pattern), e);
    }
  }

  /**
   * Format date by given pattern.
   * 
   * @param date date to be handled.
   * @param pattern pattern use to handle given date.
   * @return a string object of format date by given pattern.
   */
  public static String getDateFormat(final Date date, final String pattern) {

    SimpleDateFormat simpleDateFormat = buildDateFormat(pattern);
    return simpleDateFormat.format(date);
  }

  /**
   * Format date from given date string and date pattern by format pattern.<br>
   * <p>
   * e.g:<br>
   * date: '2012-12-21 13:14:20'<br>
   * datePattern: 'yyyy-MM-dd'<br>
   * formatPattern: 'yyyy-MM-dd 23:59:59'<br>
   * result: "2012-12-21 23:59:59"
   * </p>
   * 
   * @param date date string to be handled.
   * @param datePattern pattern to handle date string to date object.
   * @param formatPattern pattern use to format given date.
   * @return format date from given date string and date pattern by format
   *         pattern.
   * @throws DateException
   */
  public static String getDateFormat(final String date, final String datePattern,
    final String formatPattern) {

    Date parsedDate = getDateFromString(date, datePattern);
    SimpleDateFormat simpleDateFormat = buildDateFormat(formatPattern);
    return simpleDateFormat.format(parsedDate);
  }

  /**
   * Get specify seconds back form given date.
   * 
   * @param secondsBack how many second want to be back.
   * @param date date to be handled.
   * @return a new Date object.
   */
  public static Date getDateOfSecondsBack(final int secondsBack, final Date date) {

    return dateBack(Calendar.SECOND, secondsBack, date);
  }

  /**
   * Get specify minutes back form given date.
   * 
   * @param minutesBack how many minutes want to be back.
   * @param date date to be handled.
   * @return a new Date object.
   */
  public static Date getDateOfMinutesBack(final int minutesBack, final Date date) {

    return dateBack(Calendar.MINUTE, minutesBack, date);
  }

  /**
   * Get specify hours back form given date.
   * 
   * @param hoursBack how many hours want to be back.
   * @param date date to be handled.
   * @return a new Date object.
   */
  public static Date getDateOfHoursBack(final int hoursBack, final Date date) {

    return dateBack(Calendar.HOUR_OF_DAY, hoursBack, date);
  }

  /**
   * Get specify days back from given date.
   * 
   * @param daysBack how many days want to be back.
   * @param date date to be handled.
   * @return a new Date object.
   */
  public static Date getDateOfDaysBack(final int daysBack, final Date date) {

    return dateBack(Calendar.DAY_OF_MONTH, daysBack, date);
  }

  /**
   * Get specify weeks back from given date.
   * 
   * @param weeksBack how many weeks want to be back.
   * @param date date to be handled.
   * @return a new Date object.
   */
  public static Date getDateOfWeeksBack(final int weeksBack, final Date date) {

    return dateBack(Calendar.WEEK_OF_MONTH, weeksBack, date);
  }

  /**
   * Get specify months back from given date.
   * 
   * @param monthsBack how many months want to be back.
   * @param date date to be handled.
   * @return a new Date object.
   */
  public static Date getDateOfMonthsBack(final int monthsBack, final Date date) {

    return dateBack(Calendar.MONTH, monthsBack, date);
  }

  /**
   * Get specify years back from given date.
   * 
   * @param yearsBack how many years want to be back.
   * @param date date to be handled.
   * @return a new Date object.
   */
  public static Date getDateOfYearsBack(final int yearsBack, final Date date) {

    return dateBack(Calendar.YEAR, yearsBack, date);
  }

  /**
   * Get the second within the minute by given date.<br>
   * E.g.: at 10:04:15.250 PM the SECOND is 15.
   * 
   * @param date date to be handled.
   * @return second within the minute by given date.
   */
  public static int getSecondOfMinute(final Date date) {

    return getNumberOfGranularity(Calendar.SECOND, date);
  }

  /**
   * Get the minute within the hour by given date.<br>
   * E.g.: at 10:04:15.250 PM the MINUTE is 4.
   * 
   * @param date date to be handled.
   * @return minute within the hour by given date.
   */
  public static int getMinuteOfHour(final Date date) {

    return getNumberOfGranularity(Calendar.MINUTE, date);
  }

  /**
   * Get the hour of the day by given date. HOUR_OF_DAY is used for the 24-hour
   * clock(0-23).<br>
   * E.g.: at 10:04:15.250 PM the HOUR_OF_DAY is 22.
   * 
   * @param date date to be handled.
   * @return the hour of the day by given date.
   */
  public static int getHourOfDay(final Date date) {

    return getNumberOfGranularity(Calendar.HOUR_OF_DAY, date);
  }

  /**
   * Get the day of the week by given date. This field takes values SUNDAY = 1,
   * MONDAY = 2, TUESDAY = 3, WEDNESDAY = 4, THURSDAY = 5, FRIDAY = 6, and
   * SATURDAY = 7.
   * 
   * @param date date to be handled.
   * @return the day of the week by given date.
   */
  public static int getDayOfWeek(final Date date) {

    int dayOfWeek = getNumberOfGranularity(Calendar.DAY_OF_WEEK, date);
    return dayOfWeek;
  }

  /**
   * Get the day of the month by given date. The first day of the month has
   * value 1.
   * 
   * @param date date to be handled.
   * @return the day of the month by given date.
   */
  public static int getDayOfMonth(final Date date) {

    return getNumberOfGranularity(Calendar.DAY_OF_MONTH, date);
  }

  /**
   * Get the day number within the year by given date. The first day of the year
   * has value 1.
   * 
   * @param date date to be handled.
   * @return the day number within the year by given date.
   */
  public static int getDayOfYear(final Date date) {

    return getNumberOfGranularity(Calendar.DAY_OF_YEAR, date);
  }

  /**
   * Get the week of the month by given date. The first week of the month has
   * value 1.
   * 
   * @param date date to be handled.
   * @return the week of the month by given date.
   */
  public static int getWeekOfMonth(final Date date) {

    return getNumberOfGranularity(Calendar.WEEK_OF_MONTH, date);
  }

  /**
   * Get the week number within the year by given date. The first week of the
   * year, the first week of the year value is 1. Subclasses define the value of
   * WEEK_OF_YEAR for days before the first week of the year.
   * 
   * @param date date to be handled.
   * @return the week number within the year by given date.
   */
  public static int getWeekOfYear(final Date date) {

    return getNumberOfGranularity(Calendar.WEEK_OF_YEAR, date);
  }

  /**
   * Get the month of year by given date. This is a calendar-specific value. The
   * first month of the year in the calendars is JANUARY which is 1; the last
   * depends on the number of months in a year.
   * 
   * @param date date to be handled.
   * @return the month of year by given date.
   */
  public static int getMonthOfYear(final Date date) {

    return getNumberOfGranularity(Calendar.MONTH, date) + 1;
  }

  /**
   * Get the year by given date.
   * 
   * @param date date to be handled.
   * @return the year by given date.
   */
  public static int getYear(final Date date) {

    return getNumberOfGranularity(Calendar.YEAR, date);
  }

  /**
   * Return true when if the given date is the first day of the month; false
   * otherwise.
   * 
   * @param date date to be tested.
   * @return true when if the given date is the first day of the month; false
   *         otherwise.
   */
  public static boolean isFirstDayOfTheMonth(final Date date) {

    return getDayOfMonth(date) == 1;
  }

  /**
   * Return true if the given date is the last day of the month; false
   * otherwise.
   * 
   * @param date date to be tested.
   * @return true if the given date is the last day of the month; false
   *         otherwise.
   */
  public static boolean isLastDayOfTheMonth(final Date date) {

    Date dateOfMonthsBack = getDateOfMonthsBack(-1, date);
    int dayOfMonth = getDayOfMonth(dateOfMonthsBack);
    Date dateOfDaysBack = getDateOfDaysBack(dayOfMonth, dateOfMonthsBack);
    return dateOfDaysBack.equals(date);
  }

  /**
   * Return current system date.
   * 
   * @return a new date object which date is current system date.
   */
  public static Date getCurrentDate() {

    return new Date();
  }

  /**
   * Allocates a Date object and initializes it to represent the specified
   * number of milliseconds since the standard base time known as "the epoch",
   * namely January 1, 1970, 00:00:00 GMT.
   * 
   * @param times number of milliseconds.
   * @return a Date object and initializes it to represent the specified number
   *         of milliseconds since the standard base time known as "the epoch",
   *         namely January 1, 1970, 00:00:00 GMT.
   */
  public static Date BuildDate(final long times) {

    return new Date(times);
  }

  /**
   * Get how many seconds between two date.
   * 
   * @param date1 date to be tested.
   * @param date2 date to be tested.
   * @return how many seconds between two date.
   */
  public static long subSeconds(final Date date1, final Date date2) {

    return subTime(date1, date2, DatePeriod.SECOND);
  }

  /**
   * Get how many minutes between two date.
   * 
   * @param date1 date to be tested.
   * @param date2 date to be tested.
   * @return how many minutes between two date.
   */
  public static long subMinutes(final Date date1, final Date date2) {

    return subTime(date1, date2, DatePeriod.MINUTE);
  }

  /**
   * Get how many hours between two date.
   * 
   * @param date1 date to be tested.
   * @param date2 date to be tested.
   * @return how many hours between two date.
   */
  public static long subHours(final Date date1, final Date date2) {

    return subTime(date1, date2, DatePeriod.HOUR);
  }

  /**
   * Get how many days between two date, the date pattern is 'yyyy-MM-dd'.
   * 
   * @param dateString1 date string to be tested.
   * @param dateString2 date string to be tested.
   * @return how many days between two date.
   * @throws DateException
   */
  public static long subDays(final String dateString1, final String dateString2) {

    Date date1 = getDateFromString(dateString1, DEFAULT_DATE_SIMPLE_PATTERN);
    Date date2 = getDateFromString(dateString2, DEFAULT_DATE_SIMPLE_PATTERN);
    return subDays(date1, date2);
  }

  /**
   * Get how many days between two date.
   * 
   * @param date1 date to be tested.
   * @param date2 date to be tested.
   * @return how many days between two date.
   */
  public static long subDays(final Date date1, final Date date2) {

    return subTime(date1, date2, DatePeriod.DAY);
  }

  /**
   * Get how many months between two date, the date pattern is 'yyyy-MM-dd'.
   * 
   * @param dateString1 date string to be tested.
   * @param dateString2 date string to be tested.
   * @return how many months between two date.
   * @throws DateException
   */
  public static long subMonths(final String dateString1, final String dateString2) {

    Date date1 = getDateFromString(dateString1, DEFAULT_DATE_SIMPLE_PATTERN);
    Date date2 = getDateFromString(dateString2, DEFAULT_DATE_SIMPLE_PATTERN);
    return subMonths(date1, date2);
  }

  /**
   * Get how many months between two date.
   * 
   * @param date1 date to be tested.
   * @param date2 date to be tested.
   * @return how many months between two date.
   */
  public static long subMonths(final Date date1, final Date date2) {

    Calendar calendar1 = buildCalendar(date1);
    int monthOfDate1 = calendar1.get(Calendar.MONTH);
    int yearOfDate1 = calendar1.get(Calendar.YEAR);
    Calendar calendar2 = buildCalendar(date2);
    int monthOfDate2 = calendar2.get(Calendar.MONTH);
    int yearOfDate2 = calendar2.get(Calendar.YEAR);
    int subMonth = Math.abs(monthOfDate1 - monthOfDate2);
    int subYear = Math.abs(yearOfDate1 - yearOfDate2);
    return subYear * 12 + subMonth;
  }

  /**
   * Get how many years between two date, the date pattern is 'yyyy-MM-dd'.
   * 
   * @param dateString1 date string to be tested.
   * @param dateString2 date string to be tested.
   * @return how many years between two date.
   * @throws DateException
   */
  public static long subYears(final String dateString1, final String dateString2) {

    Date date1 = getDateFromString(dateString1, DEFAULT_DATE_SIMPLE_PATTERN);
    Date date2 = getDateFromString(dateString2, DEFAULT_DATE_SIMPLE_PATTERN);
    return subMonths(date1, date2);
  }

  /**
   * Get how many years between two date.
   * 
   * @param date1 date to be tested.
   * @param date2 date to be tested.
   * @return how many years between two date.
   */
  public static long subYears(final Date date1, final Date date2) {

    return Math.abs(getYear(date1) - getYear(date2));
  }

  /**
   * Returns the beginning of the given day. <br/>
   * e.g: '2012-12-21 21:21:21' => '2012-12-21 00:00:00'
   * 
   * @param date date to be handled.
   * @return a new date is beginning of the given day.
   * @throws DateException
   */
  public static Date formatToStartOfDay(final Date date) {

    try {
      SimpleDateFormat dateFormat = buildDateFormat(DEFAULT_DATE_SIMPLE_PATTERN);
      String formattedDate = dateFormat.format(date);
      return dateFormat.parse(formattedDate);
    } catch (ParseException pe) {
      throw new DateException("Unparseable date specified.", pe);
    }
  }

  /**
   * Returns the beginning of the given day. <br/>
   * e.g: '2012-12-21 21:21:21' => '2012-12-21 23:59:59'
   * 
   * @param date date to be handled.
   * @return a new date is ending of the given day.
   * @throws DateException
   */
  public static Date formatToEndOfDay(final Date date) {

    return getDateOfSecondsBack(1, getDateOfDaysBack(-1, formatToStartOfDay(date)));
  }

  /**
   * Get a SimpleDateFormat object by given pattern.
   * 
   * @param pattern date format pattern.
   * @return a SimpleDateFormat object by given pattern.
   */
  private static SimpleDateFormat buildDateFormat(final String pattern) {

    SimpleDateFormat simpleDateFormat = simpleDateFormatCache.get();
    if (simpleDateFormat == null) {
      simpleDateFormat = new SimpleDateFormat();
      simpleDateFormatCache.set(simpleDateFormat);
    }
    simpleDateFormat.applyPattern(pattern);
    return simpleDateFormat;
  }

  /**
   * Gets a calendar using the default time zone and locale. The Calendar
   * returned is based on the current time in the default time zone with the
   * default locale.
   * 
   * @return a Calendar object.
   */
  private static Calendar buildCalendar() {

    Calendar calendar = calendarCache.get();
    if (calendar == null) {
      calendar = GregorianCalendar.getInstance();
      calendarCache.set(calendar);
    }
    return calendar;
  }

  /**
   * Gets a calendar using the default time zone and locale. The Calendar
   * returned is based on the given time in the default time zone with the
   * default locale.
   * 
   * @return a Calendar object use given date.
   */
  private static Calendar buildCalendar(final Date date) {

    Calendar calendar = buildCalendar();
    calendar.setTime(date);
    return calendar;
  }

  private static long subTime(final Date date1, final Date date2, final long granularity) {

    long time1 = date1.getTime();
    long time = date2.getTime();
    long subTime = Math.abs(time1 - time);
    return subTime / granularity;
  }

  private static int getNumberOfGranularity(final int granularity, final Date date) {

    Calendar calendar = buildCalendar(date);
    return calendar.get(granularity);
  }

  private static long getTimeBackInMillis(final int granularity, final int numberToBack,
    final Date date) {

    Calendar calendar = buildCalendar(date);
    calendar.add(granularity, -numberToBack);
    long timeBackInMillis = calendar.getTimeInMillis();
    return timeBackInMillis;
  }

  private static Date dateBack(final int granularity, final int numberToBack, final Date date) {

    long timeBackInMillis = getTimeBackInMillis(granularity, numberToBack, date);
    return BuildDate(timeBackInMillis);
  }
}
