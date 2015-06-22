package io.antani.motherdocker.exceptions

class ParserException extends Exception {

    ParserException() {
        super()
    }

    ParserException(String message) {
        super(message)
    }

    ParserException(Throwable t) {
        super(t)
    }

    ParserException(String message, Throwable t) {
        super(message, t)
    }

}
