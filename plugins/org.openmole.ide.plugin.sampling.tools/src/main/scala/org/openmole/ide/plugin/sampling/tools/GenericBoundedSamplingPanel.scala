/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.sampling.tools

import scala.swing._
import swing.Swing._
import scala.swing.event.SelectionChanged
import swing.ListView._
import org.openmole.ide.core.implementation.dataproxy.BoundedDomainDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.data.IBoundedDomainDataUI
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import scala.collection.mutable.HashMap
import scala.swing.BorderPanel.Position._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos.TwoCombosRowWidget
import scala.collection.JavaConversions._

object GenericBoundedSamplingPanel {
  def rowFactory(csPanel: GenericBoundedSamplingPanel) = new Factory[IPrototypeDataProxyUI,String] {
    override def apply(row: TwoCombosRowWidget[IPrototypeDataProxyUI,String], p: MyPanel) = {
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
  
import GenericBoundedSamplingPanel._
class GenericBoundedSamplingPanel(val ifactors: List[(IPrototypeDataProxyUI,String,IBoundedDomainDataUI)] = List.empty,
                                  val domains : List[String]) extends PluginPanel("wrap 2") {
  
  var domainCombos: Option[MultiTwoCombos[IPrototypeDataProxyUI,String]]= None
  var rowMap = new HashMap[TwoCombosRowWidget[IPrototypeDataProxyUI,String],IBoundedDomainPanelUI]
  var extMap = new HashMap[TwoCombosRowWidget[IPrototypeDataProxyUI,String],IBoundedDomainDataUI]
   
  if (!Proxys.prototypes.isEmpty){
    rowFactory(this)
    val protos = Proxys.prototypes.filter{_.dataUI.coreObject.`type`.erasure == classOf[Double]}.toList
    val csrs = if (ifactors.size > 0) ifactors.map{f=> 
      val rw = new TwoCombosRowWidget(protos,f._1,domains,f._2,"defined on ",ADD)
      extMap += rw->f._3
      rw}
    else {
      List(new TwoCombosRowWidget(protos,protos(0),domains, domains(0),"defined on ",ADD))
    }
    
    domainCombos = Some(new MultiTwoCombos[IPrototypeDataProxyUI,String]("Factors",
                                                                         csrs,
                                                                         rowFactory(this),
                                                                         CLOSE_IF_EMPTY,
                                                                         ADD,
                                                                         true))
    
    contents+= domainCombos.get.panel
  }
 
  def factors = domainCombos match {
    case x : Some[MultiTwoCombos[IPrototypeDataProxyUI,String]]=> x.get.rowWidgets.map{
        r=>(r.content._1,r.content._2,rowMap(r).saveContent(""))}.toList
    case _=> List[(IPrototypeDataProxyUI,String,IBoundedDomainDataUI)]()
  }                                     
  
  def addRow(twocombrow: TwoCombosRowWidget[IPrototypeDataProxyUI,String],dd: IBoundedDomainDataUI, p: IPrototype[_]) : Unit = {
    rowMap+= twocombrow ->  dd.buildPanelUI
    twocombrow.panel.extend(rowMap(twocombrow).peer) 
  }
  
  def addRow(twocombrow: TwoCombosRowWidget[IPrototypeDataProxyUI,String],  p: IPrototype[_]):Unit =
    addRow(twocombrow ,BoundedDomainDataProxyFactory.factoryByName(twocombrow.combo2.selection.item).buildDataProxyUI.dataUI,p)
  
}