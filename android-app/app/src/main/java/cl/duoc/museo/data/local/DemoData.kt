package cl.duoc.museo.data.local

import cl.duoc.museo.util.Security

/**
 * Datos de demostración FICTICIOS. Se cargan la primera vez que se abre la app,
 * así el MVP funciona aunque el backend no esté levantado. Los IDs coinciden con
 * los del backend Spring Boot para que la sincronización reemplace la caché.
 */
object DemoData {

    private fun tour(name: String) =
        "https://artsandculture.google.com/search?q=" + name.replace(" ", "%20")

    val museums = listOf(
        MuseumEntity(1, "Museo Nacional de Arte Colonial", "Colombia", "Bogotá", 4.5970, -74.0745,
            "Recorre siglos de historia a través de colecciones de arte colonial iberoamericano. Explora salas virtuales, exposiciones y material educativo.",
            "", "Historia", "Patrimonio Colonial Digital", tour("Museo Colonial Bogota"), 4.8, true, "MUSEO:1"),
        MuseumEntity(2, "Museo de Arte Moderno", "México", "Ciudad de México", 19.4229, -99.1773,
            "Colección de arte mexicano del siglo XX con recorridos virtuales por sus salas circulares.",
            "", "Arte moderno", "Arte Moderno en Línea", tour("Museo de Arte Moderno Mexico"), 4.6, true, "MUSEO:2"),
        MuseumEntity(3, "Museo del Prado Virtual", "España", "Madrid", 40.4138, -3.6921,
            "Visita virtual a obras maestras de la pintura europea. Registro de demostración para el MVP.",
            "", "Bellas artes", "Red Iberoamericana", tour("Museo del Prado"), 4.9, true, "MUSEO:3"),
        MuseumEntity(4, "Museo de Arte de Lima", "Perú", "Lima", -12.0605, -77.0369,
            "Arte peruano desde la época precolombina hasta el presente, con recorridos 360°.",
            "", "Historia", "Patrimonio Colonial Digital", tour("Museo de Arte de Lima"), 4.7, true, "MUSEO:4"),
        MuseumEntity(5, "Museo Nacional de Bellas Artes", "Chile", "Santiago", -33.4344, -70.6436,
            "Colección de arte chileno y universal en un edificio patrimonial del Parque Forestal.",
            "", "Bellas artes", "Red Iberoamericana", tour("Museo Nacional de Bellas Artes Chile"), 4.7, true, "MUSEO:5"),
        MuseumEntity(6, "Museo Nacional de Bellas Artes (AR)", "Argentina", "Buenos Aires", -34.5839, -58.3929,
            "Pintura y escultura argentina y europea con visitas virtuales guiadas.",
            "", "Bellas artes", "Arte Moderno en Línea", tour("Museo Nacional de Bellas Artes Buenos Aires"), 4.6, true, "MUSEO:6"),
        MuseumEntity(7, "Museo de Arte de São Paulo", "Brasil", "São Paulo", -23.5614, -46.6559,
            "Colección internacional exhibida en sus icónicos caballetes de vidrio.",
            "", "Arte moderno", "Arte Moderno en Línea", tour("MASP Sao Paulo"), 4.8, true, "MUSEO:7"),
        MuseumEntity(8, "Museo Nacional de Antropología", "México", "Ciudad de México", 19.4260, -99.1863,
            "Patrimonio arqueológico y etnográfico de Mesoamérica en recorridos virtuales.",
            "", "Arqueología", "Red Iberoamericana", tour("Museo Nacional de Antropologia"), 4.9, true, "MUSEO:8"),
        MuseumEntity(9, "Museu Nacional de Arte Antiga", "Portugal", "Lisboa", 38.7050, -9.1610,
            "Arte portugués y europeo de los siglos XII al XIX.",
            "", "Historia", "Red Iberoamericana", tour("Museu Nacional de Arte Antiga"), 4.6, true, "MUSEO:9"),
        MuseumEntity(10, "Centro de Arte Contemporáneo de Quito", "Ecuador", "Quito", -0.2127, -78.5063,
            "Arte contemporáneo ecuatoriano en un antiguo hospital militar restaurado.",
            "", "Arte contemporáneo", "Arte Moderno en Línea", tour("Centro de Arte Contemporaneo Quito"), 4.5, true, "MUSEO:10"),
        MuseumEntity(11, "Museo Nacional de Costa Rica", "Costa Rica", "San José", 9.9326, -84.0716,
            "Historia natural y cultural costarricense en el antiguo Cuartel Bellavista.",
            "", "Historia", "Patrimonio Colonial Digital", tour("Museo Nacional de Costa Rica"), 4.5, true, "MUSEO:11"),
        MuseumEntity(12, "Museo Nacional de Artes Visuales", "Uruguay", "Montevideo", -34.9143, -56.1520,
            "Principal colección de arte uruguayo, con exposiciones temporales virtuales.",
            "", "Bellas artes", "Red Iberoamericana", tour("Museo Nacional de Artes Visuales Montevideo"), 4.4, true, "MUSEO:12")
    )

    val products = listOf(
        ProductEntity(1, "Curso de Curaduría Virtual", "CURSO",
            "Aprende a diseñar exposiciones digitales: guion curatorial, montaje virtual y mediación en línea. 8 semanas.",
            49.0, "", 25, true, "PROD:1", 4.7),
        ProductEntity(2, "Historia del Arte Iberoamericano", "LIBRO",
            "Un recorrido completo por el desarrollo del arte en Iberoamérica, con análisis de obras, contextos históricos y perspectivas contemporáneas.",
            35.0, "", 40, true, "PROD:2", 4.7),
        ProductEntity(3, "Kit de Conservación de Documentos", "MATERIAL",
            "Carpetas libres de ácido, guantes de nitrilo, pinceles y fichas de registro para conservación preventiva.",
            120.0, "", 12, true, "PROD:3", 4.5),
        ProductEntity(4, "Curso Museos y Comunidad", "CURSO",
            "Estrategias de desarrollo de audiencias y vinculación territorial para museos. 6 semanas.",
            39.0, "", 30, true, "PROD:4", 4.6),
        ProductEntity(5, "Curso de Museología Digital", "CURSO",
            "Digitalización de colecciones, metadatos y plataformas de museos virtuales. 10 semanas.",
            59.0, "", 20, true, "PROD:5", 4.8),
        ProductEntity(6, "El Arte Colonial en Iberoamérica", "LIBRO",
            "Libro digital sobre la historia del arte colonial en la región iberoamericana.",
            28.0, "", 100, true, "PROD:6", 4.6),
        ProductEntity(7, "Guantes de algodón para manipulación", "MATERIAL",
            "Pack de 6 pares de guantes de algodón para manipulación de obras y documentos.",
            15.0, "", 60, true, "PROD:7", 4.3),
        ProductEntity(8, "Réplica de cerámica precolombina", "ARTICULO",
            "Réplica artesanal con certificado de origen, ideal para material educativo.",
            45.0, "", 8, true, "PROD:8", 4.4)
    )

    val users = listOf(
        UserEntity(name = "Admin Académico", email = "admin@museo.cl",
            passwordHash = Security.sha256("Admin123"), role = Role.ADMIN.name, avatar = "avatar_3"),
        UserEntity(name = "Investigadora Demo", email = "investigador@museo.cl",
            passwordHash = Security.sha256("Investiga123"), role = Role.INVESTIGADOR.name, avatar = "avatar_2"),
        UserEntity(name = "María Fernanda López", email = "cliente@museo.cl",
            passwordHash = Security.sha256("Cliente123"), role = Role.CLIENTE.name, avatar = "avatar_1",
            shippingAddress = "Av. Ficticia 123, Santiago")
    )

    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.userDao().findByEmail("admin@museo.cl") != null) return
        users.forEach { db.userDao().insert(it) }
        db.museumDao().upsertAll(museums)
        db.productDao().upsertAll(products)
    }
}
