<?php
/**
 * Copyright (c) 2016.
 * @author Nikola Tesic (nikolatesic@gmail.com)
 */

/**
 * Created by PhpStorm.
 * User: Nikola
 * Date: 12/10/2016
 * Time: 12:00 AM
 */

namespace ntesic\Helpers;

use Phalcon\Exception;
use Phalcon\Mvc\Model;
use \Phalcon\Tag as BaseTag;

class Tag extends BaseTag
{

    /**
     * @param $name
     * @param $content
     * @param null $options
     * @return string
     */
    public static function tag($name, $content, $options = null)
    {
        $output = '';
        $output = self::tagHtml($name, $options, false, false, true);
        $output .= $content;
        $output .= self::tagHtmlClose($name, true);
        return $output;
    }

    /**
     * Encodes special characters into HTML entities.
     * @param string $content the content to be encoded
     * @param boolean $doubleEncode whether to encode HTML entities in `$content`. If false,
     * HTML entities in `$content` will not be further encoded.
     * @return string the encoded content
     * @see decode()
     * @see http://www.php.net/manual/en/function.htmlspecialchars.php
     */
    public static function encode($content, $doubleEncode = true)
    {
        return htmlspecialchars($content, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8', $doubleEncode);
    }

    /**
     * Generates an appropriate input ID for the specified attribute name or expression.
     *
     * This method converts the result [[getInputName()]] into a valid input ID.
     * For example, if [[getInputName()]] returns `Post[content]`, this method will return `post-content`.
     * @param Model $model the model object
     * @param string $attribute the attribute name or expression. See [[getAttributeName()]] for explanation of attribute expression.
     * @return string the generated input ID
     * @throws Exception if the attribute name contains non-word characters.
     */
    public static function getInputId($model, $attribute)
    {
        $name = strtolower(static::getInputName($model, $attribute));
        return str_replace(['[]', '][', '[', ']', ' ', '.'], ['', '-', '-', '', '-', '-'], $name);
    }

    /**
     * Generates an appropriate input name for the specified attribute name or expression.
     *
     * This method generates a name that can be used as the input name to collect user input
     * for the specified attribute. The name is generated according to the [[Model::formName|form name]]
     * of the model and the given attribute name. For example, if the form name of the `Post` model
     * is `Post`, then the input name generated for the `content` attribute would be `Post[content]`.
     *
     * See [[getAttributeName()]] for explanation of attribute expression.
     *
     * @param Model $model the model object
     * @param string $attribute the attribute name or expression
     * @return string the generated input name
     * @throws Exception if the attribute name contains non-word characters.
     */
    public static function getInputName($model, $attribute)
    {
        $reflector = new \ReflectionClass($model);
        $formName = $reflector->getShortName();
        if (!preg_match('/(^|.*\])([\w\.]+)(\[.*|$)/', $attribute, $matches)) {
            throw new Exception('Attribute name must contain word characters only.');
        }
        $prefix = $matches[1];
        $attribute = $matches[2];
        $suffix = $matches[3];
        if ($formName === '' && $prefix === '') {
            return $attribute . $suffix;
        } elseif ($formName !== '') {
            return $formName . $prefix . "[$attribute]" . $suffix;
        } else {
            throw new Exception(get_class($model) . '::formName() cannot be empty for tabular inputs.');
        }
    }
}