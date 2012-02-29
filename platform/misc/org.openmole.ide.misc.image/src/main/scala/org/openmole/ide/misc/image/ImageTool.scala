/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.misc.image

//import java.awt.Graphics
import java.awt.Image
//import java.awt.Rectangle
//import java.awt.Toolkit
//import java.awt.image.BufferedImage
//import java.awt.image.FilteredImageSource
//import java.awt.image.ReplicateScaleFilter
//import org.apache.batik.transcoder.TranscodingHints
//import org.apache.batik.transcoder.TranscoderInput
//import org.apache.batik.transcoder.TranscoderOutput
//import org.apache.batik.transcoder.image.ImageTranscoder
//import org.apache.batik.transcoder.SVGAbstractTranscoder._
import java.awt.Toolkit
import java.awt.image.FilteredImageSource
import java.awt.image.ReplicateScaleFilter
import org.openide.util.ImageUtilities

object ImageTool { 
  
  def loadImage(path: String,width: Int, height: Int): Image = {
    path.split('.')(1) match {
      // case "svg"=> svg2Image(path, width, height)
      case "png"=> png2Image(path, width, height)
    }
  }
  
  def png2Image(pngpath: String,width: Int, height: Int): Image = 
//    (new MagickImage(new ImageInfo(pngpath)).scaleImage(width, height)).writeImage(new ImageInfo("/tmp/aa.png"))
//    ImageUtilities.loadImage("/tmp/aa.png")}
//    
    
      
    
    Toolkit.getDefaultToolkit.createImage(
      new FilteredImageSource(ImageUtilities.loadImage(pngpath).getSource,new ReplicateScaleFilter(width,height)))
  
  
//  def svg2Image(svgpath: String,width: Int, height: Int): Image = {
//    val transcoder = new MyTranscoder
//    val  hints = new TranscodingHints 
//    hints.put(KEY_WIDTH, width)
//    hints.put(KEY_HEIGHT, height)
//    transcoder.setTranscodingHints(hints)
//
//    transcoder.transcode(new TranscoderInput(svgpath), null)
//    transcoder.image.get
//    ImageUtilities.loadImage(svgpath)
//  }
  
//  class MyTranscoder extends ImageTranscoder {
//    val image:Option[BufferedImage] = None
//    override def createImage(w: Int,h: Int)= new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
//    override def writeImage(img: BufferedImage, out: TranscoderOutput) {}
//  }
}
