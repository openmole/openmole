/**
  * Created by mathieu on 10/05/17.
  */

import org.openmole.site._
import better.files._

object MacroSite extends App {

  override def main(args: Array[String]) = {

    val targetFile = File(args(0))
    targetFile.createIfNotExists()


    val header =
      """
      package org.openmole.site

        trait JSPage {
          def name: String
          def file: String
          def details: Seq[JSPage]
          }
        case class JSMainPage(name: String, file: String, details: Seq[JSPage] = Seq()) extends JSPage
        case class JSDocumentationPage(name: String, file: String, details: Seq[JSPage] = Seq(), children: Seq[JSDocumentationPage] = Seq()) extends JSPage

      object JSPages {

        def toJSPage(file: String): Option[JSPage] = all.filter(_.file == file).headOption
      """.stripMargin

    val footer = "\n\n}"

    case class MacroPage(page: Page, name: String, isDoc: Boolean, file: String)
    def valName(page: Page) = page.location.mkString("_").replaceAll(" ", "_").replaceAll("-", "_").toLowerCase

    implicit def pageSeqToMacroPageSeq(ps: Seq[Page]): Seq[MacroPage] = ps.map { p => MacroPage(p, valName(p), Pages.isDoc(p), Pages.file(p)) }

    val pageMap: Seq[MacroPage] = Pages.all
    val topPagesChildren: Seq[MacroPage] = DocumentationPages.topPagesChildren

    def listOf(prefix: String, macroPages: Seq[MacroPage]) = {

      if (macroPages.isEmpty) ""
      else
        s""", $prefix=Seq(${
          macroPages.map {
            _.name
          }.mkString(",")
        })"""
    }

    def children(page: Page) =
      page match {
        case dp: DocumentationPage => listOf("children", dp.children)
        case _ => ""
      }


  val content = pageMap.foldLeft(header) { case (acc, macropage) =>
    val constr = macropage.isDoc match {
      case true => "JSDocumentationPage"
      case _ => "JSMainPage"
    }
    acc +
      s"""\nlazy val ${macropage.name} = $constr("${macropage.page.name}", "${macropage.file}"${children(macropage.page)}${listOf("details", macropage.page.details)})"""
      }+ s"""\n\nlazy val all: Seq[JSPage] = Seq(${pageMap.map{_.name}.mkString(", ")})\n\nlazy val topPagesChildren = Seq(${topPagesChildren.map{_.name}.mkString(", ")})
                 """.stripMargin + footer

    println(content)
    targetFile overwrite content

  }
}