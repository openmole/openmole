package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.GUIVariable.ValueType
import org.openmole.gui.shared.data.GUIVariable.ValueType.*
import org.openmole.gui.shared.data.{GUIOMRSectionContent, GUIVariable, SafePath}
import com.raquo.laminar.api.L.*

import scala.annotation.tailrec

object RowData:
  object Content:
    given Conversion[String, Content] = s => Content(s)

  case class Content(value: String, html: Option[HtmlElement] = None)
  def toDataContent(c: Content) = scaladget.bootstrapnative.Table.DataContent(c.value, c.html)

case class RowData(headers: Seq[String] = Seq(), content: Seq[Seq[RowData.Content]] = Seq(), dimensions: Seq[Int])

case class ScalarColumn(column: Seq[String])

case class ArrayColumn(column: Seq[Seq[String]])

case class Column(header: String, content: ScalarColumn | ArrayColumn)

object Column {
  def contentToSeqOfSeq(content: ScalarColumn | ArrayColumn): Seq[Seq[String]] = {
    content match {
      case ScalarColumn(s) => Seq(s)
      case ArrayColumn(a) => a
    }
  }
}

case class ColumnData(columns: Seq[Column])

object ResultData {


  // Convert a string to an array, considering it is of dimension 1
  def fromStringToArray(s: String): Seq[String] = s.drop(1).dropRight(1).split(",")

  def fromCSV(content: String) = {

    trait Pending
    case class ArrayPending(string: String, balance: Int) extends Pending
    case class QuotePending(string: String, balance: Int) extends Pending
    case class RegularPending(string: String) extends Pending

    def balance(s: String) = s.count(_ == '[') - s.count(_ == ']')

    case class ParsingException(message: String) extends Exception(message)

    def dimension(s: String): Int =
      @tailrec
      def dimension0(toBeParsed: String, dim: Int): Int =
        if (toBeParsed.isEmpty) dim
        else
          val cur = toBeParsed.head
          cur match
            case ']' => dim
            case '[' => dimension0(toBeParsed.tail, dim + 1)
            case _ => dimension0(toBeParsed.tail, dim)

      dimension0(s, 0)

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
    val dims = body.headOption.map {
      _.map { s => dimension(s) }
    }.toSeq.flatten

    RowData(header, body.map(_.map(v => RowData.Content(v))), dims)
  }


  def fromOMR(sections: Seq[GUIOMRSectionContent]) =
    implicit class WrapStringArray(content: Array[String]):
      def beautify: String = s"[${content.mkString(", ")}]"

    def safepathToLink(f: SafePath) = a(href := org.openmole.gui.shared.api.downloadFile(f), cls := "downloadLink", f.name)

    def beautify(valueType: ValueType): Array[RowData.Content] =
      valueType match
        case ValueLong(l) => Array(l.toString)
        case ValueInt(i) => Array(i.toString)
        case ValueDouble(d) => Array(d.toString)
        case ValueString(s) => Array(s)
        case ValueBoolean(b) => Array(b.toString)
        case ValueFile(f) => Array(f.name)
        case ValueArrayLong(aL) => aL.map(_.toString)
        case ValueArrayInt(aI) => aI.map(_.toString)
        case ValueArrayDouble(aD) => aD.map(_.toString)
        case ValueArrayString(aS) => aS.map(x => x)
        case ValueArrayBoolean(aB) => aB.map(_.toString)
        case ValueArrayFile(aF) => aF.map(f => RowData.Content(f.name, Some(safepathToLink(f))))
        case ValueArrayArrayLong(aaL) => aaL.map(_.map(_.toString).beautify)
        case ValueArrayArrayInt(aaI) => aaI.map(_.map(_.toString).beautify)
        case ValueArrayArrayDouble(aaD) => aaD.map(_.map(_.toString).beautify)
        case ValueArrayArrayString(aaS) => aaS.map(_.beautify)
        case ValueArrayArrayBoolean(aaB) => aaB.map(_.map(_.toString).beautify)
        case ValueArrayArrayFile(aaF) =>
          aaF.map: af =>
            val html: HtmlElement = div("[", af.map(safepathToLink).flatMap(c => Seq[HtmlElement](c, span(","))).dropRight(1), "]")
            val content = af.map(_.name).beautify
            RowData.Content(content, Some(html))

    def dimension(valueType: ValueType) =
      valueType match
        case ValueLong(_) | ValueInt(_) | ValueBoolean(_) | ValueDouble(_) | ValueFile(_) | ValueString(_) => -1
        case ValueArrayLong(_) | ValueArrayInt(_) | ValueArrayBoolean(_) | ValueArrayDouble(_) | ValueArrayFile(_) | ValueArrayString(_) => 0
        case ValueArrayArrayLong(_) | ValueArrayArrayInt(_) | ValueArrayArrayBoolean(_) | ValueArrayArrayDouble(_)
             | ValueArrayArrayFile(_) | ValueArrayArrayString(_) => 1

    (sections map : sec =>
      val headers = sec.variables map (_.name)
      val columns = sec.variables.flatMap(_.value.map(beautify))
      val maxSize = columns.map(_.size).max
      val normalizedColumns = columns.map: c=>
        c.size match
          case 1 => Array.fill(maxSize)(c.head)
          case _=> c

      val rows = normalizedColumns.transpose
      val dimensions = sec.variables.map(_.value.headOption.map(dimension).getOrElse(0))

      RowData(headers, rows, dimensions)
    ).head


}
