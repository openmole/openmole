/*
 * Arrays.java   Jul 14, 2004
 *
 * Copyright (c) 2004 Stan Salvador
 * stansalvador@hotmail.com
 */

package org.openmole.tool.dtw.util;


public interface DistanceFunction
{
   public double calcDistance(double[] vector1, double[] vector2);
}