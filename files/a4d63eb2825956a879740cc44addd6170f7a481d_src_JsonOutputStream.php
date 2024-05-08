<?php

declare(strict_types=1);

namespace Jasny\IteratorStream;

/**
 * Write to a stream as JSON array.
 */
class JsonOutputStream extends AbstractOutputStream
{
    /**
     * Option; Don't create a json array, but output one element per line.
     */
    public const OUTPUT_LINES = 1073741824;

    /**
     * @var int
     */
    protected $options;

    /**
     * @var string|null
     */
    protected $delimiter = null;


    /**
     * Class constructor.
     *
     * @param resource|string|null $stream
     * @param int                  $options  Binary set of JSON_* options and optionally JsonOutputStream::OUTPUT_LINES
     */
    public function __construct($stream = null, int $options = 0)
    {
        parent::__construct($stream);

        $this->options = $options;
    }


    /**
     * Begin writing to the stream.
     *
     * @return void
     */
    protected function begin(): void
    {
        if (($this->options & self::OUTPUT_LINES) === 0) {
            fwrite($this->stream, "["  . "\n");
        }
    }

    /**
     * End writing to the stream.
     *
     * @return void
     */
    protected function end(): void
    {
        if (($this->options & self::OUTPUT_LINES) === 0) {
            fwrite($this->stream, (isset($this->delimiter) ? "\n" : '') . ']');
        }
    }

    /**
     * JSON encode an element, with error checking
     *
     * @param mixed $element
     * @return string
     */
    protected function jsonEncode($element): string
    {
        $json = json_encode($element, $this->options);

        if ($json === false) {
            trigger_error("JSON encode failed; " . json_last_error_msg(), E_USER_WARNING);
            return json_encode(null);
        }

        // Indent for pretty print
        if (($this->options & \JSON_PRETTY_PRINT) > 0 && (~$this->options & self::OUTPUT_LINES) > 0) {
            $json = rtrim("    " . str_replace("\n", "\n    ", $json));
        }

        return $json;
    }

    /**
     * Write an element to the stream.
     *
     * @param mixed $element
     * @return void
     */
    protected function writeElement($element): void
    {
        $json = $this->jsonEncode($element);

        fwrite($this->stream, $this->delimiter . $json);

        if (!isset($this->delimiter)) {
            $this->delimiter = (($this->options & self::OUTPUT_LINES) > 0 ? '' : ',') . "\n";
        }
    }
}
