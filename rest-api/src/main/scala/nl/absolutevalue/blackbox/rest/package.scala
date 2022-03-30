package nl.absolutevalue.blackbox

package object rest {
  case class RunRequest(language: String, code: String)
  case class AcceptedResponse(uuid: String)
  case class RunResultRequest(uuid: String)
  case class RunRequestInProgressReponse(uuid: String, state: String)
  case class RunCompletedResponse(code: Int, stdout: String, stderr: String)
}
