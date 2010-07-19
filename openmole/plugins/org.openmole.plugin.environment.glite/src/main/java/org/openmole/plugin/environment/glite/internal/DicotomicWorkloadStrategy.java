/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.environment.glite.internal;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;


import org.openmole.core.model.execution.IStatistic;
import org.openmole.core.model.execution.batch.SampleType;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class DicotomicWorkloadStrategy implements IWorkloadManagmentStrategy {

    final Map<SampleType,Double> maxOverSubmitRatio = new EnumMap<SampleType, Double>(SampleType.class);
    double epsilon;

    public DicotomicWorkloadStrategy(double maxOverSubmitRatioWaiting, double maxOverSubmitRatioRunning, double epsilon) {
        this.maxOverSubmitRatio.put(SampleType.WAITING, maxOverSubmitRatioWaiting);
        this.maxOverSubmitRatio.put(SampleType.RUNNING, maxOverSubmitRatioRunning);
        this.epsilon = epsilon;
    }

    @Override
    public Long getWhenJobShouldBeResubmited(SampleType type, IStatistic finishedStat, IStatistic runningStat) {

        Long[] finished = finishedStat.getOrderedSamples(type);
        Long[] running = runningStat.getOrderedSamples(type);

        if (finished.length == 0) {
            return Long.MAX_VALUE;

        }
        if (running.length == 0) {
            return Long.MAX_VALUE;
        }

        Long tmax = finished[finished.length - 1];
        Long tmin = finished[0];

        Long t;
        Long lastTPToSmall = Long.MAX_VALUE;
        double p;
        double ratio = maxOverSubmitRatio.get(type);
        
        do {

            t = (tmax + tmin) / 2;
            int n1 = nbSup(finished, t);
            int n4 = running.length;
            int n3 = nbSup(running, t);
            int n2 = finished.length;

            double n4bis = n3;
            int indice = 0;

            while (indice < running.length && running[indice] < t) {

                int overS = nbSup(finished, running[indice]);
                n4bis += ((double)n1) / overS;
                indice++;
            }
            
            p = (n1 + n4bis) / (n2 + n4);

            if(p < ratio) {
                if(p > 0.0) {
                   lastTPToSmall = t;
                }

                tmax = t;
            } else {
                
                tmin = t;
            }

        } while((tmax - tmin) > 1 && Math.abs(p - ratio) > epsilon );



        if(Math.abs(p - ratio) > epsilon) {
            t = lastTPToSmall;
        }

        return t;

    }

    int nbSup(Long[] samples, Long t) {
        int indice = Arrays.binarySearch(samples, t);

        while (indice > 0 && samples[indice] == samples[indice - 1]) {
            indice--;
        }

        if(indice >= 0) {
            return samples.length - indice;
        } else {
            return samples.length + indice + 1;
        }
        
    }
}
