package org.openmole.gui.client.core

import org.openmole.gui.client.core.dataui._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.dataui._
import rx._

import scala.scalajs._
import scala.scalajs.js.annotation.JSExport
import scala.util.{ Failure, Success, Try }

/*
 * Copyright (C) 30/10/14 // mathieu.leclaire@openmole.org
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

@JSExport("UIFactories")
object UIFactories {

  @JSExport
  val factoryMap = js.Dictionary.empty[FactoryWithDataUI]

  @JSExport
  val authenticationMap = js.Dictionary.empty[AuthenticationFactoryUI]
}

object ClientService {

  private val uiFactories = Var(UIFactories.factoryMap.toMap)
  private val authenticationFactoryMap = Var(UIFactories.authenticationMap.toMap)
  private val uiDataBags: Var[Seq[DataBagUI]] = Var(Seq())

  // Factories
  def taskFactories = uiFactories().values.filter { f ⇒ isTaskUI(f.dataUI) }.toSeq

  def prototypeFactories: Seq[FactoryWithDataUI] = uiFactories().values.filter { f ⇒ isPrototypeUI(f.dataUI) }.toSeq

  def authenticationFactories: Seq[AuthenticationFactoryUI] = authenticationFactoryMap().values.toSeq

  def factories = uiFactories().values.toSeq

  //DataBagUIs
  def taskDataBagUIs: Seq[TaskDataBagUI] = uiDataBags().filter {
    isTaskUI
  }.map(_.asInstanceOf[TaskDataBagUI])

  def prototypeDataBagUIs: Seq[PrototypeDataBagUI] = uiDataBags().filter {
    isPrototypeUI
  }.map(_.asInstanceOf[PrototypeDataBagUI])

  def dataBagUIs = uiDataBags()

  def setName(db: DataBagUI, name: String) = get(db).map {
    _.name() = name
  }

  private def get(db: DataBagUI) = uiDataBags().find(_.uuid == db.uuid)

  def isTaskUI(dataUI: DataUI) = {
    dataUI match {
      case t: TaskDataUI ⇒ true
      case _             ⇒ false
    }
  }

  def isPrototypeUI(d: DataUI) = {
    d match {
      case t: PrototypeDataUI ⇒ true
      case _                  ⇒ false
    }
  }

  def panelUI(d: AuthenticationData): PanelUI = {
    val f = authenticationUI(d)
    f.panelUI(d.asInstanceOf[f.DATA])
  }

  def isPrototypeUI(db: DataBagUI): Boolean = isPrototypeUI(db.dataUI())

  def isTaskUI(db: DataBagUI): Boolean = isTaskUI(db.dataUI())

  def isPrototypeUI(f: FactoryWithDataUI): Boolean = isPrototypeUI(f.dataUI)

  def isTaskUI(f: FactoryWithDataUI): Boolean = isTaskUI(f.dataUI)

  def prototypeUI(db: DataBagUI): Option[PrototypeDataUI] = {
    db.dataUI() match {
      case p: PrototypeDataUI ⇒ Some(p)
      case _                  ⇒ None
    }
  }

  def +=(dataKey: String, factoryUI: FactoryWithDataUI) = uiFactories() += dataKey -> factoryUI

  def +=(dataBagUI: DataBagUI) = {
    if (!exists(dataBagUI))
      uiDataBags() = dataBagUI +: uiDataBags()
  }

  def -=(dataBagUI: DataBagUI) = uiDataBags() = uiDataBags().filter {
    _.uuid != dataBagUI.uuid
  }

  def existsPrototype(name: String) = prototypeDataBagUIs.exists(_.name() == name)

  def exists(dataBagUI: DataBagUI) = uiDataBags().exists(p ⇒
    p.uuid == dataBagUI.uuid
  )

  def taskData = taskDataBagUIs.map {
    _.dataUI().data
  }

  def prototypeData = prototypeDataBagUIs.map {
    _.dataUI().data
  }

  def authenticationUI(data: AuthenticationData) = authenticationFactoryMap()(data.getClass.getName)

  implicit def dataToDataClassName(d: Data) = d.getClass.getName

  implicit def tryToT[T](t: Try[T]): T = t match {
    case Success(s) ⇒ s
    case Failure(f) ⇒
      println("in failure " + f)
      throw (f)
  }

  private def _dataUI(data: Data) = {
    uiFactories().get(data.getClass.getName) match {
      case Some(f: FactoryWithDataUI) ⇒ Try(f.dataUI)
      case _                          ⇒ failure(data)
    }
  }

  private def dataUI(data: TaskData): TaskDataUI = {
    _dataUI(data) match {
      case Success(d: TaskDataUI) ⇒ d
      case _                      ⇒ failure(data)
    }
  }

  private def dataUI(data: PrototypeData): PrototypeDataUI = {
    _dataUI(data) match {
      case Success(d: PrototypeDataUI) ⇒ d
      case _                           ⇒ failure(data)
    }
  }

  private def failure[T <: Data](data: T) = Failure(new Throwable("The data " + data.getClass.getName + " cannot be recontructed on the server."))

  //Implicit converters for pluging handling convinience

  implicit def tVarToT[T](s: Var[T]): T = s()

  implicit def taskDataUItoTaskData(dataUI: TaskDataUI): TaskData = dataUI.data

  implicit def taskDataToTaskDataUI(data: TaskData): TaskDataUI = dataUI(data)

  implicit def taskDataUISeqtoTaskDataSeq(s: Seq[TaskDataUI]): Seq[TaskData] = s.map {
    taskDataUItoTaskData
  }

  implicit def protoDataUItoProtoData(dataUI: PrototypeDataUI): PrototypeData = dataUI.data

  implicit def prototypeDataToPrototypeDataUI(data: PrototypeData): PrototypeDataUI = dataUI(data)

  implicit def protoDataUISeqtoProtoDataSeq(s: Seq[PrototypeDataUI]): Seq[PrototypeData] = s.map {
    protoDataUItoProtoData
  }

  implicit def libConv(seq: Var[Seq[Var[String]]]): Seq[String] = seq().map { e ⇒ e() }

  implicit def dataUIToFactoryUI(d: DataUI): Option[FactoryWithDataUI] = uiFactories().values.find {
    _.dataUI == d
  }

  implicit def dataBagUIToFactoryUI(p: Option[DataBagUI]): Option[FactoryWithDataUI] = p flatMap { dataBagUI ⇒
    uiFactories().values.find { f ⇒
      f.dataUI.getClass == dataBagUI.dataUI().getClass
    }
  }

  implicit def dataBagsUIFromFactoryUI(f: FactoryWithDataUI): Seq[DataBagUI] = f.dataUI match {
    case t if isTaskUI(t)      ⇒ taskDataBagUIs
    case p if isPrototypeUI(p) ⇒ prototypeDataBagUIs
    case _                     ⇒ dataBagUIs
  }

}