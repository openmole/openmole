package org.openmole.site

/*
 * Copyright (C) 23/06/17 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import tools._
import stylesheet._
import scalatags.Text.all._

object UserGuide {

  def father(page: Page) =
    Pages.all.collect { case doc: DocumentationPage ⇒ doc }.filter {
      _.children.contains(page)
    }.headOption

  private def buildTabs(docPages: Seq[DocumentationPage], current: Page) = {
    val tabs = Tabs()

    docPages.foldLeft(tabs)((tabs, p) ⇒ {

      val isCurrent = p.title == current.title

      val withDetails = div(
        div(detailButtons)(
          for {
            d ← p.details
          } yield {
            div(paddingTop := 10)(linkButton(d.name, d.file, classIs(btn ++ btn_danger)))
          }
        ),
        div(paddingTop := 20)(
          if (isCurrent) {
            div(p.content)
          }
          else div()
        )
      )

      tabs.add(p.name, withDetails, isCurrent, p)

    }) //)

    //    println(" +++ The tabs: afetr fold l" + tabs.tabs.map {
    //      _.title
    //    })
    //  tabs
  }

  def addCarousel(current: Page) = {

    val methodTabs = buildTabs(DocumentationPages.root.language.method.children, current)
    val envTabs = buildTabs(DocumentationPages.root.language.environment.children, current)
    val taskTabs = buildTabs(DocumentationPages.root.language.model.children, current)

    val parent = father(current)
    val currentStep = {
      parent match {
        case Some(dp: DocumentationPage) ⇒
          if (dp == DocumentationPages.root.language.model) 0
          else if (dp == DocumentationPages.root.language.method) 1
          else 2
        case _ ⇒ 0
      }
    }

    new StepCarousel(
      currentStep,
      Step("1. MODEL", taskTabs.render, DocumentationPages.root.language.model.scala),
      Step("2. METHOD", methodTabs.render, DocumentationPages.root.language.method.profile),
      Step("3. ENVIRONMENT ", envTabs.render, DocumentationPages.root.language.environment.ssh)
    ).render

  }

}
