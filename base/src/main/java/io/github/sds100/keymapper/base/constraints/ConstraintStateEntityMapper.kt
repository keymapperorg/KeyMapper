package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.ConstraintGroupEntity
import java.util.UUID

object ConstraintStateEntityMapper {

    /**
     * For [GroupEntity][io.github.sds100.keymapper.data.entities.GroupEntity], which only ever
     * stores a single flat list of constraints combined with one mode. Its constraints never
     * have a [io.github.sds100.keymapper.data.entities.ConstraintEntity.groupUid].
     */
    fun fromEntity(entities: List<ConstraintEntity>, constraintMode: Int): ConstraintState {
        val mode = ConstraintModeEntityMapper.fromEntity(constraintMode)
        val constraints = entities.map { ConstraintEntityMapper.fromEntity(it) }

        val groups = if (constraints.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ConstraintGroup(
                    uid = UUID.randomUUID().toString(),
                    constraints = constraints,
                    mode = mode,
                ),
            )
        }

        return ConstraintState(groups = groups, mode = mode)
    }

    /**
     * @return the constraint entities and the constraint mode to save.
     */
    fun toEntity(state: ConstraintState): Pair<List<ConstraintEntity>, Int> {
        val group = state.groups.singleOrNull()
        val entities = group?.constraints?.map { ConstraintEntityMapper.toEntity(it) }
        val mode = group?.mode ?: state.mode

        return Pair(entities.orEmpty(), ConstraintModeEntityMapper.toEntity(mode))
    }

    /**
     * For [KeyMapEntity][io.github.sds100.keymapper.data.entities.KeyMapEntity], which supports
     * multiple nameable constraint groups.
     *
     * Constraints that were saved before constraint groups existed do not have a
     * [ConstraintEntity.groupUid] and the constraint mode is the mode of the only group.
     *
     * If there are multiple groups then each constraint entity has a [ConstraintEntity.groupUid]
     * for the group it is in, and the group's mode/name are looked up from the matching
     * [io.github.sds100.keymapper.data.entities.ConstraintGroupEntity].
     */
    fun fromEntityWithGroups(
        entities: List<ConstraintEntity>,
        groupEntities: List<ConstraintGroupEntity>,
        constraintMode: Int,
    ): ConstraintState {
        val mode = ConstraintModeEntityMapper.fromEntity(constraintMode)

        val hasGroups = entities.any { it.groupUid != null }

        if (!hasGroups) {
            val constraints = entities.map { ConstraintEntityMapper.fromEntity(it) }

            val groups = if (constraints.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    ConstraintGroup(
                        uid = UUID.randomUUID().toString(),
                        constraints = constraints,
                        mode = mode,
                    ),
                )
            }

            return ConstraintState(groups = groups, mode = mode)
        }

        val groups = entities
            .groupBy { it.groupUid }
            .map { (groupUid, groupConstraintEntities) ->
                val groupEntity = groupEntities.find { it.uid == groupUid }
                val constraints = groupConstraintEntities.map {
                    ConstraintEntityMapper.fromEntity(it)
                }

                ConstraintGroup(
                    uid = groupUid ?: constraints.first().uid,
                    name = groupEntity?.name,
                    constraints = constraints,
                    mode = ConstraintModeEntityMapper.fromEntity(
                        groupEntity?.mode ?: ConstraintEntity.DEFAULT_MODE,
                    ),
                )
            }

        return ConstraintState(groups = groups, mode = mode)
    }

    /**
     * @return the constraint entities, the constraint group entities, and the constraint mode
     * to save.
     */
    fun toEntityWithGroups(
        state: ConstraintState,
    ): Triple<List<ConstraintEntity>, List<ConstraintGroupEntity>, Int> {
        val groupEntities = state.groups.map { group ->
            ConstraintGroupEntity(
                uid = group.uid,
                name = group.name,
                mode = ConstraintModeEntityMapper.toEntity(group.mode),
            )
        }

        val entities = state.groups.flatMap { group ->
            group.constraints.map { constraint ->
                ConstraintEntityMapper.toEntity(constraint).copy(groupUid = group.uid)
            }
        }

        return Triple(entities, groupEntities, ConstraintModeEntityMapper.toEntity(state.mode))
    }
}
