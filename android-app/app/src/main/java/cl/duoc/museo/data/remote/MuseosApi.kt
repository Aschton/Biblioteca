package cl.duoc.museo.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** ms-museos (puerto 8081): museos, avisos y QR de museos. */
interface MuseosApi {
    @GET("api/museums")
    suspend fun museums(
        @Query("country") country: String? = null,
        @Query("project") project: String? = null
    ): List<MuseumDto>

    @GET("api/museums/{id}") suspend fun museum(@Path("id") id: Long): MuseumDto
    @POST("api/museums") suspend fun createMuseum(@Body body: MuseumDto): MuseumDto
    @PUT("api/museums/{id}") suspend fun updateMuseum(@Path("id") id: Long, @Body body: MuseumDto): MuseumDto
    @DELETE("api/museums/{id}") suspend fun deleteMuseum(@Path("id") id: Long): Response<Unit>

    @GET("api/qr/{code}") suspend fun resolveQr(@Path("code") code: String): QrResolveDto

    @GET("api/notices") suspend fun notices(): List<NoticeDto>
    @POST("api/notices") suspend fun createNotice(@Body body: NoticeDto): NoticeDto
}
