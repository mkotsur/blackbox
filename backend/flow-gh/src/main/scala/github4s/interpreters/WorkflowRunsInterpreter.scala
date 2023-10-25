package github4s.interpreters

import github4s.GHResponse
import github4s.algebras.{Users, WorkflowRuns}
import github4s.domain.{Pagination, User, WorkflowRun}
import github4s.http.HttpClient
import github4s.domain._
import github4s.Decoders._
import io.circe.generic.auto._

class WorkflowRunsInterpreter[F[_]](implicit client: HttpClient[F]) extends WorkflowRuns[F] {

  override def getWorkflows(
      owner: String,
      repo: String,
      pagination: Option[Pagination],
      headers: Map[String, String]
  ): F[GHResponse[WorkflowRun.Envelope]] =
    client.get[WorkflowRun.Envelope](s"repos/${owner}/${repo}/actions/runs", headers)

}
