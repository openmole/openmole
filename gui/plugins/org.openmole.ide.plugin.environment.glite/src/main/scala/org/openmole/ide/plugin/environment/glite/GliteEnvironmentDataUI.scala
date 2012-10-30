/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.glite

import org.openmole.plugin.environment.glite._
import org.openmole.ide.core.model.data.IEnvironmentDataUI
import org.openmole.plugin.environment.jsaga._
import org.openmole.plugin.environment.glite.MyProxy
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.ide.plugin.environment.tools.RequirementDataUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._

class GliteEnvironmentDataUI(
    val name: String = "",
    val vo: String = "",
    val voms: String = "",
    val bdii: String = "",
    val proxy: Boolean = false,
    val proxyURL: String = "",
    val runtimeMemory: String = Workspace.preference(BatchEnvironment.MemorySizeForRuntime),
    val requirements: RequirementDataUI = new RequirementDataUI) extends IEnvironmentDataUI {

  def coreObject = {
    val rtm = if (runtimeMemory != "") runtimeMemory.toInt else Workspace.preference(BatchEnvironment.MemorySizeForRuntime).toInt

    if (vo == "" || voms == "" || bdii == "") throw new UserBadDataError("The glite environment " + name + " is not properly set")

    try {
      if (proxy && proxyURL != "")
        new GliteEnvironment(
          vo,
          voms,
          bdii,
          Some(new MyProxy(proxyURL)),
          // requirements = requirements.toMap,
          runtimeMemory = Some(rtm))
      else new GliteEnvironment(vo, voms, bdii,
        //requirements = requirements.toMap, 
        runtimeMemory = Some(rtm))
    } catch {
      case e: UserBadDataError ⇒ throw e
      case e: Exception ⇒ throw new UserBadDataError(e, "An error occured when initializing the glite environment" + name + ". Please check your certificate settings in the Preferences menu.")
    }
  }

  def coreClass = classOf[GliteEnvironment]

  def imagePath = "img/glite.png"

  override def fatImagePath = "img/glite_fat.png"

  def buildPanelUI = new GliteEnvironmentPanelUI(this)
}
