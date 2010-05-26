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


package org.openmole.core.workflow.implementation.execution.batch;

import org.openmole.core.workflow.model.execution.batch.IFailureControl;
import org.openmole.commons.tools.stat.FailureRate;

public class FailureControl implements IFailureControl {

	FailureRate failureRate;

	public FailureControl(int historySize) {
		super();
		failureRate = new FailureRate(historySize);
	}

	@Override
	public void failed()  {
		failureRate.failed();
	}

	@Override
	public void success()  {
		failureRate.success();
	}
	
	@Override
	public double getFailureRate() {
		return failureRate.getFailureRate();
	}
	
	

}
