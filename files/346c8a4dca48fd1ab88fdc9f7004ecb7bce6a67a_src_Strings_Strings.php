<?php

namespace Vinala\Kernel\String;

use Vinala\Kernel\Collections\Collection;
use Vinala\Kernel\String\Exception\StringOutIndexException;

/**
 * The String surface.
 *
 * @version 3.0
 *
 * @author Youssef Had
 *
 * @since v3.3.0
 */
class Strings
{
    //--------------------------------------------------------
    // Properties
    //--------------------------------------------------------

    //

    //--------------------------------------------------------
    // Constants
    //--------------------------------------------------------

    /**
     * String constant for both side trime.
     *
     * @var string
     */
    const TRIM_BOTH = 'both';

    /**
     * String constant for end side trime.
     *
     * @var string
     */
    const TRIM_END = 'end';

    /**
     * String constant for strat side trime.
     *
     * @var string
     */
    const TRIM_START = 'start';

    //--------------------------------------------------------
    // Constructor
    //--------------------------------------------------------

    public function __construct()
    {
        //
    }

    //--------------------------------------------------------
    // Functions
    //--------------------------------------------------------

    /**
     * Get length of a given string.
     *
     * @param string $string
     *
     * @return int
     */
    public static function length($string)
    {
        return strlen($string);
    }

    /**
     * Splite a given string to many strings by a limiter.
     *
     * @param string $string
     * @param string $limit
     *
     * @return array
     */
    public static function splite($string, $limit)
    {
        return explode($limit, $string);
    }

    /**
     * Concat strings as many as args.
     *
     * @param param[]
     *
     * @return string
     */
    public static function concat()
    {
        $args = func_get_arg();
        $string = '';

        foreach ($args as $arg) {
            $string .= $arg;
        }

        return $string;
    }

    /**
     * Compare between two strings.
     *
     * @param string $str1
     * @param string $str2
     * @param bool   $ignoreCase
     *
     * @return bool
     */
    public static function compare($str1, $str2, $ignoreCase = true)
    {
        if ($ignoreCase) {
            if (strcasecmp($str1, $str2) == 0) {
                return true;
            }
        } else {
            if (strcmp($str1, $str2) == 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Join array element to string.
     *
     * @param array  $strings
     * @param string $separator
     * @param int    $start
     * @param int    $count
     *
     * @return simplexml_load_string
     */
    public static function join($strings, $separator, $start = 0, $count = -1)
    {
        $string = '';

        $end = $count == -1 ? Collection::count($strings) : $count;

        for ($i = $start; $i < $end; $i++) {
            $string .= $strings[$i].$separator;

            if ($i == ($end - 1)) {
                $string .= $separator;
            }
        }

        return $string;
    }

    /**
     * Replace string by other string.
     *
     * @param string $target
     * @param string $search
     * @param string $object
     *
     * @return string
     */
    public static function replace($target, $search, $object)
    {
        return str_replace($search, $object, $target);
    }

    /**
     * Check if string contains another string.
     *
     * @param string $string
     * @param string $substring
     *
     * @return bool
     */
    public static function contains($string, $substring)
    {
        if (strpos($string, $substring) !== false) {
            return true;
        }

        return false;
    }

    /**
     * Get the char at an index from a given string.
     *
     * @param string $string
     * @param int    $index
     *
     * @return string | bool
     */
    public static function at($string, $index)
    {
        if (self::length($string) >= ($index + 1)) {
            return $string[$index];
        }

        exception(StringOutIndexException::class);
    }

    /**
     * Insert a substring inside another string.
     *
     * @param string $string
     * @param string $substring
     *
     * @return string
     */
    public static function insert($string, $substring, $index)
    {
        exception_if(!self::checkIndex($string, $index), StringOutIndexException::class);

        $str = '';

        for ($i = 0; $i < static::length($string); $i++) {
            if ($i == $index) {
                $str .= $substring;
            }

            $str .= $string[$i];
        }

        return $str;
    }

    /**
     * Get substring from string.
     *
     * @param string $string
     * @param int    $indexStart
     * @param int    $count
     *
     * @return string
     */
    public static function substring($string, $substring, $count = null)
    {
        return mb_substr($string, $indexStart, $count, 'UTF-8');
    }

    /**
     * Check if index is in string range.
     *
     * @param string $string
     * @param int    $index
     *
     * @return bool
     */
    protected static function checkIndex($string, $index)
    {
        if (static::length($string) > ($index + 1)) {
            return true;
        }

        return false;
    }

    /**
     * Trim a string.
     *
     * @param string $string
     * @param string $chars
     * @param string side
     *
     * @return string
     */
    public static function trim($string, $side = self::TRIM_BOTH, $chars = null)
    {
        if ($side == self::TRIM_START) {
            return ltrim($string, $chars);
        } elseif ($side == self::TRIM_END) {
            return rtrim($string, $chars);
        } elseif ($side == self::TRIM_BOTH) {
            return trim($string, $chars);
        }
    }

    /**
     * Convert string to lower.
     *
     * @param string $string
     *
     * @return string
     */
    public static function toLower($string)
    {
        return strtolower($string);
    }

    /**
     * Convert string to upper.
     *
     * @param string $string
     *
     * @return string
     */
    public static function toUpper($string)
    {
        return strtoupper($string);
    }

    /**
     * Convert first char in string to upper.
     *
     * @param string $string
     *
     * @return string
     */
    public static function firstUpper($string)
    {
        return ucfirst($string);
    }

    /**
     * Convert first chars of paragraph to upper.
     *
     * @param string $string
     *
     * @return string
     */
    public static function wordsUpper($string)
    {
        return ucwords($string);
    }

    /**
     * Check if string starts with another string of collection of strings.
     *
     * @param string       $string
     * @param string|array $substring
     *
     * @return bool
     */
    public static function startsWith($string, $substrings)
    {
        if (is_array($substrings)) {
            foreach ((array) $substrings as $substring) {
                if ($substring != '' && mb_strpos($string, $substring) === 0) {
                    return true;
                }
            }
        } elseif (is_string($substrings)) {
            if ($substrings != '' && mb_strpos($string, $substrings) === 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check ifstring ends with another string of collection of strings.
     *
     * @param string       $string
     * @param string|array $substring
     *
     * @return bool
     */
    public static function endsWith($string, $substrings)
    {
        if (is_array($substrings)) {
            foreach ((array) $substrings as $substring) {
                if ((string) $substring === static::subString($string, -static::length($substring))) {
                    return true;
                }
            }
        } elseif (is_string($substrings)) {
            if ((string) $substring === static::subString($string, -static::length($substring))) {
                return true;
            }
        }

        return false;
    }
}
