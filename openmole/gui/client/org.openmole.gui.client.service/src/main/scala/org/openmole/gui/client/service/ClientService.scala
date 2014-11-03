package org.openmole.gui.client.service

import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui._
import org.openmole.gui.ext.data._
import org.openmole.gui.shared.Api

import rx._
import autowire._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
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

sealed class UIFactories {
  val instance: Var[Map[Class[_], FactoryUI]] = Var(Map())

  def merge(m: (Class[_], FactoryUI)) = instance() = instance() ++ Map(m._1 -> m._2)
  def remove(dataClass: Class[_]) = instance() = instance().filterKeys(k ⇒ k != dataClass)
}

object ClientService {

  val uiFactories: Var[UIFactories] = Var(new UIFactories)
  def merge(m: (Class[_], FactoryUI)) = {
    println("Add UI factory " + m._2 + " for data class " + m._1.getName)
    uiFactories().merge(m)
  }
  def remove(dataClass: Class[_]) = uiFactories().remove(dataClass)

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

  private def dataUI(data: TaskData) = uiFactories().instance().get(data.getClass) match {
    case Some(f: TaskFactoryUI) ⇒ Try(f.dataUI)
    case _                      ⇒ failure(data)
  }

  private def dataUI(data: PrototypeData[_]) = uiFactories().instance().get(data.getClass) match {
    case Some(f: PrototypeFactoryUI) ⇒ Try(f.dataUI)
    case _                           ⇒ failure(data)
  }

  private def failure[T <: Data](data: T) = Failure(new Throwable("The data " + data.name + " cannot be recontructed on the server."))

}
