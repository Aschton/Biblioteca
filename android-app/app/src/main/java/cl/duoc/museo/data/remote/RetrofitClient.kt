package cl.duoc.museo.data.remote

import cl.duoc.museo.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val museos: MuseosApi by lazy { retrofit(BuildConfig.MUSEOS_URL).create(MuseosApi::class.java) }
    val catalogo: CatalogoApi by lazy { retrofit(BuildConfig.CATALOGO_URL).create(CatalogoApi::class.java) }
    val pedidos: PedidosApi by lazy { retrofit(BuildConfig.PEDIDOS_URL).create(PedidosApi::class.java) }

    /** API externa (no es nuestra): pronóstico del tiempo de Open-Meteo. */
    val openMeteo: OpenMeteoApi by lazy { retrofit(BuildConfig.OPEN_METEO_URL).create(OpenMeteoApi::class.java) }
}
