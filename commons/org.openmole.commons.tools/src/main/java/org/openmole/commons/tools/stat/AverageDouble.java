/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.commons.tools.stat;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AverageDouble {
	Integer historySize = 100; 

	double value;
	Long weight;
	Queue<Double> history = new LinkedList<Double>();

	public AverageDouble() {
		super();
		this.value = 0.0;
		this.weight = 0L;
	}
	
	public AverageDouble(Integer historySize) {
		super();
		this.value = 0.0;
		this.weight = 0L;
		this.historySize = historySize;
	}
	
	
	public AverageDouble(Double value) {
		super();
		this.value = value;
		this.weight = 1L;

	}

	public synchronized void add(Double nval) {
		if(weight < historySize) {
			value = ((value * weight) + nval) / (weight+1);
			weight++;
			history.offer(nval);
		}
		else {
			Double oldVal = history.remove();
			Double tot = (value * weight) - oldVal;
			tot += nval;
			value = tot / weight;
			history.offer(nval);
		}

	}

	public synchronized double getValue() {
		return value;
	}
	
	public synchronized Long getWeight() {
		return weight;
	}

	public Integer getHistorySize() {
		return historySize;
	}
	
	
}
