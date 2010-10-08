/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.mole;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author reuillon
 */
public interface ISubMoleExecution {
    public static final String finished = "finished";
    public static final String allJobsWaitingInGroup = "allJobsWaitingInGroup";

    ISubMoleExecution getParent();
    boolean isRoot();

    int getNbJobInProgess();
    void incNbJobInProgress() throws InternalProcessingError, UserBadDataError;
    void decNbJobInProgress() throws InternalProcessingError, UserBadDataError;
    void incNbJobInProgress(int val) throws InternalProcessingError, UserBadDataError;
    void decNbJobInProgress(int val) throws InternalProcessingError, UserBadDataError;

    void incNbJobWaitingInGroup() throws InternalProcessingError, UserBadDataError;
    void decNbJobWaitingInGroup();
    void decNbJobWaitingInGroup(int value);
}
