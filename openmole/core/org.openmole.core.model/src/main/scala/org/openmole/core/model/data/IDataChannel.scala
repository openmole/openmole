/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.data


/**
 *
 * A data channel allow to transmit data between remotes task capsule (under some condition) within a mole.
 * Two capsules could be linked with a {@link IDataChannel} if:
 *      - they belong to the same mole,
 *      - there is no capsule with more than one input slot in a path between the two capsules.
 *
 * @author reuillon
 */
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.job.ITicket
import org.openmole.core.model.mole.IMoleExecution

trait IDataChannel {

    /**
     *
     * Get the capsule from which the data channel starts.
     *
     * @return the capsule from whitch the data channel starts
     */
    def start: IGenericCapsule


    /**
     *
     * Get the capsule to which the data channel ends.
     *
     * @return the capsule to which the data channel ends.
     */
    def end:  IGenericCapsule


    /**
     *
     * Get the name of the variable transported by this data channel.
     *
     * @return the name of the variable transported by this data channel.
     */
    def variableNames: Iterable[String]

   
    /**
     *
     * Get the set of data of that will actually be transmitted as input to the
     * ending task capsule. This is computed by intersecting the set of variable
     * names transported by this data channel and the set of input of the ending
     * task.
     *
     * @return the transmitted data
    */
    def data: Iterable[IData[_]]

    def provides(context: IContext, ticket: ITicket, toClone: Set[String], moleExecution: IMoleExecution)
    def consums(context: IContext, ticket: ITicket, moleExecution: IMoleExecution): (IContext, Set[String])

}
