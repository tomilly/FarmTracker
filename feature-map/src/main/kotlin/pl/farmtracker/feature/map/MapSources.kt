package pl.farmtracker.feature.map

/**
 * Źródła mapy (BRIEF §7). Wszystkie w Web Mercator (EPSG:3857), który rysuje MapLibre.
 */
internal object MapSources {

    /** Mapa bazowa: OpenFreeMap (dane OSM, wektorowo) – bez klucza, dozwolona w aplikacjach. */
    const val BASE_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

    const val ORTHO_SOURCE_ID = "geoportal-orto"
    const val ORTHO_LAYER_ID = "geoportal-orto-layer"
    const val ORTHO_MAX_ZOOM = 19f

    /** Ortofotomapa Geoportalu (WMTS, siatka EPSG:3857 z poziomami 0–19 zgodnymi z kaflami XYZ). */
    const val ORTHO_TILES =
        "https://mapy.geoportal.gov.pl/wss/service/PZGIK/ORTO/WMTS/StandardResolution" +
            "?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=ORTOFOTOMAPA&STYLE=default" +
            "&FORMAT=image/jpeg&TILEMATRIXSET=EPSG:3857&TILEMATRIX=EPSG:3857:{z}&TILEROW={y}&TILECOL={x}"

    const val PARCELS_SOURCE_ID = "kieg-dzialki"
    const val PARCELS_LAYER_ID = "kieg-dzialki-layer"

    /**
     * Granice i numery działek z KIEG (WMS). Serwer rysuje je dopiero od ok. 1,2 m/piksel
     * (kafle zoom 17), wcześniej zwraca pusty obraz – stąd [PARCELS_MIN_ZOOM].
     */
    const val PARCELS_TILES =
        "https://integracja.gugik.gov.pl/cgi-bin/KrajowaIntegracjaEwidencjiGruntow" +
            "?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0&LAYERS=dzialki,numery_dzialek&STYLES=" +
            "&FORMAT=image/png&TRANSPARENT=TRUE&CRS=EPSG:3857&BBOX={bbox-epsg-3857}&WIDTH=256&HEIGHT=256"

    /** Od tego zoomu MapLibre pobiera kafle rastrowe poziomu 17 (zaokrągla zoom). */
    const val PARCELS_MIN_ZOOM = 16.5

    const val GUGIK_ATTRIBUTION = "© GUGiK"

    const val RASTER_TILE_SIZE = 256
}
