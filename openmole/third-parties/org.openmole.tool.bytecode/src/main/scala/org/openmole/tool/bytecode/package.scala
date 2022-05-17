/**
 * Created by Romain Reuillon on 02/05/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.tool

import org.objectweb.asm._
import scala.collection._

package object bytecode {

  def listAllClasses(byteCode: Array[Byte]): List[Type] = {
    val classes = mutable.HashSet[Type]()

    val cv = new ClassVisitor(Opcodes.ASM5) {

      override def visitInnerClass(name: String, outerName: String, innerName: String, access: Int): Unit =
        classes += Type.getObjectType(name)

      override def visitAttribute(attr: Attribute): Unit =
        if(attr.`type` != "TASTY") classes += Type.getType(attr.`type`)

      override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor = {
        classes ++= Type.getArgumentTypes(desc)
        classes += Type.getReturnType(desc)

        new MethodVisitor(Opcodes.ASM5) {
          override def visitTypeInsn(opcode: Int, `type`: String): Unit =
            classes += Type.getObjectType(`type`)

          override def visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) =
            classes += Type.getType(desc)

          override def visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) = {
            classes ++= Type.getArgumentTypes(desc)
            classes += Type.getReturnType(desc)
          }

          override def visitInvokeDynamicInsn(name: String, desc: String, bsm: Handle, bsmArgs: Object*): Unit = {
            classes ++= Type.getArgumentTypes(desc)
            classes += Type.getReturnType(desc)
          }

          override def visitLocalVariable(name: String, desc: String, signature: String, start: Label, end: Label, index: Int) =
            classes += Type.getType(desc)
        }
      }

      override def visitField(access: Int, name: String, desc: String, signature: String, value: scala.Any): FieldVisitor = {
        classes += Type.getType(desc)

        new FieldVisitor(Opcodes.ASM5) {
          override def visitAttribute(attr: Attribute): Unit =
            classes += Type.getType(attr.`type`)
        }
      }

    }

    val cr = new ClassReader(byteCode)
    cr.accept(cv, ClassReader.EXPAND_FRAMES)
    classes.toList
  }

}
