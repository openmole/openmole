/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

package org.openmole.plugin.domain.interval;

import org.openmole.core.implementation.domain.FiniteDomain;
import org.openmole.core.implementation.domain.Interval;
import org.openmole.core.model.domain.IDiscretizedIntervalDomain;
import org.openmole.core.model.domain.IDomainWithRange;
import org.openmole.core.model.domain.IInterval;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public abstract class LogarithmIntervalDomain<T> extends FiniteDomain<T> implements IDiscretizedIntervalDomain<T>, IDomainWithRange<T> {
    private Interval<T> interval;
    private String nbStep;


    public LogarithmIntervalDomain(Interval<T> interval, String nbStep) {
        this.interval = interval;
        this.nbStep = nbStep;
    }

    public String getNbStep() {
        return nbStep;
    }

    public void setNbStep(String nbStep) {
        this.nbStep = nbStep;
    }

    @Override
    public IInterval<? extends T> getInterval() {
        return interval;
    }

}
