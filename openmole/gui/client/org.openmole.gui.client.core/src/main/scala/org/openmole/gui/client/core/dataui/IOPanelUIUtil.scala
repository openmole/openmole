package org.openmole.gui.client.core.dataui

import org.openmole.gui.client.core.{ PrototypeFactoryUI, ClientService }
import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import org.openmole.gui.misc.js.{ ClassKeyAggregator, InputFilter }
import org.scalajs.dom.html.TableCell

import scalatags.JsDom.all._
import scalatags.JsDom.{ tags }

/*
 * Copyright (C) 05/03/15 // mathieu.leclaire@openmole.org
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

object IOPanelUIUtil {

  def filtered(ifilter: InputFilter, dataUI: InOutputDataUI, mappingsFactory: IOMappingsFactory) = ClientService.prototypeDataBagUIs.map {
    p ⇒ inoutputUI(p, mappingsFactory)
  }.filter { i ⇒
    ifilter.contains(i.protoDataBagUI.name()) &&
      !ifilter.nameFilter().isEmpty &&
      !dataUI.exists(i)
  }

  def prototypeHeaderSequence: List[String] = List("Name", "Type", "Dim")

  def buildHeaders(headers: Seq[String]) = thead(
    tags.tr(
      for (k ← headers) yield {
        tags.th(k)
      }
    )
  )

  def buildProto(name: String) = {
    val newProto = DataBagUI.prototype(PrototypeFactoryUI.doubleFactory, name)
    ClientService += newProto
    newProto
  }

  def buildPrototypeTableView(io: InOutputUI, todo: () ⇒ Unit) = Seq(
    clickablePrototypeTD(io.protoDataBagUI, todo),
    labelTD(io.protoDataBagUI.dataUI().dataType, label_primary),
    basicTD(io.protoDataBagUI.dataUI().dimension().toString)
  ) ++ mappingsTD(io)

  def clickablePrototypeTD(p: PrototypeDataBagUI, todo: () ⇒ Unit) = bs.td(col_md_2)(
    a(p.name(),
      cursor := "pointer",
      onclick := { () ⇒
        todo()
      })).render

  def emptyTD(nb: Int) = for (i ← (0 to nb - 1)) yield {
    bs.td(col_md_1)("").render
  }

  def labelTD(s: String, labelType: ClassKeyAggregator) = bs.td(col_md_1)(bs.label(s, labelType)).render

  def basicTD(s: String) = bs.td(col_md_1)(tags.span(s)).render

  def mappingsTD(i: InOutputUI) = {
    for (
      f ← i.mappings().fields.map { f ⇒
        f.panelUI
      }
    ) yield {
      tags.td(f.view).render
    }
  }

  def delButtonTD(todo: () ⇒ Unit) = bs.td(col_md_1)(bs.button(glyph(glyph_minus))(onclick := { () ⇒
    todo()
  }
  )).render

  def coloredTR(tds: Seq[TableCell], filter: () ⇒ Boolean, click: () ⇒ Unit = () ⇒ {}) = {
    bs.tr(
      if (filter()) warning
      else nothing
    )(if (filter()) {
        Seq(cursor := "pointer",
          onclick := { () ⇒ click() }
        )
      }
      else {
        Seq(cursor := "default")
      }, tds)
  }

  def saveInOutputsUI(inouts: Seq[InOutputUI]) = {
    inouts.map {
      _.mappings().fields.map {
        _.panelUI.save
      }
    }
  }

}
