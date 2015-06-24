package org.openmole.gui.server.core

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

import org.openmole.core.workflow.task.PluginSet

import org.openmole.gui.ext.data._
import org.openmole.gui.ext.dataui.{ FactoryWithPanelUI, FactoryWithDataUI }

import scala.collection.mutable
import scala.util.{ Failure, Try }

object ServerFactories {
  lazy private val instance = new ServerFactories

  def coreObject(dataBag: DataBag): Try[Any] = instance.factories.synchronized {
    instance.factories.get(dataBag.data.getClass()) match {
      case Some(f: Factory) ⇒ f.coreObject(PluginSet.empty) //FIXME AND TAKE THE PLUGINS
      case _                ⇒ Failure(new Throwable("The data " + dataBag.name + " cannot be recontructed on the server."))
    }
  }

  def add(dataClass: Class[_], factory: Factory, factoryUI: FactoryWithDataUI) = instance.factories.synchronized {
    instance.factories += dataClass -> factory
    instance.factoriesUI += dataClass.getName -> factoryUI
  }

  def addAuthenticationFactory(dataClass: Class[_], factoryUI: FactoryWithPanelUI) = instance.authenticationFactories.synchronized {
    instance.authenticationFactories += dataClass -> factoryUI
  }

  def remove(dataClass: Class[_]) = instance.factories.synchronized {
    instance.factories -= dataClass
    instance.factoriesUI -= dataClass.getName
  }

  def removeAuthenticationFactory(dataClass: Class[_]) = instance.authenticationFactories.synchronized {
    instance.authenticationFactories -= dataClass
  }

  def factoriesUI = instance.factoriesUI.toMap

  def authenticationFactoriesUI = instance.authenticationFactories.toMap
}

class ServerFactories {
  val factories = new mutable.WeakHashMap[Class[_], Factory]
  val factoriesUI = new mutable.WeakHashMap[String, FactoryWithDataUI]
  val authenticationFactories = new mutable.WeakHashMap[Class[_], FactoryWithPanelUI]
}
