/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.workflow.model.resource;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * Generic interface for all portable components.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IPortable {

    /**
     *
     * Get resources requiered by this portable component.
     * 
     * @return the requiered resources
     */
    Iterable<IResource> getResources() throws InternalProcessingError, UserBadDataError;
}
