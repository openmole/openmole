/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.serialization

import java.awt.Color
import java.io.File
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.task.serialization.StoreIntoCSVTask
import scala.collection.JavaConversions._
import org.openmole.core.implementation.data.Prototype._

class StoreIntoCSVTaskDataUI(val name: String = "",
                             val columns: List[(IPrototypeDataProxyUI, String)] = List.empty,
                             val protoFile: Option[IPrototypeDataProxyUI] = None) extends TaskDataUI {

  def coreObject(inputs: IDataSet, outputs: IDataSet, parameters: IParameterSet, plugins: IPluginSet) = {
    val builder = protoFile match {
      case Some(x: IPrototypeDataProxyUI) ⇒
        StoreIntoCSVTask(
          name,
          protoFile.get.dataUI.coreObject.asInstanceOf[IPrototype[File]])(plugins)
      case None ⇒ throw new UserBadDataError("No output prototype file is defined !")
    }
    columns.foreach { e ⇒ builder addColumn (e._1.dataUI.coreObject.asInstanceOf[IPrototype[Array[_]]], e._2) }
    builder.toTask
  }

  def coreClass = classOf[StoreIntoCSVTask]

  override def imagePath = "img/storeIntoCSV.png"

  def fatImagePath = "img/storeIntoCSV_fat.png"

  def buildPanelUI = new StoreIntoCSVTaskPanelUI(this)
}
