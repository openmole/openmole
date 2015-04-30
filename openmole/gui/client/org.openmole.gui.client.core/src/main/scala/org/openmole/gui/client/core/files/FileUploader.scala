package org.openmole.gui.client.core.files

import org.scalajs.dom.raw.{ FileList, FormData, XMLHttpRequest }

/*
 * Copyright (C) 29/04/15 // mathieu.leclaire@openmole.org
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

object FileUploader {

  def apply(fileList: FileList,
            destinationPath: String) = {
    val formData = new FormData

    for (i ← 0 to fileList.length - 1) {
      val file = fileList(i)
      formData.append(destinationPath + "/" + file.name, file)
    }

    val xhr = new XMLHttpRequest
    xhr.open("POST", "uploadfiles")
    xhr.send(formData)
  }

}
