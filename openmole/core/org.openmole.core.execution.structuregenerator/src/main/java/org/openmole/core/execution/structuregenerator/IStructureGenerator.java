/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.execution.structuregenerator;

import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * @author reuillon
 */
public interface IStructureGenerator {
    Class generateClass(ComplexNode application) throws InternalProcessingError;
}
