package nl.absolutevalue.blackbox.flow.gh

import cats.data.Kleisli
import cats.effect.{IO, IOApp, Resource}
import com.github.sardine.{DavResource, Sardine}
import com.typesafe.config.Config
import github4s.GithubConfig
import github4s.algebras.WorkflowRuns
import github4s.http.HttpClient
import github4s.interpreters.{StaticAccessToken, WorkflowRunsInterpreter}
import nl.absolutevalue.blackbox.flow.gh.Streams
import nl.absolutevalue.blackbox.flow.gh.Streams.State
import nl.absolutevalue.blackbox.flow.gh.conf.OwncloudConf
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import github4s.Github

/** Goal of this experiment:
  *   - Make connection to OwnCloud container;
  *   - Use clean configuration without hardcoding anything;
  *   - Map Sardine path to dataset-level paths;
  *   - Prototype flow model with dependency injection and separated IO
  */
object FlowDemo extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val run: IO[Unit] = {

    val accessToken = new StaticAccessToken[IO](sys.env.get("GH_TOKEN"))

    implicit val httpClient: HttpClient[IO] =
      new HttpClient[IO](JavaNetClientBuilder[IO].create, implicitly[GithubConfig], accessToken)
//
    val workflowRuns = new WorkflowRunsInterpreter[IO]
//
    workflowRuns
      .getWorkflows("mkotsur", "blackbox")
      .flatMap(res => IO.fromEither(res.result))
      .map(_.workflow_runs.head)
      .flatMap(wr => Logger[IO].info(s"${wr.status} ${wr.html_url} ${wr.head_branch}")) >>
      Streams.pipelines[IO].evalMap(s => logger.info(s.toString)).compile.drain >>
      Logger[IO].info(s"- End Demo")
  }
}
