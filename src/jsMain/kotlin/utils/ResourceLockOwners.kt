package utils

import screeps.api.MutableRecord
import screeps.utils.mutableRecordOf

external interface ResourceLockOwners : MutableRecord<String, String>

fun ResourceLockOwners() = mutableRecordOf<String, String>().unsafeCast<ResourceLockOwners>()
