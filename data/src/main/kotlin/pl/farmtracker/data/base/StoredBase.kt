package pl.farmtracker.data.base

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import pl.farmtracker.core.domain.Base
import pl.farmtracker.core.domain.geo.GeoPoint

/** Format pliku z bazą (osobny od domeny – jak pola). */
@Serializable
internal data class StoredBase(val lat: Double? = null, val lon: Double? = null)

internal fun StoredBase.toDomain(): Base? =
    if (lat != null && lon != null) Base(GeoPoint(latitude = lat, longitude = lon)) else null

internal fun Base.toStored() = StoredBase(lat = location.latitude, lon = location.longitude)

internal object StoredBaseSerializer : OkioSerializer<StoredBase> {

    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: StoredBase = StoredBase()

    override suspend fun readFrom(source: BufferedSource): StoredBase = try {
        json.decodeFromString(StoredBase.serializer(), source.readUtf8())
    } catch (e: SerializationException) {
        throw CorruptionException("Uszkodzony plik z bazą", e)
    }

    override suspend fun writeTo(t: StoredBase, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(StoredBase.serializer(), t))
    }
}
