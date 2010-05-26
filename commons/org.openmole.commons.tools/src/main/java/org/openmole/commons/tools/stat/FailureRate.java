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


public class FailureRate {
	
	//static final int minStat = 1;
	
	AverageDouble avg;
	
	public FailureRate(int historySize) {
		avg = new AverageDouble(historySize);
		
		for(int i = 0; i < avg.getHistorySize(); i++) {
			avg.add(0.0);
		}		
	}
	
	public void success() {
		avg.add(0.0);
	}
	
	public void failed() {
		avg.add(1.0);
	}
	
	public double getFailureRate() {
		return avg.getValue();
	}
}
