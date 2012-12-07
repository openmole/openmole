/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.ide.core.implementation.execution

import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace.Workspace

object PasswordListner {

  lazy val apply = {
    EventDispatcher.listen(Workspace.instance, new PasswordListener, Workspace.PasswordRequired.getClass)
    Unit
  }
}

class PasswordListener extends EventListener[Workspace] {
  override def triggered(obj: Workspace, event: Event[Workspace]): Unit = {
    event match {
      case Workspace.PasswordRequired ⇒
        try {
          val dd = new DialogDescriptor(PasswordDialog.panel.peer, "Preferences access")
          val result = DialogDisplayer.getDefault.notify(dd)
          if (result == NotifyDescriptor.OK_OPTION) PasswordDialog.ok(true)
          else PasswordDialog.ok(false)
        } catch {
          case e: UserBadDataError ⇒ StatusBar().warn("The preference password is not set. All the actions requiring encrypted data are unvailable")
          case _: Throwable ⇒
        }
      case _ ⇒
    }

  }
}
