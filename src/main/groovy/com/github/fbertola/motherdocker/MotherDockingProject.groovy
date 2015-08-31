package com.github.fbertola.motherdocker

import com.github.fbertola.motherdocker.exceptions.ProjectException
import com.spotify.docker.client.DockerClient
import groovy.util.logging.Slf4j

import static com.github.fbertola.motherdocker.utils.ParsingUtils.getServiceNameFromNet

@Slf4j
class MotherDockingProject {

    DockerClient client = null
    Map createdContainers = [:]
    List<MotherDockingService> services = []

    public MotherDockingProject(client, parsedServices) {
        this.client = client

        topologicalSort(parsedServices).each { Map parsedService ->
            analyzeLinks(parsedService)
            analyzeVolumesFrom(parsedService)
            analyzeNet(parsedService)

            def name = parsedService['name']

            services << new MotherDockingService(name, client, parsedService)
        }

        log.info('Image plan: {}', services.collect { it['name'] })
    }

    void start() {
        services.each { s ->
            s.start(createdContainers)
            createdContainers[s.name] = s.containerId
        }
    }

    void restart() {
        services.each {s -> s.restart()}
    }

    void restart(serviceName) {
        services.find { s ->
            s.name.equals(serviceName)
        }.restart()
    }

    void stop() {
        services.reverse().each { s -> s.stop() }
    }

    Map getServicesInfo() {
        return services.inject([:]) { map, s ->
            map[s.name] = s.getServiceInfo()
            return map
        } as Map
    }

    private def analyzeLinks(parsedService) {
        (parsedService['links'] ?: []).each { String link ->
            def serviceName = link.split(':')[0]

            if (!serviceExistInProject(serviceName)) {
                throw new ProjectException("Service '${parsedService['name']}' has a link to service '$serviceName' which does not exist.")
            }
        }
    }

    private def analyzeVolumesFrom(parsedService) {
        (parsedService['volumesFrom'] ?: []).each { String volumeName ->
            if (!serviceExistInProject(volumeName)) {
                try {
                    client.inspectImage(volumeName)
                } catch (Exception ignored) {
                    throw new ProjectException("Service '${parsedService['name']}' mounts volumes from '$volumeName', which is not the name of a service or container.")
                }
            }
        }
    }

    private def analyzeNet(parsedService) {
        if ('net' in parsedService) {
            def netName = getServiceNameFromNet(parsedService['net'] as String)

            if (!serviceExistInProject(netName)) {
                try {
                    client.inspectImage(netName as String)
                } catch (Exception ignored) {
                    throw new RuntimeException("Service '${parsedService['name']}' is trying to use the network of '$netName', which is not the name of a service or container.")
                }
            }

            parsedService['net'] = netName
        } else {
            parsedService['net'] = 'bridge'
        }
    }

    def serviceExistInProject(String name) {
        return (services.find { it.name.equals(name) } != null)
    }

    private static def topologicalSort(services) {
        def unmarked = services.clone()
        def temporaryMarked = new HashSet()
        def sortedServices = []

        def getServiceNames = { links ->
            links.collect { String link -> link.split(':')[0] }
        }

        def getServiceDependents = { serviceDictionary ->
            def name = serviceDictionary['name'] as String

            return services.findAll({
                name in getServiceNames((it['links'] ?: [])) ||
                        name in it['volumesFrom'] ?: [] ||
                        name == getServiceNameFromNet(it['net'] as String)
            })
        }

        def visit
        visit = { n ->
            def name = n['name'] as String
            if (name in temporaryMarked) {
                if (name in getServiceNames(n['links'] ?: [])) {
                    throw new RuntimeException("A service cannot link to itself: ${name}")
                }

                if (name in n['volumesFrom'] ?: []) {
                    throw new RuntimeException("A service can not mount itself as volume: ${name}")
                }

                throw new RuntimeException("Circular import between ${name} and $temporaryMarked")
            }

            if (n in unmarked) {
                temporaryMarked << name

                getServiceDependents(n).each { visit(it) }

                temporaryMarked -= name
                unmarked -= n
                sortedServices.add(0, n)
            }
        }

        while (unmarked) {
            visit(unmarked[-1])
        }

        return sortedServices
    }

}
