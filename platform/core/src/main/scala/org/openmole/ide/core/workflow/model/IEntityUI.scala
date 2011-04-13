/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

trait IEntityUI {
  def entityType: Class[_]
  
  def name: String
}

//public interface IEntityUI {
//    String getName();
//    Class getType();
//}
