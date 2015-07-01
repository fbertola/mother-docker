# MotherDocker (mʌðɚdʌkɚ) 

[![Build Status](https://travis-ci.org/fbertola/mother-docker.svg)](https://travis-ci.org/fbertola/mother-docker)
[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/fbertola/mother-docker/blob/master/LICENSE)

A simple [Docker](https://github.com/dotcloud/docker) orchestrator written in Groovy.

## About

_MotherDocker_ was created as a tool for easing integration testing with _Docker_ on the _JVM_. It is a partial rewrite in _Groovy_ of [Docker Compose](https://docs.docker.com/compose/) (formely _Fig_).
Although this library doesn't have (and was not intended to) all the system management options of _Docker Compose_, it can fully understand its **YAML** configuration files.         

_MotherDocker_ is based on the excellent [Spotify's Docker Client](https://github.com/spotify/docker-client).

## Usage

_to-do_

## Docker-Compose extensions

_MotherDocker_ adds some new useful extensions to the original _YAML_ syntax:

#### Wait Strategies

When building a complex multi-container project, it is often useful to have the possibility to **wait** for an image to reach a certain state before proceeding.
_MotherDocker_ provides three different wait strategies:

- `time`: sleeps for the specified amount of time (milliseconds)
- `log_message`: wait until the specified string shows up in the logs
- `exec`: wait until the specified command terminates successfully

To avoid an infinite wait (e.g. when an error occurred inside a container) all the above commands are terminated after an amount of time configurable via the system property `motherdocker.wait.max`
Additionally, the `log_message` strategy accepts the system property `motherdocker.exec.retry.pause` which specifies how often it checks for the `exec` status.
 
## F.A.Q.

* **This is intended to be a replacement of Docker Compose?**

No, _MotherDocker_ it is **not** intended to be a replacement for _Docker Compose_. It lacks all the images management options (e.g. the `up` or `restart` commands) but nevertheless it can fully understand its configuration files.

* **Why the name?**

For the _lulz_ :stuck_out_tongue:
