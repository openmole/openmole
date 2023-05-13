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

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, _}
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._



object Training extends PageContent(html"""
${h2{"The eX Modelo school"}}
Since 2019, the OpenMOLE team organizes a ${b{"summer school about model exploration"}} (sensitivity analysis, calibration, validation, ${i{"etc."}}).
The applied examples of the theoretical concepts taught during the school use the tools available on the OpenMOLE platform, making it a great opportunity to learn both about model exploration in general, and the use of the platform!

$br

For more information on the school, please visit the ${aa("eX Modelo website", href := shared.link.exmodelo)}.

${h2{"Short and free discovery trainings"}}

We are also open to come and present OpenMOLE at institutions or to teams who would like to use the platform.
To discuss about this possibility, please contact us via the ${aa("forum", href := shared.link.forum)} or the ${aa("chat", href := shared.link.chat)}.
""")
