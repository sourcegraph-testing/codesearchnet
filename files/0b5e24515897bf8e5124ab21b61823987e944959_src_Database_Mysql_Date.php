<?php
namespace Phine\Framework\Database\Mysql;

use Phine\Framework\Database\Interfaces\BaseImpl\BaseType;
use Phine\Framework\System;

/**
 * Represents a mysql date
 */
class Date extends BaseType
{
    /**
     * @return System\Date
     * (non-PHPdoc)
     * @see Phine/Framework/Database/Interfaces/IDatabaseType#FromDBString($value)
     */
    function FromDBString($value)
    {
        if ($value === null)
            return null;
        
      $parts = explode("-", $value);
       return new System\Date($parts[2], $parts[1], $parts[0], 0, 0 ,0);
    }
    
    /**
     * Returns the database string representation of the date.
     * @return string
     * @param System\Date $value
     * (non-PHPdoc)
     * @see Phine/Framework/Database/Interfaces/IDatabaseType#ToDBString($value)
     */
    function ToDBString($value)
    {
        //Type save:
        return $this->_ToDBString($value);
    }
    /**
     * 
     * @param System\Date $value
     * @return \string
     */
    private function _ToDBString(System\Date $value = null)
    {
        if ($value === null)
         return $value;
        
        return $value->ToString('Y-m-d');
    }
    /**
         *
         * @return \Phine\Framework\System\Date 
         */
    function DefaultInstance()
    {
        return new System\Date(0, 0, 0, 0, 0, 0);
    }
    
    
}