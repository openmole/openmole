/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
import org.openmole.core.model.domain.IDomainWithCenter;
import org.openmole.core.model.domain.IDomainWithRange;

/**
 *
 * @author reuillon
 */
public abstract class UniformelyDiscretizedIntervalDomain<T> extends FiniteDomain<T> implements IDiscretizedIntervalDomain<T>, IDomainWithCenter<T>, IDomainWithRange<T> {

    private Interval<T> interval;
    private String step;

    public UniformelyDiscretizedIntervalDomain(Interval<T> interval, String step) {
        this.interval = interval;
        this.step = step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStep() {
        return step;
    }

    @Override
    public Interval<? extends T> getInterval() {
        return interval;
    }


}
