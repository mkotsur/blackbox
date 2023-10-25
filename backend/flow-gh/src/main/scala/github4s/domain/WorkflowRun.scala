package github4s.domain

object WorkflowRun {
  final case class Envelope(total_count: Int, workflow_runs: List[WorkflowRun])
}

final case class WorkflowRun(
    id: Long,
    workflow_id: Long,
    name: String,
    node_id: String,
    head_branch: String,
    head_sha: String,
    run_number: Long,
    event: String, //"event": "push"
    status: String, // "status": "queued"
    html_url: String,
    url: String

//"check_suite_id": 42
//"check_suite_node_id": "MDEwOkNoZWNrU3VpdGU0Mg=="
//"display_title": "Update readme.md"
//"conclusion": null
//"pull_requests": []
//"created_at": "2020-01-22T19:33:08Z"
//"updated_at": "2020-01-22T19:33:08Z"
)
