package pl.farmtracker.data.field

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.FieldStatus
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon

/*
 * Format pliku z polami. Osobne klasy zapisu (a nie adnotacje na modelu domeny), żeby zmiana
 * formatu nie ruszała domeny. Enumy zapisujemy nazwą; nieznana wartość → domyślna.
 */

@Serializable
internal data class StoredFields(val fields: List<StoredField> = emptyList())

@Serializable
internal data class StoredField(
    val id: String,
    val name: String,
    val color: String,
    val polygons: List<StoredPolygon>,
    val parcelIds: List<String> = emptyList(),
    val entries: List<StoredPoint> = emptyList(),
    /** Dawny zapis jednego wjazdu – tylko do odczytu starszych plików. */
    val entry: StoredPoint? = null,
    val status: String = FieldStatus.PLANNED.name,
    val order: Int = 0,
)

@Serializable
internal data class StoredPolygon(val outer: List<StoredPoint>, val holes: List<List<StoredPoint>> = emptyList())

@Serializable
internal data class StoredPoint(val lat: Double, val lon: Double)

internal fun Field.toStored() = StoredField(
    id = id,
    name = name,
    color = color.name,
    polygons = shape.map { polygon ->
        StoredPolygon(outer = polygon.outer.map { it.toStored() }, holes = polygon.holes.map { hole -> hole.map { it.toStored() } })
    },
    parcelIds = parcelIds,
    entries = entryPoints.map { it.toStored() },
    status = status.name,
    order = order,
)

internal fun StoredField.toDomain() = Field(
    id = id,
    name = name,
    color = FieldColor.entries.firstOrNull { it.name == color } ?: FieldColor.BLUE,
    shape = polygons.map { polygon ->
        GeoPolygon(outer = polygon.outer.map { it.toDomain() }, holes = polygon.holes.map { hole -> hole.map { it.toDomain() } })
    },
    parcelIds = parcelIds,
    entryPoints = entries.ifEmpty { listOfNotNull(entry) }.map { it.toDomain() },
    status = FieldStatus.entries.firstOrNull { it.name == status } ?: FieldStatus.PLANNED,
    order = order,
)

private fun GeoPoint.toStored() = StoredPoint(lat = latitude, lon = longitude)

private fun StoredPoint.toDomain() = GeoPoint(latitude = lat, longitude = lon)

internal object StoredFieldsSerializer : OkioSerializer<StoredFields> {

    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: StoredFields = StoredFields()

    override suspend fun readFrom(source: BufferedSource): StoredFields = try {
        json.decodeFromString(StoredFields.serializer(), source.readUtf8())
    } catch (e: SerializationException) {
        throw CorruptionException("Uszkodzony plik z polami", e)
    }

    override suspend fun writeTo(t: StoredFields, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(StoredFields.serializer(), t))
    }
}
