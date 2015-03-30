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
package bst

import scala.util.parsing.input.Positional

final case class BstFile(commands: List[BstCommand])

sealed trait BstCommand extends Positional

final case class BstEntry(fields: List[String],
                          integers: List[String],
                          strings: List[String]) extends BstCommand

final case class BstExecute(name: String) extends BstCommand

final case class BstFunction(name: String,
                             instructions: BstBlock) extends BstCommand

final case class BstIntegers(integers: List[String]) extends BstCommand

final case class BstIterate(name: String) extends BstCommand

final case class BstMacro(name: String, value: String) extends BstCommand

case object BstRead extends BstCommand

final case class BstReverse(name: String) extends BstCommand

case object BstSort extends BstCommand

final case class BstStrings(strings: List[String]) extends BstCommand

// the different instructions that may occur in a function
sealed trait BstInstruction extends Positional
// a block delimited by braces
final case class BstBlock(instructions: List[BstInstruction]) extends BstInstruction
// pushes the name of the given variable on the stack
final case class BstPushName(name: String) extends BstInstruction
// references a name
//  - if it is a variable or macro, its value will be pushed on stack
//  - if it is a function name, the function is called
final case class BstRefName(name: String) extends BstInstruction
// pushes the given string on the stack
final case class BstPushString(string: String) extends BstInstruction
// pushes the given integer on the stack
final case class BstPushInt(integer: Int) extends BstInstruction

// a built-in function in BibTeX
sealed trait BstBuiltIn extends BstInstruction
case object BstInferior extends BstBuiltIn
case object BstSuperior extends BstBuiltIn
case object BstEquals extends BstBuiltIn
case object BstPlus extends BstBuiltIn
case object BstMinus extends BstBuiltIn
case object BstMultiply extends BstBuiltIn
case object BstAssign extends BstBuiltIn
case object BstAddPeriod extends BstBuiltIn
case object BstCallType extends BstBuiltIn
case object BstChangeCase extends BstBuiltIn
case object BstChrToInt extends BstBuiltIn
case object BstCite extends BstBuiltIn
case object BstDuplicate extends BstBuiltIn
case object BstEmpty extends BstBuiltIn
case object BstFormatName extends BstBuiltIn
case object BstIf extends BstBuiltIn
case object BstIntToChr extends BstBuiltIn
case object BstIntToStr extends BstBuiltIn
case object BstMissing extends BstBuiltIn
case object BstNewline extends BstBuiltIn
case object BstNumNames extends BstBuiltIn
case object BstPop extends BstBuiltIn
case object BstPreamble extends BstBuiltIn
case object BstPurify extends BstBuiltIn
case object BstQuote extends BstBuiltIn
case object BstSkip extends BstBuiltIn
case object BstStack extends BstBuiltIn
case object BstSubstring extends BstBuiltIn
case object BstSwap extends BstBuiltIn
case object BstTextLength extends BstBuiltIn
case object BstTextPrefix extends BstBuiltIn
case object BstTop extends BstBuiltIn
case object BstType extends BstBuiltIn
case object BstWarning extends BstBuiltIn
case object BstWhile extends BstBuiltIn
case object BstWidth extends BstBuiltIn
case object BstWrite extends BstBuiltIn