<?php
/**
 * @link https://github.com/ixocreate
 * @copyright IXOCREATE GmbH
 * @license MIT License
 */

declare(strict_types=1);

namespace Ixocreate\Database\Generator;

use Doctrine\ORM\Mapping\ClassMetadataInfo;

/**
 * Class AbstractGenerator
 * @package Ixocreate\Database\Generator
 */
abstract class AbstractGenerator implements GeneratorInterface
{
    /**
     * @var string
     */
    protected $namespace;

    /**
     * @var string
     */
    protected $tablePrefix;

    /**
     * @var string
     */
    protected $fileHeader = "";

    /**
     * @var ClassMetadataInfo[]
     */
    protected $fullMetadata = [];

    abstract protected function generateCode(string $name, ClassMetadataInfo $metadata);

    /**
     * @param ClassMetadataInfo $metadata
     * @param string $destinationPath
     * @param bool $overwrite
     * @return string|null
     */
    public function generate(ClassMetadataInfo $metadata, $destinationPath, $overwrite = false) : ?string
    {
        $name = $this->getGeneratedClassName($metadata);

        $content = $this->generateCode($name, $metadata);

        $path = $destinationPath . DIRECTORY_SEPARATOR . \str_replace('\\', '/', $this->namespace)
            . \str_replace('\\', '/', $this->getNamespacePostfix()) . $name . $this->getFilenamePostfix() . '.php';

        if ($this->writeFile($content, $path, $overwrite)) {
            return $path;
        }

        return null;
    }

    protected function getGeneratedClassName(ClassMetadataInfo $metadata): string
    {
        $name = $metadata->name;
        if (\mb_strpos($metadata->name, $this->tablePrefix) === 0) {
            $name = \mb_substr($metadata->name, \mb_strlen($this->tablePrefix));
        }
        return $name;
    }

    /**
     * @param string $content
     * @param string $path
     * @param bool $overwrite
     * @return bool
     */
    public function writeFile(string $content, string $path, $overwrite = false)
    {
        $dir = \dirname($path);

        if (! \is_dir($dir)) {
            \mkdir($dir, 0775, true);
        }

        if (!\file_exists($path) || (\file_exists($path) && $overwrite)) {
            \file_put_contents($path, $content);
            \chmod($path, 0664);

            return true;
        }

        return false;
    }

    /**
     * Generates the namespace, if class do not have namespace, return empty string instead.
     *
     * @param string $fullClassName
     *
     * @return string $namespace
     */
    protected function getClassNamespace($fullClassName)
    {
        if (\mb_strpos($fullClassName, '\\') === false) {
            return '';
        }
        $namespace = \mb_substr($fullClassName, 0, \mb_strrpos($fullClassName, '\\'));
        return $namespace;
    }

    /**
     * @param $fullClassName
     * @return mixed
     */
    protected function getRepositoryClassNamespace($fullClassName)
    {
        return $this->replaceClassNamespaceType($fullClassName, 'Repository');
    }

    /**
     * @param $fullClassName
     * @return mixed
     */
    protected function getResourceClassNamespace($fullClassName)
    {
        return $this->replaceClassNamespaceType($fullClassName, 'Resource');
    }

    /**
     * @param $fullClassName
     * @return mixed
     */
    protected function getEntityClassNamespace($fullClassName)
    {
        return $this->replaceClassNamespaceType($fullClassName, 'Entity');
    }

    protected function replaceClassNamespaceType($fullClassName, $type)
    {
        $namespace = $this->getClassNamespace($fullClassName);
        return \preg_replace('/(Repository|Resource|Entity|Metadata)$/', $type, $namespace);
    }

    /**
     * @return string
     */
    protected function generateNamespace()
    {
        return 'namespace ' . $this->namespace . \trim($this->getNamespacePostfix(), '\\') . ';';
    }

    /**
     * Generates the class name
     *
     * @param string $fullClassName
     *
     * @return string
     */
    protected function getClassName($fullClassName)
    {
        if (\mb_strrpos($fullClassName, '\\') === false) {
            return $fullClassName;
        }
        return \mb_substr($fullClassName, \mb_strrpos($fullClassName, '\\') + 1, \mb_strlen($fullClassName));
    }

    /**
     * @param $fullClassName
     * @return mixed|string
     */
    protected function getClassNameKiwi($fullClassName)
    {
        $classNamespace = $this->getClassNamespace($fullClassName);
        $className = $this->getClassName($fullClassName);

        $namespaceParts = \array_filter(\explode('\\', $classNamespace));

        foreach ($namespaceParts as $namespacePart) {
            if (\mb_strpos($className, $namespacePart) === 0) {
                $className = \preg_replace('/^' . \preg_quote($namespacePart, '/') . '/', '', $className);
            }
        }

        return $className;
    }

    /**
    * Generates the namespace statement, if class do not have namespace, return empty string instead.
    *
    * @param string $fullClassName the full repository class name
    *
    * @return string $namespace
    */
    protected function getEntityClassName($fullClassName)
    {
        return $this->getClassNameKiwi($fullClassName);
    }

    /**
     * @param string $fullClassName
     * @return string
     */
    protected function getEntityFQCN($fullClassName)
    {
        return $this->namespace . "Entity\\" . $this->getEntityClassName($fullClassName);
    }

    /**
     * Generates the namespace statement, if class do not have namespace, return empty string instead.
     *
     * @param string $fullClassName the full repository class name
     *
     * @return string $namespace
     */
    protected function getRepositoryClassName($fullClassName)
    {
        return $this->getClassNameKiwi($fullClassName) . 'Repository';
    }

    /**
     * @param string $fullClassName
     * @return string
     */
    protected function getRepositoryFQCN($fullClassName)
    {
        return $this->getRepositoryClassNamespace($fullClassName) . "\\" . $this->getRepositoryClassName($fullClassName);
    }

    /**
     * Generates the namespace statement, if class do not have namespace, return empty string instead.
     *
     * @param string $fullClassName the full repository class name
     *
     * @return string $namespace
     */
    protected function getResourceClassName($fullClassName)
    {
        return $this->getClassNameKiwi($fullClassName) . 'Resource';
    }

    /**
     * @param string $fullClassName
     * @return string
     */
    protected function getResourceFQCN($fullClassName)
    {
        return $this->getResourceClassNamespace($fullClassName) . "\\" . $this->getResourceClassName($fullClassName);
    }

    /**
     * @param $content
     * @param $start
     * @param $ending
     * @return bool|null|string
     */
    protected function getStringBetween($content, $start, $ending)
    {
        $startPosition = \mb_strpos($content, $start);
        if ($startPosition === false) {
            return null;
        }

        $endPosition = \mb_strpos($content, $ending, $startPosition + \mb_strlen($start));
        if ($endPosition === false) {
            return null;
        }

        return \mb_substr($content, $startPosition, $endPosition);
    }

    /**
     * @param $content
     * @param $insert
     * @param $start
     * @param $ending
     * @return null|string
     */
    protected function insertStringBetween($content, $insert, $start, $ending)
    {
        $startPosition = \mb_strpos($content, $start);
        if ($startPosition === false) {
            return null;
        }

        $endPosition = \mb_strpos($content, $ending, $startPosition + \mb_strlen($start));
        if ($endPosition === false) {
            return null;
        }

        $cut = $startPosition + \mb_strlen($start);
        $prefix = \mb_substr($content, 0, $cut);
        $postfix = \mb_substr($content, $cut);

        return $prefix . $insert . $postfix;
    }

    protected function getFullyQualifiedTypeByMappingType($type)
    {
        return $type = \Doctrine\DBAL\Types\Type::getType($type);
    }

    /**
     * @param ClassMetadataInfo[] $fullMetadata
     * @return $this
     */
    public function setFullMetadata(array $fullMetadata)
    {
        $metadata = [];
        foreach ($fullMetadata as $tmpMetadata) {
            $metadata[$tmpMetadata->name] = $tmpMetadata;
        }

        $this->fullMetadata = $metadata;
        return $this;
    }

    /**
     * @param string $fileHeader
     * @return $this
     */
    public function setFileHeader($fileHeader)
    {
        $this->fileHeader = $fileHeader;
        return $this;
    }

    /**
     * @return string
     */
    public function getFileHeader(): string
    {
        return $this->fileHeader;
    }

    /**
     * @param string $namespace
     * @return $this
     */
    public function setNamespace(string $namespace)
    {
        $this->namespace = $namespace;
        return $this;
    }

    /**
     * @param string $tablePrefix
     * @return $this
     */
    public function setTablePrefix(string $tablePrefix)
    {
        $this->tablePrefix = $tablePrefix;
        return $this;
    }

    /**
     * @return string
     */
    public function getFilenamePostfix(): string
    {
        return '';
    }
}
