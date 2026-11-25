package cl.duoc.museo.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** ms-pedidos (puerto 8083): pedidos, solicitudes y envío. */
interface PedidosApi {
    @POST("api/orders") suspend fun createOrder(@Body body: OrderRequest): OrderDto
    @GET("api/orders") suspend fun orders(@Query("userEmail") userEmail: String? = null): List<OrderDto>
    @PATCH("api/orders/{id}/status") suspend fun updateStatus(@Path("id") id: Long, @Body body: StatusUpdate): OrderDto
    @DELETE("api/orders/{id}") suspend fun deleteOrder(@Path("id") id: Long): Response<Unit>

    @POST("api/shipping/quote") suspend fun quote(@Body body: ShippingQuoteRequest): ShippingQuoteDto
}
