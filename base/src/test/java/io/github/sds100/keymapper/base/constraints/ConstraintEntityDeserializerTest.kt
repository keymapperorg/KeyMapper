package io.github.sds100.keymapper.base.constraints

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.ConstraintGroupEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test

/**
 * Key maps saved before constraint groups and the isNot/groupUid fields were added to
 * ConstraintEntity have no "isNot", "groupUid" or "constraintGroups" keys in their JSON at all.
 */
class ConstraintEntityDeserializerTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(ConstraintEntity.DESERIALIZER)
        .registerTypeAdapter(ConstraintGroupEntity.DESERIALIZER)
        .registerTypeAdapter(TriggerEntity.DESERIALIZER)
        .registerTypeAdapter(TriggerKeyEntity.SERIALIZER)
        .registerTypeAdapter(TriggerKeyEntity.DESERIALIZER)
        .registerTypeAdapter(ActionEntity.DESERIALIZER)
        .registerTypeAdapter(EntityExtra.DESERIALIZER)
        .registerTypeAdapter(KeyMapEntity.DESERIALIZER)
        .create()

    @Test
    fun `constraint entity json without isNot or groupUid deserializes to defaults`() {
        val json = """{"type":"constraint_screen_on","extras":[],"uid":"uid1"}"""

        val entity = gson.fromJson<ConstraintEntity>(json)

        assertThat(entity.isNot, `is`(false))
        assertThat(entity.groupUid, nullValue())
    }

    @Test
    fun `key map entity json without constraintGroups deserializes to an empty list`() {
        val json = """
            {
                "trigger":{"extras":[],"flags":0,"keys":[],"mode":2},
                "actionList":[],
                "constraintList":[],
                "constraintMode":1,
                "flags":0,
                "isEnabled":true,
                "uid":"uid1"
            }
        """.trimIndent()

        val entity = gson.fromJson<KeyMapEntity>(json)

        assertThat(entity.constraintGroups, empty())
    }
}
