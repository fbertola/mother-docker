package org.github.fbertola.motherdocker.exceptions

class ProjectException extends Exception {

    ProjectException() {
        super()
    }

    ProjectException(String message) {
        super(message)
    }

    ProjectException(Throwable t) {
        super(t)
    }

    ProjectException(String message, Throwable t) {
        super(message, t)
    }

}
