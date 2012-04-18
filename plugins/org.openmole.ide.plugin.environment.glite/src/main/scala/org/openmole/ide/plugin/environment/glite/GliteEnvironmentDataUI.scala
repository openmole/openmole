/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.glite

import org.openmole.plugin.environment.glite._
import org.openmole.ide.core.model.data.IEnvironmentDataUI
import org.openmole.plugin.environment.jsaga.Requirement._
import org.openmole.plugin.environment.glite.MyProxy
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

class GliteEnvironmentDataUI(val name: String="",
                             val vo: String="",
                             val voms: String="",
                             val bdii: String="",
                             val proxy: Boolean= false,
                             val proxyURL: String="", 
                             val requirement: Boolean= false,
                             val architecture64: Boolean = false, 
                             val runtimeMemory: String = Workspace.preference(BatchEnvironment.MemorySizeForRuntime),
                             val workerNodeMemory: String="",
                             val maxCPUTime:String="",
                             val otherRequirements: String= "") extends IEnvironmentDataUI {
  
  def coreObject = {
    val requirementMap = new HashMap[String,String]
    if (architecture64 == true) requirementMap+= CPU_ARCHITECTURE-> "x86_64"
    if (workerNodeMemory != "") requirementMap+= MEMORY->runtimeMemory
    if (maxCPUTime != "") requirementMap+= CPU_TIME-> maxCPUTime
    if (otherRequirements != "")requirementMap+= "REQUIREMENTS"->otherRequirements
    val rtm = if (runtimeMemory != "") runtimeMemory.toInt else Workspace.preference(BatchEnvironment.MemorySizeForRuntime).toInt
    
    
    if (vo == "" || voms == "" || bdii == "") throw new UserBadDataError("The glite environment "+name+" is not properly set")
    
    try {
      if (proxy && proxyURL != "") 
        new GliteEnvironment(
          vo,
          voms,
          bdii,
          Some(new MyProxy(proxyURL)),
          requirements = requirementMap.toMap,
          runtimeMemory = rtm)
      else new GliteEnvironment(vo,voms,bdii, requirements = requirementMap.toMap, runtimeMemory = rtm)
    }
    catch {
      case _=> throw new UserBadDataError("An error occured when initialiazing the glite environment" + name +". Please check your certificate settings in the Preferences menu.")
    }
  }
  
  def coreClass = classOf[GliteEnvironment] 
  
  def imagePath = "img/glite.png" 
  
  override def fatImagePath = "img/glite_fat.png"
  
  def buildPanelUI = new GliteEnvironmentPanelUI(this)
}
