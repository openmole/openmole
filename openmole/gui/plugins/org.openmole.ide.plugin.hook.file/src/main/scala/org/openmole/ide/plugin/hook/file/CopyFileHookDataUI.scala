/*
 * Copyright (C) 2011 Mathieu Leclaire
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

package org.openmole.ide.plugin.hook.file

import java.io.File
import org.openmole.core.model.data.Prototype
import org.openmole.plugin.hook.file._
import org.openmole.ide.core.implementation.data.HookDataUI
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }
import org.openmole.ide.core.implementation.serializer.Update
import org.openmole.ide.misc.tools.util.Converters
import Converters._

class CopyFileHookDataUI(val name: String = "",
                         val prototypes: List[(PrototypeDataProxyUI, String)] = List.empty,
                         val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                         val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                         val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends HookDataUI with Update[CopyFileHookDataUI010] {
  def coreClass = ???
  def buildPanelUI = ???
  def coreObject = ???
  def doClone(inputs: Seq[PrototypeDataProxyUI], outputs: Seq[PrototypeDataProxyUI], parameters: Map[PrototypeDataProxyUI, String]) = ???
  def update = new CopyFileHookDataUI010(name, prototypes, inputs, outputs, inputParameters)
}

class CopyFileHookDataUI010(val name: String = "",
                            val prototypes: List[(PrototypeDataProxyUI, String, Int)] = List.empty,
                            val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                            val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                            val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends HookDataUI {

  def coreClass = classOf[CopyFileHook]

  def coreObject = util.Try {
    val cfh = CopyFileHook()
    prototypes.foreach {
      case (p, s, _) ⇒ cfh.copy(p.dataUI.coreObject.get.asInstanceOf[Prototype[File]], s)
    }
    initialise(cfh)
    cfh.toHook
  }

  def buildPanelUI = new CopyFileHookPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new CopyFileHookDataUI010(name, Proxies.instance.filterListTupleIn(prototypes), ins, outs, params)
}
