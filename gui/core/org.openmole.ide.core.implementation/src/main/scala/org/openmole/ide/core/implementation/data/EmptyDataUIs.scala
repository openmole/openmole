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

import java.awt.Point
import org.openmole.core.model.data._
import org.openmole.core.model.sampling._
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.prototype._
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.implementation.panel.ComponentCategories
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory.IPrototypeFactoryUI
import org.openmole.ide.core.model.panel._
import org.openmole.core.implementation.task._
import org.openmole.ide.core.model.sampling._
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.netbeans.api.visual.widget.Scene
import org.openmole.misc.tools.obj.ClassUtils
import scala.swing.TabbedPane

object EmptyDataUIs {

  val emptyPrototypeProxy: IPrototypeDataProxyUI = new PrototypeDataProxyUI(GenericPrototypeDataUI[Int], false)

  val emptyTaskProxy: ITaskDataProxyUI = new TaskDataProxyUI(new EmptyTaskDataUI)

  class EmptyPrototypeFactoryUI extends IPrototypeFactoryUI {
    def category = ComponentCategories.PROTOTYPE
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
    def protoType = ClassUtils.nanifest(classOf[Any])
    def coreObject = Prototype[Any]("")
    def fatImagePath = "img/empty.png"
    def buildPanelUI = new GenericPrototypePanelUI(GenericPrototypeDataUI.base.head)
  }

  class EmptySamplingCompositionDataUI extends ISamplingCompositionDataUI {
    def name = "Empty sampling data UI"
    def dim = 0
    def coreClass = classOf[Sampling]
    def coreObject = new EmptySampling
    def imagePath = "img/empty.png"
    def fatImagePath = "img/empty.png"
    def buildPanelUI = new EmptySamplingCompositionPanelUI
    def isAcceptable(sampling: ISamplingDataUI) = false
    def isAcceptable(factor: IFactorDataUI) = false
    def domains = List.empty
    def samplings = List.empty
    def connections = List.empty
    def finalSampling = None
  }

  class EmptySamplingCompositionPanelUI extends ISamplingCompositionPanelUI {
    def peer = new PluginPanel("").peer
    def addDomain(domainProxy: IDomainProxyUI, location: Point, d: Boolean) {}
    def addSampling(samplingProxy: ISamplingProxyUI, location: Point, display: Boolean) {}
    def setFinalSampling(samplingProxy: ISamplingProxyUI) {}
    def setSamplingProxy(samplingProxy: ISamplingProxyUI, b: Boolean) {}
    def saveContent(name: String) = new EmptySamplingCompositionDataUI
    def remove(samplingComponent: ISamplingComponent): Unit = {}
    def scene = new Scene
    def testConnections(arityTest: Boolean) = {}
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

    def coreObject(inputs: DataSet, outputs: DataSet, parameters: ParameterSet, plugins: PluginSet) = {
      val taskBuilder = EmptyTask(name)(plugins)
      taskBuilder addInput inputs
      taskBuilder addOutput outputs
      taskBuilder addParameter parameters
      taskBuilder.toTask
    }

    def fatImagePath = "img/empty.png"
  }

  class EmptyTaskPanelUI extends ITaskPanelUI {
    def peer = new PluginPanel("").peer
    def saveContent(name: String) = new EmptyTaskDataUI
  }

}
