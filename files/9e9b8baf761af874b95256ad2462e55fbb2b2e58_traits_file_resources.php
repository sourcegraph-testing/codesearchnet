<?php
/**
 * @author Chris Zuber <shgysk8zer0@gmail.com>
 * @package shgysk8zer0\Core_API
 * @version 1.0.0
 * @copyright 2015, Chris Zuber
 * @license http://opensource.org/licenses/GPL-3.0 GNU General Public License, version 3 (GPL-3.0)
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
namespace shgysk8zer0\Core_API\Traits;

/**
 * Converts procedural filesystem functions into Object-oriented methods
 * @see https://php.net/manual/en/ref.filesystem.php
 */
trait File_Resources
{
	/**
	 * File Handle
	 * @var resource
	 */
	protected $fhandle = null;

	/**
	 * $fhandle's lock status
	 * @var bool
	 */
	protected $flocked = false;

	/**
	 * Opens file or URL
	 *
	 * @param  string $filename           Filename or URL
	 * @param  bool   $use_include_path   Use include path?
	 * @param  string $mode               Type of access required to the stream
	 * @param  resource $context          https://php.net/manual/en/book.stream.php
	 * @return self
	 * @see https://php.net/manual/en/function.fopen.php
	 */
	final protected function fopen(
		$filename = 'php://temp',
		$use_include_path = false,
		$mode = 'a+',
		$context = null
	)
	{
		$this->fhandle = is_resource($context)
			? fopen($filename, $mode, $use_include_path, $context)
			: fopen($filename, $mode, $use_include_path);

		return $this;
	}

	/**
	 * Open Internet or Unix domain socket connection
	 *
	 * @param  string  $hostname [description]
	 * @param  int     $port     Port number
	 * @param  int     $errno    Holds system level error number
	 * @param  string  $errstr   The error message as a string
	 * @param  float   $timeout  The connection timeout, in seconds
	 * @return self
	 * @see https://php.net/manual/en/function.fpassthru.php
	 * @todo Test this
	 */
	final protected function fsockopen(
		$hostname,
		$port = -1,
		&$errno = 0,
		&$errstr = '',
		$timeout = null
	)
	{
		$this->fhandle = fsockopen($hostname, $port, $errno, $errstr, $timeout);
		return $this;
	}

	/**
	 * Portable advisory file locking
	 *
	 * @param  int    $operation  Type of lock (LOCK_SH, LOCK_EX, or LOCK_UN)
	 * @param  int    $wouldblock Set to 1 if the lock would block
	 * @return self
	 * @see https://php.net/manual/en/function.flock.php
	 */
	final protected function flock($operation = LOCK_EX, &$wouldblock = 0)
	{
		$this->flocked = flock($this->fhandle, $operation, $wouldblock);
		return $this;
	}

	/**
	 * Closes an open file pointer. Also removes any flock
	 *
	 * @param void
	 * @return self
	 */
	final protected function fclose()
	{
		if ($this->flocked) {
			$this->flock(LOCK_UN);
		}
		fclose($this->fhandle);
	}

	/**
	 * Binary-safe file write
	 *
	 * @param  string $string The string that is to be written.
	 * @param  int    $length Stop after $length bytes have been written
	 * @return self
	 * @see https://php.net/manual/en/function.fwrite.php
	 */
	final protected function fwrite($string, $length = null)
	{
		return is_int($length)
			? fwrite($this->fhandle, $string, $length)
			: fwrite($this->fhandle, $string);
	}

	/**
	 * Alias of fwrite
	 */
	final protected function fputs($string, $length = null)
	{
		return $this->fwrite($string, $length);
	}

	/**
	 * Truncates a file to a given length
	 *
	 * @param  int     $size The size to truncate to.
	 * @return bool
	 * @see https://php.net/manual/en/function.ftruncate.php
	 */
	final protected function ftruncate($size = 0)
	{
		return ftruncate($this->fhandle, $size);
	}

	/**
	 * Binary-safe file read
	 *
	 * @param  int    $length Up to $length number of bytes read.
	 * @return string         The read string or FALSE on failure.
	 * @see https://php.net/manual/en/function.fread.php
	 */
	final protected function fread($length = null)
	{
		if (! is_int($length)) {
			$length = $this->fstat('size');
		}
		return fread($this->fhandle, $length);
	}

	/**
	 * Gets line from file pointer
	 *
	 * @param  int    $length Reading ends when $length - 1 bytes have been read
	 * @return string         String of $length - 1 bytes
	 * @see https://php.net/manual/en/function.fgets.php
	 */
	final protected function fgets($length = null)
	{
		if (! is_int($length)) {
			$length = $this->fstat('size');
		}
		return fgets($this->fhandle, $length);
	}

	/**
	 * Gets line from file pointer and strip HTML tags
	 * Unlike the function, this uses an array for $allowable_tags
	 *
	 * @param  int    $length         Length of the data to be retrieved
	 * @param  array  $allowable_tags Array of tags to allow
	 * @return string                 A string of up to length - 1 bytes read
	 * @see https://php.net/manual/en/function.fgetss.php
	 */
	final protected function fgetss($length, array $allowable_tags = array())
	{
		array_walk(
			$allowable_tags,
			function($tag)
			{
				$tag = '<' . trim('<>', trim($tag)). '>';
			}
		);
		if (! is_int($length)) {
			$length = $this->fstat('size');
		}
		return fgetss($this->fhandle, $length, join(null, $allowable_tags));
	}

	/**
	 *  Gets character from file pointer
	 *
	 * @param void
	 * @return string A string containing a single character read from the file
	 * @see https://php.net/manual/en/function.fgetc.php
	 */
	final protected function fgetc()
	{
		return fgetc($this->fhandle);
	}

	/**
	 * Gets line from file pointer and parse for CSV fields
	 *
	 * @param  int     $length    Maximum line length (0 is unlimited)
	 * @param  string  $delimiter Sets the field delimiter (1 character only)
	 * @param  string  $enclosure Sets the field enclosure (1 character only)
	 * @param  string  $escape    Sets the escape character (1 character only)
	 * @return array              An indexed array containing the fields read.
	 * @see https://php.net/manual/en/function.fgetcsv.php
	 */
	final protected function fgetcsv(
		$length = 0,
		$delimiter = ',',
		$enclosure = '"',
		$escape = '\\'
	)
	{
		return fgetcsv($this->fhandle, $length, $delimiter, $enclosure, $escape);
	}

	/**
	 * Format line as CSV and write to file pointer
	 *
	 * @param  array  $fields      An array of values.
	 * @param  string $delimiter   Sets the field delimiter (1 character only)
	 * @param  string $enclosure   Sets the field enclosure (1 character only)
	 * @param  string $escape_char Sets the escape character (1 character only)
	 * @return int                 Length of the written string or FALSE on failure.
	 * @see https://php.net/manual/en/function.fputcsv.php
	 */
	final protected function fputcsv(
		array $fields,
		$delimiter = ',',
		$enclosure = '"',
		$escape_char = '\\'
	)
	{
		return fputcsv(
			$this->fhandle,
			$fields,
			$delimiter,
			$enclosure,
			$escape_char
		);
	}

	/**
	 * Output all remaining data on a file pointer
	 *
	 * @param void
	 * @return int Number of characters read from handle
	 * @see https://php.net/manual/en/function.fpassthru.php
	 */
	final protected function fpassthru()
	{
		return fpassthru($this->fhandle);
	}

	/**
	 * Returns the current position of the file read/write pointer
	 *
	 * @param void
	 * @return int File pointer's offset into the file stream.
	 * @see https://php.net/manual/en/function.ftell.php
	 */
	final protected function ftell()
	{
		return ftell($this->fhandle);
	}

	/**
	 * Returns the number of bytes from pointer position to end of file
	 *
	 * @param void
	 * @return int Bytes remaining to eof
	 */
	final protected function freamining()
	{
		return $this->fstat('size') - $this->ftell();
	}

	/**
	 * Seeks on a file pointer
	 *
	 * @param  int     $offset The offset.
	 * @param  int     $whence SEEK_SET, SEEK_CUR, or SEEK_END
	 * @return bool            true on success, false on failure
	 */
	final protected function fseek($offset = 1, $whence = SEEK_SET)
	{
		return (fseek($this->fhandle, $offset, $whence) === 0);
	}

	final protected function fstat($key = null)
	{
		if (is_string($key)) {
			return fstat($this->fhandle)[$key];
		} else {
			return fstat($this->fhandle);
		}
	}

	/**
	 * Parses input from a file according to a format
	 *
	 * @param  string $format     [description]
	 * @param  array  $params     [description]
	 * @return mixed
	 * @see https://php.net/manual/en/function.fscanf.php
	 * @todo Test and make work with additional params beyond $format
	 */
	final protected function fscanf($format, array &$params = array())
	{
		if (empty($params)) {
			return fscanf($this->fhandle, $format);
		}
		array_shift($params, $this->fhandle, $format);
		$results = call_user_func_array('fscanf', $params);
		array_splice($params, 2);
		return $results;
	}

	/**
	 * Rewind the position of a file pointer
	 *
	 * @param void
	 * @return self
	 * @see https://php.net/manual/en/function.rewind.php
	 */
	final protected function rewind()
	{
		rewind($this->fhandle);
		return $this;
	}

	/**
	 * Tests for end-of-file on a file pointer
	 *
	 * @param void
	 * @return bool    If the file pointer is at EOF or an error occurs
	 */
	final protected function feof()
	{
		return feof($this->fhandle);
	}

	/**
	 * Flushes the output to a file
	 *
	 * @param void
	 * @return bool  TRUE on success or FALSE on failure.
	 * @see https://php.net/manual/en/function.fflush.php
	 */
	final protected function fflush()
	{
		return fflush($this->fhandle);
	}

	/**
	 *  Write a string to a file
	 *
	 * @param mixed   $data  The data to write. String or single dimension array
	 * @param int     $flags FILE_APPEND... no others have any effect
	 */
	final public function filePutContents($data, $flags = FILE_APPEND)
	{
		if ($flags & FILE_APPEND) {
			$this->fseek(-1, SEEK_END);
		} else {
			$this->ftruncate();
		}

		if ($flags & LOCK_EX) {
			$this->flock(LOCK_EX);
		}
		if (is_array($data)) {
			$data = join(null, $data);
		}
		$this->fwrite($data);
	}

	/**
	 * Reads entire file into a string
	 *
	 * @param int     $offset The offset where the reading starts on the original stream.
	 * @param int     $maxlen Maximum length of data read
	 */
	final public function fileGetContents($offset = -1, $maxlen = null)
	{
		$this->fseek($offset + 1, SEEK_SET);
		return $this->fread($maxlen);
	}

	/**
	 * Read an entire file parsed as CSV
	 *
	 * @param string $delimiter Sets the field delimiter (1 character only)
	 * @param string $enclosure Sets the field enclosure (1 character only)
	 * @param string $escape    Sets the escape character (1 character only)
	 * @return array            Multi-dimensional indexed array (rows & columns)
	 */
	final public function fileGetCSV(
		$delimiter = ',',
		$enclosure = '"',
		$escape = '\\'
	)
	{
		$rows = [];
		$this->rewind();

		while (! $this->feof()) {
			$row = $this->fgetcsv(0, $delimiter, $enclosure, $escape);
			if ($row !== false) {
				array_push($rows, $row);
			}
			unset($row);
		}
		return $rows;
	}

	/**
	 * Format line as CSV and write to file
	 *
	 * @param array  $fields      An array of values.
	 * @param string $delimiter   Sets the field delimiter (1 character only)
	 * @param string $enclosure   Sets the field enclosure (1 character only)
	 * @param string $escape_char Sets the escape character (1 character only)
	 */
	final public function filePutCSV(
		array $fields,
		$delimiter = ',',
		$enclosure = '"',
		$escape_char = '\\'
	)
	{
		$this->fputcsv($fields, $delimiter, $enclosure, $escape_char);
	}

	/**
	 * Outputs a file
	 *
	 * @param void
	 * @return int The number of bytes read from the file
	 */
	final public function readfile()
	{
		$this->rewind();
		return $this->fpassthru();
	}

	/**
	 * Reads entire file into an array
	 *
	 * @param  int     $flags [FILE_IGNORE_NEW_LINES, FILE_SKIP_EMPTY_LINES]
	 * @return array          Each line of file as an array
	 * @see https://php.net/manual/en/function.file.php
	 * @todo Make handling of FILE_IGNORE_NEW_LINES match original function
	 */
	final public function file($flags = 0)
	{
		$contents = [];
		$ignore_nl = $flags & FILE_IGNORE_NEW_LINES;
		$skip_empty = $flags & FILE_SKIP_EMPTY_LINES;
		$this->rewind();

		while (! $this->feof()) {
			$line = $this->fgets();
			if ($ignore_nl) {
				$line = rtrim($line, PHP_EOL);
			}
			if (! $skip_empty or strlen($line) !== 0) {
				array_push($contents, $line);
			}
		}
		return $contents;
	}

	/**
	 * Gets file size
	 *
	 * @param void
	 * @return int   The size of the file in bytes
	 * @see https://php.net/manual/en/function.filesize.php
	 */
	final public function filesize()
	{
		return $this->fstat('size');
	}

	/**
	 * Gets file inode
	 *
	 * @param void
	 * @return int inode number of the file
	 * @see https://php.net/manual/en/function.fileinode.php
	 */
	final public function fileinode()
	{
		return $this->fstat('ino');
	}

	/**
	 *  Gets last access time of file
	 *
	 * @param void
	 * @return int   The time the file was last accessed
	 */
	final public function fileatime()
	{
		return $this->fstat('atime');
	}

	/**
	 * Gets inode change time of file
	 *
	 * @param void
	 * @return int The time the file was last changed
	 */
	final public function filectime()
	{
		return $this->fstat('ctime');
	}

	/**
	 * Gets file modification time
	 *
	 * @param void
	 * @return int The time the file was last modified
	 */
	final public function filemtime()
	{
		return $this->fstat('mtime');
	}

	/**
	 * Gets file owner
	 *
	 * @param void
	 * @return int The user ID of the owner of the file
	 */
	final public function fileowner()
	{
		return $this->fstat('uid');
	}

	/**
	 * Gets file group
	 *
	 * @param void
	 * @return int The group ID of the file
	 */
	final public function filegroup()
	{
		return $this->fstat('gid');
	}

	/**
	 * Gets file permissions
	 *
	 * @param void
	 * @return int The file's permissions as a numeric mode
	 */
	final public function fileperms()
	{
		return $this->fstat('mode');
	}
}
