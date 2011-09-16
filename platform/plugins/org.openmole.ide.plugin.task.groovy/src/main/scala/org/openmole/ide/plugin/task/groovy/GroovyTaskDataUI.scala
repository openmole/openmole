/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.groovy

import java.awt.Color
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.groovy.GroovyTask

class GroovyTaskDataUI(val name: String,val code: String="",val lib: String="") extends TaskDataUI {
  
  override def coreObject = new GroovyTask(name,code) {
    if (!lib.isEmpty) addLib(lib)}
  
  override def coreClass= classOf[GroovyTask]
  
  override def imagePath = "img/groovyTask.png"
  
  override def buildPanelUI = new GroovyTaskPanelUI(this)
  
  override def borderColor = new Color(61,104,130)
  
  override def backgroundColor = new Color(61,104,130,128)
}
