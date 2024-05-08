<?php /** @copyright Alejandro Salazar (c) 2016 */
namespace Math\Combinatorics;

/******************************************* Disclaimer ********************************************
 * This class is based on the efforts of David Sanders <shangxiao@php.net>, author of the 
 * Combinatorics PEAR package.
 * 
 * You can see his original source at https://pear.php.net/package/Math_Combinatorics
 **************************************************************************************************/

/**
 * The <kbd>Permutation</kbd> class provides the Math functionality to calculate a list of permutations
 * given a supplied set of data.
 * <p>It provides with an instance approach and static methods to use it without the need to 
 * explicitly create an instance.</p>
 *
 * @author     Alejandro Salazar (alphazygma@gmail.com)
 * @category   Math
 * @version    1.0
 * @license    http://www.gnu.org/licenses/lgpl-3.0.en.html GNU LGPLv3
 * @link       https://github.com/alphazygma/Combinatorics
 * @package    Math
 * @subpackage Combinatorics
 */
class Permutation
{
    /** @var \Math\Combinatorics\Combination */
    protected $_combination;
    
    public function __construct()
    {
        // Note: I would usually code towards using dependency injection and using interfaces,
        // however, this library is not such one that we are expecting possible multiple 
        // implementations of the combination class, and thus the reason why it is tightly coupled.
        $this->_combination = new Combination();
    }
    
    /**
     * Creates all the possible permutations for the source data set.
     * <p>Static method allows to use the functionality as a utility rather than as an instance 
     * approach.</p>
     * 
     * @param array $sourceDataSet The source data from which to calculate permutations.
     * @param int   $subsetSize    (Optional)<br/>If supplied, only permutations of the indicated
     *      size will be returned.
     *      <p>If the subset size is greater than the source data set size, only permutations for
     *      the wil be calculated for largest SET.</p>
     *      <p>If the subset size is less or equal to 0, only one permutation will be returned with
     *      no elements.</p>
     * @return array A list of arrays containing all the combinations from the source data set.
     */
    public static function get(array $sourceDataSet, $subsetSize = null)
    {
        $combination = new static($sourceDataSet, $subsetSize);
        return $combination->getPermutations($sourceDataSet, $subsetSize);
    }
    
    /**
     * Creates all the possible permutations for the source data set.
     * 
     * @param array $sourceDataSet The source data from which to calculate permutations.
     * @param int   $subsetSize    (Optional)<br/>If supplied, only permutations of the indicated
     *      size will be returned.
     *      <p>If the subset size is greater than the source data set size, only permutations for
     *      the wil be calculated for largest SET.</p>
     *      <p>If the subset size is less or equal to 0, only one permutation will be returned with
     *      no elements.</p>
     * @return array A list of arrays containing all the combinations from the source data set.
     */
    public function getPermutations(array $sourceDataSet, $subsetSize = null)
    {
        $combinationMap = $this->_combination->getCombinations($sourceDataSet, $subsetSize);
        
        $permutationsMap = [];
        foreach ($combinationMap as $combination) {
            $permutationsMap = array_merge(
                $permutationsMap,
                $this->_findPermutations($combination)
            );
        }
        
        return $permutationsMap;
    }
    
    /**
     * Recursive function to find the permutations of the given combination.
     *
     * @param array $combination Current combination set
     * @return array Permutations of the current combination
     */
    private function _findPermutations($combination)
    {
        // If the combination only has 1 element, then the permutation is the same as the combination
        if (count($combination) <= 1) {
            return [$combination];
        }

        $permutationList = [];

        $startKey = $this->_processSubPermutations($combination, $permutationList);

        // Now that the first element has been rotated to the end, we calculate permutations until
        // we reach the first element which is now at the end of the combiatnion.
        $key = key($combination);
        while ($key != $startKey) {
            $this->_processSubPermutations($combination, $permutationList);
            $key = key($combination);
        }

        return $permutationList;
    }
    
    /**
     * This function takes the supplied combination and calculates the sub permutations, which then
     * are added to the supplied permutation list.
     * </p><b>Note:</b> Additionally, the first element of the combination will be rotated to the end
     * for further sub-permutation calculations.</p>
     * 
     * @param array $combination
     * @param array $permutationList
     */
    private function _processSubPermutations(&$combination, &$permutationList)
    {
        // A combination is a list of values, so, to be able to calculate the permutations, we
        // need to fix some elements so we can create the other variations, such variations are
        // calculated by recursively finding permutations on the combination subset.
        list($shiftedKey, $shiftedVal) = $this->_arrayShiftAssoc($combination);
        $subPermutations = $this->_findPermutations($combination);

        // Re-appending (at the beginning of the array) the shifted value with it's sub permutations
        foreach ($subPermutations as $permutation) {
            $permutationList[] = array_merge([$shiftedKey => $shiftedVal], $permutation);
        }
        
        // Based on PHP handling of arrays, re-adding the shifted key guarantees that it will be
        // appended to the end of the array and thus contributing towards the order needed for the
        // permutation
        $combination[$shiftedKey] = $shiftedVal;
        
        // Making sure the pointer is at the beginning of the array
        reset($combination);
        
        return $shiftedKey;
    }
    
    /**
     * Similar implementation to <kbd>array_shift()</kbd> where keys are preserved.
     *
     * @param  array $array Reference to the array to shift
     * @return array Array with 1st element as the shifted key and the 2nd element as the shifted value.
     */
    private function _arrayShiftAssoc(array &$array)
    {
        if (empty($array)) {
            return null;
        }
        
        // Make sure that the array variable is pointing to the beginning of the array
        reset($array);
        
        // Now, get the first key and first value
        $firstKey   = key($array);
        $firstValue = current($array); // equivalent to $array[$firstKey]
        
        unset($array[$firstKey]);
        
        return [$firstKey, $firstValue];
    }
}
