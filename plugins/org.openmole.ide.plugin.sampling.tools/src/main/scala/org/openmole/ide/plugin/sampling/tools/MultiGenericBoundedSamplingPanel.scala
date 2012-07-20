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
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.data.IBoundedDomainDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.IData
import org.openmole.ide.misc.widget.multirow.IFactory
import org.openmole.ide.misc.widget.multirow.IPanel
import org.openmole.ide.misc.widget.multirow.MultiPanel
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import scala.collection.mutable.HashMap
import scala.swing.BorderPanel.Position._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.collection.JavaConversions._

object MultiGenericBoundedSamplingPanel {

  class GenericBoundedSamplingPanel(protoContent: List[IPrototypeDataProxyUI],
                                    boundedDomainContent: List[IBoundedDomainDataProxyUI],
                                    data: GenericBoundedSamplingData) extends PluginPanel("wrap")
      with IPanel[GenericBoundedSamplingData] {

    val protoComboBox = new MyComboBox(protoContent)
    val boundedDomainComboBox = new MyComboBox(boundedDomainContent.map { _.toString })
    var dPanel = data.boundedDomainDataUI.getOrElse { boundedDomainContent(0).dataUI }.buildPanelUI

    data.prototypeProxy match {
      case Some(x: IPrototypeDataProxyUI) ⇒ protoComboBox.selection.item = x
      case _ ⇒
    }

    data.boundedDomainProxy match {
      case Some(x: String) ⇒
        boundedDomainComboBox.selection.item = x
      case _ ⇒
    }

    contents += new PluginPanel("wrap 3") {
      contents += protoComboBox
      contents += new Label("defined on ")
      contents += boundedDomainComboBox
    }
    contents += dPanel.peer

    boundedDomainComboBox.selection.reactions += {
      case SelectionChanged(`boundedDomainComboBox`) ⇒
        if (contents.size == 2) contents.remove(1)
        dPanel = boundedDomainContent.filter { it ⇒
          boundedDomainComboBox.selection.item == it.toString
        }.head.dataUI.buildPanelUI
        contents += dPanel.peer
    }

    def content = {
      new GenericBoundedSamplingData(Some(protoComboBox.selection.item),
        Some(boundedDomainComboBox.selection.item),
        Some(dPanel.saveContent("")))
    }
  }

  class GenericBoundedSamplingData(val prototypeProxy: Option[IPrototypeDataProxyUI] = None,
                                   val boundedDomainProxy: Option[String] = None,
                                   val boundedDomainDataUI: Option[IBoundedDomainDataUI] = None) extends IData

  class GenericBoundedSamplingFactory(protoContent: List[IPrototypeDataProxyUI],
                                      boundedDomainContent: List[IBoundedDomainDataProxyUI]) extends IFactory[GenericBoundedSamplingData] {
    def apply = new GenericBoundedSamplingPanel(protoContent,
      boundedDomainContent,
      new GenericBoundedSamplingData)
  }

}
import MultiGenericBoundedSamplingPanel._
class MultiGenericBoundedSamplingPanel(protoContent: List[IPrototypeDataProxyUI] = List.empty,
                                       boundedDomainContent: List[IBoundedDomainDataProxyUI] = List.empty,
                                       initPanels: List[GenericBoundedSamplingPanel]) extends MultiPanel("Factors",
  new GenericBoundedSamplingFactory(protoContent, boundedDomainContent),
  initPanels) {
}

//
//object MultiGenericBoundedSamplingPanel {
//
//  class GenericBoundedSamplingPanel(protoContent: List[IPrototypeDataProxyUI],
//                                    BoundedDomainContent: List[IBoundedBoundedDomainDataProxyUI],
//                                    data: GenericBoundedSamplingData) extends PluginPanel("wrap")
//      with IPanel[GenericBoundedSamplingData] {
//
//    var dPanel = BoundedDomainPanel(data.BoundedDomainProxy)
//
//    val protoComboBox = new MyComboBox(protoContent)
//    val BoundedDomainComboBox = new MyComboBox(BoundedDomainContent)
//    BoundedDomainComboBox.selection.reactions += {
//      case SelectionChanged(`BoundedDomainComboBox`) ⇒
//        contents.remove(1)
//        dPanel = BoundedDomainPanel(Some(BoundedDomainComboBox.selection.item))
//        contents += dPanel.peer
//    }
//
//    contents += new PluginPanel("wrap 3") {
//      contents += protoComboBox
//      contents += new Label("defined on ")
//      contents += BoundedDomainComboBox
//    }
//    contents += dPanel.peer
//
//    def BoundedDomainPanel(BoundedDomainProxy: Option[IBoundedBoundedDomainDataProxyUI]) =
//      BoundedDomainProxy.getOrElse(BoundedDomainContent(0)).dataUI.buildPanelUI
//
//    def content = new GenericBoundedSamplingData(Some(protoComboBox.selection.item),
//      Some(BoundedDomainComboBox.selection.item),
//      Some(dPanel.saveContent("")))
//  }
//
//  class GenericBoundedSamplingData(val prototypeProxy: Option[IPrototypeDataProxyUI] = None,
//                                   val BoundedDomainProxy: Option[IBoundedBoundedDomainDataProxyUI] = None,
//                                   val BoundedDomainDataUI: Option[IBoundedBoundedDomainDataUI] = None) extends IData
//
//  class GenericBoundedSamplingFactory(protoContent: List[IPrototypeDataProxyUI],
//                                      BoundedDomainContent: List[IBoundedBoundedDomainDataProxyUI]) extends IFactory[GenericBoundedSamplingData] {
//    def apply = new GenericBoundedSamplingPanel(protoContent,
//      BoundedDomainContent,
//      new GenericBoundedSamplingData)
//  }
//
//}
//import MultiGenericBoundedSamplingPanel._
//class MultiGenericBoundedSamplingPanel(protoContent: List[IPrototypeDataProxyUI] = List.empty,
//                                       BoundedDomainContent: List[IBoundedBoundedDomainDataProxyUI] = List.empty,
//                                       initPanels: List[GenericBoundedSamplingPanel]) extends MultiPanel("Factors",
//  new GenericBoundedSamplingFactory(protoContent, BoundedDomainContent),
//  initPanels) {
//
//  //var BoundedDomainCombos: Option[MultiTwoCombos[IPrototypeDataProxyUI, String]] = None
//  //var rowMap = new HashMap[GenericBoundedSamplingPanel, IBoundedDomainPanelUI]
//  //var extMap = new HashMap[GenericBoundedSamplingPanel, IBoundedDomainDataUI]
//
//}

//object GenericBoundedBoundedSamplingPanel {
//  def rowFactory(csPanel: GenericBoundedBoundedSamplingPanel) = new Factory[IPrototypeDataProxyUI, String] {
//    override def apply(row: TwoCombosRowWidget[IPrototypeDataProxyUI, String], p: MyPanel) = {
//      import row._
//      val twocombrow: TwoCombosRowWidget[IPrototypeDataProxyUI, String] =
//        new TwoCombosRowWidget(comboContentA, selectedA, comboContentB, selectedB, inBetweenString, plus)
//      val protoObject = selectedA.dataUI.coreObject
//      if (csPanel.extMap.contains(row)) csPanel.addRow(twocombrow, csPanel.extMap(row), protoObject) else csPanel.addRow(twocombrow, protoObject)
//      twocombrow.combo2.selection.reactions += {
//        case SelectionChanged(twocombrow.`combo2`) ⇒ csPanel.addRow(twocombrow, protoObject)
//      }
//      twocombrow
//    }
//  }
//}
//
//import GenericBoundedBoundedSamplingPanel._
//class GenericBoundedBoundedSamplingPanel(val ifactors: List[(IPrototypeDataProxyUI, String, IBoundedBoundedDomainDataUI)] = List.empty,
//                                  val BoundedDomains: List[String]) extends PluginPanel("wrap 2") {
//
//  var BoundedDomainCombos: Option[MultiTwoCombos[IPrototypeDataProxyUI, String]] = None
//  var rowMap = new HashMap[TwoCombosRowWidget[IPrototypeDataProxyUI, String], IBoundedBoundedDomainPanelUI]
//  var extMap = new HashMap[TwoCombosRowWidget[IPrototypeDataProxyUI, String], IBoundedBoundedDomainDataUI]
//
//  if (!Proxys.prototypes.isEmpty) {
//    rowFactory(this)
//    val protos = Proxys.prototypes.filter { _.dataUI.coreObject.`type`.erasure == classOf[Double] }.toList
//    val csrs = if (ifactors.size > 0) ifactors.map { f ⇒
//      val rw = new TwoCombosRowWidget(protos, f._1, BoundedDomains, f._2, "defined on ", ADD)
//      extMap += rw -> f._3
//      rw
//    }
//    else {
//      List(new TwoCombosRowWidget(protos, protos(0), BoundedDomains, BoundedDomains(0), "defined on ", ADD))
//    }
//
//    BoundedDomainCombos = Some(new MultiTwoCombos[IPrototypeDataProxyUI, String]("Factors",
//      csrs,
//      rowFactory(this),
//      CLOSE_IF_EMPTY,
//      ADD,
//      true))
//
//    contents += BoundedDomainCombos.get.panel
//  }
//
//  def factors = BoundedDomainCombos match {
//    case x: Some[MultiTwoCombos[IPrototypeDataProxyUI, String]] ⇒ x.get.rowWidgets.map {
//      r ⇒ (r.content._1, r.content._2, rowMap(r).saveContent(""))
//    }.toList
//    case _ ⇒ List[(IPrototypeDataProxyUI, String, IBoundedBoundedDomainDataUI)]()
//  }
//
//  def addRow(twocombrow: TwoCombosRowWidget[IPrototypeDataProxyUI, String], dd: IBoundedBoundedDomainDataUI, p: IPrototype[_]): Unit = {
//    rowMap += twocombrow -> dd.buildPanelUI
//    twocombrow.panel.extend(rowMap(twocombrow).peer)
//  }
//
//  def addRow(twocombrow: TwoCombosRowWidget[IPrototypeDataProxyUI, String], p: IPrototype[_]): Unit =
//    addRow(twocombrow, BoundedBoundedDomainDataProxyFactory.factoryByName(twocombrow.combo2.selection.item).buildDataProxyUI.dataUI, p)
//
//}