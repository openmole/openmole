package org.openmole.gui.client.core.files

import org.openmole.gui.client.tool.plot.Tools

import scala.annotation.tailrec

case class RowData(headers: Seq[String] = Seq(), content: Seq[Seq[String]] = Seq(), dimensions: Seq[Int])

case class ScalarColumn(column: Seq[String])
case class ArrayColumn(column: Seq[Seq[String]])

case class Column(header: String, content: ScalarColumn | ArrayColumn)

object Column {
  def contentToSeqOfSeq(content: ScalarColumn | ArrayColumn): Seq[Seq[String]] = {
    content match {
      case ScalarColumn(s)=> Seq(s)
      case ArrayColumn(a)=> a 
    }
  }
}

case class ColumnData(columns: Seq[Column])

object ResultData {

  def fromCSV(content: String) = {

    trait Pending
    case class ArrayPending(string: String, balance: Int) extends Pending
    case class QuotePending(string: String, balance: Int) extends Pending
    case class RegularPending(string: String) extends Pending

    def balance(s: String) = s.count(_ == '[') - s.count(_ == ']')

    case class ParsingException(message: String) extends Exception(message)

    @tailrec
    def parse0(toBeChecked: String, pending: Option[Pending], parsed: List[String]): Seq[String] = {
      if (toBeChecked.isEmpty) {
        pending match {
          case Some(ar: ArrayPending) => if (ar.balance == 0) parsed :+ ar.string else throw ParsingException("Unfinished array")
          case Some(rp: RegularPending) => parsed :+ rp.string
          case _ => parsed
        }
      } else {
        val currentChar = toBeChecked.head
        val tail = toBeChecked.tail
        currentChar match {
          case ',' => pending match {
            case Some(rp: RegularPending) => parse0(tail, None, parsed :+ rp.string)
            case Some(ap: ArrayPending) =>
              if (ap.balance == 0) parse0(tail, None, parsed :+ ap.string)
              else parse0(tail, Some(ap.copy(ap.string :+ currentChar)), parsed)
            case _ => throw ParsingException("Coma set in bad position")
          }
          case '[' | ']' =>
            pending match {
              case Some(ap: ArrayPending) =>
                val newString = ap.string + currentChar
                parse0(tail, Some(ap.copy(string = newString, balance(newString))), parsed)
              case Some(rp: RegularPending) => throw ParsingException("'[' or ']' set in bad position")
              case _ => currentChar match {
                case '[' => parse0(tail, Some(ArrayPending(currentChar.toString, balance = 1)), parsed)
                case _ => throw ParsingException("']' set in bad position")
              }
            }
          case ' ' | '"' => parse0(tail, pending, parsed)
          case c: Char => pending match {
            case Some(ap: ArrayPending) => parse0(tail, Some(ap.copy(string = ap.string :+ c)), parsed)
            case Some(rp: RegularPending) => parse0(tail, Some(rp.copy(string = rp.string :+ c)), parsed)
            case _ => parse0(tail, Some(RegularPending(c.toString)), parsed)
          }
        }
      }
    }

    val lines: Seq[String] = content.split("\n")
    val header: Seq[String] = lines.head.split(",").toSeq
    val body = lines.tail.map(line => parse0(line, None, List()))
    val dims = body.headOption.map{_.map{s=> dimension(s)}}.toSeq.flatten

    RowData(header, body, dims)
  }

  def dimension(s: String): Int = {

    @tailrec
    def dimension0(toBeParsed: String, dim: Int): Int = {
      if (toBeParsed.isEmpty) dim
      else {
        val cur = toBeParsed.head
        cur match {
          case ']' => dim
          case '[' => dimension0(toBeParsed.tail, dim + 1)
          case _ => dimension0(toBeParsed.tail, dim)
        }
      }
    }

    dimension0(s, 0)
  }

  // Convert a string to an array, considering it is of dimension 1
  def fromStringToArray(s: String): Seq[String] = {
    s.drop(1).dropRight(1).split(",")
  }

//  def getColumnsFrom(resultData: RowData, headersQuery: Seq[String]) = {
//
//    val headersQuery
//    .flatMap { h =>
//      resultData.headers.indexOf(h) match {
//        case -1 => Seq()
//        case i: Int =>
//      }
//    }
//  }


}
