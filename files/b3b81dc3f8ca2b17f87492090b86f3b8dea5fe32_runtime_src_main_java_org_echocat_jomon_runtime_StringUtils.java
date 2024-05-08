/*****************************************************************************************
 * *** BEGIN LICENSE BLOCK *****
 *
 * Version: MPL 2.0
 *
 * echocat Jomon, Copyright (c) 2012-2013 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * *** END LICENSE BLOCK *****
 ****************************************************************************************/

package org.echocat.jomon.runtime;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    protected StringUtils() {}

    /**
     * Count the number of occurrences of sub in s
     */
    @Nonnegative
    public static int countSubString(@Nonnull String string, @Nonnull String sub) {
        int count = 0;
        int pos = 0;
        do {
            pos = string.indexOf(sub, pos);
            if (pos != -1) {
                count++;
                pos += sub.length();
            }
        } while (pos != -1);
        return count;
    }

    /**
     * Split a string and return the values as an array.
     *
     * @param delimiters delimiters that are accepted by StringTokenizer
     */
    @Nonnull
    public static List<String> splitToList(@Nonnull String source, @Nonnull String delimiters, boolean returnDelimiters, boolean trim) {
        final List<String> values = new ArrayList<>();
        if (source != null) {
            final StringTokenizer st = new StringTokenizer(source, delimiters, returnDelimiters);
            while (st.hasMoreTokens()) {
                String value = st.nextToken();
                if (trim) {
                    value = trim(value);
                }
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Split a string and return the values as an array.
     *
     * @param delimiters delimiters that are accepted by StringTokenizer
     */
    @Nonnull
    public static String[] split(@Nonnull String source, @Nonnull String delimiters, boolean returnDelimiters, boolean trim) {
        final List<String> values = splitToList(source, delimiters, returnDelimiters, trim);
        final String[] result = new String[values.size()];
        values.toArray(result);
        return result;
    }

    @Nonnull
    public static String[] split(@Nonnull String source, @Nonnull String delimiters, boolean returnDelimiters) {
        return split(source, delimiters, returnDelimiters, false);
    }

    /**
     * Appends the separator string, if <code>{@link StringBuilder#length() buf.lenght()} &gt; 0</code> and then appends the element.
     */
    public static void addElement(@Nonnull StringBuilder buf, @Nonnull String separator, @Nonnull String element) {
        if (buf.length() > 0) {
            buf.append(separator);
        }
        buf.append(element);
    }

    /**
     * Convert a string to lower case. (Faster than String.toLowerCase)
     */
    public static void toLowerCase(char[] chars) {
        for (int a = 0; a < chars.length; a++) {
            chars[a] = Character.toLowerCase(chars[a]);
        }
    }

    /**
     * Test whether 'find' can be found at position 'startPos' in the string 'src'.
     */
    public static boolean startsWith(char[] src, char[] find, int startAt) {
        int startPos = startAt;
        boolean result = true;

        // Check ranges
        if (src.length < startPos + find.length) {
            result = false;
        } else {
            final int max = find.length;
            for (int a = 0; a < max && result; a++) {
                if (src[startPos] != find[a]) {
                    result = false;
                }
                startPos++;
            }
        }
        return result;
    }

    public static boolean containsNoneOf(CharSequence stringToSearch, CharSequence... stringsToSearchFor) {
        return indexOfAny(stringToSearch, stringsToSearchFor) == -1;
    }

    public static boolean containsAnyOf(CharSequence stringToSearch, CharSequence... stringsToSearchFor) {
        return indexOfAny(stringToSearch, stringsToSearchFor) > -1;
    }

    public static boolean containsAny(CharSequence charSequence, List<Character> searchChars) {
        final char[] charArray = new char[searchChars.size()];
        for (int i = 0; i < searchChars.size(); i++) {
            charArray[i] = searchChars.get(i);
        }
        return containsAny(charSequence, charArray);
    }

    /**
     * Same as String.indexOf but (slightly) faster ;)
     *
     * Hint: Performance optimized code - so some warnings are suppressed.
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "LabeledStatement", "ValueOfIncrementOrDecrementUsed", "ContinueStatement", "ContinueStatementWithLabel"})
    public static int indexOf(char[] src, char[] find, int startAt) {
        int startPos = startAt;
        final int max = src.length - find.length;
        if (startPos > max) {
            return -1;
        }
        final char find0 = find[0];
        final int len = find.length;
        int j;
        int k;

        // Find the first character
        startOver:
        while (startPos <= max) {
            if (src[startPos++] == find0) {
                // First character found - look for the rest
                j = startPos;
                k = 1;
                while (k < len) {
                    if (src[j++] != find[k++]) {
                        continue startOver;
                    }
                }
                return startPos - 1;
            }
        }
        return -1;
    }
}
