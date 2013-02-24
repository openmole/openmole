package org.openmole.web

import _root_.akka.actor.{ Props, ActorSystem }
import org.scalatra._
import org.scalatra.servlet._
import scalate.ScalateSupport
import servlet._
import java.io._
import java.security.MessageDigest
import concurrent.{ Await, ExecutionContext, Future }
import scala.util.matching.Regex
import concurrent.Future
import concurrent.duration._
import org.openmole.ide.core.implementation.execution._
import org.openmole.ide.core.implementation.serializer._
import org.openmole.core.serializer.SerializerService
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import javax.servlet.{ MultipartConfigElement, ServletException }
import scala.collection.JavaConversions._

class MyRepoServlet(val system: ActorSystem) extends ScalatraServlet with ScalateSupport with FileUploadSupport with FutureSupport with SlickSupport {
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(3 * 1024 * 1024), fileSizeThreshold = Some(1024 * 1024 * 1024)))

  protected implicit def executor: ExecutionContext = system.dispatcher

  val digest = MessageDigest.getInstance("MD5")
  def md5(t: String): String = digest.digest(t.getBytes).map("%02x".format(_)).mkString

  def recursiveListFiles(f: File, r: Regex): Array[File] = {
    val these = f.listFiles
    val good = these.filter(f ⇒ r.findFirstIn(f.getName).isDefined)
    good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_, r))
  }

  get("/uploadMole") {
    contentType = "text/html"
    new AsyncResult() {
      val is = Future {
        ssp("/uploadMole", "body" -> "Please upload an om file below!")
      }
    }
  }

  get("/oms") {

    /*    <html>
      <body>
        <h1>Hello, world!</h1>
        Say<a href="hello-scalate">hello to Scalate</a>
        .
      </body>
    </html>*/
  }

  def createFile(name: String): File = {

    //    println(" system property = " + System.getProperty("user.dir"))

    val ftest = new File(servletContext.getRealPath("/")).getAbsolutePath()
    val path: String = servletContext.getRealPath("/")
    val file = new File(path + "/repository/" + name)
    println("FILE name = " + file.getName + " / " + file.getPath)

    /*try {
      //file.createNewFile()
      //file.setWritable(true)
    } catch {
      case e: IOException ⇒ println("Error " + e)
    } */
    file
  }

  // This method processes the uploaded file in some way.
  def processFile(upload: Option[FileItem]) = {
    upload match {
      case Some(fileI: FileItem) ⇒
        val filePath: String = "/tmp/"
        println(">> " + fileI.getName)
        try {
          val file: File = new File(filePath + fileI.getName)
          fileI.write(file)
        } catch {
          case e: IOException ⇒ println("Error " + e)
        }

    }
  }

  post("/uploadMole") {
    contentType = "text/html"

    implicit def timeout = Duration(10, SECONDS)

    val x = new AsyncResult() {
      val is = Future {

        val document = fileParams("file")
        println("why not try to write ?")

        try {
          val tempFile = File.createTempFile("scalatra-test-", document.name)
          document.write(tempFile)
          "file size: " + tempFile.length
        } catch {
          case e: IOException ⇒ println("IO Error " + e)
          case s: SecurityException ⇒ println("Security Error " + s)
        }

        //tempFile.deleteOnExit

        //val data = fileParams.get("file")
        //processFile(data)

        //file.setWritable(true)

        /*val out = new FileOutputStream("/tmp/test.om")
            File.copy(x, out)
            out.close()*/
        /*
        fileParams.get("file") match {
          case Some(file) ⇒
            try {
              //val f: File = createFile("task.om")
              //val f = createFile("task.om")
              //file.write(f)


              println("datafile = " + f)
              new GUISerializer(f.toString).unserialize
              ScenesManager.moleScenes.map { s ⇒ println("name : " + s.manager.name) }

            } catch {
              case e: CannotResolveClassException ⇒ None -> "The uploaded xml was not a valid serialized object."
            }
          case None ⇒ println("Error when read file")
        }
  */

      }
    }

    println(x.timeout)
    x
  }

}