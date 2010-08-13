/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.data;

import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.job.ITicket;
import java.util.Set;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * A data channel allow to transmit data between remotes task capsule (under some condition) within a mole.
 * Two capsules could be linked with a {@link IDataChannel} if:
 *      - they belong to the same mole,
 *      - there is no capsule with more than one input slot in a path between the two capsules.
 *
 * @author reuillon
 */
public interface IDataChannel {

    /**
     *
     * Get the capsule from which the data channel starts.
     *
     * @return the capsule from whitch the data channel starts
     */
    IGenericTaskCapsule<?, ?> getStart();


    /**
     *
     * Get the capsule to which the data channel ends.
     *
     * @return the capsule to which the data channel ends.
     */
    IGenericTaskCapsule<?, ?> getEnd();

    /**
     *
     * Set the starting capsule of the data channel.
     *
     * @param start the new starting capsule of the data channel
     */
    void setStart(IGenericTaskCapsule start);

    /**
     *
     * Set the ending capsule of the data channel.
     *
     * @param end the new ending capsule of the data channel
     */
    void setEnd(IGenericTaskCapsule end);

    /**
     *
     * Get the name of the variable transported by this data channel.
     *
     * @return the name of the variable transported by this data channel.
     */
    Iterable<String> getVariableNames();

    /**
     *
     * Add a variable for being transported by this data channel.
     *
     * @param prototype the prototype of the variable to add
     */
    void add(IPrototype prototype);

    /**
     *
     * Add a variable for being transported by this data channel.
     *
     * @param name the name of the variable to add
     */
    void add(String name);

    /**
     *
     * Remove a variable from the the transported variables.
     *
     * @param prototype the prototype of the variable to remove
     */
    void remove(IPrototype prototype);
    
    /**
     * 
     * Remove a variable from the the transported variables.
     * 
     * @param name the name of the variable to remove
     */
    void remove(String name);

    /**
     *
     * Get the set of data of that will actually be transmitted as input to the
     * ending task capsule. This is computed by intersecting the set of variable
     * names transported by this data channel and the set of input of the ending
     * task.
     *
     * @return the transmitted data
     * @throws InternalProcessingError if something goes wrong because of a system failure
     * @throws UserBadDataError if something goes wrong because it is missconfigured, for instance the ending capsule has no assigned task
     */
    Iterable<IData> getData() throws InternalProcessingError, UserBadDataError;

    void provides(IContext context, ITicket ticket, Set<String> toClone, IMoleExecution moleExecution) throws InternalProcessingError, UserBadDataError;
    IContext consums(IContext context, ITicket ticket, Set<String> toClonne, IMoleExecution moleExecution) throws InternalProcessingError;

}
