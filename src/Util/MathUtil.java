/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

/**
 *
 * @author Lahari
 * @created on Apr 4, 2016
 */
public class MathUtil {
    /* Method for computing deviation of double values*/

    // Beginning of double findDeviation(double[])

    public static double findDeviation(Double[] nums) {
        double mean = findMean(nums);
        double squareSum = 0;

        for (int i = 0; i < nums.length; i++) {
            squareSum += Math.pow(nums[i] - mean, 2);
        }

        return Math.sqrt((squareSum) / (nums.length - 1));
    } // End of double findDeviation(double[])

    /**
     * Method for computing mean of an array of double values
     */
    // Beginning of double findMean(double[])
    public static double findMean(Double[] nums) {
        double sum = 0;

        for (int i = 0; i < nums.length; i++) {
            sum += nums[i];
        }

        return sum / nums.length;
    } // End of double getMean(double[])
}
