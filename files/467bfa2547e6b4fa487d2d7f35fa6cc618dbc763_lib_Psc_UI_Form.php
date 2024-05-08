<?php

namespace Psc\UI;

use \Psc\UI\HTML AS UIHTML,
    \Psc\UI\Button as UIButton,
    \Psc\Code\Code,
    \Psc\JS\Lambda,
    \stdClass,
    \Webforge\Common\String AS S,
    \Webforge\Common\ArrayUtil AS A;

class Form {
  
  // block0 für misc
  const GROUP_COLLAPSIBLE       = 0x000001;
  const CLEAR                   = 0x000002;
  const GROUP_CONTAINS_BLOCK    = 0x000004;
  
  // block bit2 für alignment
  const ALIGN_LEFT              = 0x000010;
  const ALIGN_CENTER            = 0x000020; 
  const ALIGN_RIGHT             = 0x000040; // fügt clear hinzu
  
  const LABEL_TOP = 'top';
  const LABEL_CHECKBOX = 'checkbox';
  
  // block bit3 für inputSet und input
  const BR                      = 0x000100;
  const OPTIONAL                = 0x000200;
  
  public static $uniquePrefix;
  
  /**
   * Eine Gruppe von Element mit einer Überschrift
   *
   * der Inhalt ist eingerückt und es befindet sich ein Rahmen herum
   * @param mixed $legend der inhalt der Überschrift
   * @param mixed $content der Inhalt des Containers
   * @return HTMLTag
   */
  public static function group($legend, $content, $flags = 0x000000) {
    
    $group = UIHTML::Tag('fieldset', new \stdClass)
      ->addClass('ui-corner-all')
      ->addClass('ui-widget-content')
      ->addClass('\Psc\group')
    ;
    
    $group->contentTemplate = "%legend%\n  %div%";
    $group->content->legend = UIHTML::Tag('legend',$legend);
    $group->content->div = UIHTML::Tag('div',$content,array('class'=>'content'));

    if ($flags & self::GROUP_COLLAPSIBLE) {
      $group->content->legend->addClass('collapsible');
    }
    
    return $group;
  }
  
  public static function hint($text, $br = FALSE) {
    $hint = UIHTML::tag('small',nl2br(HTML::esc($text)),array('class'=>'hint'));
    if ($br) {
      $hint->templateAppend('<br />');
    }
    return $hint;
  }

  public static function button($label, $flags = 0x000000, $iconLeft = NULL, $iconRight = NULL) {
    $button = new UIButton($label);
    
    if ($iconLeft) {
      $button->setLeftIcon($iconLeft);
    }
    
    if ($iconRight) {
      $button->setRightIcon($iconRight);
    }
    
    if (($flags & self::ALIGN_RIGHT) == self::ALIGN_RIGHT) {
      $button->getHTML()
        ->setStyle('float','right')
        ->addClass('\Psc\button-right');
    }
    
    if (($flags & self::ALIGN_LEFT) == self::ALIGN_LEFT) {
      $button->getHTML()
        ->setStyle('float','left')
          ->addClass('\Psc\button-left');
    }
      
  
    if (($flags & self::CLEAR) == self::CLEAR) {
        $button->getHTML()
          ->templateAppend("\n".'<div class="clear"></div>');
    }
    
    return $button;
  }

  
  public static function buttonSave($label = 'Speichern', $flags = 0x000000, $iconLeft = 'disk') {
    $button = self::button($label, $flags, $iconLeft);
    $button->getHTML()
      ->addClass('\Psc\button-save');
    
    return $button;
  }

  public static function buttonSaveClose($label = 'Speichern und Schließen', $flags = 0x000000, $iconLeft = 'disk', $iconRight = 'close') {
    $button = self::button($label, $flags, $iconLeft, $iconRight);
    $button->getHTML()
      ->addClass('\Psc\button-save-close');
    
    return $button;
  }

  public static function buttonReload($label = 'Reload', $flags = 0x000000, $iconLeft = 'refresh', $iconRight = NULL) {
    $button = self::button($label, $flags, $iconLeft, $iconRight);
    $button->getHTML()
      ->addClass('\Psc\button-reload');
    
    return $button;
  }

  /**
   * @return Button
   */
  public static function buttonAdd($label, $flags = 0x000000, $iconLeft = 'circle-plus') {
    $button = self::button($label, $flags, 'circle-plus');
    $button->getHTML()
      ->addClass('\Psc\add');
        
    return $button;
  }
  
  public static function buttonSet(Array $buttons, $flags = 0x000000) {
    $div = UIHTML::tag('div',$buttons,array('class'=>'\Psc\buttonset'));
    
    if (($flags & self::ALIGN_RIGHT) == self::ALIGN_RIGHT) {
      $div
        ->setStyle('float','right')
        ->addClass('\Psc\buttonset-right');
    }
    
    if (($flags & self::ALIGN_LEFT) == self::ALIGN_LEFT) {
      $div
        ->setStyle('float','left')
        ->addClass('\Psc\buttonset-left');
    }
      
  
    if (($flags & self::CLEAR) == self::CLEAR) {
      $div->templateAppend("\n".'<div class="clear"></div>');
    }
    
    return $div;
  }
  
  /**
   * Wandelt alle Parameter in FormInputs um und ruft morph() für sie auf
   *
   * Hints werden hinzufügt und die Flags bearbeitet
   * @return array()
   */
  public static function inputSet() {
    $inputs = func_get_args();
    $inputSet = new FormInputSet($inputs);
    return $inputSet;
  }
  
  /**
   * UIForm::inputSet(
   *   UIForm::Input(UIForm::text(...), 'muss nicht unbedingt angegeben werden', UIForm::BR)
   * )
   *
   * @return FormInput
   */
  public static function input(HTMLTag $formInputItem, $hint = NULL, $flags = 0x000000) {
    return new FormInput($formInputItem, $hint, $flags);
  }

  public static function open($uniquePrefix = NULL, $action = '') {
    if (isset($uniquePrefix))
      self::$uniquePrefix = $uniquePrefix.'-';
    
    $content = new \stdClass;
    $content->form = UIHTML::tag('form',new stdClass,array('method'=>'post','action'=>$action))->setOption('closeTag',FALSE);
      
    return UIHTML::tag('div',
                       $content,
                       array('class'=>'\Psc\form'))->setOption('closeTag',FALSE);
  }
  
  public static function close() {
    self::$uniquePrefix = NULL;
    return '</form></div>';
  }
  
  public static function text($label, $name = NULL, $value = '', $id = NULL) {
    list($label,$name,$id) = self::expand($label,$name,$id);
    
    $text = fHTML::text($name, $value, array('class'=>array('text','ui-widget-content','ui-corner-all'),'id'=>$id));
    self::attachLabel($text,$label);
    return $text;
  }
  
  public static function smallint($label, $name = NULL, $value = '', $size = 2, $id = NULL) {
    $text = self::text($label,$name,$value,$id);
    $text->setStyle('width',($size+1).'em');
    $text->setAttribute('maxlength',$size);
    return $text;
  }
  
  public static function textarea($label, $name = NULL, $value = '', $id = NULL) {
    list($label,$name,$id) = self::expand($label,$name,$id);
    $ta = fHTML::textarea($name, $value, array('class'=>array('textarea','ui-widget-content','ui-corner-all'),'id'=>$id));
    $ta->setStyle('height','200px');
    self::attachLabel($ta,$label);
    return $ta;
  }
  
  public static function checkbox($label, $name = NULL, $checked = FALSE, $id = NULL) {
    list($label,$name,$id) = self::expand($label,$name,$id);
    
    $c = fHTML::checkbox($name, $checked, 'true', array('id'=>$id)); // hier ist true fixed, weil wir es eh immer so brauchen
    self::attachLabel($c,$label, self::LABEL_CHECKBOX);
    return $c;
  }
  
  // benutzt von dropBox2
  public static function attachLabel($element, $label, $type = self::LABEL_TOP) {
    Code::value($type, self::LABEL_TOP, self::LABEL_CHECKBOX);

    if ($label != NULL) {
      $element->templateContent->label = fHTML::label($label, $element)
        ->addClass('\Psc\label')
      ;
    
      if ($type == self::LABEL_TOP) {
        $element->templatePrepend('%label%');  // nicht überschreiben sondern prependen
      }
    
      if ($type == self::LABEL_CHECKBOX) {
        $element->template = '%self% %label%';
        $element->templateContent->label->addClass('checkbox-label');
      }
    }
    
    return $element;
  }
  
  public static function radios($label, $name, Array $values, $selected = NULL, $id = NULL) {
    list ($label,$name,$null) = self::expand($label,$name,$id);
    $id = uniqid('radios');
    $radios = fHTML::radios($name, $values, $selected, array('id'=>$id))
                      ->setAttribute('id','radios-wrapper-for-'.$id)
                      ->addClass('radios-wrapper');
    return jQuery::widget($radios,'buttonset');
  }
  
  
  public static function select($label, $name, $values, $selected = NULL, $id = NULL) {
    list ($label,$name,$id) = self::expand($label,$name,$id);
    
    // deprecated
    if ($selected instanceof \Psc\CMS\TabsContentItem) {
      list($type,$selected) = $selected->getTabsId();
    }
    
    $select = fHTML::select($name, $values, $selected, array('class'=>array('text','ui-widget-content','ui-corner-all'),'id'=>$id));
    self::attachLabel($select,$label);
    
    return $select;
  }
  
  public static function comboBox($label, $name, $selected = NULL, $itemType = NULL, Array $commonItemData = array()) {
    list ($label,$name,$id) = self::expand($label,$name,NULL);
    
    $comboBox = new FormComboBox($label, // label
                                 $name, // name
                                 NULL, // loadedItems, leer wegen Ajax
                                 $selected // kein aktuell ausgewähltes Item (könnte vll das erste aus der Liste sein oder so
                                );
  
    $comboBox->getWidgetOptions()->delay = 500;
    $comboBox->getWidgetOptions()->selectMode = true;

    if (isset($itemType)) {
      $comboBox->getWidgetOptions()->itemType = $itemType;
    }
    
    if (count($commonItemData) > 0) {
      $comboBox->getWidgetOptions()->itemData = $commonItemData;
    }
    
    return $comboBox;
  }


  public static function comboDropBox($label,$name, $items, $assignedItems, $itemType = NULL, Array $commonItemData = array(), $getObject = FALSE) {
    if (\Psc\PSC::getProject()->isDevelopment()) {
      throw new \Psc\DeprecatedException('dont use that anymore');
    }
    list ($label,$name,$id) = self::expand($label,$name,NULL);
    
    $comboDropBox = new FormComboDropBox($label, $name, $items, $assignedItems);
    
    if (isset($itemType)) {
      $comboDropBox->getItemInfo()->type = $itemType;
    }
    
    if (count($commonItemData) > 0) {
      $comboDropBox->getItemInfo()->data = $commonItemData;
    }
    
    if (!$getObject) {
      $comboDropBox->init();
    
      return $comboDropBox->html();
    }
    
    return $comboDropBox;
  }

  /**
   * @return \Psc\UI\FormDropBox
   */
  public static function dropBox($label, $name, $buttons, $id= NULL) {
    list ($label,$name,$id) = self::expand($label,$name,$id);
    $dropBox = new \Psc\UI\FormDropBox($label, $name, $buttons, NULL);
    return $dropBox;
  }
  
  
  public static function autoComplete($label, $name, AutoComplete $ac, $value = '', $id = NULL, $options = array()) {
    list ($label,$name,$id) = self::expand($label,$name,$id);
    
    $text = self::text($label, $name, $value, $id)
        ->addClass('autocomplete')
    ;
    
    $widget = $ac->attach($text);
    
    return $widget;
  }

  public static function upload($label, $name = NULL, $value = '', $id = NULL) {
    list($label,$name,$id) = self::expand($label,$name,$id);
    
    $file = fHTML::upload($name, array('class'=>array('upload'),'id'=>$id)); // kein text als class wegen width
    self::attachLabel($file,$label);
    return $file;
  }

  
  /**
   * Erzeugt aus einem Label einen Namen des Elements
   */
  public static function convertToName($label) {
    return preg_replace('/\s+/','-',mb_strtolower($label));
  }
  
  public static function convertToId($name) {
    if (is_array($name)) return A::implode($name, '-', function ($part) { return is_array($part) ? 'Array' : $part; });
    
    return $name;
  }
  
  public static function expand($label, $name = NULL, $id = NULL) {
    if ($name == NULL)
      $name = self::convertToName($label);
      
    if ($id == NULL) {
      //$id = self::convertToId($name);
      // wenn wir das müssen wird eine id automatisch erzeugt, dies hier ist zu uneindeutig
    }
    
    if (isset(self::$uniquePrefix) && !S::startsWith($id,self::$uniquePrefix)) {
      $id = self::$uniquePrefix.$id;
    }
    
    return array($label,$name,$id);
  }
  

  public static function hidden($name, $value = '', $id = NULL) {
    list($label,$name,$id) = self::expand(NULL,$name,$id);
    
    return fHTML::hidden($name,$value)->setAttribute('id',$id);
  }
  
  

}

?>