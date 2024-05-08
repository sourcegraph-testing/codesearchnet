/**
 * Copyright (C) 2014-2019 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.numbercruncher.matrix;

import java.io.PrintStream;

import javax.annotation.Nonnull;

import com.helger.numbercruncher.mathutils.SystemOutAlignRight;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The matrix class.
 */
public class Matrix
{
  /** number of rows */
  protected int m_nRows;
  /** number of columns */
  protected int m_nCols;
  /** 2-d array of values */
  protected float m_aValues[][];

  // --------------//
  // Constructors //
  // --------------//

  /**
   * Default constructor.
   */
  protected Matrix ()
  {}

  /**
   * Constructor.
   *
   * @param rowCount
   *        the number of rows
   * @param colCount
   *        the number of columns
   */
  public Matrix (final int rowCount, final int colCount)
  {
    m_nRows = (rowCount > 0) ? rowCount : 1;
    m_nCols = (colCount > 0) ? colCount : 1;
    m_aValues = new float [m_nRows] [m_nCols];
  }

  /**
   * Constructor.
   *
   * @param values
   *        the 2-d array of values
   */
  public Matrix (final float [] [] values)
  {
    set (values);
  }

  // ---------//
  // Getters //
  // ---------//

  /**
   * Get the row count.
   *
   * @return the row count
   */
  public int rowCount ()
  {
    return m_nRows;
  }

  /**
   * Get the column count.
   *
   * @return the column count
   */
  public int columnCount ()
  {
    return m_nCols;
  }

  /**
   * Get the value of element [r,c] in the matrix.
   *
   * @param r
   *        the row index
   * @param c
   *        the column index
   * @return the value
   * @throws MatrixException
   *         for an invalid index
   */
  public float at (final int r, final int c) throws MatrixException
  {
    if ((r < 0) || (r >= m_nRows) || (c < 0) || (c >= m_nCols))
    {
      throw new MatrixException (MatrixException.INVALID_INDEX);
    }

    return m_aValues[r][c];
  }

  /**
   * Get a row of this matrix.
   *
   * @param r
   *        the row index
   * @return the row as a row vector
   * @throws MatrixException
   *         for an invalid index
   */
  public RowVector getRow (final int r) throws MatrixException
  {
    if ((r < 0) || (r >= m_nRows))
    {
      throw new MatrixException (MatrixException.INVALID_INDEX);
    }

    final RowVector rv = new RowVector (m_nCols);
    for (int c = 0; c < m_nCols; ++c)
    {
      rv.m_aValues[0][c] = m_aValues[r][c];
    }

    return rv;
  }

  /**
   * Get a column of this matrix.
   *
   * @param c
   *        the column index
   * @return the column as a column vector
   * @throws MatrixException
   *         for an invalid index
   */
  public ColumnVector getColumn (final int c) throws MatrixException
  {
    if ((c < 0) || (c >= m_nCols))
    {
      throw new MatrixException (MatrixException.INVALID_INDEX);
    }

    final ColumnVector cv = new ColumnVector (m_nRows);
    for (int r = 0; r < m_nRows; ++r)
    {
      cv.m_aValues[r][0] = m_aValues[r][c];
    }

    return cv;
  }

  /**
   * Copy the values of this matrix.
   *
   * @return the values
   */
  @SuppressFBWarnings ("EI_EXPOSE_REP")
  public float [] [] values ()
  {
    return m_aValues;
  }

  /**
   * Copy the values of this matrix.
   *
   * @return the copied values
   */
  public float [] [] copyValues2D ()
  {
    final float v[][] = new float [m_nRows] [m_nCols];

    for (int r = 0; r < m_nRows; ++r)
    {
      for (int c = 0; c < m_nCols; ++c)
      {
        v[r][c] = m_aValues[r][c];
      }
    }

    return v;
  }

  // ---------//
  // Setters //
  // ---------//

  /**
   * Set the value of element [r,c].
   *
   * @param r
   *        the row index
   * @param c
   *        the column index
   * @param value
   *        the value
   * @throws MatrixException
   *         for an invalid index
   */
  public void set (final int r, final int c, final float value) throws MatrixException
  {
    if ((r < 0) || (r >= m_nRows) || (c < 0) || (c >= m_nCols))
    {
      throw new MatrixException (MatrixException.INVALID_INDEX);
    }

    m_aValues[r][c] = value;
  }

  /**
   * Set this matrix from a 2-d array of values. If the rows do not have the
   * same length, then the matrix column count is the length of the shortest
   * row.
   *
   * @param values
   *        the 2-d array of values
   */
  protected void set (final float values[][])
  {
    m_nRows = values.length;
    m_nCols = values[0].length;
    m_aValues = values;

    for (int r = 1; r < m_nRows; ++r)
    {
      m_nCols = Math.min (m_nCols, values[r].length);
    }
  }

  /**
   * Set a row of this matrix from a row vector.
   *
   * @param rv
   *        the row vector
   * @param r
   *        the row index
   * @throws MatrixException
   *         for an invalid index or an invalid vector size
   */
  public void setRow (final RowVector rv, final int r) throws MatrixException
  {
    if ((r < 0) || (r >= m_nRows))
    {
      throw new MatrixException (MatrixException.INVALID_INDEX);
    }
    if (m_nCols != rv.m_nCols)
    {
      throw new MatrixException (MatrixException.INVALID_DIMENSIONS);
    }

    for (int c = 0; c < m_nCols; ++c)
    {
      m_aValues[r][c] = rv.m_aValues[0][c];
    }
  }

  /**
   * Set a column of this matrix from a column vector.
   *
   * @param cv
   *        the column vector
   * @param c
   *        the column index
   * @throws MatrixException
   *         for an invalid index or an invalid vector size
   */
  public void setColumn (final ColumnVector cv, final int c) throws MatrixException
  {
    if ((c < 0) || (c >= m_nCols))
    {
      throw new MatrixException (MatrixException.INVALID_INDEX);
    }
    if (m_nRows != cv.m_nRows)
    {
      throw new MatrixException (MatrixException.INVALID_DIMENSIONS);
    }

    for (int r = 0; r < m_nRows; ++r)
    {
      m_aValues[r][c] = cv.m_aValues[r][0];
    }
  }

  // -------------------//
  // Matrix operations //
  // -------------------//

  /**
   * Return the transpose of this matrix.
   *
   * @return the transposed matrix
   */
  public Matrix transpose ()
  {
    final float tv[][] = new float [m_nCols] [m_nRows]; // transposed values

    // Set the values of the transpose.
    for (int r = 0; r < m_nRows; ++r)
    {
      for (int c = 0; c < m_nCols; ++c)
      {
        tv[c][r] = m_aValues[r][c];
      }
    }

    return new Matrix (tv);
  }

  /**
   * Add another matrix to this matrix.
   *
   * @param m
   *        the matrix addend
   * @return the sum matrix
   * @throws MatrixException
   *         for invalid size
   */
  public Matrix add (final Matrix m) throws MatrixException
  {
    // Validate m's size.
    if ((m_nRows != m.m_nRows) && (m_nCols != m.m_nCols))
    {
      throw new MatrixException (MatrixException.INVALID_DIMENSIONS);
    }

    final float sv[][] = new float [m_nRows] [m_nCols]; // sum values

    // Compute values of the sum.
    for (int r = 0; r < m_nRows; ++r)
    {
      for (int c = 0; c < m_nCols; ++c)
      {
        sv[r][c] = m_aValues[r][c] + m.m_aValues[r][c];
      }
    }

    return new Matrix (sv);
  }

  /**
   * Subtract another matrix from this matrix.
   *
   * @param m
   *        the matrix subrrahend
   * @return the difference matrix
   * @throws MatrixException
   *         for invalid size
   */
  public Matrix subtract (final Matrix m) throws MatrixException
  {
    // Validate m's size.
    if ((m_nRows != m.m_nRows) && (m_nCols != m.m_nCols))
    {
      throw new MatrixException (MatrixException.INVALID_DIMENSIONS);
    }

    final float dv[][] = new float [m_nRows] [m_nCols]; // difference values

    // Compute values of the difference.
    for (int r = 0; r < m_nRows; ++r)
    {
      for (int c = 0; c < m_nCols; ++c)
      {
        dv[r][c] = m_aValues[r][c] - m.m_aValues[r][c];
      }
    }

    return new Matrix (dv);
  }

  /**
   * Multiply this matrix by a constant.
   *
   * @param k
   *        the constant
   * @return the product matrix
   */
  public Matrix multiply (final float k)
  {
    final float pv[][] = new float [m_nRows] [m_nCols]; // product values

    // Compute values of the product.
    for (int r = 0; r < m_nRows; ++r)
    {
      for (int c = 0; c < m_nCols; ++c)
      {
        pv[r][c] = k * m_aValues[r][c];
      }
    }

    return new Matrix (pv);
  }

  /**
   * Multiply this matrix by another matrix.
   *
   * @param m
   *        the matrix multiplier
   * @return the product matrix
   * @throws MatrixException
   *         for invalid size
   */
  public Matrix multiply (final Matrix m) throws MatrixException
  {
    // Validate m's dimensions.
    if (m_nCols != m.m_nRows)
    {
      throw new MatrixException (MatrixException.INVALID_DIMENSIONS);
    }

    final float pv[][] = new float [m_nRows] [m.m_nCols]; // product values

    // Compute values of the product.
    for (int r = 0; r < m_nRows; ++r)
    {
      for (int c = 0; c < m.m_nCols; ++c)
      {
        float dot = 0;
        for (int k = 0; k < m_nCols; ++k)
        {
          dot += m_aValues[r][k] * m.m_aValues[k][c];
        }
        pv[r][c] = dot;
      }
    }

    return new Matrix (pv);
  }

  /**
   * Multiply this matrix by a column vector: this*cv
   *
   * @param cv
   *        the column vector
   * @return the product column vector
   * @throws MatrixException
   *         for invalid size
   */
  public ColumnVector multiply (final ColumnVector cv) throws MatrixException
  {
    // Validate cv's size.
    if (m_nRows != cv.m_nRows)
    {
      throw new MatrixException (MatrixException.INVALID_DIMENSIONS);
    }

    final float pv[] = new float [m_nRows]; // product values

    // Compute the values of the product.
    for (int r = 0; r < m_nRows; ++r)
    {
      float dot = 0;
      for (int c = 0; c < m_nCols; ++c)
      {
        dot += m_aValues[r][c] * cv.m_aValues[c][0];
      }
      pv[r] = dot;
    }

    return new ColumnVector (pv);
  }

  /**
   * Multiply a row vector by this matrix: rv*this
   *
   * @param rv
   *        the row vector
   * @return the product row vector
   * @throws MatrixException
   *         for invalid size
   */
  public RowVector multiply (final RowVector rv) throws MatrixException
  {
    // Validate rv's size.
    if (m_nCols != rv.m_nCols)
    {
      throw new MatrixException (MatrixException.INVALID_DIMENSIONS);
    }

    final float pv[] = new float [m_nRows]; // product values

    // Compute the values of the product.
    for (int c = 0; c < m_nCols; ++c)
    {
      float dot = 0;
      for (int r = 0; r < m_nRows; ++r)
      {
        dot += rv.m_aValues[0][r] * m_aValues[r][c];
      }
      pv[c] = dot;
    }

    return new RowVector (pv);
  }

  /**
   * Print the matrix values.
   *
   * @param width
   *        the column width
   */
  public void print (final int width, @Nonnull final PrintStream aPS)
  {
    final SystemOutAlignRight ar = new SystemOutAlignRight (aPS);

    for (int r = 0; r < m_nRows; ++r)
    {
      ar.print ("Row ", 0);
      ar.print (r + 1, 2);
      ar.print (":", 0);

      for (int c = 0; c < m_nCols; ++c)
      {
        ar.print (m_aValues[r][c], width);
      }
      ar.println ();
    }
  }
}
