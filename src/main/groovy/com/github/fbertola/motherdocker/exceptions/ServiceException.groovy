package com.github.fbertola.motherdocker.exceptions

class ServiceException extends Exception {

    ServiceException() {
        super()
    }

    ServiceException(String message) {
        super(message)
    }

    ServiceException(Throwable t) {
        super(t)
    }

    ServiceException(String message, Throwable t) {
        super(message, t)
    }

}
