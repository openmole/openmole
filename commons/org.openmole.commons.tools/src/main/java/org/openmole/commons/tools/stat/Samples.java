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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Samples {

    Integer historySize;
    Queue<Long> history = new LinkedList<Long>();

    public Samples(Integer historySize) {
        super();
        this.historySize = historySize;
    }

    public synchronized void add(Long nval) {
        if (history.size() < historySize) {
            history.offer(nval);
        } else {
            history.remove();
            history.offer(nval);
        }
    }

    public synchronized Long[] getOrderedValues() {
        Long[] ret =  history.toArray(new Long[0]);

        Arrays.sort(ret);
        return ret;
    }

    public synchronized int getCurrentHistorySize() {
        return history.size();
    }
}
