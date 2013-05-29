/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.execution

import scala.swing.ScrollPane

object ExecutionSupport extends ScrollPane {
  def changeView(etp: ExecutionManager) = contents = etp
}

