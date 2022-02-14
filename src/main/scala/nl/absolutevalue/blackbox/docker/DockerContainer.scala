package nl.absolutevalue.blackbox.docker

case class DockerContainer(containerId: String, state: DockerContainer.State)

object DockerContainer {
  enum State(terminal: Boolean):
    case Created extends State(false)
    case Running extends State(false)
    case ExitFail(stdout: String, stderr: String, code: Int) extends State(terminal = true)
    case ExitSuccess(stdout: String, stderr: String) extends State(terminal = true)

  object State {

    class StateParsingError(state: String, code: Int)
        extends RuntimeException(
          s"Couln't parse Docker container state \"$state\" with code \"$code\""
        )

    def state(state: String, code: Int): Either[StateParsingError, State] = {
      state.toLowerCase match {
        case "running"             => Right(Running)
        case "exited" if code == 0 => Right(ExitSuccess("", ""))
        case "exited"              => Right(ExitFail("", "", code))
        case _                     => Left(new StateParsingError(state, code))
      }
    }

  }
}
