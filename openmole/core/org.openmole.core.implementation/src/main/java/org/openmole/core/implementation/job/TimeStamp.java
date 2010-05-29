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

package org.openmole.core.implementation.job;

import org.openmole.core.workflow.model.job.ITimeStamp;
import org.openmole.core.workflow.model.job.State;

/**
 *
 * @author reuillon
 */
public class TimeStamp implements ITimeStamp {

    final State state;
    final String hostName;
    final Long time;

    public TimeStamp(State state, String hostName, Long time) {
        this.state = state;
        this.hostName = hostName;
        this.time = time;
    }

 
    @Override
    public State getState() {
        return state;
    }

    @Override
    public String getHostName() {
        return hostName;
    }

    @Override
    public Long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "TimeStamp{" + "state=" + state + "hostName=" + hostName + "time=" + time + '}';
    }

    

}
