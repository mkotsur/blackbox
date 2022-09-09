# Common issues

## Docker socket permission and connectivity

At this moment (v0.1), Blackbox's process needs direct access to the socket of Docker service (`/var/run/docker.sock`).

**Symptom**

```
java.lang.RuntimeException: java.io.IOException: com.sun.jna.LastErrorException: [61] Connection refused
```

or

```
java.lang.RuntimeException: java.io.IOException: com.sun.jna.LastErrorException: [13] Permission denied
```

**Solution**

Ensure that Docker service is up and running and the user running Blackbox has got `rw` permissions on the socket file.

## Data samples and/or outputs paths are not mountable

**Symptom:**
This error when trying to run container.

```
com.github.dockerjava.api.exception.InternalServerErrorException: Status 500: Mounts denied:
The path /Users/mike/Projects/sm-data/.data is not shared from the host and is not known to Docker.
You can configure shared paths from Docker -> Preferences... -> Resources -> File Sharing.
See https://docs.docker.com/desktop/mac for more info.

	at com.github.dockerjava.core.DefaultInvocationBuilder.execute(DefaultInvocationBuilder.java:247)
	at com.github.dockerjava.core.DefaultInvocationBuilder.post(DefaultInvocationBuilder.java:102)
	at com.github.dockerjava.core.exec.StartContainerCmdExec.execute(StartContainerCmdExec.java:31)
```

**Solution:** Docker needs to mount this paths inside the container. You can configure them with env variables
`RDX_DATA_SAMPLES_PATH` and `RDX_OUTPUTS_PATH` respectively.
