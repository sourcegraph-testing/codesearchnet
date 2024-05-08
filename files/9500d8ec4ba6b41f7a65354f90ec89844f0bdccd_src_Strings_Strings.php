<?php

/**
 * Copyright (c) 2010-2016 Romain Cottard
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

namespace Eureka\Component\String;

/**
 * String class to manage string.
 *
 * @author Romain Cottard
 */
class Strings
{
    /**
     * @var string $string String to manipulate
     */
    protected $string = '';

    /**
     * @var null|bool If we use mbstring php extension.
     */
    protected static $useMbstring = null;

    /**
     * @var array $functions List of function (standard / mb_string extention)
     */
    protected static $functions = array(

        //~ Without mb_string functions
        0 => array(
            'strlen'     => 'strlen', 'strpos' => 'strpos', 'substr' => 'substr', 'truncate' => 'truncate',
            'strtolower' => 'strtolower', 'strtoupper' => 'strtoupper',
        ),

        //~ With mb_string functions
        1 => array(
            'strlen'     => 'mb_strlen', 'strpos' => 'mb_strpos', 'substr' => 'mb_substr', 'truncate' => 'mb_truncate',
            'strtolower' => 'mb_strtolower', 'strtoupper' => 'mb_strtoupper',
        ),
    );

    /**
     * @var array $charMapping List of characters mapping.
     */
    protected static $charMapping = array(
        'À' => 'a', 'Á' => 'a', 'Ä' => 'a', 'Å' => 'a', 'Ç' => 'c', 'È' => 'e', 'É' => 'e', 'Ê' => 'e', 'Ë' => 'e',
        'Ì' => 'i', 'Í' => 'i', 'Î' => 'i', 'Ï' => 'i', 'Ñ' => 'n', 'Ò' => 'o', 'Ó' => 'o', 'Ô' => 'o', 'Õ' => 'o',
        'Ö' => 'o', 'Ø' => 'o', 'Ù' => 'u', 'Ú' => 'u', 'Û' => 'u', 'Ü' => 'u', 'Ý' => 'y', 'à' => 'a', 'á' => 'a',
        'â' => 'a', 'ã' => 'a', 'ä' => 'a', 'å' => 'a', 'ç' => 'c', 'è' => 'e', 'é' => 'e', 'ê' => 'e', 'ë' => 'e',
        'ì' => 'i', 'í' => 'i', 'î' => 'i', 'ï' => 'i', 'ñ' => 'n', 'ò' => 'o', 'ó' => 'o', 'ô' => 'o', 'õ' => 'o',
        'ö' => 'o', 'ø' => 'o', 'ù' => 'u', 'ú' => 'u', 'û' => 'u', 'ü' => 'u', 'ý' => 'y', 'ÿ' => 'y', '@' => 'a',
        'Œ' => 'oe', 'œ' => 'oe', 'Æ' => 'ae', 'æ' => 'ae',
    );

    /**
     * @var array $charMapping List of characters mapping.
     */
    protected static $charStrip = array("'" => '-', ' ' => '-', '.' => '-');

    /**
     * Class constructor.
     *
     * @param string $string
     */
    public function __construct($string = '')
    {
        if (static::$useMbstring === null) {
            static::$useMbstring = extension_loaded('mbstring');
        }

        $this->string = $string;

        if (static::$useMbstring) {

            mb_detect_order(array('UTF-8', 'ASCII'));
            $encoding = mb_detect_encoding($this->string);

            if ($encoding !== false) {
                mb_internal_encoding($encoding);
            }
        }
    }

    /**
     * Return string
     *
     * @return string
     */
    public function __toString()
    {
        return $this->string;
    }

    /**
     * Strlen function. Use mbstring extension if loaded.
     *
     * @return integer Nb chars.
     */
    public function length()
    {
        $function = static::$functions[(int) static::$useMbstring]['strlen'];

        return $function($this->string);
    }

    /**
     * Strpos function. Use mbstring extension if loaded.
     *
     * @param  string $needle The string to find in haystack.
     * @param  int    $offset The search offset. If it is not specified, 0 is used.
     * @return mixed False if not found, else position.
     */
    public function strpos($needle, $offset = 0)
    {
        $function = static::$functions[(int) static::$useMbstring]['strpos'];

        return $function($this->string, $needle, $offset);
    }

    /**
     * Substring function. Use mbstring extension if loaded.
     *
     * @param  integer $start
     * @param  integer $length
     * @return Strings  Part of string.
     */
    public function substr($start = 0, $length = null)
    {
        $function = static::$functions[(int) static::$useMbstring]['substr'];

        return new self($function($this->string, $start, $length));
    }

    /**
     * Set string to lower case
     *
     * @return self
     */
    public function toLower()
    {
        $function = static::$functions[(int) static::$useMbstring]['strtolower'];

        $this->string = $function($this->string);

        return $this;
    }

    /**
     * Set string to upper case
     *
     * @return self
     */
    public function toUpper()
    {
        $function = static::$functions[(int) static::$useMbstring]['strtoupper'];

        $this->string = $function($this->string);

        return $this;
    }

    /**
     * Trim string
     *
     * @param  string $chars List of chars to trim.
     * @return self
     */
    public function trim($chars = " \t\n\r\0\x0B")
    {
        $this->string = trim($this->string, $chars);

        return $this;
    }

    /**
     * Replace '&' by '&amp;'
     *
     * @return self
     */
    public function amp()
    {
        $this->replace('&amp;', '&')
            ->replace('&', '&amp;');

        return $this;
    }

    /**
     * Get char at index
     *
     * @param integer $index
     * @return string
     */
    public function getChar($index)
    {
        return (isset($this->string[$index]) ? $this->string[$index] : '');
    }

    /**
     * Get char at index
     *
     * @return string
     */
    public function getRandomChar()
    {
        return $this->string[mt_rand(0, $this->length() - 1)];
    }

    /**
     * Check if string is an email.
     *
     * @return bool
     */
    public function isEmail()
    {
        return filter_var($this->string, FILTER_VALIDATE_EMAIL);
    }

    /**
     * Check if string is an email.
     *
     * @return bool
     */
    public function isPhone()
    {
        $phone = clone $this;

        return (bool) preg_match('`^[+]?[0-9]{0,3}[0-9()]{0,3}[0-9]{9,10}$`', (string) $phone->cleanPhone());
    }

    /**
     * Clean string tags into string.
     *
     * @return self
     */
    public function cleanHtml()
    {
        $this->string = strip_tags($this->string);

        return $this;
    }

    /**
     * Clean string as a phone number.
     *
     * @return self
     */
    public function cleanPhone()
    {
        $this->string = preg_replace('`[^0-9+()]?`', '', $this->string);

        return $this;
    }

    /**
     * Strip string and remove accent and non basic characters.
     *
     * @return self
     */
    public function strip()
    {
        $this->noAccent()
            ->trim()
            ->toLower()
            ->replace(array_keys(static::$charStrip), array_values(static::$charStrip))
            ->pregReplace(array('#[^a-z0-9-]#S', '#-+#S'), array('', '-'));

        return $this;
    }

    /**
     * Convert string with accent to same string with no accent.
     *
     * @return self
     */
    public function noAccent()
    {
        $this->string = strtr($this->string, static::$charMapping);

        return $this;
    }

    /**
     * Decode html string into string
     *
     * @param  integer $type
     * @param  string  $encode
     * @return self
     */
    public function htmld($type = ENT_COMPAT, $encode = 'UTF-8')
    {
        $this->string = html_entity_decode($this->string, $type, $encode);

        return $this;
    }

    /**
     * Encode string into html string.
     *
     * @param  int    $type
     * @param  string $encode
     * @return self
     */
    public function htmle($type = ENT_COMPAT, $encode = 'UTF-8')
    {
        $this->string = htmlentities($this->string, $type, $encode);

        return $this;
    }

    /**
     * Decode html string into string
     *
     * @param  int $type
     * @return self
     */
    public function htmlsd($type = ENT_COMPAT)
    {
        $this->string = htmlspecialchars_decode($this->string, $type);

        return $this;
    }

    /**
     * Encode string into html string.
     *
     * @param  int    $type
     * @param  string $encode
     * @return self
     */
    public function htmlse($type = ENT_COMPAT, $encode = 'UTF-8')
    {
        $this->string = htmlspecialchars($this->string, $type, $encode);

        return $this;
    }

    /**
     * Concat string with current string.
     *
     * @param  string $string String to concat
     * @param  bool   $prepend Boolean
     * @return self
     */
    public function concat($string, $prepend = false)
    {
        $this->string = $prepend ? (string) $string . $this->string : $this->string . (string) $string;

        return $this;
    }

    /**
     * Preg replace text in string.
     *
     * @param  string $pattern
     * @param  string $replace
     * @param  int    $limit
     * @param  int    $count
     * @return self
     */
    public function pregReplace($pattern, $replace, $limit = -1, &$count = null)
    {
        $this->string = preg_replace($pattern, $replace, $this->string, $limit, $count);

        return $this;
    }

    /**
     * Replace text in string.
     *
     * @param  string|array $search
     * @param  string|array $replace
     * @param  int          $count
     * @return self
     */
    public function replace($search, $replace, &$count = null)
    {
        $this->string = str_replace($search, $replace, $this->string, $count);

        return $this;
    }

    /**
     * Truncate text after x chars (and add suffix, like '...')
     *
     * @param  int    $length
     * @param  string $suffix
     * @param  bool   $lastSpace
     * @return Strings  Truncated string.
     */
    public function truncate($length = 30, $suffix = '...', $lastSpace = true)
    {
        $string = clone $this;
        $string->htmld();

        if ($string->length() > $length) {
            $string = $string->substr(0, $length - $string->length());

            if (true === $lastSpace) {
                $string->pregReplace('/\s+?(\S+)?$/', '');
            }

            $string->concat($suffix);
        }

        return $string;
    }

    /**
     * Generate code
     *
     * @param  int $nbChars Nb characters in code.
     * @return Strings
     */
    public function gencode($nbChars = 8)
    {
        $chars = new self('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789');

        for ($index = 0; $index < $nbChars; $index++) {
            $this->string .= $chars->getRandomChar();
        }

        return $this;
    }
}
