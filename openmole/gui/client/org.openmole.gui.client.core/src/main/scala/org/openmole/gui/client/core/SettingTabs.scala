package org.openmole.gui.client.core

import org.openmole.gui.client.core.dataui._
import org.openmole.gui.ext.dataui._

import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.misc.js.JsRxTags._
import rx._

/*
 * Copyright (C) 28/01/15 // mathieu.leclaire@openmole.org
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

object SettingTabs {

  object SettingTab extends Enumeration {

    case class SettingTab(val name: String, val panelUIs: Seq[PanelUI], val id: String = getID) {
      def save = panelUIs.map {
        _.save
      }
    }

    def ioTab(name: String, panelUIs: Seq[PanelUI]) = SettingTab(name, panelUIs)

    def inputTab(panelUIs: Seq[PanelUI]) = SettingTab("Inputs", panelUIs)

    def outputTab(panelUIs: Seq[PanelUI]) = SettingTab("Outputs", panelUIs)

    def environmentTab(panelUIs: Seq[PanelUI]) = SettingTab("Environment", panelUIs)

    def prototypeTab(panelUIs: Seq[PanelUI]) = SettingTab("Prototypes", panelUIs)

  }

  import SettingTab._

  def apply(panel: GenericPanel, db: DataBagUI): SettingTabs = new SettingTabs(db.dataUI() match {
    case io: IODataUI ⇒
      val name = io match {
        case h: HookDataUI ⇒ "Hooks"
        case _             ⇒ "Settings"
      }
      Seq(
        ioTab(name, Seq(io.panelUI)),
        inputTab(Seq(io.inputDataUI().panelUI(panel))), //new InputPanelUI(panel, t.inputDataUI()))),
        outputTab(Seq(io.outputDataUI().panelUI(panel)))
      )
    case e: EnvironmentDataUI ⇒ Seq(environmentTab(Seq(e.panelUI)))

    case _                    ⇒ Seq()

  }
  )

  def apply(dataUI: CapsuleDataUI): SettingTabs = new SettingTabs(Seq(
    dataUI.dataUI.map {
      d ⇒ ioTab("Settings", Seq(d.panelUI))
    },
    dataUI.inputDataUI.map {
      i ⇒ inputTab(Seq(i.panelUI))
    },
    dataUI.outputDataUI.map {
      o ⇒ outputTab(Seq(o.panelUI))
    },
    dataUI.environment.map {
      env ⇒ environmentTab(Seq(env.panelUI))
    },
    Some(ioTab("Hooks", dataUI.hooks.map {
      _.panelUI
    }))).flatten)
}

import SettingTabs.SettingTab._

class SettingTabs(tabs: Seq[SettingTab]) {
  val currentTab = Var(tabs.headOption)

  val view = tags.div(
    Rx {
      bs.nav("settingsNav", nav_pills,
        (for (c ← tabs) yield {
          navItem(c.id, c.name, () ⇒ { currentTab() = Some(c) }, currentTab() == Some(c))
        }): _*
      )
    }, Rx {
      tags.div(currentTab().map { t: SettingTab ⇒
        for (el ← t.panelUIs) yield {
          tags.div(el.view)
        }
      }.getOrElse(Seq()).toSeq: _*
      )
    }
  )

  def set(index: Int) = currentTab() = Some(tabs(index))

  def save = tabs.map {
    _.save
  }

}
