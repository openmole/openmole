package org.openmole.web

import _root_.akka.actor.ActorSystem
import org.scalatra._
import scalate.ScalateSupport
import servlet.{ FileItem, FileUploadSupport }
import java.io.{ PrintStream, File, InputStream }
import javax.servlet.annotation.MultipartConfig

import org.openmole.core.serializer._
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.data.Context
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import concurrent.Future
import org.openmole.core.model.mole.ExecutionContext

import slick.driver.H2Driver.simple._
import slick.jdbc.meta.MTable

import Database.threadLocalSession

import javax.sql.rowset.serial.SerialClob
import reflect.ClassTag
import scala.None

@MultipartConfig(maxFileSize = 3 * 1024 * 1024 /*max file size of 3 MiB*/ ) //research scala multipart config
class MoleRunner(val system: ActorSystem) extends ScalatraServlet with SlickSupport with ScalateSupport with FileUploadSupport with FlashMapSupport with FutureSupport {

  protected implicit def executor: concurrent.ExecutionContext = system.dispatcher

  val cachedMoles = new DataHandler[String, IMoleExecution](system)

  def getStatus(exec: IMoleExecution): String = {
    if (!exec.started)
      "Stopped"
    else if (!exec.finished)
      "Running"
    else
      "Finished"
  }

  def processXMLFile[A: ClassTag](file: Option[FileItem], is: Option[InputStream]): (Option[A], String) = {
    file match {
      case Some(data) ⇒
        if (data.getContentType.isDefined && data.getContentType.get == "text/xml") {
          try {
            val ret = is.map(SerializerService.deserialize[A](_))
            if(!ret.forall(evidence$1.runtimeClass.isInstance(_)))
              None -> s"The uploaded xml is not a subtype of the type you wished to deserialize to: ${evidence$1.runtimeClass} vs ${ret.get.getClass}"
            else
              ret -> ""
          } catch {
            case e: CannotResolveClassException ⇒ None -> "The uploaded xml was not a valid serialized object."
            case c: ClassCastException ⇒ None -> "Blargh"
          }
        } else
          None -> "The uploaded data was not of type text/xml"
      case None ⇒ None -> "No data was uploaded."
    }
  }

  get("/index.html") {
    contentType = "text/html"

    //todo: Move this to the Scalatra file.
    db withSession {
      if (MTable.getTables("MoleData").list().isEmpty)
        MoleData.ddl.create // check that table exists somehow
    }

    new AsyncResult() {
      val is = Future {
        ssp("/index.ssp")
      }
    }
  }

  get("/") {
    new AsyncResult() {
      val is = Future { redirect(url("index.html")) }
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

  post("/createMole") {

    val data = fileParams.get("file")

    val inS = data.map(_.getInputStream)

    //TODO: make sure this is released

    new AsyncResult {
      val is = Future {

        contentType = "text/html"

        val moleExec = processXMLFile[(Context,ExecutionContext) => IMoleExecution](data, inS)

        val context = new ExecutionContext(new PrintStream(new File("./out")), null)

        moleExec match {
          case (Some(pEx), _) ⇒ {
            val exec = pEx(Context.empty, context)

            val clob = new SerialClob(SerializerService.serialize(exec).toCharArray)

            db withSession {
              MoleData.insert((exec.id, getStatus(exec), clob))
            }
            cachedMoles.add(exec.id, exec)
            redirect(url("execs"))
          }
          case (_, error) ⇒ ssp("/createMole", "body" -> "Please upload a serialized mole execution below!", "errors" -> List(error))
        }
      }
    }
  }

  //todo: update this for async
  post("/xml/createMole") {
    val moleExec = processXMLFile[IMoleExecution](fileParams.get("file"), fileParams.get("file").map(_.getInputStream))

    moleExec match {
      case (Some(exec), _) ⇒ {
        cachedMoles.add(exec.id, exec)
        redirect("/xml/execs")
      }
      case (_, error) ⇒ <error>{ error }</error>
    }
  }

  get("/execs") {
    new AsyncResult() {
      contentType = "text/html"

      val is = Future {
        db withSession {
          val ids = for {
            m ← MoleData
          } yield m.id.asColumnOf[String]

          ssp("/loadedExecutions", "ids" -> ids.list)
        }
      }
    }
  }

  get("/execs/:id") {
    new AsyncResult() {
      val is = Future {
        contentType = "text/html"

        val pRams = params("id")

        val mole = db withSession {

          MoleData.filter(_.id === pRams).map(_.clobbedMole).list().headOption match {
            case Some(head) ⇒ Some(SerializerService.deserialize[IMoleExecution](head.getAsciiStream))
            case _ ⇒ None
          }
        }

        def returnStatusPage(exec: IMoleExecution) = {
          val pageData = (exec.moleJobs foldLeft Map("Ready" -> 0,
            "Running" -> 0,
            "Completed" -> 0,
            "Failed" -> 0,
            "Cancelled" -> 0)) {
              case (statMap, job) ⇒ statMap.updated(job.state.name, statMap(job.state.name) + 1)
            } + ("id" -> pRams) + ("status" -> getStatus(exec)) + ("totalJobs" -> exec.moleJobs.size)

          ssp("/executionData", pageData.toSeq: _*)
        }

        (cachedMoles.get(pRams), mole) match {
          case (Some(exec), _) ⇒ returnStatusPage(exec)
          case (None, Some(exec)) ⇒ cachedMoles.add(exec.id, exec); returnStatusPage(exec)
          case _ ⇒ ssp("createMole", "body" -> mole.toString)
        }

      }
    }
  }

  get("/start/:id") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {

        val exec = cachedMoles.get(params("id"))
        println(exec)

        exec foreach { x ⇒ x.start; println("started") }

        redirect("/execs/" + params("id"))
      }
    }
  }

  get("/xml/execs") {
    contentType = "text/xml"

    <mole-execs>
      { for (key ← cachedMoles.getKeys) yield <execID>{ key }</execID> }
    </mole-execs>
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    resourceNotFound()
  }
}
