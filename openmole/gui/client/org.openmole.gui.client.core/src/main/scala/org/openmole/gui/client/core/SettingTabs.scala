package org.openmole.gui.client.core

import org.openmole.gui.client.service.dataui._
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

    case class SettingTab(val name: String, val panelUIs: Seq[PanelUI], val defaultActive: Boolean = false, val id: String = getID) {
      def save = panelUIs.map {
        _.save
      }
    }

    def taskTab(panelUIs: Seq[PanelUI], default: Boolean = false) = SettingTab("Settings", panelUIs, default)

    def inputTab(panelUIs: Seq[PanelUI], default: Boolean = false) = SettingTab("Inputs", panelUIs, default)

    def outputTab(panelUIs: Seq[PanelUI], default: Boolean = false) = SettingTab("Outputs", panelUIs, default)

    def environmentTab(panelUIs: Seq[PanelUI], default: Boolean = false) = SettingTab("Environment", panelUIs, default)

    def hookTab(panelUIs: Seq[PanelUI], default: Boolean = false) = SettingTab("Hooks", panelUIs, default)

    def prototypeTab(panelUIs: Seq[PanelUI], default: Boolean = false) = SettingTab("Prototypes", panelUIs, default)

  }

  import SettingTab._

  def apply(db: DataBagUI): SettingTabs = db match {
    case dbio: IODataBagUI ⇒ new SettingTabs(dbio.dataUI() match {
      case t: TaskDataUI ⇒ taskTab(Seq(t.panelUI), true)
      case h: HookDataUI ⇒ hookTab(Seq(h.panelUI))
    },
      inputTab(Seq(dbio.inputDataUI().panelUI)),
      outputTab(Seq(dbio.outputDataUI().panelUI))
    )
    case db: DataBagUI ⇒ db.dataUI() match {
      case e: EnvironmentDataUI ⇒ new SettingTabs(environmentTab(Seq(e.panelUI)))
      case _                    ⇒ new SettingTabs
    }
  }

  def apply(dataUI: CapsuleDataUI): SettingTabs = new SettingTabs(Seq(
    dataUI.dataUI.map { d ⇒ taskTab(Seq(d.panelUI), true) },
    dataUI.inputDataUI.map { i ⇒ inputTab(Seq(i.panelUI)) },
    dataUI.outputDataUI.map { o ⇒ outputTab(Seq(o.panelUI)) },
    dataUI.environment.map { env ⇒ environmentTab(Seq(env.panelUI)) },
    Some(hookTab(dataUI.hooks.map { _.panelUI }))).flatten: _*)
}

import SettingTabs.SettingTab._

class SettingTabs(tabs: SettingTab*) {
  val currentTab = Var(tabs.headOption)

  val view = tags.div(
    bs.nav("settingsNav", nav_pills,
      (for (c ← tabs) yield {
        navItem(c.id, c.name, () ⇒ currentTab() = Some(c), c.defaultActive)
      }): _*
    ), Rx {
      tags.div(currentTab().map { t: SettingTab ⇒
        for (el ← t.panelUIs) yield {
          tags.div(el.view)
        }
      }.getOrElse(Seq()).toSeq: _*
      )
    }
  )

  def save = tabs.map { _.save }

}
