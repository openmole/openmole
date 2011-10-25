/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.workspace.Workspace
import scala.swing.ScrollPane

object ExecutionSupport extends ScrollPane{
println("Executino Support constructor ")
  EventDispatcher.listen(Workspace.instance, PasswordListener , classOf[Workspace.PasswordRequired])
  def changeView(etp: ExecutionManager) = contents= etp
}
