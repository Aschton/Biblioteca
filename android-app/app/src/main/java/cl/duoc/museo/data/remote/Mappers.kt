package cl.duoc.museo.data.remote

import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.ProductEntity

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
