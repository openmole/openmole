package org.openmole.rest.client

import java.io.File
import javax.net.ssl.{ SSLContext, SSLSocket }

import org.apache.http.client.methods._
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.ssl.{ TrustSelfSignedStrategy, SSLContextBuilder, SSLConnectionSocketFactory }
import org.apache.http.entity.mime._
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.openmole.rest.message._
import org.openmole.tool.tar._
import org.openmole.tool.file._

import scala.concurrent.duration._
import scala.io.Source
import scala.util.{ Failure, Success, Try }
import org.json4s._
import org.json4s.jackson.JsonMethods._

object RESTClient extends App {

  val url = args(0)
  val password = args(1)

  val client =
    new Client {
      override def address: String = url
      override def timeout: Duration = 5 minutes
    }

  val token = client.requestToken(password).left.get.token

  val script =
    """
      |val i = Val[Double]
      |val res = Val[Double]
      |val test = Val[File]
      |
      |val file = workDirectory / "test.txt"
      |
      |val exploration = ExplorationTask(i in (0.0 to 10.0 by 1.0))
      |
      |val model =
      |  ScalaTask("val res = i * 2") set (
      |    inputs += i,
      |    outputs += (i, res, test),
      |    test := file
      |  )
      |
      |val copyFile = CopyFileHook(test, workDirectory / "result${i}.txt")
      |
      |val env = EGIEnvironment("vo.complex-systems.eu", name = "complexsystems")
      |
      |exploration -< (model on env hook ToStringHook() hook copyFile)
    """.stripMargin

  val project = new File("/tmp/project")
  project.mkdirs()

  project / "test.txt" content = "test"
  project / "script.oms" content = script

  val projectArchive = new File("/tmp/archive.tgz")
  project.archive(projectArchive)

  val id = client.start(token, projectArchive, "script.oms")
  println(id)
  Iterator.continually(client.state(token, id.left.get.id)).takeWhile(_.left.get.state == ExecutionState.running).foreach { s ⇒
    println(s)
    Thread.sleep(1000)
  }

  println(client.state(token, id.left.get.id))
  println(client.output(token, id.left.get.id))

  val res = new File("/tmp/result.tgz")
  println(client.download(token, id.left.get.id, "/", res))
  println(client.remove(token, id.left.get.id))

}

case class HttpError(code: Int, error: Option[Error])

trait Client {

  implicit val formats = DefaultFormats

  def address: String
  def timeout: Duration

  def requestToken(password: String): Either[Token, HttpError] = {
    val uri = new URIBuilder(address + "/token").setParameter("password", password).build
    val post = new HttpPost(uri)
    execute(post) { response ⇒
      parse(response.content).extract[Token]
    }
  }

  def start(token: String, workDirectory: File, scriptPath: String): Either[ExecutionId, HttpError] = {
    def file = {
      val builder = MultipartEntityBuilder.create()
      builder addBinaryBody ("workDirectory", workDirectory)
      builder.build
    }

    val uri =
      new URIBuilder(address + "/start").
        setParameter("token", token).
        setParameter("script", scriptPath).build

    val post = new HttpPost(uri)
    post.setEntity(file)
    execute(post) { response ⇒
      parse(response.content).extract[ExecutionId]
    }
  }

  def state(token: String, id: String): Either[State, HttpError] = {
    val uri =
      new URIBuilder(address + "/state").
        setParameter("token", token).
        setParameter("id", id).build
    val post = new HttpPost(uri)

    execute(post) { response ⇒
      val json = parse(response.content)
      json \ "state" match {
        case JString(ExecutionState.failed)   ⇒ json.extract[Failed]
        case JString(ExecutionState.running)  ⇒ json.extract[Running]
        case JString(ExecutionState.finished) ⇒ json.extract[Finished]
        case _                                ⇒ sys.error("Unexpected state in: " + json)
      }
    }
  }

  def output(token: String, id: String): Either[Output, HttpError] = {
    val uri =
      new URIBuilder(address + "/output").
        setParameter("token", token).
        setParameter("id", id).build

    val post = new HttpPost(uri)
    execute(post) { response ⇒ parse(response.content).extract[Output] }
  }

  def download(token: String, id: String, path: String, file: File): Either[Unit, HttpError] = {
    val uri =
      new URIBuilder(address + "/").
        setParameter("token", token).
        setParameter("id", id).
        setParameter("path", path).build
    val post = new HttpPost(uri)
    execute(post) {
      response ⇒
        response.getEntity.getContent().copy(file)
    }
  }

  def remove(token: String, id: String): Either[Unit, HttpError] = {
    val uri =
      new URIBuilder(address + "/remove").
        setParameter("token", token).
        setParameter("id", id).build
    val post = new HttpPost(uri)
    execute(post) { _ ⇒ Unit }
  }

  def execute[T](request: HttpRequestBase)(f: CloseableHttpResponse ⇒ T): Either[T, HttpError] = withClient { client ⇒
    val response = client.execute(request)
    try
      response.getStatusLine.getStatusCode match {
        case c if c < 400 ⇒ Left(f(response))
        case c            ⇒ Right(HttpError(c, parse(response.content).extractOpt[Error]))
      }
    finally response.close
  }

  implicit class ResponseDecorator(response: CloseableHttpResponse) {
    def content = {
      val source = Source.fromInputStream(response.getEntity.getContent)
      try source.mkString
      finally source.close
    }
  }

  def withClient[T](f: CloseableHttpClient ⇒ T): T = {
    val client = HttpClients.custom().setSSLSocketFactory(factory).build()
    try f(client)
    finally client.close
  }

  @transient lazy val factory = {
    val builder = new SSLContextBuilder
    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy)
    new SSLConnectionSocketFactory(builder.build, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER) {
      override protected def prepareSocket(socket: SSLSocket) = {
        socket.setSoTimeout(timeout.toMillis.toInt)
      }
    }
  }

}