/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.groovy

import java.awt.Color
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.groovy.GroovyTask

class GroovyTaskDataUI(val name: String="",
                       val code: String="",
                       val libs: List[String]= List.empty,
                       val plugins: List[String]= List.empty) extends TaskDataUI {
  
  def coreObject = {
    val gt= new GroovyTask(name,code) 
    libs.foreach(gt.addLib) 
    plugins.foreach{gt.addPlugin}
    gt}
  
  def coreClass= classOf[GroovyTask]
  
  def imagePath = "img/groovyTask.png"
  
  override def fatImagePath = "img/groovyTask_fat.png"
  
  def buildPanelUI = new GroovyTaskPanelUI(this)
  
  def borderColor = new Color(61,104,130)
  
  def backgroundColor = new Color(61,104,130,128)
}
