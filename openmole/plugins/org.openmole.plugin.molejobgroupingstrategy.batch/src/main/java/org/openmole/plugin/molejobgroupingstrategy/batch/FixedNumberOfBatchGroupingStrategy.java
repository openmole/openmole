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
package org.openmole.plugin.molejobgroupingstrategy.batch;


import org.openmole.core.workflow.implementation.mole.MoleJobCategory;
import org.openmole.core.workflow.model.execution.IMoleJobCategory;
import org.openmole.core.workflow.model.execution.IMoleJobGroupingStrategy;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon@openmole.fr>
 */
public class FixedNumberOfBatchGroupingStrategy implements IMoleJobGroupingStrategy {

    final private int batchSize;
    private Integer currentBatchNumber = 0;

    public FixedNumberOfBatchGroupingStrategy(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public IMoleJobCategory getCategory(IContext context) throws InternalProcessingError, UserBadDataError {
        Object[] tab = {currentBatchNumber};
        IMoleJobCategory jobCategory = new MoleJobCategory(tab);
        currentBatchNumber = (currentBatchNumber+1)%batchSize;
        return jobCategory;

    }
}
