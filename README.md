# MotherDocker (mʌðɚdʌkɚ) 

[![Build Status](https://travis-ci.org/fbertola/mother-docker.svg)](https://travis-ci.org/fbertola/mother-docker)
[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/fbertola/mother-docker/blob/master/LICENSE)

A simple [Docker](https://github.com/dotcloud/docker) orchestrator written in Groovy.

## About

_MotherDocker_ was created as a tool for easing integration testing with _Docker_ on the _JVM_. It is a partial rewrite in _Groovy_ of [Docker Compose](https://docs.docker.com/compose/) (formely Fig).
Although this library doesn't have (and was not intended to) all the system management options of _Docker Compose_, it can fully understand the **YAML** configuration files.         

_MotherDocker_ is based on the excellent [Spotify's Docker Client](https://github.com/spotify/docker-client).

## F.A.Q.

* **This is intended to be a replacement of Docker Compose?**

No, _MotherDocker_ it is **not** intended to be a replacement for _Docker Compose_. It lacks all the images management options (e.g. the `up` or `restart` commands) but nevertheless it can fully understand its configuration files.

* **Why the name?**

You know, for the _lulz_ :stuck_out_tongue:
