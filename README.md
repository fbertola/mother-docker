# MotherDocker (mʌðɚdʌkɚ) 

[![Build Status](https://travis-ci.org/fbertola/mother-docker.svg)](https://travis-ci.org/fbertola/mother-docker)
[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/fbertola/mother-docker/blob/master/LICENSE)

A simple [Docker](https://github.com/dotcloud/docker) orchestrator written in Groovy.

## About

_MotherDocker_ was created as a tool for easing integration testing with _Docker_ on the _JVM_. It is a partial rewrite in _Groovy_ of [Docker Compose](https://docs.docker.com/compose/) (formerly _Fig_).
Although this library doesn't have (and was not intended to) all the system management options of _Docker Compose_, it can fully understand its **YAML** configuration files.         

_MotherDocker_ is based on the excellent [Spotify's Docker Client](https://github.com/spotify/docker-client).

## Usage

```groovy
// Initializes the Docker client
new DefaultDockerClient("unix:///var/run/docker.sock").withCloseable { client ->
    
    // Creates the project
    def project = MotherDocker.buildProjectFromFile(filename, client)
    
    // Starts the project
    project.start()
    
    // Retrieves the services info
    def info = project.getSericesInfo()
    
    // Application logic...
    
    // Stop the project
    project.stop()
    
}
```

#### Integration with test frameworks

If you're using [Spock](https://github.com/spockframework/spock) you could use the provided `@WithDockerConfig` extension (as an example see [this](https://github.com/fbertola/mother-docker/blob/master/src/test/groovy/com/github/fbertola/motherdocker/MotherDockerTest.groovy)) or, if you're using [Junit](https://github.com/junit-team/junit), you could use a simple [Rule](https://github.com/junit-team/junit/wiki/Rules), for example:

```java
public class MotherDockingRule extends ExternalResource {
 
    private MotherDockingProject project;  
 
    public MotherDockingRule(String filename) {
        DockerClient client = new DefaultDockerClient("unix:///var/run/docker.sock");
        this.project = (MotherDockingProject) MotherDocker.buildProjectFromFile(filename, client);
    }
 
    public Map<String, ContainerInfo> getServicesInfo() {
        return project.getServicesInfo();
    }
 
    @Override
    public Statement apply(Statement base, Description description) {
        return super.apply(base, description);
    }
 
    @Override
    protected void before() throws Throwable {
        super.before();
        project.start();
    }
 
    @Override
    protected void after() {
        super.after();
        project.stop();
    }
    
}
```

When using Spring's testing annotation like `@RunWith(SpringJUnit4ClassRunner.class)` and `@SpringApplicationConfiguration` you will want use `@ClassRule` instead of `@Rule` to have to containers ready before the Spring's context start.

## Installation

If you're using _Maven_:

```xml
<dependency>
    <groupId>com.github.fbertola</groupId>
    <artifactId>mother-docker</artifactId>
    <version>1.0.0-Beta3</version>
</dependency>
```

If you're using _Gradle_:

```groovy
compile 'com.github.fbertola:mother-docker:1.0.0-Beta3'
```

## Docker-Compose extensions

_MotherDocker_ adds some new useful extensions to the original _YAML_ syntax:

#### Networking

It is often advisable not to statically bind specific ports in your containers, either because this will prevents from running the same container twice (e.g. when container creation is a part of multiple integration tests) or because some service might already using it. In these cases, it is useful to let Docker assign an ephemeral port to all the exposed ones; this is possible with the `publish_all` option.

For further documentation see [this link](https://docs.docker.com/articles/networking/#binding-ports).

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

* **It is possible to run multiple instances of MotherDocker on the same CI?**

Definitely _yes_, but it depends on how the containers are configured.
One of the major problems is port collision, that is, two or more containers try to listen on the same ports. The safest way is to use [links](https://docs.docker.com/userguide/dockerlinks/) or to use the `publish_all` option, as explained earlier.
Additionally, be careful when using _external volumes_.

* **Why the name?**

For the _lulz_ :stuck_out_tongue:
