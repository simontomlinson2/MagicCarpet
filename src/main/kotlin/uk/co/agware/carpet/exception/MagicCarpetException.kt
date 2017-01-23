package uk.co.agware.carpet.exception;
/**
 * Created by Simon on 29/12/2016.
 */
class MagicCarpetException : RuntimeException {

    constructor(message: String?, cause: Throwable) : super(message, cause){}
    constructor(message: String?) : super(message){}
}
