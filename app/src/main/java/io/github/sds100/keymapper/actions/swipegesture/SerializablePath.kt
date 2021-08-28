package io.github.sds100.keymapper.actions.swipegesture

import android.graphics.Path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.apache.commons.codec.binary.Base64
import timber.log.Timber
import java.io.*

class SerializablePathSerializer : KSerializer<SerializablePath> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "SerializablePath", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SerializablePath) {
        encoder.encodeString(value.toSerializedString())
    }

    override fun deserialize(decoder: Decoder): SerializablePath {
        return SerializablePath(decoder.decodeString())
    }
}

@Serializable(with = SerializablePathSerializer::class)
class SerializablePath(): Path() {
    private var pathPoints = ArrayList<Array<Float>>()

    constructor(serializedPath: String) : this() {
        val data: ByteArray = Base64().decode(serializedPath)
        val ois = ObjectInputStream(
            ByteArrayInputStream(data)
        )
        val o: ArrayList<Array<Float>> = ois.readObject() as ArrayList<Array<Float>>
        ois.close()
        pathPoints = o
        generatePath()
    }

    @Throws(IOException::class)
    fun toSerializedString(): String {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(pathPoints)
        oos.close()
        return String(Base64().encode(baos.toByteArray()))
    }

    fun addMove(x: Float, y: Float) {
        pathPoints.add(arrayOf(x, y))
        this.moveTo(x, y)
    }

    fun addQuad(x1: Float, y1: Float, x2: Float, y2: Float) {
        pathPoints.add(arrayOf(x1, y1, x2, y2))
        this.quadTo(x1, y1, x2, y2)
    }

    fun addLine(x: Float, y: Float) {
        pathPoints.add(arrayOf(x, y))
        this.lineTo(x, y)
    }

    fun scalePoints(xFactor: Float, yFactor: Float) {
        this.reset()

        val pointIterator = pathPoints.iterator().withIndex()

        while (pointIterator.hasNext()) {
            val pointSet = pointIterator.next()
            // Scale points
            if (xFactor != 1F && yFactor != 1F) {
                for (i in 0..pointSet.value.lastIndex) {
                    var factor: Float
                    if (i.mod(2) == 0) {
                        factor = xFactor
                    } else {
                        factor = yFactor
                    }
                    pointSet.value[i] = pointSet.value[i] * factor
                }
            }
            // Generate path
            if (pointSet.index == 0) {
                this.moveTo(pointSet.value[0], pointSet.value[1])
            } else if (pointIterator.hasNext()) {
                this.quadTo(pointSet.value[0], pointSet.value[1],
                    pointSet.value[2], pointSet.value[3])
            } else {
                this.lineTo(pointSet.value[0], pointSet.value[1])
            }
        }
    }

    // This function must be called on deserialization to regenerate Path properties
    fun generatePath() {
        scalePoints(1F, 1F)
    }
}