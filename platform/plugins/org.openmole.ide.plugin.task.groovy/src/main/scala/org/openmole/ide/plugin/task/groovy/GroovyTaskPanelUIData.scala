/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.groovy

import java.awt.Color
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.properties.PanelUIData
import org.openmole.ide.core.properties.TaskPanelUIData
import org.openmole.plugin.task.groovy.GroovyTask

class GroovyTaskPanelUIData(name: String,var code: String) extends TaskPanelUIData(name) {
  def this(n: String) = this(n,"") 
  
  override def coreObject = new GroovyTask(name,code)
  
  override def coreClass= classOf[GroovyTask]
  
  override def imagePath = "img/thumb/groovyTaskSmall.png"
  
  override def buildPanelUI = new GroovyTaskPanelUI
  
  override def borderColor = new Color(61,104,130)
  
  override def backgroundColor = new Color(61,104,130,128)
}
