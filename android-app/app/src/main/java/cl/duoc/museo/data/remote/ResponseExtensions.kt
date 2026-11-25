package cl.duoc.museo.data.remote

import retrofit2.Response

/** Lanza una excepción si el DELETE no respondió 2xx, para que el repositorio lo trate como error. */
fun Response<Unit>.ensureSuccess() {
    if (!isSuccessful) throw IllegalStateException("HTTP ${code()}")
}
