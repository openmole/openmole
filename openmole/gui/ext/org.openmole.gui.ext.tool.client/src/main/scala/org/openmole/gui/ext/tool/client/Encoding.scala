package org.openmole.gui.ext.tool.client

object Encoding {

  def encode(s: String): String = {
    (for (
      c ← s
    ) yield {
      c match {
        case '+'  ⇒ "%2b"
        case ':'  ⇒ "%3a"
        case '/'  ⇒ "%2f"
        case '?'  ⇒ "%3f"
        case '#'  ⇒ "%23"
        case '['  ⇒ "%5b"
        case ']'  ⇒ "%5d"
        case '@'  ⇒ "%40"
        case '!'  ⇒ "%21"
        case '$'  ⇒ "%24"
        case '&'  ⇒ "%26"
        case '\'' ⇒ "%27"
        case '('  ⇒ "%29"
        case ')'  ⇒ "%28"
        case '*'  ⇒ "%2a"
        case ','  ⇒ "%2c"
        case ';'  ⇒ "%3b"
        case '='  ⇒ "%3d"
        case _    ⇒ c
      }
    }).mkString("")
  }
}
