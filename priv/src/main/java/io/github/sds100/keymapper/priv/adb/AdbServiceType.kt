package io.github.sds100.keymapper.priv.adb

enum class AdbServiceType(val id: String) {
    TLS_CONNECT("_adb-tls-connect._tcp"), TLS_PAIR("_adb-tls-pairing._tcp")
}