<?php
/**
 * Created by PhpStorm.
 * User: Sergio Zambrano Delfa <sergio.zambrano@gmail.com>
 * Date: 08/11/16
 * Time: 19:55
 */

namespace Skilla\Matrix;

class Operations
{
    private $matrix;
    private $properties;
    private $valueZero;
    private $valueOne;
    private $precision;

    /**
     * Properties constructor.
     * @param Matrix $matrix
     * @param int|null $precision
     */
    public function __construct(Matrix $matrix, $precision = 15)
    {
        $this->precision = (int)$precision;
        $this->valueZero = bcadd(0, 0, $this->precision);
        $this->valueOne = bcadd(0, 1, $this->precision);
        $this->matrix = $matrix;
        $this->properties = new Properties($matrix, $precision);
    }

    /**
     * @return Matrix
     */
    public function transposed()
    {
        $transposed = new Matrix($this->matrix->getNumCols(), $this->matrix->getNumRows(), $this->precision);
        for ($i = 1; $i <= $this->matrix->getNumRows(); $i++) {
            for ($j = 1; $j <= $this->matrix->getNumCols(); $j++) {
                $transposed->setPoint($j, $i, $this->matrix->getPoint($i, $j));
            }
        }
        return $transposed;
    }

    /**
     * @return Matrix
     * @throws NotSquareException
     */
    public function adjugateMatrix()
    {
        if (!$this->properties->isSquare()) {
            throw new NotSquareException();
        }

        $adjugate = new Matrix($this->matrix->getNumRows(), $this->matrix->getNumCols(), $this->precision);

        $determinant = new Determinant($this->matrix, $this->precision);
        for ($i=1; $i<=$this->matrix->getNumRows(); $i++) {
            for ($j=1; $j<=$this->matrix->getNumCols(); $j++) {
                $cofactor = $determinant->cofactor($i, $j);
                $point = new Determinant($cofactor, $this->precision);
                $adjugate->setPoint($j, $i, $point->retrieve());
            }
        }
        return $adjugate;
    }

    public function inverse()
    {
        $determinant = new Determinant($this->matrix);
        $factor = $determinant->retrieve();
        if ($factor === $this->valueZero) {
            throw new NotInverseException();
        }
        $factor = 1/$factor;

        $transposed = $this->transposed();
        $operations = new Operations($transposed, $this->precision);
        $adjugate = $operations->adjugateMatrix();
        $operations = new Operations($adjugate, $this->precision);
        return $operations->multiplicationScalar($factor);
    }

    public function multiplicationScalar($multiplier)
    {
        $result = new Matrix($this->matrix->getNumRows(), $this->matrix->getNumCols(), $this->precision);

        for ($row=1; $row<=$this->matrix->getNumRows(); $row++) {
            for ($col=1; $col<=$this->matrix->getNumCols(); $col++) {
                $newValue = bcmul($this->matrix->getPoint($row, $col), $multiplier, $this->precision);
                $result->setPoint($row, $col, $newValue);
            }
        }
        return $result;
    }
}
