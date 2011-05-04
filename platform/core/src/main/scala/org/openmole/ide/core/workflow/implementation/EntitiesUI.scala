/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation
import org.openmole.ide.core.commons.Constants

object EntitiesUI {

  var entities = Map(Constants.TASK -> new ContainerUI,
                     Constants.PROTOTYPE -> new ContainerUI,
                     Constants.SAMPLING -> new ContainerUI,
                     Constants.ENVIRONMENT -> new ContainerUI)
}
