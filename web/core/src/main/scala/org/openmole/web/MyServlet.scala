package org.openmole.web

import _root_.akka.actor.ActorSystem
import org.scalatra._
import scalate.ScalateSupport
import servlet.{ FileItem, FileUploadSupport }
import java.io.InputStream
import javax.servlet.annotation.MultipartConfig
import org.openmole.core.serializer._
import org.openmole.core.implementation.mole.MoleExecution
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import concurrent.{ ExecutionContext, Future }
import org.openmole.web.DataHandler
import org.openmole.core.model.job.State._
import concurrent.duration._
import org.slf4j.impl.StaticLoggerBinder

@MultipartConfig(maxFileSize = 3 * 1024 * 1024) //research scala multipart config
class MyServlet(val system: ActorSystem) extends ScalatraServlet with ScalateSupport with FileUploadSupport with FlashMapSupport with FutureSupport {

  protected implicit def executor: ExecutionContext = system.dispatcher

  get("/index.html") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {
        ssp("/index.ssp")
      }
    }
  }

  get("/") {
    new AsyncResult() {
      val is = Future { halt(status = 301, headers = Map("Location" -> url("index.html"))) }
    }
  }

  get("/createMole") {
    contentType = "text/html"
    new AsyncResult() {
      val is = Future {
        ssp("/createMole", "body" -> "Please upload a serialized mole execution below!")
      }
    }
  }

  var moleExecs = Map.empty[String, MoleExecution]
  val testMoleData = new DataHandler[String, MoleExecution](system)

  def getStatus(exec: MoleExecution): String = {
    if (!exec.started)
      "Stopped"
    else if (!exec.finished)
      "Running"
    else
      "Finished"
  }

  def processXMLFile[A](file: Option[FileItem], is: Option[InputStream]) = {
    file match {
      case Some(data) ⇒
        if (data.getContentType.isDefined && data.getContentType.get == "text/xml")
          try {
            is.map(SerializerService.deserialize[A](_)) -> ""
          } catch {
            case e: CannotResolveClassException ⇒ None -> "The uploaded xml was not a valid serialized object."
          } finally {
            is.foreach(_.close())
          }
        else
          None -> "The uploaded data was not of type text/xml"
      case None ⇒ None -> "No data was uploaded."
    }
  }

  post("/createMole") {
    contentType = "text/html"

    implicit def timeout = Duration(10, SECONDS)

    val data = fileParams.get("file")

    //TODO: Make sure this is not a problem
    val ins = fileParams.get("file").map(_.getInputStream)

    new AsyncResult() {
      val is = Future {
        val moleExec = processXMLFile[MoleExecution](data, ins)

        moleExec match {
          case (Some(exec), _) ⇒ {
            //moleExecs += (exec.id -> exec) // can't be used by async pages.
            testMoleData.add(exec.id, exec)
            println("added " + exec.id + "to testMoleData.")
            halt(status = 301, headers = Map("Location" -> url("execs")))
          }
          case (_, error) ⇒ ssp("/createMole", "body" -> "Please upload a serialized mole execution below!", "errors" -> List(error))
        }
      }
    }
  }

  post("/xml/createMole") {
    val moleExec = processXMLFile[MoleExecution](fileParams.get("file"), fileParams.get("file").map(_.getInputStream))

    moleExec match {
      case (Some(exec), _) ⇒ {
        moleExecs = moleExecs + (exec.id -> exec)
        redirect("/xml/execs")
      }
      case (_, error) ⇒ <error>{ error }</error>
    }
  }

  get("/execs") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {
        ssp("/loadedExecutions", "ids" -> testMoleData.getKeys.toList)
      }
    }
  }

  get("/execs/:id") {
    contentType = "text/html"

    val pRams = params("id")

    new AsyncResult() {
      val is = Future {
        testMoleData.get(pRams) match {
          case Some(exec) ⇒ {
            println(getStatus(exec))
            println(exec)

            val pageData = (exec.moleJobs foldLeft Map("READY" -> 0,
              "RUNNING" -> 0,
              "COMPLETED" -> 0,
              "FAILED" -> 0,
              "CANCELLED" -> 0)) {
                case (statMap, job) ⇒ statMap.updated(job.state.name, statMap(job.state.name) + 1)
              } + ("id" -> pRams) + ("status" -> getStatus(exec)) + ("totalJobs" -> exec.numberOfJobs)

            ssp("/executionData", pageData.toSeq: _*)
          }
          case _ ⇒ ssp("createMole", "body" -> "no such id")
        }
      }
    }
  }

  get("/start/:id") {
    contentType = "text/html"

    println("here")

    val x = params("id")
    println("started starting")

    new AsyncResult() {
      override implicit def timeout = Duration(1, MINUTES)
      val is = Future {

        val exec = testMoleData.get(x)
        println(exec)

        exec foreach { x ⇒ x.start; println("started") }

        halt(status = 301, headers = Map("Location" -> ("/execs/" + x)))
        //ssp("/redirect", "body" -> ("Redirecting to execs/" + x + " in 5 seconds"), "title" -> "Started execution!", "redirectURL" -> ("execs/" + x))
      }
    }
  }

  get("/badAsyncTest") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {
        redirect("/createMole")
      }
    }
  }

  get("/xml/execs") {
    contentType = "text/xml"

    <mole-execs>
      { for (key ← moleExecs.keys) yield <execID>{ key }</execID> }
    </mole-execs>
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    resourceNotFound()
  }
}
