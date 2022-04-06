## Blackbox 🔲 | ![Run SBT tests](https://github.com/mkotsur/blackbox/actions/workflows/run-sbt-test.yml/badge.svg)


**Status:** Spiking Scala 3.

Secure container for data exchange. Re-vamped.
With Scala 3 and Cats Effect 3.

More info: [sara-nl/data-exchange](https://github.com/sara-nl/data-exchange)


# Using API

```shell

# Submit a run
curl -X POST localhost:8080/submit -d '{"code": "print(43);", "language": "python"}'

# Get previous runs
curl http://localhost:8080/completed

# [
#   {"code":0,"stdout":"43\n","stderr":""}
# ]
```
