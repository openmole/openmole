
/*
 *  Copyright (C) 2010 leclaire
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
package org.openmole.plugin.domain.uniformintegerdistribution;

import java.util.Iterator;
import java.util.Random;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.domain.IDomain;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@iscpif.fr>
 */
public class UniformIntegerDistribution implements IDomain<Integer>{
    private Random generator;

    UniformIntegerDistribution(long seed){
        generator = new Random(seed);
    }


    @Override
    public Iterator<? extends Integer> iterator(IContext context) throws UserBadDataError, InternalProcessingError {
        return new Iterator<Integer>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                return generator.nextInt();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }

}
