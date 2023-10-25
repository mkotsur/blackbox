package github4s.algebras

import github4s.GHResponse
import github4s.domain.*

trait WorkflowRuns[F[_]] {

  def getWorkflows(
      owner: String,
      repo: String,
      pagination: Option[Pagination] = None,
      headers: Map[String, String] = Map()
  ): F[GHResponse[WorkflowRun.Envelope]]

}
