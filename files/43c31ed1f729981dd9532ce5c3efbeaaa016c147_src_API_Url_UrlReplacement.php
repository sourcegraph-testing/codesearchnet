<?php

namespace GroupByInc\API\Url;

use GroupByInc\API\Util\StringBuilder;
use GroupByInc\API\Util\StringUtils;

class UrlReplacement
{
  /** @var int */
  private $index;
  /** @var string */
  private $replacement;
  /** @var int */
  private $op;

  /**
   * @param int    $index
   * @param string $replacement
   * @param int    $op
   */
  function __construct($index, $replacement, $op)
  {
    $this->index = $index;
    if (empty($replacement)) {
      $this->replacement = "";
    } else {
      $this->replacement = $replacement;
    }
    $this->op = $op;
  }

  /**
   * @param string $query
   * @return UrlReplacement[]
   * @throws ParserException
   */
  public static function parseQueryString($query)
  {
    $delimiterIndex = 0;
    /** @var UrlReplacement[] $replacements */
    $replacements = array();

    if (empty($query)) {
      return $replacements;
    }

    $queryBuilder = new StringBuilder($query);
    while ($delimiterIndex >= 0) {
      $pairSeparator = $queryBuilder->indexOf(Beauty::REPLACEMENT_DELIMITER);
      if ($pairSeparator < 0) {
        throw new ParserException("Replacement Query Delimiters did not match up");
      }
      $delimiterIndex = $queryBuilder->indexOf(Beauty::REPLACEMENT_DELIMITER, $pairSeparator + 2);
      if ($delimiterIndex < 0) {
        break;
      }
      array_push($replacements, self::fromString($queryBuilder->substring(0, $delimiterIndex)));
      $queryBuilder->delete(0, $delimiterIndex);

      if ($queryBuilder->getCharAt(0) == Beauty::REPLACEMENT_DELIMITER) {
        $queryBuilder->deleteCharAt(0);
      }
    }
    $finalQuery = $queryBuilder->__toString();
    if (!empty($finalQuery)) {
      array_push($replacements, self::fromString($finalQuery));
    }
    return array_reverse($replacements);
  }

  /**
   * @param StringBuilder $builder
   * @param int           $offset
   */
  public function apply(StringBuilder $builder, $offset)
  {
    $relativeIndex = $this->index - $offset;
    if ($relativeIndex < 0 ||
        ($relativeIndex >= $builder->length() && $this->op == OperationType::Swap) ||
        ($relativeIndex > $builder->length() && $this->op == OperationType::Insert)
    ) {
      return;
    }
    if ($this->op == OperationType::Insert) {
      $builder->insert($relativeIndex, $this->replacement);
    } else if ($this->op == OperationType::Swap) {
      $builder->replace($relativeIndex, strlen($this->replacement), $this->replacement);
    }
  }

  /**
   * @return string
   */
  public function __toString()
  {
    $builder = new StringBuilder();
    if ($this->op == OperationType::Insert) {
      $builder->append(Beauty::INSERT_INDICATOR);
    }
    $builder->append($this->index);
    $builder->append(Beauty::REPLACEMENT_DELIMITER);
    $builder->append($this->replacement);

    return (string)$builder;
  }

  /**
   * @param string $string
   * @throws ParserException
   * @return UrlReplacement
   */
  public static function fromString($string)
  {
    $builder = new StringBuilder($string);
    $op = OperationType::Swap;
    $delimiterIndex = $builder->indexOf(Beauty::REPLACEMENT_DELIMITER);
    if ($delimiterIndex < 0) {
      throw new ParserException("Argument did not match expected format: " . $string);
    }
    $replacementValue = $builder->substring($delimiterIndex + 1);
    $indexString = $builder->substring(0, $delimiterIndex);
    if (StringUtils::startsWith($indexString, Beauty::INSERT_INDICATOR)) {
      $op = OperationType::Insert;
      $indexString = substr($indexString, 1);
    }

    if (is_numeric($indexString)) {
      return new UrlReplacement($indexString, $replacementValue, $op);
    } else {
      throw new ParserException("Invalid index: " . $indexString);
    }
  }

  /**
   * @param UrlReplacement $__value__
   * @return bool
   */
  public function __is_equal($__value__)
  {
    if (!$__value__ instanceof UrlReplacement) {
      return false;
    }
    if ($this->index != $__value__->index || $this->op != $__value__->op) {
      return false;
    }
    return $this->replacement == $__value__->replacement;
  }

  /**
   * @param UrlReplacement[] $replacements
   * @return string
   */
  public static function buildQueryString(array $replacements)
  {
    $builder = new StringBuilder();
    foreach ($replacements as $replacement) {
      if (strlen($builder) != 0) {
        $builder->append(Beauty::REPLACEMENT_DELIMITER);
      }
      $builder->append($replacement);
    }
    return $builder;
  }
}