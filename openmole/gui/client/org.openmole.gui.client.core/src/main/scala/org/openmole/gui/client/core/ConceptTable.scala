package org.openmole.gui.client.core

import org.openmole.gui.ext.dataui.DataBagUI
import org.openmole.gui.client.service.ClientService._
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.Forms
import org.openmole.gui.misc.js.Forms._
import scalatags.JsDom.all._
import rx._

/*
 * Copyright (C) 13/01/15 // mathieu.leclaire@openmole.org
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
object ConceptTable {

  object ConceptFilter extends Enumeration {

    case class ConceptState(name: String) extends Val(name)

    val ALL = ConceptState("All")
    val TASKS = ConceptState("Tasks")
    val PROTOTYPES = ConceptState("Prototypes")
  }

}

import ConceptTable.ConceptFilter._

class ConceptTable(initDataBagUIs: Seq[DataBagUI]) {

  val dataBagUIs = Var(initDataBagUIs)

  val filter = Var(ALL)
  var nameFilter = Var("")

  def contains(db: DataBagUI) = db.name().contains(nameFilter())

  private val filters = Map[ConceptState, DataBagUI ⇒ Boolean](
    (ALL, contains),
    (TASKS, db ⇒ isTaskUI(db) && contains(db)),
    (PROTOTYPES, db ⇒ isPrototypeUI(db) && contains(db))
  //("Environments", isEnvironmentUI)
  )

  val view = Forms.table(
    caption("List of existing entities. Click on it to parametrize them"),
    thead( /*tr(
        th("Name", `class` := "col-md-8"),
        th("Type", `class` := "col-md-4")
      )*/ ),
    Rx {
      tbody(
        for (db ← dataBagUIs() if filters(filter())(db)) yield {
          tr(
            td(db.name())(`class` := "col-md-8"),
            td(Forms.label(db.dataUI().dataType, label_primary + "col-md-4"))
          )
        }
      )
    }
  ).render
}
