package io.antani.motherdocker

import io.antani.motherdocker.exceptions.ServiceException

class MotherDockerService {

    private def name = ''
    private def client = null
    private def project = ''
    private def links = []
    private def externalLinks = []
    private def volumesFrom = []
    private def net = ''
    private def options = [:]

    MotherDockerService(name, client, project = 'default', links, externalLinks = null, volumesFrom, net, options) {
        if (!isValidIdentifier(name)) {
            throw new ServiceException("Invalid service name '$name' - only alpha-numerical chars re allowed")
        }

        if (!isValidIdentifier(project)) {
            throw new ServiceException("Invalid project name '$name' - only alpha-numerical chars re allowed")
        }

        if ('image' in options && 'build' in options) {
            throw new ServiceException("Service $name has both an image and build path specified. A service can either be built to image or use an existing image, not both.")
        }

        if (!('image' in options) && !('build' in options)) {
            throw new ServiceException("Service $name has neither an image nor a build path specified. Exactly one must be provided.")
        }

        this.name = name
        this.client = client
        this.project = project
        this.links = links ?: []
        this.externalLinks = externalLinks ?: []
        this.volumesFrom = volumesFrom ?: []
        this.net = net
        this.options = options
    }

    def start(options) {
        /* start container */
    }

    def stop(options) {
        /* stop container */
    }

    def kill(options) {
        /* kill container */
    }

    private def createContainer(options) {
        ensureImageExists()

        // create container
    }

    private def ensureImageExists() {

        if (image()) {
            return true
        }

        if (canBeBuilt()) {
            // build()
        } else {
            // pull
        }
    }

    private def image() {
        // inspect image
        return null;
    }

    private def imageName() {
        if (canBeBuilt()) {
            return fullName()
        } else {
            return options['image']
        }
    }

    private def canBeBuilt() {
        return options['build'] != null
    }

    private def fullName() {
        return "${project}_${name}"
    }

    private static def isValidIdentifier(identifier) {
        return identifier =~ /^[a-zA-Z0-9]$/
    }
}
