package org.openmole.site

/*
 * Copyright (C) 19/04/17 // mathieu.leclaire@openmole.org
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

import org.openmole.site

import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.stylesheet.{ all ⇒ sheet }
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import sheet._
import bs._

object UserGuide {

  val contentReplacer = utils.replacer

  private def buildTabs(docPages: Seq[JSDocumentationPage], current: JSPage) = {
    val tabs = Tabs(sheet.pills)

    docPages.foldLeft(tabs)((tabs, t) ⇒ {

      val isCurrent = t == current

      val withDetails = tags.div(
        tags.div(sitesheet.detailButtons)(
          for {
            d ← t.details
          } yield {
            tags.div(scalatags.JsDom.all.paddingTop := 10)(bs.linkButton(d.name, d.file, btn_danger))
          }
        ),
        tags.div(scalatags.JsDom.all.paddingTop := 20)(
          if (isCurrent) contentReplacer.tag else tags.div
        )
      )

      tabs.add(t.name, withDetails, isCurrent, onclickExtra = () ⇒ Search.to(t))
    })
  }

  def addCarousel(current: JSPage) = {

    val methodTabs = buildTabs(JSPages.documentation_language_methods.children, current)
    val envTabs = buildTabs(JSPages.documentation_language_environments.children, current)
    val taskTabs = buildTabs(JSPages.documentation_language_models.children, current)

    val parent = site.utils.father(current)
    val currentStep = {
      parent match {
        case Some(dp: JSDocumentationPage) ⇒
          if (dp == JSPages.documentation_language_models) 0
          else if (dp == JSPages.documentation_language_methods) 1
          else 2
        case _ ⇒ 0
      }
    }

    val carrousel =
      new StepCarousel(
        currentStep,
        Step("1. MODEL", taskTabs.render, JSPages.documentation_language_models_scala),
        Step("2. METHOD", methodTabs.render, JSPages.documentation_language_methods_profiles),
        Step("3. ENVIRONMENT ", envTabs.render, JSPages.documentation_language_environments_ssh)
      )

    org.scalajs.dom.window.document.body.appendChild(tags.div(sitesheet.mainDiv)(carrousel.render))
    contentReplacer.replaceWith(shared.sitexDoc)
    carrousel.introReplacer.replaceWith(shared.sitexIntro)
    carrousel.introMoreReplacer.replaceWith(shared.sitexIntroMore)
  }

}

