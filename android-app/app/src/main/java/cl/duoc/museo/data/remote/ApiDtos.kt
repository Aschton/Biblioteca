package cl.duoc.museo.data.remote

import retrofit2.Retrofit

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

data class NoticeDto(
    val id: Long? = null,
    val title: String,
    val body: String,
    val category: String,
    val targetRole: String = "ALL",
    val destination: String? = "",
    val createdAt: Long? = null
)

data class QrResolveDto(val code: String, val type: String, val id: Long, val title: String, val description: String)

// ---------- Endpoints: un Retrofit por microservicio ----------
