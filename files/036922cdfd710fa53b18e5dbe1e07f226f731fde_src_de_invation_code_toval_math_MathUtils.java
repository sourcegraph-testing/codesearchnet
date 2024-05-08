package de.invation.code.toval.math;

import java.math.BigInteger;

public class MathUtils {

        /**
         * Calculates the factorial of <code>number</code>.
         *
         * @param number Basic number for operation
         * @return The factorial of <code>number</code>
         */
        public static BigInteger getFactorial(int number) {
                BigInteger factorial = BigInteger.ONE;
                for (int i = number; i > 1; i--) {
                        factorial = factorial.multiply(new BigInteger(Integer.toString(i)));
                }
                return factorial;
        }

        /**
         * Returns the binomial coefficient.<br>
         * For any set containing n elements, this is the number of distinct
         * k-element subsets of it.
         *
         * @param n Set size
         * @param k Subset size
         * @return The binomial coefficient of <code>n</code> and <code>k</code>
         */
        public static long binCoeff(int n, int k) {
                if (k > n) {
                        return 0;
                }
                if (k == 0 || k == n) {
                        return 1;
                }
                long result = 1;
                for (int i = 1; i <= k; i++) {
                        result *= (n - i + 1) / i;
                }
                return result;
        }

        /**
         * Constucts a truth-table for the given column-number. <br>
         * The number of table rows is 2^columnCount.<br>
         * A truth-table with three columns looks like this:<br>
         * (0=<tt>false</tt>, 1=<tt>true</tt>)<br>
         * <br>
         * <ul>
         * <li>0 0 0</li>
         * <li>0 0 1</li>
         * <li>0 1 0</li>
         * <li>0 1 1</li>
         * <li>1 0 0</li>
         * <li>1 0 1</li>
         * <li>1 1 0</li>
         * <li>1 1 1</li>
         * </ul>
         * <br>
         *
         * @param colCount The number of columns.
         * @return The constructed truth-table
         */
        public static boolean[][] getTruthTable(int colCount) {
                boolean[][] table = new boolean[colCount][(int) Math.pow(2, colCount)];
                for (int i = 1; i <= colCount; i++) {
                        for (int j = 0; j < (int) Math.pow(2, colCount); j++) {
                                //System.out.println(i+" "+j+" "+(int) Math.floor(j/Math.pow(2,i-1)));
                                table[i - 1][j] = (((int) Math.floor(j / Math.pow(2, i - 1))) % 2) == 0;
                        }
                }
                return table;
        }

        /**
         * Returns the number of places after the decimal separator of the given
         * number.
         *
         * @param <N> Number type
         * @param number Basic number for operation
         * @return The number of places after the decimal separator of
         * <code>number</code>
         */
        public static <N extends Number> int getRHD(N number) {
                String numberAsString = String.valueOf(number);
                return numberAsString.substring(numberAsString.lastIndexOf('.') + 1).length();
        }

        /**
         * Returns the number of zeros after the decimal separator of the given
         * number before another digit appears.
         *
         * @param number Basic number for operation
         * @return The number of zeros after the decimal separator before
         * another digit appears
         */
        public static int getRHD(double number) {
                double n = number % 1;
                int rhd = 0;
                while ((n *= 10) < 1) {
                        rhd++;
                }
                return ++rhd;
        }

}
