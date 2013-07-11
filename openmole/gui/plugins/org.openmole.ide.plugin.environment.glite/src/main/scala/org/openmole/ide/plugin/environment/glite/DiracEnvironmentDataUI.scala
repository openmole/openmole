/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.glite

import org.openmole.ide.core.model.data.IEnvironmentDataUI
import org.openmole.plugin.environment.glite.DIRACGliteEnvironment

class DiracEnvironmentDataUI(val name: String = "",
                             val voName: String = "",
                             val service: String = "",
                             val group: String = "",
                             val bdii: String = "",
                             val vomsURL: String = "",
                             val setup: String = "",
                             val fqan: Option[String] = None,
                             val cpuTime: Option[String] = None,
                             val openMOLEMemory: Option[Int] = None) extends IEnvironmentDataUI {

  def coreObject = new DIRACGliteEnvironment(voName, service, group, bdii, vomsURL, setup, fqan, cpuTime, openMOLEMemory)

  def coreClass = classOf[DIRACGliteEnvironment]

  def imagePath = "img/dirac.png"

  override def fatImagePath = "img/dirac_fat.png"

  def buildPanelUI = new DiracEnvironmentPanelUI(this)
}
