/*
* This file is part of the ToolXiT project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package toolxit.bibtex
package machine

import scala.collection.mutable.Stack

/**
 * This trait proovides stack capabilities to a machine.
 *
 * @author Lucas Satabin
 *
 */
trait StackMachine {
  this: Environment ⇒

  // ==== the stack ====
  protected[this] val stack = Stack.empty[StackValue]

  /* pushes an int on the stack */
  protected[this] def push(i: Int) {
    stack.push(SIntValue(i))
  }

  /* pushes a string on the stack */
  protected[this] def push(s: String) {
    stack.push(SStringValue(s))
  }

  /* pushes a symbol on the stack */
  protected[this] def push(v: StackValue) {
    stack.push(v)
  }

  /* pops an integer from the stack. If the value is not an 
   * integer or does not exist, None is returned */
  protected[this] def popInt = {
    if (stack.isEmpty)
      None
    else
      stack.pop match {
        case SIntValue(i) ⇒ Some(i)
        case _            ⇒ None
      }
  }

  /* pops a string from the stack. If the value is not a 
   * string or does not exist, None is returned */
  protected[this] def popString = {
    if (stack.isEmpty)
      None
    else
      stack.pop match {
        case SStringValue(s) ⇒ Some(s)
        case _               ⇒ None
      }
  }

  /* pops a function from the stack. If the value is not a 
   * function or does not exist, None is returned */
  protected[this] def popFunction = {
    if (stack.isEmpty)
      None
    else
      stack.pop match {
        case FunctionValue(instr) ⇒
          // it is directly a block on the stack
          Some(instr)
        case SStringValue(str) ⇒
          // it is a name on the stack, look if it represents a function
          functions.get(str).map(_.instr)
        case _ ⇒ None
      }
  }

  /* pops a value from the stack. If the stack is empty returns None */
  protected[this] def pop = {
    if (stack.isEmpty)
      None
    else
      Some(stack.pop)
  }

}

// ==== values that are pushed on the stack ====
sealed trait StackValue {
  def toVar: Variable
}
final case class SStringValue(value: String) extends StackValue {
  def toVar = StringVariable(value)
}
case object NullStringValue extends StackValue {
  def toVar = StringVariable()
}
final case class SIntValue(value: Int) extends StackValue {
  def toVar = IntVariable(value)
}
final case class FunctionValue(instructions: bst.BstBlock) extends StackValue {
  def toVar = FunctionVariable(instructions)
}
case object MissingValue extends StackValue {
  def toVar = StringVariable()
}