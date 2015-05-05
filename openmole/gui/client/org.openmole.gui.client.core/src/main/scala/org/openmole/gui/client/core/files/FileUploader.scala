package org.openmole.gui.client.core.files

import org.scalajs.dom.raw.{ ProgressEvent, FileList, FormData, XMLHttpRequest }

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

sealed trait FileUploadState {
  def ratio: Int = 0

  def display: String = ""
}

case class Uploading(override val ratio: Int) extends FileUploadState {
  override def display: String = "Uploading... " + ratio + " %"
}

case class Finalizing(override val ratio: Int = 100,
                      override val display: String = "Finalizing...") extends FileUploadState

case class Uploaded(override val ratio: Int = 100) extends FileUploadState

case class Standby() extends FileUploadState

object FileUploader {

  def fileNames(fileList: FileList): Seq[String] = {
    var nameList = Seq[String]()
    for (i ← 0 to fileList.length - 1) {
      nameList = nameList :+ fileList(i).name
    }
    nameList
  }

  def apply(fileList: FileList,
            destinationPath: String,
            fileUploadState: FileUploadState ⇒ Unit) = {
    var formData = new FormData

    for (i ← 0 to fileList.length - 1) {
      val file = fileList(i)
      formData.append(destinationPath + "/" + file.name, file)
    }

    val xhr = new XMLHttpRequest

    xhr.upload.onprogress = (e: ProgressEvent) ⇒ {
      fileUploadState(Uploading((e.loaded.toDouble * 100 / e.total).toInt))
    }

    xhr.upload.onloadend = (e: ProgressEvent) ⇒ {
      fileUploadState(Finalizing())
    }

    xhr.onloadend = (e: ProgressEvent) ⇒ {
      fileUploadState(Uploaded())
    }

    xhr.open("POST", "uploadfiles", true)
    xhr.send(formData)
  }

}
