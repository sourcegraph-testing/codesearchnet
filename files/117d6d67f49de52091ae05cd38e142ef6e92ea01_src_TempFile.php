<?php

declare(strict_types=1);

namespace NAttreid\Utils;

use Nette\SmartObject;
use Nette\Utils\Random;

/**
 * Docasny soubor
 *
 * @property string $delimiter
 * @property string $enclosure
 * @property string $escapeChar
 *
 * @author Attreid <attreid@gmail.com>
 */
class TempFile
{
	use SmartObject;

	/** @var string */
	private $file;

	/** @var string */
	private $name;

	/** @var resource */
	private $handler;

	/** @var string */
	private $delimiter = ';';

	/** @var string */
	private $enclosure = '"';

	/** @var string */
	private $escapeChar = "\\";

	protected function getDelimiter(): string
	{
		return $this->delimiter;
	}

	protected function setDelimiter(string $delimiter): void
	{
		$this->delimiter = $delimiter;
	}

	protected function getEnclosure(): string
	{
		return $this->enclosure;
	}

	protected function setEnclosure(string $enclosure): void
	{
		$this->enclosure = $enclosure;
	}

	protected function getEscapeChar(): string
	{
		return $this->escapeChar;
	}

	protected function setEscapeChar(string $escapeChar): void
	{
		$this->escapeChar = $escapeChar;
	}

	/**
	 *
	 * @param string $name pokud je null vygeneruje se random
	 * @param bool $timePrefix
	 */
	public function __construct(string $name = null, bool $timePrefix = false)
	{
		if ($name === null) {
			$name = Random::generate();
		}
		if ($timePrefix) {
			$date = new \DateTime;
			$name = $date->format('Y-m-d_H-i-s') . '_' . $name;
		}
		$this->name = $name;
		$this->file = $this->getUniqueFile($name);
		$this->handler = fopen($this->file, 'w+');
	}

	/**
	 * @param string $name
	 * @return string
	 */
	private function getUniqueFile(string $name): string
	{
		$file = sys_get_temp_dir() . '/' . $name;
		if (file_exists($file)) {
			$char = Random::generate(1);
			return $this->getUniqueFile($char . '_' . $name);
		} else {
			return $file;
		}
	}

	/**
	 * Zapise do souboru
	 * @param string $str
	 * @return self
	 */
	public function write(string $str): self
	{
		fwrite($this->handler, $str);
		return $this;
	}

	/**
	 * Zapise jako csv radek
	 * @param array $data
	 * @return self
	 */
	public function writeCsv(array $data): self
	{
		fputcsv($this->handler, $data, $this->delimiter, $this->enclosure, $this->escapeChar);
		return $this;
	}

	public function move(string $path): ?string
	{
		$file = $path . '/' . $this->name;
		if (@rename($this->file, $file)) {
			return $file;
		}
		return null;
	}

	public function copy(string $path): ?string
	{
		$file = $path . '/' . $this->name;
		if (@copy($this->file, $file)) {
			return $file;
		}
		return null;
	}

	public function __destruct()
	{
		fclose($this->handler);
		@unlink($this->file);
	}

	public function __toString()
	{
		return $this->file;
	}

}
