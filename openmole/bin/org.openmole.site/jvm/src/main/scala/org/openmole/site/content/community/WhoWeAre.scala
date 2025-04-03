package org.openmole.site.content.community

/*
 * Copyright (C) 2023 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.site.content.header.*

object WhoWeAreTool {
  import org.openmole.site.FileResource
  def member(image: FileResource, name: String): Frag =
    div(paddingTop := 60)(
      img(src := image.file, height := 70, paddingRight := 10),
      span(name, memberStyle)
    )
}

import WhoWeAreTool._

object WhoWeAre extends PageContent(
  div(centerBox100)(
    a(img(src := Resource.img.partner.iscpif.file,  partners), href := shared.link.partner.iscpif, target := "_blank"),
    a(img(src := Resource.img.partner.geocite.file, partners), href := shared.link.partner.parisgeo, target := "_blank")
  ),

  member(Resource.img.people.helene, "Hélène ARDUIN"),
  div(width := "75%", paddingLeft := 70)(
    html"""CNRS postdoc at ${aa("IDEES Rouen", href := shared.link.partner.ideesrouen)} (UMR 6266). As a modeller and advanced user of OpenMOLE ${b{"she leads the development of the OpenMOLE documentation"}}."""
  ),

  member(Resource.img.people.paul, "Paul CHAPRON"),
  div(width := "75%", paddingLeft := 70) (
    html"""Sustainable development researcher at ${aa("IGN", href := shared.link.partner.ign)}, interested in OpenMOLE-assisted modeling methodology, exploration of models and their results."""
  ),

  member(Resource.img.people.guillaume, "Guillaume CHEREL"),
  div(width := "75%", paddingLeft := 70) (
    html"""CNRS research engineer at ${aa("ISC-PIF", href := shared.link.partner.iscpif)}. Scientific computing and simulation researcher, he worked on the conception of methods for simulation experiments and parameter space exploration and worked on the development of @aa("MGO", href := shared.link.repo.mgo)."""
  ),

  member(Resource.img.people.etienne, "Étienne DELAY"),
  div(width := "75%", paddingLeft := 70) (
    html"""Etienne Delay is a social geographer in CIRAD-ES and UMR SENS. He's inveterate on research issues proposed by landscape dynamics and cooperation processes and solidarities. He has developed a research methodology based on fieldwork and companion modeling combined with agent-based modeling formalization.
      Inside the OpenMOLE project, he is a beta-user and contributor to the communication and documentation efforts."""
  ),

  member(Resource.img.people.mathieu, "Mathieu LECLAIRE"),
  div(width := "75%", paddingLeft := 70) (
    html"""CNRS researcher engineer at ${aa("ISC-PIF", href := shared.link.partner.iscpif)} (UPS 3611) and ${aa("Géographie-cités", href := shared.link.partner.parisgeo)} (UMR 8504).
    Developer of the OpenMOLE Platform and ${b{"lead developer of the Graphical User Interface"}}. He developed both the ${aa("Scaladget", href := shared.link.repo.scaladget)} library and ${aa("ScalaWUI", href := shared.link.repo.scalawui)} skeleton."""
  ),

  member(Resource.img.people.jo, "Jonathan PASSERAT-PALMBACH"),
  div(width := "75%", paddingLeft := 70)(
    html"""Research associate at Imperial College London, in the ${aa("BioMedIA group", href := shared.link.partner.biomedia)}. Developer of the OpenMOLE Platform, he co-developed ${aa("GridScale", href := shared.link.repo.gridscale)}. Regularly inserts bugs in the code to keep his team-mates on task."""
  ),

  member(Resource.img.people.julien, "Julien PERRET"),
  div(width := "75%", paddingLeft := 70)(
    html"""Sustainable development researcher at ${aa("IGN", href := shared.link.partner.ign)} and EHESS."""
  ),

  member(Resource.img.people.juste, "Juste RAIMBAULT"),
  div(width := "75%", paddingLeft := 70)(
    html"""CNRS postdoc at UCL CASA (Centre for Advanced Spatial Analysis) and research associate at ${aa("Géographie-cités", href := shared.link.partner.parisgeo)} (UMR CNRS 8504). As a geographer, he contributes to the development of exploration methods for simulation models."""
  ),

  member(Resource.img.people.romain, "Romain REUILLON"),
  div(width := "75%", paddingLeft := 70)(
    html"""CNRS researcher at ${aa("ISC-PIF", href := shared.link.partner.iscpif)} and ${aa("Géographie-cités", href := shared.link.partner.parisgeo)}. Scientific manager and ${b("lead developer of OpenMOLE")}, he developed both ${aa("MGO", href := shared.link.repo.mgo)} and ${aa("GridScale", href := shared.link.repo.gridscale)}."""
  ),

  member(Resource.img.people.seb, "Sebastien REY-COYREHOURCQ"),
  div(width := "75%", paddingLeft := 70)(
    html"""Research engineer in Geomatics at ${aa("IDEES Rouen", href := shared.link.partner.ideesrouen)} (UMR 6266). I love Complex Systems, Agent-Based Models, and Evolutionary Algorithm. I probably compile and beta-test every dev version of OpenMOLE since 2010 :) I also participate in developing the ${aa("MGO", href := shared.link.repo.mgo)} framework and the ${aa("Gama-plugin", href := shared.link.repo.gamaPlugin)}."""
  )

)
