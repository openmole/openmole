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

package org.openmole.ide.plugin.sampling.combine

import scala.swing._
import swing.Swing._
import scala.swing.event.SelectionChanged
import swing.ListView._
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openide.util.Lookup
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import scala.collection.mutable.HashMap
import scala.swing.BorderPanel.Position._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos.TwoCombosRowWidget
import scala.collection.JavaConversions._

object CompleteSamplingPanelUI{
  def rowFactory(csPanel: CompleteSamplingPanelUI) = new Factory[IPrototypeDataProxyUI,String] {
    override def apply(row: TwoCombosRowWidget[IPrototypeDataProxyUI,String], p: Panel) = {
      import row._
      val twocombrow: TwoCombosRowWidget[IPrototypeDataProxyUI,String] = 
        new TwoCombosRowWidget(comboContentA,selectedA,comboContentB,selectedB, inBetweenString,plus)
      val protoObject = selectedA.dataUI.coreObject
      if (csPanel.extMap.contains(row)) csPanel.addRow(twocombrow, csPanel.extMap(row),protoObject) else csPanel.addRow(twocombrow,protoObject)
      twocombrow.combo2.selection.reactions += {
        case SelectionChanged(twocombrow.`combo2`)=>csPanel.addRow(twocombrow,protoObject)
      }
      twocombrow
    }
  }
}

class CompleteSamplingPanelUI(cud: CompleteSamplingDataUI) extends MigPanel("wrap 2","","") with ISamplingPanelUI {
  var sampleDomainCombos: Option[MultiTwoCombos[IPrototypeDataProxyUI,String]]= None
  var rowMap = new HashMap[TwoCombosRowWidget[IPrototypeDataProxyUI,String],IDomainPanelUI]
  var extMap = new HashMap[TwoCombosRowWidget[IPrototypeDataProxyUI,String],IDomainDataUI]
   
  import CompleteSamplingPanelUI._
  if (!Proxys.prototypes.isEmpty){
    rowFactory(this)
    val protos = Proxys.prototypes.toList
    val domains = Lookup.getDefault.lookupAll(classOf[IDomainFactoryUI]).toList.map{_.displayName}
    val csrs = if (cud.factors.size>0) cud.factors.map{f=> 
      val rw = new TwoCombosRowWidget(protos,f._1,domains,f._2,"defined on ",ADD)
      extMap += rw->f._3
      rw}
    else {
      List(new TwoCombosRowWidget(protos,protos(0),domains, domains(0),"defined on ",ADD))
    }
    
    sampleDomainCombos = Some(new MultiTwoCombos[IPrototypeDataProxyUI,String]("Factors",
                                                                               csrs,
                                                                               rowFactory(this)))
    
    contents+= sampleDomainCombos.get.panel
  }
 

  def addRow(twocombrow: TwoCombosRowWidget[IPrototypeDataProxyUI,String],dd: IDomainDataUI, p: IPrototype[_]) : Unit = {
    rowMap+= twocombrow ->  dd.buildPanelUI
    twocombrow.panel.extend(rowMap(twocombrow).peer) 
  }
  
  def addRow(twocombrow: TwoCombosRowWidget[IPrototypeDataProxyUI,String],  p: IPrototype[_]):Unit =
    addRow(twocombrow ,DomainDataProxyFactory.factoryByName(twocombrow.combo2.selection.item).buildDataProxyUI.dataUI,p)
  
  override def saveContent(name: String) = sampleDomainCombos match {
    case x:Some[MultiTwoCombos[IPrototypeDataProxyUI,String]]=> new CompleteSamplingDataUI(name,x.get.rowWidgets.map(r=>
          (r.content._1,r.content._2,rowMap(r).saveContent(""))).toList)
    case _=> new CompleteSamplingDataUI(name,List[(IPrototypeDataProxyUI,String,IDomainDataUI)]())
  }
                                         
}
