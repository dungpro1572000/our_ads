package com.dungz.our_ads.model

// We have 4 kind of data : String, Boolean, Int, Long
// value is default value if cannot get data from remote config key
data class DataKey<T>(val key: String, val value: T)
