package xyz.phanta.wpd.model

import java.nio.file.Path

interface Asset : RenderingModel<RenderingContext> {

    val assetKey: String

}

interface AssetResolver {

    fun resolveAsset(key: String): Asset

}

interface AssetAdapter {

    fun canParse(path: Path, mimetype: String?): Boolean

    fun parse(key: String, data: ByteArray, deps: AssetResolver): Asset

}

interface AssetAdapterResolver {

    fun adapterFor(key: String, path: Path): AssetAdapter

}
