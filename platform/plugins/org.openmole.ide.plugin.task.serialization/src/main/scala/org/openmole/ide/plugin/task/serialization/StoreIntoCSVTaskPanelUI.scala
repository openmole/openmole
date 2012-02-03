/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.plugin.task.serialization

import java.io.File
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.MigPanel
import scala.collection.mutable.HashSet
import scala.swing._
import scala.swing.event.ButtonClicked
import swing.Swing._

class StoreIntoCSVTaskPanelUI(sdu: StoreIntoCSVTaskDataUI) extends MigPanel("wrap 2") with ITaskPanelUI{
  var columns = new HashSet[ColumnPanel]
  val loaded = sdu.columns.groupBy(_._1)
  val protoFileComboBox = new ComboBox(Proxys.prototype.filter(p=>p.dataUI.coreObject.`type`.erasure == classOf[File]).toList)
  if (sdu.protoFile.isDefined) protoFileComboBox.selection.item= sdu.protoFile.get
  Proxys.prototype.filter(_.dataUI.dim>0).foreach(columns+= buildColumn(_))
  contents+= new Label("File Prototype to be stored")
  contents+= protoFileComboBox
  
  def buildColumn(pud: IPrototypeDataProxyUI) = {
    val tf= new TextField(15){enabled = false}
    val cb = new CheckBox(pud.dataUI.displayName) {reactions+= {case ButtonClicked(cb) =>tf.enabled = selected}}
    contents+= (cb,"gap para")
    contents+= tf
    if (loaded.contains(pud)) {
      cb.selected = true
      tf.text = loaded(pud).head._2
      tf.enabled = true
    }
    new ColumnPanel(pud,tf)
  }
  
  override def saveContent(name: String) = {
    new StoreIntoCSVTaskDataUI(name,columns.flatMap {
      case co: ColumnPanel=> if (co.selected) List(co.column) else None
      case _=> None}.toList,Some(protoFileComboBox.selection.item))}
    
  class ColumnPanel(pud: IPrototypeDataProxyUI,tf: TextField){
    def selected = tf.enabled
    def column= (pud,if (tf.text == "") pud.dataUI.name else tf.text)
  }
}
