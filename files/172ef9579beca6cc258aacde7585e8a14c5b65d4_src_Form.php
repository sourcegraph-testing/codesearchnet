<?php
namespace Helpers;

/**
 * Class Form
 *
 * Help to create forms
 * 
 * @package Helpers
 * @author Jamie Ynonan <jamiea31@gmail.com>
 * @version 1.0.0
 */
class Form
{
    use BaseHtmlTrait;

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $method
     * @param string $action
     * @param array $htmlOptions
     * @return string
     */
    public static function open(
        $method = 'POST',
        $action = '',
        array $htmlOptions = []
    ) {
        return '<form method="'. $method .'" action="'. $action .'"'
            . self::htmlOptions($htmlOptions) .'>';
    }

    /**
     * @return string
     */
    public static function close()
    {
        return '</form>';
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $text default
     * @param array $htmlOptions
     * @return string
     */
    public static function submit($text = '', array $htmlOptions = [])
    {
        return '<button type="submit"'. self::htmlOptions($htmlOptions) .'>'. $text .'</button>';
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $text
     * @param array $htmlOptions
     * @return string
     */
    public static function label($text, array $htmlOptions = [])
    {
        return '<label'. self::htmlOptions($htmlOptions) .'>'. $text .'</label>';
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $name
     * @param string $value
     * @param array $htmlOptions
     * @return string
     */
    public static function text($name, $value = '', array $htmlOptions = [])
    {
    	return self::input('text', $name, $value) . self::htmlOptions($htmlOptions) .'>';
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $name
     * @param array $htmlOptions
     * @return string
     */
    public static function password($name, array $htmlOptions = [])
    {
        return self::input('password', $name) . self::htmlOptions($htmlOptions) .'>';
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $name
     * @param string $value
     * @param array $htmlOptions
     * @return string
     */
    public static function hidden($name, $value = '', array $htmlOptions = [])
    {
        return self::input('hidden', $name, $value) . self::htmlOptions($htmlOptions) .'>';
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $name
     * @param string $value
     * @param array $htmlOptions
     * @return string
     */
    public static function email($name, $value = '', array $htmlOptions = [])
    {
        return self::input('email', $name, $value) . self::htmlOptions($htmlOptions) .'>';
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $name
     * @param mixed $value
     * @param null|mixed $checkedValue
     * @param array $htmlOptions
     * @return string
     */
    public static function radio(
        $name,
        $value,
        $checkedValue = null,
        array $htmlOptions = []
    ) {
    	$radio = self::input('radio', $name, $value);
    	if ($checkedValue == $value) {
    		$radio .= ' checked="checked"';
    	}
    	$radio .= self::htmlOptions($htmlOptions) .'>';
    	return $radio;
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $name
     * @param mixed $value
     * @param array $checkedValues
     * @param array $htmlOptions
     * @return string
     */
    public static function checkbox(
        $name,
        $value,
        array $checkedValues = [],
        array $htmlOptions = []
    ) {
        $check = self::input('checkbox', $name, $value);
        if (in_array($value, $checkedValues)) {
            $check .= ' checked="checked"';
        }
        $check .= self::htmlOptions($htmlOptions) .'>';
        return $check;
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $name
     * @param string $text
     * @param array $htmlOptions
     * @return string
     */
    public static function textarea($name, $text = '', array $htmlOptions = [])
    {
        return '<textarea name="'. $name .'"'. self::htmlOptions($htmlOptions)
            .'>'. $text .'</textarea>';
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param string $name
     * @param array $options
     * @param null $value if $multiple is true the $value must be array
     * @param bool $multiple
     * @param array $htmlOptions
     * @throws \InvalidArgumentException if $multiple is true and $value is not array
     * @throws \InvalidArgumentException if $multiple is false and $value is array
     * @return string
     */
    public static function select(
        $name,
        array $options = [],
        $value = null,
        $multiple = false,
        array $htmlOptions = []
    ) {
        if ($multiple === true && !is_array($value)) {
            throw new \InvalidArgumentException('if $multiple is true, $value must be array');
        }
        if ($multiple === false && is_array($value)) {
            throw new \InvalidArgumentException('if $multiple is false, $value can not be array');
        }
        $select = '<select name="'. $name .'"'. self::htmlOptions($htmlOptions);
        if ($multiple === true) {
            $select .= ' multiple';
        }
        $select .= '>';
        $select .= self::selectOptions($options, $value, $multiple);
        $select .= '</select>';
        return $select;
    }

    /**
     * @uses BaseHtmlTrait::BaseHtmlTrait
     *
     * @param array $options
     * @param string $actualValue
     * @param bool $multiple
     * @return string
     */
    private static function selectOptions(
        array $options = [],
        $actualValue = '',
        $multiple = false
    ) {
        $option = '';
        foreach ($options as $value => $label) {
            if (is_array($label)) {
                $option .= '<optgroup label="'. $value .'">';
                $option .= self::selectOptions($label, $actualValue, $multiple);
                $option .= '</optgroup>';
            } else {
                $selected = (!empty($value) && (
                    ($multiple === true && in_array($value, $actualValue)) ||
                    ($multiple === false && $value == $actualValue)
                )) ? ' selected' : '';
                $option .= '<option value="'. $value .'"'. $selected .'>'
                    . $label .'</option>';
            }
        }
        return $option;
    }

    /**
     * @param string $type
     * @param string $name
     * @param string $value
     * @return string
     */
    private static function input($type, $name, $value = '')
    {
        return '<input type="'. $type .'" name="'. $name .'" value="'. $value .'"';
    }
}
