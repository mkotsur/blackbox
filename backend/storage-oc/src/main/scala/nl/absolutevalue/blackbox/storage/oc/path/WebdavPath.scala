package nl.absolutevalue.blackbox.storage.oc.path

import java.net.URI

//TODO: should user path be an option???
case class WebdavPath(serverUri: URI, serverSuffix: String, userPath: Option[String] = None)
