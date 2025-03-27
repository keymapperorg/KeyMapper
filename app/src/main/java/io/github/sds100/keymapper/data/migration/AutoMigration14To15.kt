package io.github.sds100.keymapper.data.migration

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn("keymaps", "folder_name")
class AutoMigration14To15 : AutoMigrationSpec
