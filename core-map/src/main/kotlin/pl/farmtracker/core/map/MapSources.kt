package pl.farmtracker.core.map

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
     * Granice działek z KIEG (WMS), bez numerów. Serwer rysuje je tylko przy rozdzielczości obrazka
     * lepszej niż ok. 2 m/piksel, więc kafel 256 dp pobieramy jako 768 px (3×). Dzięki temu granice
     * widać już z kafli zoom 15 (ok. 1,2 km na kafel), a na ekranach 2,5–3× obraz i tak jest ostry.
     */
    const val PARCELS_TILES =
        "https://integracja.gugik.gov.pl/cgi-bin/KrajowaIntegracjaEwidencjiGruntow" +
            "?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0&LAYERS=dzialki&STYLES=" +
            "&FORMAT=image/png&TRANSPARENT=TRUE&CRS=EPSG:3857&BBOX={bbox-epsg-3857}&WIDTH=768&HEIGHT=768"

    /**
     * Od tego zoomu mapy MapLibre bierze kafle rastrowe 256 z poziomu 15 (zoom + 1, zaokrąglony) –
     * na telefonie to ok. 1,5–2 km szerokości ekranu, więc całe pole mieści się z zapasem.
     */
    const val PARCELS_MIN_ZOOM = 13.5

    const val FIELDS_SOURCE_ID = "fields"
    const val FIELD_LABELS_SOURCE_ID = "field-labels"
    const val FIELDS_FILL_LAYER_ID = "fields-fill"
    const val FIELDS_LINE_LAYER_ID = "fields-line"
    const val FIELDS_LABEL_LAYER_ID = "fields-label"

    const val DRAFT_SOURCE_ID = "draft"
    const val DRAFT_POINTS_SOURCE_ID = "draft-points"
    const val DRAFT_FILL_LAYER_ID = "draft-fill"
    const val DRAFT_LINE_LAYER_ID = "draft-line"
    const val DRAFT_POINTS_LAYER_ID = "draft-points"

    const val ENTRIES_SOURCE_ID = "entries"
    const val ENTRIES_CIRCLE_LAYER_ID = "entries-circle"
    const val ENTRIES_LABEL_LAYER_ID = "entries-label"

    const val SELECTION_SOURCE_ID = "selected-parcel"
    const val SELECTION_FILL_LAYER_ID = "selected-parcel-fill"
    const val SELECTION_LINE_LAYER_ID = "selected-parcel-line"

    const val GUGIK_ATTRIBUTION = "© GUGiK"

    const val RASTER_TILE_SIZE = 256
}
