package org.openmole.gui.server.state

/*
 * Copyright (C) 21/07/14 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.core.workspace.Workspace

import slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

class GuiDB {

  //TODO: how to set GuiserverDB ?
  val db = Database.forURL("jdbc:h2:" + Workspace.file("GuiserverDB"), driver = "org.h2.Driver")

  /*def withSession { implicit s ⇒
    Project.instance.ddl.create
  }*/

  /*def createTables = withSession({
    List(Project, Session).filterNot {
      t ⇒
        MTable.getTables.list.exists(_.name.name == t.tableName)
    }.foreach {
      _.ddl.create
    }
  })*/
}
