package com.soywiz.util

import org.yaml.snakeyaml.*

val YAML = Yaml(
    DumperOptions().apply {
        this.isPrettyFlow = false
        this.width = 10000000
        this.splitLines = false
    }
        //.apply { defaultScalarStyle = DumperOptions.ScalarStyle.DOUBLE_QUOTED }
        //.apply { isPrettyFlow = true }
)
