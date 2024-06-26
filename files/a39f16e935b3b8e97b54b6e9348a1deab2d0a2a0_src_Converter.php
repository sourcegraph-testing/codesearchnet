<?php

namespace Pilipinews\Common;

use League\HTMLToMarkdown\Environment;
use League\HTMLToMarkdown\Converter\CodeConverter;
use League\HTMLToMarkdown\Converter\CommentConverter;
use League\HTMLToMarkdown\Converter\ConverterInterface;
use League\HTMLToMarkdown\Converter\DefaultConverter;
use League\HTMLToMarkdown\Converter\DivConverter;
use League\HTMLToMarkdown\Converter\HardBreakConverter;
use League\HTMLToMarkdown\Converter\HorizontalRuleConverter;
use League\HTMLToMarkdown\Converter\PreformattedConverter;
use League\HTMLToMarkdown\Converter\TextConverter;
use League\HTMLToMarkdown\HtmlConverter;
use Pilipinews\Common\Converters\BlockquoteConverter;
use Pilipinews\Common\Converters\EmphasisConverter;
use Pilipinews\Common\Converters\HeaderConverter;
use Pilipinews\Common\Converters\ImageConverter;
use Pilipinews\Common\Converters\LinkConverter;
use Pilipinews\Common\Converters\ListBlockConverter;
use Pilipinews\Common\Converters\ListItemConverter;
use Pilipinews\Common\Converters\ParagraphConverter;
use Pilipinews\Common\Converters\TableBlockConverter;
use Pilipinews\Common\Converters\TableDataConverter;
use Pilipinews\Common\Converters\TableRowConverter;

/**
 * Converter
 *
 * @package Pilipinews
 * @author  Rougin Gutib <rougingutib@gmail.com>
 */
class Converter extends HtmlConverter
{
    /**
     * Initializes the converter instance
     */
    public function __construct()
    {
        $environment = new Environment;

        $environment->addConverter(new BlockquoteConverter);
        $environment->addConverter(new CodeConverter);
        $environment->addConverter(new CommentConverter);
        $environment->addConverter(new DivConverter);
        $environment->addConverter(new EmphasisConverter);
        $environment->addConverter(new HardBreakConverter);
        $environment->addConverter(new HeaderConverter);
        $environment->addConverter(new HorizontalRuleConverter);
        $environment->addConverter(new ImageConverter);
        $environment->addConverter(new LinkConverter);
        $environment->addConverter(new ListBlockConverter);
        $environment->addConverter(new ListItemConverter);
        $environment->addConverter(new ParagraphConverter);
        $environment->addConverter(new PreformattedConverter);
        $environment->addConverter(new TextConverter);
        $environment->addConverter(new TableBlockConverter);
        $environment->addConverter(new TableDataConverter);
        $environment->addConverter(new TableRowConverter);

        $environment->getConfig()->setOption('bold_style', '');
        $environment->getConfig()->setOption('header_style', 'atx');
        $environment->getConfig()->setOption('italic_style', '');
        $environment->getConfig()->setOption('strip_tags', true);
        $environment->getConfig()->setOption('suppress_errors', true);
        $environment->getConfig()->setOption('hard_break', true);

        parent::__construct($environment);
    }

    /**
     * Parses the HTML into Markdown format.
     *
     * @param  string $html
     * @return string
     */
    public function convert($html)
    {
        $html = parent::convert((string) $html);

        $html = $this->encode('--', '-', $html, false);
        $html = $this->encode('\#', '#', $html);
        $html = $this->encode('\*', '*', $html);
        $html = $this->encode('\_', '_', $html);
        $html = $this->encode('•', '*', $html);
        $html = $this->encode('…', '...', $html);

        $html = $this->encode('\[', '[', (string) $html);
        $html = $this->encode('\]', ']', (string) $html);
        $html = str_replace(array('—', '–', '\-'), '-', $html);
        $html = str_replace(array('“', '”'), '"', $html);
        $html = str_replace(array('’', '‘'), "'", $html);
        $html = str_replace("\n\n\n", "\n\n", $html);
        $html = str_replace("\n ", "\n", (string) $html);

        return str_replace('\.', '.', (string) $html);
    }

    /**
     * Converts the specified string.
     *
     * @param  string  $from
     * @param  string  $text
     * @param  string  $html
     * @param  boolean $whole
     * @return string
     */
    protected function encode($from, $text, $html, $whole = true)
    {
        $html = str_replace($from . ' ', $text . ' ', $html);

        $html = str_replace(' ' . $from, ' ' . $text, $html);

        $html = str_replace('>' . $from, '>' . $text, $html);

        $html = str_replace($from . '<', $text . '<', $html);

        return $whole ? str_replace($from, $text, $html) : $html;
    }
}
