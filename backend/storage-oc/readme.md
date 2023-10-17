# Owncloud Storage Plugin

## OwnCloud in Docker For Testing 

```
docker pull owncloud/server
docker run --rm --name oc-eval -d -p8080:8080 owncloud/server
docker logs -f  oc-eval
```