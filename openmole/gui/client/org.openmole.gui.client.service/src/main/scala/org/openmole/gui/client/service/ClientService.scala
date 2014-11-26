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

  private def factories[T <: DataUI] = {
    uiFactories.values.filter { f ⇒
      f.dataUI.isInstanceOf[T]
    }.toSeq
  }

  def taskFactories = factories[TaskDataUI]
  def prototypeFactories = factories[PrototypeDataUI[_]]

  implicit def dataToDataClassName(d: Data) = d.getClass.getCanonicalName

  implicit def tryToT[T](t: Try[T]): T = t match {
    case Success(s) ⇒ s
    case Failure(f) ⇒ throw (f)
  }

  implicit def taskDataUItoTaskData(dataUI: TaskDataUI): TaskData = dataUI.data

  implicit def taskDataToTaskDataUI(data: TaskData): TaskDataUI = dataUI(data)

  implicit def taskDataUISeqtoTaskDataSeq(s: Seq[TaskDataUI]): Seq[TaskData] = s.map {
    taskDataUItoTaskData
  }

  implicit def protoDataUItoProtoData(dataUI: PrototypeDataUI[_]): PrototypeData[_] = dataUI.data

  implicit def prototypeDataToPrototypeDataUI(data: PrototypeData[_]): PrototypeDataUI[_] = dataUI(data)

  implicit def protoDataUISeqtoProtoDataSeq(s: Seq[PrototypeDataUI[_]]): Seq[PrototypeData[_]] = s.map {
    protoDataUItoProtoData
  }

  implicit def protoDataUIStringMapToProtoDataString(m: Map[PrototypeDataUI[_], String]): Map[PrototypeData[_], String] = m.map { case (dataUI: PrototypeDataUI[_], s) ⇒ dataUI.data -> s }.toMap

  private def factoryUI(data: Data) = uiFactories.get(data.getClass.getCanonicalName) match {
    case Some(f: FactoryUI) ⇒ Try(f.dataUI)
    case _                  ⇒ failure(data)
  }

  private def dataUI(data: TaskData): TaskDataUI = factoryUI(data) match {
    case Success(d: TaskDataUI) ⇒ d
    case _                      ⇒ failure(data)
  }

  private def dataUI(data: PrototypeData[_]): PrototypeDataUI[_] = factoryUI(data) match {
    case Success(d: PrototypeDataUI[_]) ⇒ d
    case _                              ⇒ failure(data)
  }

  private def failure[T <: Data](data: T) = Failure(new Throwable("The data " + data.name + " cannot be recontructed on the server."))

}

trait ServiceFlag //FIXME: pour avoir une class pour bootstrapJS