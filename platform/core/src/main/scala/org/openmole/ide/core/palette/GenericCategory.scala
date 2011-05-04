/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.palette


class GenericCategory(val n: String, val dn: String, val c: GenericChildren) extends ICategory{
  
  override def name = n
  override def displayName = dn
  override def children = c
}