package cl.duoc.museo.data.remote

import cl.duoc.museo.BuildConfig
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.ProductEntity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---------- DTOs (mismos nombres de campo que el backend Spring Boot) ----------

data class MuseumDto(
    val id: Long? = null,
    val name: String,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val imageUrl: String? = "",
    val classification: String,
    val project: String,
    val virtualTourUrl: String,
    val rating: Double? = 0.0,
    val published: Boolean = true,
    val qrCode: String? = null
)

data class ProductDto(
    val id: Long? = null,
    val name: String,
    val category: String,
    val description: String,
    val price: Double,
    val imageUrl: String? = "",
    val stock: Int,
    val active: Boolean = true,
    val qrCode: String? = null,
    val rating: Double? = 0.0
)

data class AvailabilityDto(val productId: Long, val available: Boolean, val stock: Int)

data class OrderItemRequest(val productId: Long, val quantity: Int)

data class OrderRequest(
    val userEmail: String,
    val userName: String,
    val items: List<OrderItemRequest>,
    val country: String,
    val shippingType: String,
    val kind: String = "COMPRA"
)

data class OrderItemDto(val productId: Long, val productName: String, val quantity: Int, val unitPrice: Double)

data class OrderDto(
    val id: Long,
    val code: String,
    val userEmail: String,
    val userName: String,
    val createdAt: Long,
    val items: List<OrderItemDto>,
    val subtotal: Double,
    val shipping: Double,
    val total: Double,
    val status: String,
    val country: String,
    val shippingType: String,
    val kind: String? = "COMPRA"
)

data class StatusUpdate(val status: String)

data class ShippingQuoteRequest(val country: String, val shippingType: String, val items: List<OrderItemRequest>)
data class ShippingQuoteDto(val subtotal: Double, val shipping: Double, val total: Double, val estimatedDays: String)

data class QrResolveDto(val code: String, val type: String, val id: Long, val title: String, val description: String)

// ---------- Endpoints ----------

interface ApiService {
    @GET("api/museums")
    suspend fun museums(
        @Query("country") country: String? = null,
        @Query("project") project: String? = null
    ): List<MuseumDto>

    @GET("api/museums/{id}") suspend fun museum(@Path("id") id: Long): MuseumDto
    @POST("api/museums") suspend fun createMuseum(@Body body: MuseumDto): MuseumDto
    @PUT("api/museums/{id}") suspend fun updateMuseum(@Path("id") id: Long, @Body body: MuseumDto): MuseumDto

    @GET("api/products")
    suspend fun products(@Query("category") category: String? = null, @Query("q") q: String? = null): List<ProductDto>

    @GET("api/products/{id}") suspend fun product(@Path("id") id: Long): ProductDto
    @POST("api/products") suspend fun createProduct(@Body body: ProductDto): ProductDto
    @PUT("api/products/{id}") suspend fun updateProduct(@Path("id") id: Long, @Body body: ProductDto): ProductDto
    @PATCH("api/products/{id}/deactivate") suspend fun deactivateProduct(@Path("id") id: Long): ProductDto

    @GET("api/products/{id}/availability")
    suspend fun availability(@Path("id") id: Long, @Query("qty") qty: Int): AvailabilityDto

    @POST("api/orders") suspend fun createOrder(@Body body: OrderRequest): OrderDto
    @GET("api/orders") suspend fun orders(@Query("userEmail") userEmail: String? = null): List<OrderDto>
    @PATCH("api/orders/{id}/status") suspend fun updateStatus(@Path("id") id: Long, @Body body: StatusUpdate): OrderDto

    @POST("api/shipping/quote") suspend fun quote(@Body body: ShippingQuoteRequest): ShippingQuoteDto

    @GET("api/qr/{code}") suspend fun resolveQr(@Path("code") code: String): QrResolveDto

}

object RetrofitClient {
    val api: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// ---------- Mappers DTO <-> Entity ----------

fun MuseumDto.toEntity() = MuseumEntity(
    id = id ?: 0, name = name, country = country, city = city, latitude = latitude, longitude = longitude,
    description = description, imageUrl = imageUrl ?: "", classification = classification, project = project,
    virtualTourUrl = virtualTourUrl, rating = rating ?: 0.0, published = published,
    qrCode = qrCode ?: "MUSEO:${id ?: 0}"
)

fun MuseumEntity.toDto(sendId: Boolean = true) = MuseumDto(
    id = if (sendId) id else null, name = name, country = country, city = city, latitude = latitude,
    longitude = longitude, description = description, imageUrl = imageUrl, classification = classification,
    project = project, virtualTourUrl = virtualTourUrl, rating = rating, published = published, qrCode = qrCode
)

fun ProductDto.toEntity() = ProductEntity(
    id = id ?: 0, name = name, category = category, description = description, price = price,
    imageUrl = imageUrl ?: "", stock = stock, active = active, qrCode = qrCode ?: "PROD:${id ?: 0}",
    rating = rating ?: 0.0
)

fun ProductEntity.toDto(sendId: Boolean = true) = ProductDto(
    id = if (sendId) id else null, name = name, category = category, description = description,
    price = price, imageUrl = imageUrl, stock = stock, active = active, qrCode = qrCode, rating = rating
)
