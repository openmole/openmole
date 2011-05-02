/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.palette

class GenericCategory(val n: String, val c: GenericChildren) extends ICategory{
  override def name = n
  override def children = c
}