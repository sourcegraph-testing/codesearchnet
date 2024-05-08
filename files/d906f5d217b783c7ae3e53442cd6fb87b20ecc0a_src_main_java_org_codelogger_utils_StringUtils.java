package org.codelogger.utils;

import static com.google.common.collect.Lists.newArrayList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codelogger.utils.component.StringProcessChain;

/**
 * A useful tools to handle string, likes judge blank or not blank, first
 * character to upper or lower case, index with, judge contains, encoding,
 * replace, delete and so on...
 *
 * @author DengDefei
 */
public class StringUtils {

  public static final int INDEX_OF_NOT_FOUND = -1;

  private final static String WORD_PATTERN = "[\\p{N}\\p{L}]+[\\p{Pd}]|[\\p{N}\\p{L}]+";

  private final static Vector<Pattern> patternCache = new Vector<Pattern>(1);

  private StringUtils() {

  }

  /**
   * Returns true if the source string is null or length = 0; false otherwise.
   *
   * @param source string to be tested.
   * @return true if the source string is null or length = 0; false otherwise.
   */
  public static boolean isEmpty(final String source) {

    return source == null || source.length() == 0;
  }

  /**
   * Returns true if the source string is null or all characters in this string
   * is space; false otherwise.
   *
   * @param source string to be tested.
   * @return true if the source string is null or all characters in this string
   *         is space; false otherwise.
   */
  public static boolean isBlank(final String source) {

    if (isEmpty(source)) {
      return true;
    }
    int strLen = source.length();
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(source.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the source string is not null and at least one character in
   * this string is not space; false otherwise.
   *
   * @param source string to be tested.
   * @return true if the source string is not null and at least one character in
   *         this string is not space; false otherwise.
   */
  public static boolean isNotBlank(final String source) {

    return !isBlank(source);
  }

  /**
   * Convert first character in given string to upper case.<br>
   * If given source string is blank or first character is upper case, return
   * it's self.
   *
   * @param source string to be tested.
   * @return a new string has been converted or it's self.
   */
  public static String firstCharToUpperCase(final String source) {

    if (isBlank(source) || Character.isUpperCase(source.charAt(0))) {
      return source;
    }
    StringBuilder destination = getStringBuild();
    destination.append(source.substring(0, 1).toUpperCase());
    destination.append(source.substring(1));
    return destination.toString();
  }

  /**
   * Convert first character in given string to lower case.<br>
   * If given source string is blank or first character is lower case, return
   * it's self.
   *
   * @param source string want to be handle.
   * @return a new string has been converted or it's self.
   */
  public static String firstCharToLowerCase(final String source) {

    if (isBlank(source) || Character.isLowerCase(source.charAt(0))) {
      return source;
    }
    StringBuilder destination = getStringBuild();
    destination.append(source.substring(0, 1).toLowerCase());
    destination.append(source.substring(1));
    return destination.toString();
  }

  /**
   * Returns the index within given source string of the first occurrence of the
   * specified target string with ignore case sensitive.
   *
   * @param source source string to be tested.
   * @param target target string to be tested.
   * @return index number if found, -1 otherwise.
   */
  public static int indexOfIgnoreCase(final String source, final String target) {

    int targetIndex = source.indexOf(target);
    if (targetIndex == INDEX_OF_NOT_FOUND) {
      String sourceLowerCase = source.toLowerCase();
      String targetLowerCase = target.toLowerCase();
      targetIndex = sourceLowerCase.indexOf(targetLowerCase);
      return targetIndex;
    } else {
      return targetIndex;
    }
  }

  /**
   * Returns the index within given source string of the first occurrence of the
   * specified target string with ignore case sensitive, starting at the specify
   * index.
   *
   * @param source source string to be tested.
   * @param target target string to be tested.
   * @return index number if found, -1 otherwise.
   */
  public static int indexOfIgnoreCase(final String source, final String target, final int fromIndex) {

    int targetIndex = source.indexOf(target, fromIndex);
    if (targetIndex == INDEX_OF_NOT_FOUND) {
      String sourceLowerCase = source.toLowerCase();
      String targetLowerCase = target.toLowerCase();
      targetIndex = sourceLowerCase.indexOf(targetLowerCase, fromIndex);
      return targetIndex;
    } else {
      return targetIndex;
    }
  }

  /**
   * Returns true if given string have white span, false otherwise.<br>
   *
   * @param source string to be tested.
   * @return true if given string have white span, false otherwise.
   */
  public static boolean containsWhitespace(final String source) {

    int strLen = source.length();
    for (int i = 0; i < strLen; i++) {
      if (Character.isWhitespace(source.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if given string contains given target string ignore case,
   * false otherwise.<br>
   *
   * @param source string to be tested.
   * @param target string to be tested.
   * @return true if given string contains given target string ignore case,
   *         false otherwise.
   */
  public static boolean containsIgnoreCase(final String source, final String target) {

    if (source != null && target != null) {
      return indexOfIgnoreCase(source, target) != INDEX_OF_NOT_FOUND;
    } else {
      return false;
    }
  }

  /**
   * Returns words in given text by given word minimum length.<br>
   * <p>
   * e.g: <br>
   * text: "I have a dream, but now it is only a dream."<br>
   * wordMinimumLength: 2 <br>
   * result:["have","dream","but","now","it","is","only","dream"].<br>
   * <p>
   * NB. "1word","word1" also were words.
   *
   * @param text source string to be handled.
   * @param wordMinimumLength word minimum length to partition.
   * @return words in given text by given word minimum length.
   */
  public static List<String> getWords(final String text, final int wordMinimumLength) {

    List<String> allWords = newArrayList();
    if (text == null) {
      return allWords;
    }
    Matcher matcher = matchText(text);
    for (; matcher.find();) {
      String word = matcher.group(0);
      if (wordMinimumLength < 2) {
        allWords.add(word);
      } else {
        if (word.length() >= wordMinimumLength) {
          allWords.add(word);
        }
      }
    }
    return allWords;
  }

  /**
   * 根据正则表达式提取字符串
   *
   * @param text 源字符串
   * @param regex 正则表达式
   * @return 所以符合正则表达式的字符串的集合
   */
  public static List<String> getStringsByRegex(final String text, final String regex) {

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(text);
    List<String> matchedStrings = new ArrayList<String>();
    while (matcher.find()) {
      matchedStrings.add(matcher.group(0));
    }
    return matchedStrings;
  }

  /**
   * Returns true if given text contains given word; false otherwise.
   *
   * @param text string text to be tested.
   * @param word string word to be tested.
   * @return true if given text contains given word; false otherwise.
   */
  public static boolean containsWord(final String text, final String word) {

    if (text == null || word == null) {
      return false;
    }
    if (text.contains(word)) {
      Matcher matcher = matchText(text);
      for (; matcher.find();) {
        String matchedWord = matcher.group(0);
        if (matchedWord.equals(word)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if given text contains given word ignore case; false
   * otherwise.
   *
   * @param text string text to be tested.
   * @param word string word to be tested.
   * @return true if given text contains given word ignore case; false
   *         otherwise.
   */
  public static boolean containsWordIgnoreCase(final String text, final String word) {

    if (text == null || word == null) {
      return false;
    }
    return containsWord(text.toLowerCase(), word.toLowerCase());
  }

  /**
   * Trim all white space in the given string.<br>
   * If given source string self if it's null or length == 0 or not have white
   * space.
   *
   * @param source
   * @return a new string has been removed all white space if given source
   *         string have white space; given source string self otherwise.
   */
  public static String trimAllWhitespace(final String source) {

    return StringProcessChain.getInstance(source).trimAllWhitespace().getResult();
  }

  /**
   * Returns true if given source string start with target string ignore case
   * sensitive; false otherwise.
   *
   * @param source string to be tested.
   * @param target string to be tested.
   * @return true if given source string start with target string ignore case
   *         sensitive; false otherwise.
   */
  public static boolean startsWithIgnoreCase(final String source, final String target) {

    if (source.startsWith(target)) {
      return true;
    }
    if (source.length() < target.length()) {
      return false;
    }
    return source.substring(0, target.length()).equalsIgnoreCase(target);
  }

  /**
   * Returns true if given source string end with target string ignore case
   * sensitive; false otherwise.
   *
   * @param source string to be tested.
   * @param target string to be tested.
   * @return true if given source string end with target string ignore case
   *         sensitive; false otherwise.
   */
  public static boolean endsWithIgnoreCase(final String source, final String target) {

    if (source.endsWith(target)) {
      return true;
    }
    if (source.length() < target.length()) {
      return false;
    }
    return source.substring(source.length() - target.length()).equalsIgnoreCase(target);
  }

  /**
   * Start a string process chain for given source.
   *
   * @param source source String.
   * @return return a StringProcessChain for given source.
   */
  public static StringProcessChain startStringProcess(final String source) {

    return StringProcessChain.getInstance(source);
  }

  /**
   * Returns a random alphabetic: A-Z.
   *
   * @return a random alphabetic: A-Z
   */
  public static char getRandomUpperCaseAlphabetic() {

    return (char) MathUtils.randomInt(65, 90);
  }

  /**
   * Returns a random alphabetic: a-z.
   *
   * @return a random alphabetic: a-z
   */
  public static char getRandomLowerCaseAlphabetic() {

    return (char) MathUtils.randomInt(97, 122);
  }

  /**
   * Returns a random alphabetic: a-zA-Z.
   *
   * @return a random alphabetic: a-zA-Z
   */
  public static char getRandomAlphabetic() {

    int randomInt = MathUtils.randomInt(1);
    if (randomInt == 1) {
      return getRandomUpperCaseAlphabetic();
    } else {
      return getRandomLowerCaseAlphabetic();
    }
  }

  /**
   * Returns a random given length size alphabetic string.
   *
   * @param stringLength the length of the random string you want.
   * @return a random given length size alphabetic string.
   */
  public static String getRandomString(final int stringLength) {

    StringBuilder stringBuilder = getStringBuild();
    for (int i = 0; i < stringLength; i++) {
      stringBuilder.append(getRandomAlphabetic());
    }
    return stringBuilder.toString();
  }

  /**
   * Returns a random passowrd char: a-zA-Z0-9.
   *
   * @return a random passowrd char: a-zA-Z0-9
   */
  public static char getRandomPasswordChar() {

    int randomInt = MathUtils.randomInt(2);
    if (randomInt == 0) {
      return (char) MathUtils.randomInt(48, 57);
    } else {
      return getRandomAlphabetic();
    }
  }

  /**
   * Returns a random given length size passowrd string([a-zA-Z0-9]+).
   *
   * @param stringLength the length of the random passowrd you want.
   * @return a random given length size passowrd string([a-zA-Z0-9]+).
   */
  public static String getRandomPasswordString(final int stringLength) {

    StringBuilder stringBuilder = getStringBuild();
    for (int i = 0; i < stringLength; i++) {
      stringBuilder.append(getRandomPasswordChar());
    }
    return stringBuilder.toString();
  }

  /**
   * Return encoded string by given charset.
   *
   * @param source source string to be handle.
   * @param sourceCharset source string charset name.
   * @param encodingCharset want encoding to which charset.
   * @return a new string has been encoded.
   * @throws IllegalArgumentExceptionWORD_PATTERN If the named charset is not
   *           supported
   */
  public static String encoding(final String source, final String sourceCharset,
    final String encodingCharset) throws IllegalArgumentException {

    byte[] sourceBytes;
    String encodeString = null;
    try {
      sourceBytes = source.getBytes(sourceCharset);
      encodeString = new String(sourceBytes, encodingCharset);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(String.format("Unsupported encoding:%s or %s",
        sourceCharset, encodingCharset));
    }
    return encodeString;
  }

  /**
   * Sort the array in reverse order.</br>反转字符串。
   *
   * @param string string to be handled.
   */
  public static String reverse(final String string) {

    return isEmpty(string) ? string : new StringBuilder(string).reverse().toString();
  }

  /**
   * Count how much target in souce string.</br>统计target在source中出现的次数。
   *
   * @param source source string
   * @param target target string
   * @return the count of target in source string.
   */
  public static int count(final String source, final String target) {

    if (isEmpty(source) || isEmpty(target)) {
      return 0;
    }
    int targetLength = target.length();
    int replacedLength = source.length() - source.replace(target, "").length();
    return replacedLength / targetLength;
  }

  /**
   * Count how much target in souce string.</br>统计target在source中出现的次数。
   *
   * @param source source string
   * @param target target string
   * @return the count of target in source string.
   */
  public static int countIgnoreCase(final String source, final String target) {

    if (isEmpty(source) || isEmpty(target)) {
      return 0;
    }
    return count(source.toLowerCase(), target.toLowerCase());
  }

  private static StringBuilder getStringBuild() {

    return new StringBuilder();
  }

  private static synchronized Pattern getWordPatternFromCache() {

    if (patternCache.isEmpty()) {
      return Pattern.compile(WORD_PATTERN);
    } else {
      Pattern pattern = patternCache.get(0);
      patternCache.remove(0);
      return pattern;
    }
  }

  private static Matcher matchText(final String text) {

    Pattern pattern = getWordPatternFromCache();
    Matcher matcher = pattern.matcher(text);
    patternCache.add(pattern);
    return matcher;
  }
}