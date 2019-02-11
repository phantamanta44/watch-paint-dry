package xyz.phanta.wpd.renderer.adapter

import xyz.phanta.wpd.model.Asset
import xyz.phanta.wpd.model.AssetResolver

class JsonAdapter : AbstractFileTypeAdapter("application/json", ".json") {

    override fun parse(key: String, data: ByteArray, deps: AssetResolver): Asset {
        TODO("no impl")
    }

}
