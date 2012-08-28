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

import org.openmole.core.model.data.IVariable
import org.openmole.core.model.sampling.ISampling
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.core.model.task.IPluginSet
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI
import org.openmole.ide.core.model.data.IPrototypeDataUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.panel.IPrototypePanelUI
import java.awt.Color
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import scala.swing.TabbedPane

object EmptyDataUIs {

  val emptyPrototypeProxy: IPrototypeDataProxyUI = new PrototypeDataProxyUI(new EmptyPrototypeDataUI, false)

  val emptyTaskProxy: ITaskDataProxyUI = new TaskDataProxyUI(new EmptyTaskDataUI)

  class EmptyPrototypeDataUI extends IPrototypeDataUI[Any] {
    def name = ""
    def dim = 0
    def coreClass = classOf[IPrototype[_]]
    def coreObject = new Prototype[Any]("")
    def fatImagePath = "img/empty.png"
    def buildPanelUI = new EmptyPrototypePanelUI
    def displayTypedName = ""
  }

  class EmptyPrototypePanelUI extends IPrototypePanelUI[Any] {
    def peer = new PluginPanel("").peer
    def saveContent(name: String) = new EmptyPrototypeDataUI
  }

  class EmptySamplingDataUI extends ISamplingDataUI {
    def name = ""
    def dim = 0
    def coreClass = classOf[ISampling]
    def coreObject = new EmptySampling
    def imagePath = "img/empty.png"
    def fatImagePath = "img/empty.png"
    def buildPanelUI = new EmptySamplingPanelUI
    def displayTypedName = ""
  }

  class EmptySamplingPanelUI extends ISamplingPanelUI {
    def peer = new PluginPanel("").peer
    def saveContent(name: String) = new EmptySamplingDataUI
  }

  class EmptySampling extends Sampling {
    def prototypes = List.empty
    def build(context: IContext) = List[Iterable[IVariable[_]]]().toIterator
  }

  class EmptyTaskDataUI extends TaskDataUI {
    def name = ""
    def buildPanelUI = new EmptyTaskPanelUI
    def coreClass = classOf[EmptyTask]
    def updateImplicts(ipList: List[IPrototypeDataProxyUI],
                       opList: List[IPrototypeDataProxyUI]) = {}

    def coreObject(inputs: IDataSet, outputs: IDataSet, parameters: IParameterSet, plugins: IPluginSet) = {
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
