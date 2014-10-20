package org.openmole.gui.client.factoryui

/*
 * Copyright (C) 24/09/14 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.dataui._
import org.openmole.gui.ext.data._
import scala.collection.JavaConversions._
import scala.collection.mutable

import scala.util.{ Failure, Try }

object ClientFactories {

  implicit def dataSoDataUI(data: Seq[Data]): Seq[DataUI] = data.map { d ⇒ dataUI(d) }.filter { _.isSuccess }.map { _.get }

  /*implicit def dataUItoData(dataUI: DataUI): Option[Data] = dataUI match {
    case (t: TaskDataUI)         ⇒ Some(t.data)
    case (t: PrototypeDataUI[_]) ⇒ Some(t.data)
    case _                       ⇒ None
  }*/

  implicit def taskDataUItoTaskData(dataUI: TaskDataUI): TaskData = dataUI.data
  implicit def protoDataUItoProtoData(dataUI: PrototypeDataUI[_]): PrototypeData[_] = dataUI.data

  implicit def protoDataUISeqtoProtoDataSeq(s: Seq[PrototypeDataUI[_]]): Seq[PrototypeData[_]] = s.map { protoDataUItoProtoData }
  implicit def taskDataUISeqtoTaskDataSeq(s: Seq[TaskDataUI]): Seq[TaskData] = s.map { taskDataUItoTaskData }

  implicit def protoDataUIStringMapToProtoDataString(m: Map[PrototypeDataUI[_], String]): Map[PrototypeData[_], String] = m.map { case (dataUI: PrototypeDataUI[_], s) ⇒ dataUI.data -> s }.toMap

  lazy private val instance = new ClientFactories

  def add(dataClass: Class[_], factory: FactoryUI) = instance.factories.synchronized {
    println("Add Client" + dataClass)
    instance.factories += dataClass -> factory
  }

  def dataUI(data: Data): Try[DataUI] = instance.factories.synchronized {
    instance.factories.get(data.getClass()) match {
      case Some(f: TaskFactoryUI)      ⇒ Try(f.dataUI)
      case Some(f: PrototypeFactoryUI) ⇒ Try(f.dataUI)
      case _                           ⇒ Failure(new Throwable("The data " + data.name + " cannot be recontructed on the server."))
    }
  }

  def remove(dataClass: Class[_]) = instance.factories.synchronized {
    instance.factories -= dataClass
  }
}

class ClientFactories {
  val factories = new mutable.WeakHashMap[Class[_], FactoryUI]
}

trait FactoryUI

trait TaskFactoryUI extends FactoryUI {
  def dataUI: TaskDataUI
}

trait PrototypeFactoryUI extends FactoryUI {
  def dataUI: PrototypeDataUI[_]
}
