/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.groupingstrategy.batch;

import org.openmole.core.implementation.mole.MoleJobGroup;
import org.openmole.core.model.mole.IMoleJobGroup;
import org.openmole.core.model.mole.IGroupingStrategy;
import org.openmole.core.model.data.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class FixedNumberOfMoleJobsGroupingStrategy implements IGroupingStrategy {

    final private int numberOfMoleJobs;
    private Integer currentBatchNumber = 0;
    private Integer currentNumberOfJobs = 0;

    public FixedNumberOfMoleJobsGroupingStrategy(int numberOfMoleJobs) {
        this.numberOfMoleJobs = numberOfMoleJobs;
    }

    @Override
    public IMoleJobGroup group(IContext context) throws InternalProcessingError, UserBadDataError {
        Object[] args = {currentBatchNumber};
        IMoleJobGroup ret = new MoleJobGroup(args);

        currentNumberOfJobs++;
        if(currentNumberOfJobs >= numberOfMoleJobs) {
            currentNumberOfJobs = 0;
            currentBatchNumber++;
        }

        return ret;
    }




}
