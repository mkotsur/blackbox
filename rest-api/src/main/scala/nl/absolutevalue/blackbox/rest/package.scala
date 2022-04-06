package nl.absolutevalue.blackbox

import java.time.LocalDateTime

package object rest {
  case class RunRequest(language: String, code: String)
  case class AcceptedResponse(uuid: String)
  case class RunResultRequest(uuid: String)
  case class RunRequestInProgressReponse(uuid: String, state: String)
  case class RunCompletedResponse(
      uuid: String,
      code: Option[Int],
      stdout: String,
      stderr: String,
      timestamp: LocalDateTime,
      runRequest: RunRequest
  )
}
