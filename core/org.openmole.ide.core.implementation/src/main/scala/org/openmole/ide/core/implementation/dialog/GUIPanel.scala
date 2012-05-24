/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.dialog

import scala.swing._
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.core.implementation.panel.ComponentCategories
import org.openmole.ide.core.implementation.panel.ConceptMenu
import java.awt.BorderLayout
import org.openmole.ide.core.implementation.execution.PasswordListner
import org.openide.DialogDescriptor
import org.openide.DialogDescriptor._
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openide.NotifyDescriptor._
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.preference.PreferenceContent
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.action.LoadXML
import org.openmole.ide.core.implementation.action.SaveXML
import org.openmole.ide.core.implementation.dataproxy.Proxys

class GUIPanel extends MainFrame { mainframe ⇒
  title = "OpenMOLE"

  menuBar = new MenuBar {
    contents += new Menu("File") {
      contents += new MenuItem(new Action("New Mole") {
        override def apply = DialogFactory.newTabName
      })

      contents += new MenuItem(new Action("Load") {
        override def apply = {
          Proxys.clearAll
          mainframe.title = "OpenMOLE - " + LoadXML.show
        }
      })

      contents += new MenuItem(new Action("Save") {
        override def apply = {
          ScenesManager.saveCurrentPropertyWidget
          SaveXML.save(mainframe)
        }
      })

      contents += new MenuItem(new Action("Save as") {
        override def apply = SaveXML.save(mainframe, SaveXML.show.getOrElse(""))
      })

      contents += new MenuItem(new Action("Reset all") {
        override def apply = {
          ScenesManager.closeAll
          Proxys.clearAll
          mainframe.title = "OpenMOLE"
        }
      })
    }

    contents += new Menu("Tools") {
      contents += new MenuItem(new Action("Preferences") {
        override def apply = {
          val pc = new PreferenceContent
          val dd = new DialogDescriptor(pc.peer, "Preferences")
          dd.setOptions(List(OK_OPTION).toArray)
          if (DialogDisplayer.getDefault.notify(dd).equals(OK_OPTION)) pc.save
        }
      })
    }
  }

  peer.setLayout(new BorderLayout)

  peer.add((new MigPanel("") {
    contents += ConceptMenu.prototypeMenu
    contents += ConceptMenu.taskMenu
    contents += ConceptMenu.samplingMenu
    contents += ConceptMenu.environmentMenu
  }).peer, BorderLayout.NORTH)

  peer.add((ScenesManager.tabPane).peer, BorderLayout.CENTER)

  peer.add((StatusBar).peer, BorderLayout.SOUTH)
  StatusBar.inform("OpenMOLE - 0.5 - Boundless Bamboo")

  PasswordListner.apply
}
