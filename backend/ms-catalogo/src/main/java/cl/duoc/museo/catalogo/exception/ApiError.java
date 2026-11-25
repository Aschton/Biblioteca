package cl.duoc.museo.catalogo.exception;

/** Cuerpo JSON que se devuelve cuando ocurre un error. */
public record ApiError(int status, String message) {}
