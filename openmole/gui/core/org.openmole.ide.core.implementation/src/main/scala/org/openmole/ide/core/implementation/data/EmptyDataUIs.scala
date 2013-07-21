/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.data

import org.openmole.core.model.data._
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.prototype._
import org.openmole.ide.core.implementation.dataproxy.{ EnvironmentDataProxyUI, PrototypeDataProxyUI, TaskDataProxyUI }
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory.IPrototypeFactoryUI
import org.openmole.ide.core.model.panel._
import org.openmole.core.implementation.task._
import org.openmole.misc.tools.obj.ClassUtils
import org.openmole.core.model.execution.Environment
import org.openmole.core.model.job.IJob
import scala.util.Try
import org.openmole.core.implementation.execution.local.LocalEnvironment

object EmptyDataUIs {

  val emptyPrototypeProxy: IPrototypeDataProxyUI = new PrototypeDataProxyUI(GenericPrototypeDataUI[Int], generated = false)

  val emptyTaskProxy: ITaskDataProxyUI = new TaskDataProxyUI(new EmptyTaskDataUI)

  val localEnvironmentProxy: IEnvironmentDataProxyUI = new EnvironmentDataProxyUI(LocalEnvironmentDataUI)

  class EmptyPrototypeFactoryUI extends IPrototypeFactoryUI {
    def buildDataUI = GenericPrototypeDataUI[Any]

    def buildDataUI(name: String,
                    dim: Int) = buildDataUI

    def buildDataUI(prototype: Prototype[_],
                    dim: Int) = buildDataUI
  }

  class EmptyPrototypeDataUI extends IPrototypeDataUI[Any] {
    def name = ""
    def dim = 0
    def typeClassString = ""
    def factory = new EmptyPrototypeFactoryUI
    def coreClass = classOf[Prototype[_]]
    def protoType = ClassUtils.manifest(classOf[Any])
    def coreObject = Try(Prototype[Any](""))
    def fatImagePath = "img/empty.png"
    def buildPanelUI = new GenericPrototypePanelUI(GenericPrototypeDataUI.base.head)
  }

  class EmptySampling extends Sampling {
    def prototypes = List.empty
    def build(context: Context) = List[Iterable[Variable[_]]]().toIterator
  }

  class EmptyTaskDataUI extends TaskDataUI {
    def name = ""
    def buildPanelUI = new EmptyTaskPanelUI
    def coreClass = classOf[EmptyTask]
    def updateImplicts(ipList: List[IPrototypeDataProxyUI],
                       opList: List[IPrototypeDataProxyUI]) = {}

    def coreObject(plugins: PluginSet) = Try {
      val taskBuilder = EmptyTask(name)(plugins)
      initialise(taskBuilder)
      taskBuilder.toTask
    }

    def fatImagePath = "img/empty.png"
  }

  class EmptyTaskPanelUI extends ITaskPanelUI {
    val components = List()
    def peer = new PluginPanel("").peer
    def saveContent(name: String) = new EmptyTaskDataUI
  }

  object LocalEnvironmentDataUI extends IEnvironmentDataUI { dataUI ⇒
    def imagePath = ""
    def buildPanelUI = new IEnvironmentPanelUI {
      val components = List()
      def saveContent(name: String) = dataUI
      def peer = new PluginPanel("").peer
    }
    def fatImagePath = ""
    def name = "Local"
    def coreObject = Try(LocalEnvironment.default)
    def coreClass = classOf[Environment]
  }
}
