package org.openmole.rest.client

import javax.net.ssl.{SSLContext, SSLSocket}

import org.apache.http.client.methods.{CloseableHttpResponse, HttpEntityEnclosingRequestBase, HttpPost}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.ssl.{TrustSelfSignedStrategy, SSLContextBuilder, SSLConnectionSocketFactory}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.openmole.rest.messages.Token

import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success, Try}
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


  println(client.requestToken(password))

}

case class HttpError(code: Int, message: String) extends Exception


trait Client {

  implicit val formats = DefaultFormats

  def address: String
  def timeout: Duration

  def requestToken(password: String): Try[Token] = {
    val uri = new URIBuilder(address + "/token").setParameter("password", password).build
    val post = new HttpPost(uri)
    execute(post) { response =>
      parse(response.getEntity.getContent).extract[Token]
     }
  }

  def execute[T](request: HttpEntityEnclosingRequestBase)(f: CloseableHttpResponse => T): Try[T] = withClient { client =>
    val response = client.execute(request)
    try
      response.getStatusLine.getStatusCode match {
        case c if c < 400 => Success(f(response))
        case c =>
          val error = HttpError(c, responseContent(response).mkString)
          println(error.message)
          Failure(error)
      }
    finally response.close
  }


  def responseContent(response: CloseableHttpResponse) = Source.fromInputStream(response.getEntity.getContent)


  def withClient[T](f: CloseableHttpClient => T): T = {
       val client = HttpClients.custom().setSSLSocketFactory(factory).build()
      try f(client)
    finally client.close
  }

  //def start

  @transient lazy val factory = {
    val builder = new SSLContextBuilder
    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy)
    new SSLConnectionSocketFactory(builder.build, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
  }


   /* new SSLConnectionSocketFactory(, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER) {
      override protected def prepareSocket(socket: SSLSocket) = {
        socket.setSoTimeout(timeout.toMillis.toInt)
      }
    }*/

}