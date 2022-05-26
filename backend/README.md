## Blackbox 🔲 | ![Run SBT tests](https://github.com/mkotsur/blackbox/actions/workflows/run-sbt-test.yml/badge.svg)

**Web interface for Blackbox**: [mkotsur/blackbox-web](https://github.com/mkotsur/blackbox-web)

Secure container for data exchange. Re-vamped.
With Scala 3 and Cats Effect 3.

More info: [sara-nl/data-exchange](https://github.com/sara-nl/data-exchange)


# Starting the server

Requires Docker to be installed on the host!

```shell
sbt "restApi/run"
```

# Using API

```shell

# Submit a run
curl -X POST localhost:8080/submit -d '{"code": "print(43);", "language": "python"}'

# Returns UUID of accepted run.
# {"uuid":"da1d5e33-be86-4cc5-8a43-fc0df79b1cb9"}

# Get previous runs
curl http://localhost:8080/completed

#[
#  {
#    "uuid":"da1d5e33-be86-4cc5-8a43-fc0df79b1cb9",
#    "code":0,
#    "stdout":"43\n",
#    "stderr":"",
#    "timestamp":"2022-04-13T09:55:45.248969",
#    "runRequest":{"language":"python","code":"print(43);"}
#  }
# ]
```