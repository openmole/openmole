/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.groovy

import java.awt.Color
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.groovy.GroovyTask

class GroovyTaskDataUI(val name: String,val code: String) extends TaskDataUI {
  def this(n: String) = this(n,"")
  
  override def coreObject = new GroovyTask(name,code)
  
  override def coreClass= classOf[GroovyTask]
  
  override def imagePath = "img/thumb/groovyTask.png"
  
  override def buildPanelUI = new GroovyTaskPanelUI(this)
  
  override def borderColor = new Color(61,104,130)
  
  override def backgroundColor = new Color(61,104,130,128)
}
