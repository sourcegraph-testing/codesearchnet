<?php

/*
 * This file is part of the Kreta package.
 *
 * (c) Beñat Espiña <benatespina@gmail.com>
 * (c) Gorka Laucirica <gorka.lauzirika@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

namespace Kreta\SimpleApiDocBundle\Annotation;

use Nelmio\ApiDocBundle\Annotation\ApiDoc as BaseApiDoc;

/**
 * Api doc class.
 *
 * @author Beñat Espiña <benatespina@gmail.com>
 *
 * @Annotation()
 */
class ApiDoc extends BaseApiDoc
{
    /**
     * Array that contains default descriptions of each status code.
     *
     * @var array
     */
    protected $defaultStatusCodes = [
        200 => '<data>',
        201 => '<data>',
        204 => '',
        403 => 'Not allowed to access this resource',
        404 => 'Does not exist any object with id passed',
        409 => 'The resource is currently in use',
    ];

    /**
     * Array that contains the format of the Api.
     *
     * @var array
     */
    protected $format = [
        'requirement' => 'json|jsonp',
        'description' => 'Supported formats, by default json',
    ];

    /**
     * Constructor.
     *
     * @param array $data The data
     */
    public function __construct(array $data)
    {
        parent::__construct($data);
        $this->buildStatusCodes($data);
        $this->addRequirement('_format', $this->format);
    }

    /**
     * Loads data given status codes.
     *
     * @param array $data Array that contains all the data
     *
     * @return self
     */
    protected function buildStatusCodes(array $data)
    {
        if (isset($data['statusCodes'])) {
            $this->initializeStatusCodes();
            foreach ($data['statusCodes'] as $key => $element) {
                if ((int) $key < 200) {
                    $this->statusCodes($element);
                } else {
                    $this->statusCodes($key, $element);
                }
            }
        }

        return $this;
    }

    /**
     * Method that allows to choose between status
     * code passing the code and optional description.
     *
     * @param int         $statusCode        The status code
     * @param string|null $customDescription The description
     *
     * @return self
     */
    protected function statusCodes($statusCode, $customDescription = null)
    {
        if ($customDescription) {
            $description = $customDescription;
        }
        if ($customDescription !== null || array_key_exists($statusCode, $this->defaultStatusCodes)) {
            if (!isset($description)) {
                $description = $this->defaultStatusCodes[$statusCode];
            }
            $description = !is_array($description) ? [$description] : $description;
            $this->addStatusCode($statusCode, $description);
        }

        return $this;
    }

    /**
     * Purges the statusCodes array to populate with the new way.
     *
     * This method is required because the $statusCodes
     * is a private field, and the reflection is necessary.
     * 
     * @return self
     */
    protected function initializeStatusCodes()
    {
        $annotationReflection = new \ReflectionClass('Nelmio\ApiDocBundle\Annotation\ApiDoc');
        $statusCodesReflection = $annotationReflection->getProperty('statusCodes');
        $statusCodesReflection->setAccessible(true);
        $statusCodesReflection->setValue($this, []);

        return $this;
    }
}
