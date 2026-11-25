package cl.duoc.museo.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** ms-catalogo (puerto 8082): productos y cursos. */
interface CatalogoApi {
    @GET("api/products")
    suspend fun products(@Query("category") category: String? = null, @Query("q") q: String? = null): List<ProductDto>

    @GET("api/products/{id}") suspend fun product(@Path("id") id: Long): ProductDto
    @POST("api/products") suspend fun createProduct(@Body body: ProductDto): ProductDto
    @PUT("api/products/{id}") suspend fun updateProduct(@Path("id") id: Long, @Body body: ProductDto): ProductDto
    @PATCH("api/products/{id}/deactivate") suspend fun deactivateProduct(@Path("id") id: Long): ProductDto
    @DELETE("api/products/{id}") suspend fun deleteProduct(@Path("id") id: Long): Response<Unit>

    @GET("api/products/{id}/availability")
    suspend fun availability(@Path("id") id: Long, @Query("qty") qty: Int): AvailabilityDto

    @GET("api/qr/{code}") suspend fun resolveQr(@Path("code") code: String): QrResolveDto
}
