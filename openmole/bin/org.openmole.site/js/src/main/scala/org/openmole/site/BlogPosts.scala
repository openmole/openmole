//package org.openmole.site
//
//import org.scalajs.dom._
//import org.scalajs.dom.raw.HTMLElement
//
//import scala.util.{ Failure, Success }
//import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
//import scalatags.JsDom.all._
//import rx._
//
//import scalatags.Text.all.{ backgroundColor, padding }
//
///*
// * Copyright (C) 13/07/17 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//object BlogPosts {
//
//  type Category = String
//
//  val titleTag = "title"
//  val linkTag = "link"
//  val dateTag = "pubDate"
//  val categoryTag = "category"
//  val dateTasg = "date"
//  val searchedTags = Seq(titleTag, linkTag, dateTag, categoryTag)
//
//  val newsCategory = "News"
//  val shortTrainingCategory = "ShortTraining"
//  val longTrainingCategory = "LongTraining"
//
//  case class ReadNode(index: Int, name: String, value: String)
//  case class BlogPost(title: String = "", category: Category = newsCategory, link: String = "", date: Option[scalajs.js.Date] = None)
//
//  val all: Var[Seq[BlogPost]] = Var(Seq())
//  private def allBy(category: Category) = all.now.filter { _.category == category }.sortBy(_.date.get.getTime()).reverse.take(3)
//
//  all.trigger {
//    if (!all.now.isEmpty) {
//      addNewsdiv(allBy(newsCategory))
//      // addTrainings(allBy(shortTrainingCategory), shared.shortTraining)
//      // addTrainings(allBy(longTrainingCategory), shared.longTraining)
//    }
//  }
//
//  def fetch = {
//    val blogPosts = org.scalajs.dom.window.sessionStorage.getItem(shared.blogposts)
//    if (blogPosts == null) {
//      val future = ext.Ajax.get(
//        headers = Map(
//          "Accept" → "*/*"
//        ),
//        url = s"${shared.link.blog}/rss/",
//        timeout = 10000
//      ).map {
//          _.responseText
//        }
//
//      future.onComplete {
//        case Failure(f) => f.getStackTrace.mkString(" ")
//        case Success(s) =>
//          org.scalajs.dom.window.sessionStorage.setItem(shared.blogposts, s)
//          all() = stringToPost(s)
//      }
//    }
//    else
//      all() = stringToPost(blogPosts)
//  }
//
//  implicit def readNodeToBlogPost(rns: Seq[ReadNode]): BlogPost = {
//    def toBB(rs: Seq[ReadNode], blogPost: BlogPost): BlogPost = {
//      if (rs.isEmpty) blogPost
//      else toBB(rs.tail, {
//        val head = rs.head
//        val value = head.value
//        head.name match {
//          case n if n.startsWith(titleTag)    => blogPost.copy(title = value)
//          case n if n.startsWith(categoryTag) => blogPost.copy(category = value)
//          case n if n.startsWith(linkTag)     => blogPost.copy(link = value)
//          case n if n.startsWith(dateTag)     => blogPost.copy(date = Some(new scalajs.js.Date(value)))
//          case _                              => blogPost
//        }
//      })
//    }
//    toBB(rns, BlogPost())
//  }
//
//  def stringToPost(s: String): Seq[BlogPost] = {
//    val parser = new DOMParser
//
//    val tree = parser.parseFromString(s, "text/xml")
//
//    val rssItems = tree.getElementsByTagName("item")
//
//    (for {
//      i ← 0 to rssItems.length - 1
//      nodes = rssItems(i).childNodes
//      ns ← 0 to nodes.length - 1
//    } yield {
//      val node = nodes.item(ns)
//      ReadNode(i, node.nodeName, node.textContent)
//    }).filter { rn => searchedTags.contains(rn.name)
//    }.groupBy(_.index).values.map { readNodeToBlogPost }.toSeq
//  }
//
//  val newsStyle = Seq(
//    boxShadow := "0 10px 16px 0 rgba(0,0,0,0.2),0 6px 20px 0 rgba(0,0,0,0.19)",
//    backgroundColor := "white",
//    padding := 10,
//    marginTop := 5,
//    borderRadius := "5px"
//  )
//
//  val titleStyle = Seq(
//    textTransform := "uppercase",
//    maxWidth := 400
//  )
//
//  def testAndAppend(id: String, element: HTMLElement): Unit = {
//    val node = org.scalajs.dom.window.document.getElementById(id)
//    if (node != null)
//      node.appendChild(element)
//  }
//
//  def addNewsdiv(blogPosts: Seq[BlogPost]): Unit = {
//    def limitLength(s: String) = if (s.size < 50) s else s"${s.take(50)} ..."
//
//    val newsDiv = div(paddingTop := 20)(
//      h2("News"),
//      for {
//        bp ← blogPosts
//      } yield {
//        val d = bp.date.get
//        val dateString = s"${d.toLocaleDateString()}"
//        div(
//          a(href := bp.link, target := "_blank")(s"$dateString: ${limitLength(bp.title)}")(titleStyle)
//        // span(a(href := bp.link, target := "_blank")("Read more"))(moreStyle)
//        )(newsStyle)
//      }
//    ).render
//
//    testAndAppend(shared.newsPosts, newsDiv)
//  }
//
//  def addTrainings(blogPosts: Seq[BlogPost], id: String) = {
//    val trainingDiv = div(
//      for {
//        bp ← blogPosts
//      } yield {
//        div(
//          a(href := bp.link, target := "_blank")(bp.title)
//        )
//      }
//    ).render
//
//    testAndAppend(id, trainingDiv)
//  }
//}
//
