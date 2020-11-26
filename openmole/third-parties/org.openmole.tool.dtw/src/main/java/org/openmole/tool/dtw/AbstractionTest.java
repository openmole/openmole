/*
 * AbstractionTest.java   Jul 14, 2004
 *
 * Copyright (c) 2004 Stan Salvador
 * stansalvador@hotmail.com
 */

package org.openmole.tool.dtw;

import org.openmole.tool.dtw.timeseries.TimeSeries;
import org.openmole.tool.dtw.timeseries.PAA;
import org.openmole.tool.dtw.util.DistanceFunction;
import org.openmole.tool.dtw.util.DistanceFunctionFactory;
import org.openmole.tool.dtw.dtw.TimeWarpInfo;
import org.openmole.tool.dtw.dtw.WarpPath;
import org.openmole.tool.dtw.dtw.SearchWindow;
import org.openmole.tool.dtw.dtw.WarpPathWindow;

import java.util.ArrayList;


/**
 * @author Stan Salvador, stansalvador@hotmail.com
 * @since Jul 14, 2004
 */

public class AbstractionTest
{

      // PUBLIC FUNCTIONS
      public static void main(String[] args)
      {
         if (args.length!=3 && args.length!=4)
         {
            System.out.println("USAGE:  java AbstractionTest timeSeries1 timeSeries2 radius [EuclideanDistance|ManhattanDistance|BinaryDistance]");
            System.exit(1);
         }
         else
         {
            final TimeSeries tsI = new TimeSeries(args[0], false, false, ',');
            final TimeSeries tsJ = new TimeSeries(args[1], false, false, ',');

            final DistanceFunction distFn;
            if (args.length < 4)
            {
               distFn = DistanceFunctionFactory.getDistFnByName("EuclideanDistance"); 
            }
            else
            {
               distFn = DistanceFunctionFactory.getDistFnByName(args[3]);
            }   // end if
            
            final PAA shrunkI = new PAA(tsI, (int)Math.round(Math.sqrt((double)tsI.size())));
            final PAA shrunkJ = new PAA(tsJ, (int)Math.round(Math.sqrt((double)tsJ.size())));
            final WarpPath coarsePath = org.openmole.tool.dtw.dtw.DTW.getWarpPathBetween(shrunkI, shrunkJ, distFn);
            final WarpPath expandedPath = expandPath(coarsePath, shrunkI, shrunkJ);
            final SearchWindow w = new WarpPathWindow(expandedPath, Integer.parseInt(args[2]));
            final TimeWarpInfo info = org.openmole.tool.dtw.dtw.DTW.getWarpInfoBetween(tsI, tsJ, w, distFn);

            System.out.println("Warp Distance: " + info.getDistance());
            System.out.println("Warp Path:     " + info.getPath());
         }  // end if
      }  // end main()




   // Expand the small warp path ot the resolution fo tohe original time series for tsI and tsJ.
   private static WarpPath expandPath(WarpPath path, PAA tsI, PAA tsJ)
   {
      final ArrayList iPoints = new ArrayList();
      final ArrayList jPoints = new ArrayList();

      iPoints.add(new Integer(0));
      jPoints.add(new Integer(0));
      int startI = 0; //tsI.aggregatePtSize(0);
      int startJ = 0; //tsJ.aggregatePtSize(0);
      if (path.get(1).getCol() != 0)
         startI = tsI.aggregatePtSize(0)-1;
      else
         startI = (tsI.aggregatePtSize(0)-1)/2;

      if (path.get(1).getRow() != 0)
         startJ = tsJ.aggregatePtSize(0)-1;
      else
         startJ = (tsJ.aggregatePtSize(0)-1)/2;

      int lastI = 0;
      int lastJ = 0;

      for (int x=1; x<path.size()-1; x++)
      {
         int currentI = path.get(x).getCol();
         int currentJ = path.get(x).getRow();

         if ( (lastI!=currentI))
         {
            if (lastI == 0)
               startI = tsI.aggregatePtSize(0)-1;

            if (currentI == path.get(path.size()-1).getCol())
               startI -= tsI.aggregatePtSize(currentI)/2;
            {
               iPoints.add(new Integer(startI+tsI.aggregatePtSize(currentI)/2));
               startI += tsI.aggregatePtSize(currentI);
            }

            lastI = currentI;
         }
         else
         {
            iPoints.add(new Integer(startI));
         }


         if ( (lastJ!=currentJ))
         {
            if (lastJ == 0)
               startJ = tsJ.aggregatePtSize(0)-1;

            if (currentJ == path.get(path.size()-1).getRow())
               startJ -= tsJ.aggregatePtSize(currentJ)/2;
            {
               jPoints.add(new Integer(startJ+tsJ.aggregatePtSize(currentJ)/2));
               startJ += tsJ.aggregatePtSize(currentJ);
            }

            lastJ = currentJ;
         }
         else
         {
            jPoints.add(new Integer(startJ));
         }
      }  // end for loop

      iPoints.add(new Integer(tsI.originalSize()-1));
      jPoints.add(new Integer(tsJ.originalSize()-1));

      // Interpolate between coarse warp path points.
      final WarpPath expandedPath = new WarpPath();

      startI = 0;
      startJ = 0;
      int endI;
      int endJ;

      for (int p=1; p<iPoints.size(); p++)
      {
         endI = ((Integer)iPoints.get(p)).intValue();
         endJ = ((Integer)jPoints.get(p)).intValue();
         expandedPath.addLast(startI, startJ);

         if ( (endI-startI) >= (endJ-startJ))
         {
            for (int i=startI+1; i<endI; i++)
               expandedPath.addLast(i, (int)Math.round(startJ+     ((double)i-startI)/((double)endI-startI)   *(endJ-startJ)));
         }
         else
         {
            for (int j=startJ+1; j<endJ; j++)
               expandedPath.addLast(   (int)Math.round(startI+  ((double)j-startJ) / ((double)endJ-startJ) * (endI-startI)), j);
         }  // end if

         startI = endI;
         startJ = endJ;
      }  // end for loop

      expandedPath.addLast(tsI.originalSize()-1, tsJ.originalSize()-1);
      return expandedPath;
   }



}  // end class AbstractionTest
