<?php

/**
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Popiniuc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

namespace danielgp\common_lib;

/**
 * Usefull functions to get quick MySQL content
 *
 * @author Daniel Popiniuc
 */
trait MySQLiByDanielGP
{

    use MySQLiByDanielGPnumbers,
        MySQLiMultipleExecution;

    /**
     * Initiates connection to MySQL
     *
     * @param array $mySQLconfig
     *
     * $mySQLconfig           = [
     * 'host'     => MYSQL_HOST,
     * 'port'     => MYSQL_PORT,
     * 'username' => MYSQL_USERNAME,
     * 'password' => MYSQL_PASSWORD,
     * 'database' => MYSQL_DATABASE,
     * ];
     *
     * @return string
     */
    protected function connectToMySql($mySQLconfig)
    {
        if (is_null($this->mySQLconnection)) {
            extract($mySQLconfig);
            $this->mySQLconnection = new \mysqli($host, $username, $password, $database, $port);
            if ($this->mySQLconnection->connect_error) {
                $this->mySQLconnection = null;
                $erNo                  = $this->mySQLconnection->connect_errno;
                $erMsg                 = $this->mySQLconnection->connect_error;
                $msg                   = $this->lclMsgCmn('i18n_Feedback_ConnectionError');
                return sprintf($msg, $erNo, $erMsg, $host, $port, $username, $database);
            }
            return '';
        }
    }

    /**
     * Returns a Date field 2 use in a form
     *
     * @param array $value
     * @return string
     */
    protected function getFieldOutputDate($value)
    {
        $defaultValue = $this->getFieldValue($value);
        if ($defaultValue == 'NULL') {
            $defaultValue = date('Y-m-d');
        }
        $inA = [
            'type'      => 'text',
            'name'      => $value['COLUMN_NAME'],
            'id'        => $value['COLUMN_NAME'],
            'value'     => $defaultValue,
            'size'      => 10,
            'maxlength' => 10,
            'onfocus'   => implode('', [
                'javascript:NewCssCal(\'' . $value['COLUMN_NAME'],
                '\',\'yyyyMMdd\',\'dropdown\',false,\'24\',false);',
            ]),
        ];
        return $this->setStringIntoShortTag('input', $inA) . $this->setCalendarControl($value['COLUMN_NAME']);
    }

    /**
     * Returns a Time field 2 use in a form
     *
     * @param array $value
     * @param array $iar
     * @return string
     */
    protected function getFieldOutputTime($value, $iar = [])
    {
        return $this->getFieldOutputTT($value, 8, $iar);
    }

    /**
     * Provides a detection if given Query does contain a Parameter
     * that may require statement processing later on
     *
     * @param string $sQuery
     * @param string $paramIdentifier
     * @return boolean
     */
    protected function getMySQLqueryWithParameterIdentifier($sQuery, $paramIdentifier)
    {
        $sReturn = true;
        if (strpos($sQuery, $paramIdentifier) === false) {
            $sReturn = false;
        }
        return $sReturn;
    }

    /**
     * Transmit Query to MySQL server and get results back
     *
     * @param string $sQuery
     * @param string $sReturnType
     * @param array $ftrs
     * @return boolean|array|string
     */
    protected function setMySQLquery2Server($sQuery, $sReturnType = null, $ftrs = null)
    {
        if (is_null($sReturnType)) {
            $this->mySQLconnection->query(html_entity_decode($sQuery));
            return '';
        } elseif (is_null($this->mySQLconnection)) {
            return ['customError' => $this->lclMsgCmn('i18n_MySQL_ConnectionNotExisting'), 'result' => null];
        }
        $result = $this->mySQLconnection->query(html_entity_decode($sQuery));
        if ($result) {
            return $this->setMySQLquery2ServerConnected(['Result' => $result, 'RType' => $sReturnType, 'F' => $ftrs]);
        }
        $erM  = [$this->mySQLconnection->errno, $this->mySQLconnection->error];
        $cErr = sprintf($this->lclMsgCmn('i18n_MySQL_QueryError'), $erM[0], $erM[1]);
        return ['customError' => $cErr, 'result' => null];
    }

    /**
     * Turns a raw query result into various structures
     * based on different predefined $parameters['returnType'] value
     *
     * @param array $parameters
     * @return array as ['customError' => '...', 'result' => '...']
     */
    private function setMySQLquery2ServerByPattern($parameters)
    {
        $aReturn = $parameters['return'];
        $vld     = $this->setMySQLqueryValidateInputs($parameters);
        if ($vld[1] !== '') {
            return ['customError' => $vld[1], 'result' => ''];
        } elseif ($parameters['returnType'] == 'value') {
            return ['customError' => $vld[1], 'result' => $parameters['QueryResult']->fetch_row()[0]];
        }
        $counter2 = 0;
        for ($counter = 0; $counter < $parameters['NoOfRows']; $counter++) {
            $line = $parameters['QueryResult']->fetch_row();
            $this->setMySQLquery2ServerByPatternLine($parameters, $line, $counter, $counter2, $aReturn);
            $counter2++;
        }
        return ['customError' => '', 'result' => $aReturn['result']];
    }

    private function setMySQLquery2ServerByPatternKey($parameters, $line, $counter)
    {
        switch ($parameters['returnType']) {
            case 'array_key_value':
                return [$line[0], $line[1]];
            // intentionally left open
            case 'array_key2_value':
                return [$line[0] . '@' . $line[1], $line[1]];
            // intentionally left open
            case 'array_numbered':
                return [$counter, $line[0]];
            // intentionally left open
        }
    }

    private function setMySQLquery2ServerByPatternLine($parameters, $line, $counter, $counter2, &$aReturn)
    {
        if (in_array($parameters['returnType'], ['array_key_value', 'array_key2_value', 'array_numbered'])) {
            $rslt                        = $this->setMySQLquery2ServerByPatternKey($parameters, $line, $counter);
            $aReturn['result'][$rslt[0]] = $rslt[1];
        } elseif ($parameters['returnType'] == 'array_key_value2') {
            $aReturn['result'][$line[0]][] = $line[1];
        } else {
            $finfo = $parameters['QueryResult']->fetch_fields();
            $this->setMySQLquery2ServerByPatternLineAdvanced($parameters, $finfo, $line, $counter2, $aReturn);
        }
    }

    private function setMySQLquery2ServerByPatternLineAdvanced($parameters, $finfo, $line, $counter2, &$aReturn)
    {
        foreach ($finfo as $columnCounter => $value) {
            switch ($parameters['returnType']) {
                case 'array_first_key_rest_values':
                    if ($columnCounter !== 0) {
                        $aReturn['result'][$line[0]][$value->name] = $line[$columnCounter];
                    }
                    break;
                case 'array_pairs_key_value':
                    $aReturn['result'][$value->name]                                   = $line[$columnCounter];
                    break;
                case 'full_array_key_numbered':
                    $aReturn['result'][$counter2][$value->name]                        = $line[$columnCounter];
                    break;
                case 'full_array_key_numbered_with_record_number_prefix':
                    $parameters['prefix']                                              = 'RecordNo';
                // intentionally left open
                case 'full_array_key_numbered_with_prefix':
                    $aReturn['result'][$parameters['prefix']][$counter2][$value->name] = $line[$columnCounter];
                    break;
            }
        }
    }

    private function setMySQLquery2ServerConnected($inArray)
    {
        if ($inArray['RType'] == 'id') {
            return ['customError' => '', 'result' => $this->mySQLconnection->insert_id];
        } elseif ($inArray['RType'] == 'lines') {
            return ['result' => $inArray['Result']->num_rows, 'customError' => ''];
        }
        $parameters = [
            'NoOfColumns' => $inArray['Result']->field_count,
            'NoOfRows'    => $inArray['Result']->num_rows,
            'QueryResult' => $inArray['Result'],
            'returnType'  => $inArray['RType'],
            'return'      => ['customError' => '', 'result' => null]
        ];
        if (substr($inArray['RType'], -6) == 'prefix') {
            $parameters['prefix'] = $inArray['F']['prefix'];
        }
        return $this->setMySQLquery2ServerByPattern($parameters);
    }
}
