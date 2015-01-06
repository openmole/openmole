package org.openmole.gui.client.service

import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui._
import org.openmole.gui.ext.data._
import org.openmole.gui.shared.Api

import rx._
import autowire._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.annotation.JSExport
import scala.util.{ Failure, Success, Try }
import scala.scalajs._
import reflect.runtime.universe._

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
  val factoryMap = js.Dictionary.empty[FactoryUI]
}

object ClientService {

  private val uiFactories = UIFactories.factoryMap.toMap
  private val uiData: Var[Seq[Var[DataUI]]] = Var(Seq())

  def taskFactories = uiFactories.values.flatMap { f ⇒
    f.dataUI match {
      case t: TaskDataUI ⇒ Some(f.asInstanceOf[IOFactoryUI])
      case _             ⇒ None
    }
  }.toSeq

  def prototypeFactories = uiFactories.values.flatMap { f ⇒
    f.dataUI match {
      case t: PrototypeDataUI ⇒ Some(f)
      case _                  ⇒ None
    }
  }.toSeq

  def taskDataUIs: Seq[TaskDataUI] = uiData().map {
    _()
  }.flatMap {
    _ match {
      case t: TaskDataUI ⇒ Some(t)
      case _             ⇒ None
    }
  }.toSeq

  def prototypeDataUIs = uiData().map {
    _()
  }.flatMap {
    _ match {
      case t: PrototypeDataUI ⇒ Some(t)
      case _                  ⇒ None
    }
  }.toSeq

  def +=(dataUI: DataUI) = find(dataUI) match {
    case Some(d: Var[_]) ⇒
      d() = dataUI
      println("SOME ")

    case _ ⇒
      println("NONE, new one ")
      uiData() = Var(dataUI) +: uiData()
  }

  def -=(dataUI: Var[DataUI]) = {
    println("DIFF " + { uiData() diff Seq(dataUI) })
    uiData() = uiData() diff Seq(dataUI)
  }

  def find(dataUI: DataUI) = uiData().find(d ⇒ {
    println("FIND " + d().getClass + " VS " + dataUI.getClass + " and " + d().name() + " VS " + dataUI.name())
    d().getClass == dataUI.getClass && d().name() == dataUI.name()
  })

  def taskData = taskDataUIs.map {
    _.data
  }

  def prototypeData = prototypeDataUIs.map {
    _.data
  }

  implicit def dataToDataClassName(d: Data) = d.getClass.getCanonicalName

  implicit def tryToT[T](t: Try[T]): T = t match {
    case Success(s) ⇒ s
    case Failure(f) ⇒ throw (f)
  }

  private def factoryUI(data: Data) = uiFactories.get(data.getClass.getCanonicalName) match {
    case Some(f: FactoryUI) ⇒ Try(f.dataUI)
    case _                  ⇒ failure(data)
  }

  private def dataUI(data: TaskData): TaskDataUI = factoryUI(data) match {
    case Success(d: TaskDataUI) ⇒ d
    case _                      ⇒ failure(data)
  }

  private def dataUI(data: PrototypeData): PrototypeDataUI = factoryUI(data) match {
    case Success(d: PrototypeDataUI) ⇒ d
    case _                           ⇒ failure(data)
  }

  private def failure[T <: Data](data: T) = Failure(new Throwable("The data " + data.name + " cannot be recontructed on the server."))

  //Implicit converters for pluging handling convinience

  implicit def stringVarToString(s: Var[String]): String = s()

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

  implicit def outputsConv(s: OutputsUI): Outputs = s().map { t ⇒ t().data }.toSeq

  implicit def inputsConv(s: InputsUI): Inputs = s().map { t ⇒ t() }.map {
    case (pUI, opt) ⇒
      (pUI.data, opt)
  }

  implicit def dataUIToFactoryUI(d: DataUI): Option[FactoryUI] = uiFactories.values.find {
    _.dataUI == d
  }

}