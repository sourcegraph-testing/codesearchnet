<?php

namespace payapi;

final class files
{

    public    $version      = '0.0.2';

	private static $default      = 0755;

	public static function xcopy($src, $dest)
	{
		if (is_dir($src) === true && self::checkDir($dest) === true) {
			$error = 0;
			$files = 0;
			$dirs = 0;
			foreach(scandir($src) as $file) {
				if (($file != '.') && ($file != '..') && $error === 0) {
					if (is_dir($src . '/' . $file)){
						if (!is_dir($dest . '/' . $file)) {
							if (mkdir($dest . '/' . $file) !== true) {
								$error++;
							} else {
								$dirs++;
							}
						}
						self::xcopy($src . '/' . $file, $dest . '/' . $file);
					} else {
						if (copy($src . '/' . $file, $dest . '/' . $file) !== true) {
							$error++;
						} else {
							$files++;
						}
					}
				}
			}
			return ($error === 0 && $files > 0);
		}
		return false;
	}

	public static function checkDir($dir, $permissions = false)
	{
		$access = ($permissions !== false) ? $permissions : self::$default;
		if (!is_dir($dir)) {
			$this_path = null;
			$paths = explode('/', $dir);
			foreach ($paths as $path) {
				if ($path != null) {
					$this_path .= DIRECTORY_SEPARATOR . $path;
					if (!is_dir($this_path)) {
						mkdir($this_path);
					}
				}
			}
		}
		return (is_dir($dir) === true);
	}

    public static function unzip($zipFile, $to)
    {
        set_time_limit(120);
        $zip = new Zip();
        if (files::checkDir($to) !== false) {
	        return ($zip->unzip_file($zipFile, $to) === true);
        }
        return false;
    }

    public static function get($file)
    {
    	if (is_file($file) === true) {
    		return require($file);    		
    	}
    	return false;
    }

    public static function root()
    {
    	return str_replace('src' . DIRECTORY_SEPARATOR . 'si' . DIRECTORY_SEPARATOR . 'core', null, __DIR__);
    }
}