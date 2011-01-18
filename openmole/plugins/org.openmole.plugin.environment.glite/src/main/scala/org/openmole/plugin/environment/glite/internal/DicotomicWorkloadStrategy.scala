/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.glite.internal

import org.openmole.plugin.environment.glite.internal.Activator._
import org.openmole.plugin.environment.glite.GliteEnvironment
import java.util.Arrays
import java.util.logging.Logger
import org.openmole.core.model.execution.SampleType
import scala.util.Sorting._
import scala.math._


class DicotomicWorkloadStrategy extends IWorkloadManagmentStrategy {

  override def whenJobShouldBeResubmited(sample: SampleType.Value, finishedStat: Iterable[Long] , runningStat: Iterable[Long]): Long = {
    val epsilon = workspace.preferenceAsDouble(GliteEnvironment.OverSubmissionRatioEpsilonLocation)

    val maxOverSubmitRatio: PartialFunction[SampleType.Value, Double] = {
        x => x match {
        case SampleType.WAITING => workspace.preferenceAsDouble(GliteEnvironment.OverSubmissionRatioWaitingLocation)
        case SampleType.RUNNING => workspace.preferenceAsDouble(GliteEnvironment.OverSubmissionRatioRunningLocation)
      }}
   // val LOGGER = Logger.getLogger(classOf[DicotomicWorkloadStrategy].getName)
    //LOGGER.info("Sample type " + sample.getLabel)
    
    val finished = finishedStat.toArray
    val running = runningStat.toArray
    
    quickSort(finished)
    quickSort(running)
     
    //LOGGER.info(running.toIterable.toString)
    //LOGGER.info(finished.toIterable.toString)
    
    if (finished.length == 0) return Long.MaxValue
    if (running.length == 0) return Long.MaxValue

    var tmax = finished(finished.length - 1)
    var tmin = finished(0)

    //LOGGER.info("tmin " + tmin + " tmax " + tmax + " epsilon " + epsilon)
     
    var lastTPToSmall = Long.MaxValue
    val ratio = maxOverSubmitRatio(sample)
    
    var t = 0L
    var p = 0.0
    
    do {

      t = (tmax + tmin) / 2
      
      //LOGGER.info("t " + t)
      
      val n1 = nbSup(finished, t)
      val n4 = running.length
      val n3 = nbSup(running, t)
      val n2 = finished.length

      var n4bis = n3

      //LOGGER.info("n1 n2 n3 n4 " + n1 + " " + n2 + " " + n3 + " " + n4)

      var indice = 0

      while (indice < running.length && running(indice) < t) {
        val overS = nbSup(finished, running(indice))
        n4bis += (n1.doubleValue / overS).intValue
        indice += 1
      }
            
      //LOGGER.info("indice "+ indice)
      
      p = (n1.doubleValue + n4bis) / (n2 + n4)

      //LOGGER.info("p "+ p)
      
      if(p < ratio) {
        if(p > 0.0) {
          lastTPToSmall = t;
        }
        tmax = t
      } else {
        tmin = t
      }
      
      //LOGGER.info("tmin " + tmin + " tmax " + tmax + " t " + t)
    } while((tmax - tmin) > 1 && abs(p - ratio) > epsilon )

    if(abs(p - ratio) > epsilon) {
      t = lastTPToSmall
    }
    
    t
  }

  def nbSup(samples: Array[Long], t: Long): Int = {
    var indice = Arrays.binarySearch(samples, t)

    while (indice > 0 && samples(indice) == samples(indice - 1)) {
      indice -= 1
    }

    if(indice >= 0) samples.length - indice
    else samples.length + indice + 1  
  }
}
