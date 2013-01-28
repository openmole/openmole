package org.openmole.web

import org.scalatra._
import scalate.ScalateSupport
import servlet.FileUploadSupport
import java.io.File

class MyServlet extends ScalatraServlet with ScalateSupport with FileUploadSupport {
  get("/") {
    <html>
      <body>
        <img src="images/small-bart.jpg"></img>
        <h1>Hello, world!</h1>
        Say<a href="hello-scalate">hello to Scalate</a>
        .
        <form name="file" action="/" method="POST">
          <input type="File" name="imgfile"></input>
          <input type="Submit"/>
        </form>
      </body>
    </html>
  }

  post("/") {
    contentType = "text/html"
    val file = new File(servletContext.getRealPath("images/new.jpg"))
    val img = if (file.createNewFile()) {
      fileParams.get("file") map (data ⇒ {; file.getPath }) getOrElse ("images/small-bart.jpg")
    } else "images/small-bart.jpg"

    <html>
      <body>
        <img src={ img }></img>
        <h1>Hello, world!</h1>
        Say<a href="hello-scalate"> hello to Scalate</a>
        .
        <form name="file" action="/" method="POST">
          <input type="File" name="imgfile"></input>
          <input type="Submit"/>
        </form>
      </body>
    </html>
  }

  get("/bort") {
    contentType = "text/html"
    ssp("/bort", "layout" -> "WEB-INF/layouts/bort.ssp", "text" -> "Openmole is great!")
  }

  post("/bort") {
    contentType = "text/html"
    ssp("/bort", "layout" -> "WEB-INF/layouts/bort.ssp", "text" -> params.get("bortText").getOrElse(""))
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path ⇒
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }
}
