/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.serialization

import java.awt.Color
import java.io.File
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.task.serialization.StoreIntoCSVTask
import scala.collection.JavaConversions._
import au.com.bytecode.opencsv.CSVWriter
import org.openmole.core.implementation.data.Prototype._

class StoreIntoCSVTaskDataUI(val name: String="",val columns: List[(IPrototypeDataProxyUI,String)]= List.empty,val protoFile: Option[IPrototypeDataProxyUI]= None) extends TaskDataUI {
 
  def coreObject = {
    if (protoFile.isDefined)
      new StoreIntoCSVTask(name,
                           columns.map{e=> (e._1.dataUI.coreObject.asInstanceOf[IPrototype[Array[_]]],e._2)},
                           protoFile.get.dataUI.coreObject.asInstanceOf[IPrototype[File]],
                           ',',CSVWriter.NO_QUOTE_CHARACTER)
    else throw new UserBadDataError("No output prototype file is defined !")}
  
  def coreClass= classOf[StoreIntoCSVTask]
  
  def fatImagePath = "img/storeIntoCSV_fat.png"
  
  def buildPanelUI = new StoreIntoCSVTaskPanelUI(this)
  
  def borderColor = new Color(170,0,136)
  
  def backgroundColor = new Color(170,0,136,128)
}
