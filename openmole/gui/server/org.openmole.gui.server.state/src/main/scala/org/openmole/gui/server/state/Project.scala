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

import slick.driver.H2Driver.simple._
import scala.slick.lifted.TableQuery

class Project(tag: Tag) extends Table[(String, String, String)](tag, "Project") {
  def id = column[String]("PROJECTID", O.PrimaryKey)
  def name = column[String]("PROJECTNAME")
  def description = column[String]("DESCRIPTION")

  def * = (id, name, description)
}

object Project {
  lazy val instance = TableQuery[Project]
}