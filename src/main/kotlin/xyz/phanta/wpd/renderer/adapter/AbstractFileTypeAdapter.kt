package xyz.phanta.wpd.renderer.adapter

import xyz.phanta.wpd.model.AssetAdapter
import java.nio.file.Path

abstract class AbstractFileTypeAdapter(private val mimetype: String, private vararg val extensions: String) : AssetAdapter {

    override fun canParse(path: Path, mimetype: String?): Boolean =
            mimetype == this.mimetype || extensions.any { path.fileName.toString().endsWith(it) }

}
