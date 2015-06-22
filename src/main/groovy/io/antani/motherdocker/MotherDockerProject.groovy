package io.antani.motherdocker

import io.antani.motherdocker.exceptions.ProjectException

import static io.antani.motherdocker.utils.ParsingUtils.getServiceNameFromNet

class MotherDockerProject {

    def services = []

    public MotherDockerProject(parsedServices) {
        cormenTarjanTopologicalSort(parsedServices).each { Map parsedService ->
            def links = getLinks(parsedService) as Collection
            def volumes = getVolumesFrom(parsedService) as Collection
            def net = getNet(parsedService)

            def name = ''
            def client = null

            services << new MotherDockerService(
                    name: name,
                    client: client,
                    links : links,
                    volumesFrom : volumes,
                    net : net,
                    options: parsedService
            )
        }
    }

    private def getService(name) {
        def service = services.find { it.name.equals(name) }

        if (!service) {
            throw new ProjectException("Service '$name' does not exists!")
        }

        return service
    }

    private static def getLinks(parsedService) {
        def links = []

        if ('links' in parsedService) {
            (parsedService['links'] ?: []).each { String link ->
                def serviceName
                def linkName = null

                if (':' in link) {
                    (serviceName, linkName) = link.split(':', 1)
                } else {
                    serviceName = link
                }

                try {
                    links += [getService(serviceName), linkName]
                } catch (Exception ignored) {
                    throw new ProjectException("Service '${parsedService['name']}' has a link to service '$serviceName' which does not exist.")
                }
            }

            parsedService.remove('links')
        }

        return links
    }

    private static def getVolumesFrom(parsedService) {
        def volumesFrom = []

        if ('volumesFrom' in parsedService) {
            (parsedService['volumesFrom'] ?: []).each { volumeName ->
                try {
                    def service = getService(volumeName)
                    volumesFrom << service
                } catch (Exception ignore) {
                    try {
                        def container = null /*Container.from_id(self.client, volumeName)*/
                        volumesFrom << container
                    } catch (Exception ignored) {
                        throw new ProjectException("Service '${parsedService['name']}' mounts volumes from '$volumeName', which is not the name of a service or container.")
                    }
                }
            }

            parsedService.remove('volumesFrom')
        }

        return volumesFrom
    }

    private def getNet(parsedService) {
        def net

        if ('net' in parsedService) {
            def netName = getServiceNameFromNet(parsedService['net'] as String)

            if (netName) {
                try {
                    net = getService(netName)
                } catch (Exception ignore) {
                    try {
                        net = null /*Container.from_id(self.client, netName)*/
                    } catch (Exception ignored) {
                        throw new RuntimeException("Service '${parsedService['name']}' is trying to use the network of '$netName', which is not the name of a service or container.")
                    }
                }
            } else {
                net = parsedService['net']
                parsedService.remove('net')
            }
        } else {
            net = 'bridge'
        }

        return net
    }

    private static def cormenTarjanTopologicalSort(services) {
        def unmarked = services.clone() as Map
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

                getServiceDependents(n).each { visit(n) }

                temporaryMarked - name
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
