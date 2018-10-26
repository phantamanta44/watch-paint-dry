package xyz.phanta.wpd.adapter

import xyz.phanta.wpd.renderer.PageDefinition
import java.nio.file.Files
import java.nio.file.Path

interface PageAdapter {

    fun canParse(path: Path, mimetype: String?): Boolean

    fun parse(data: ByteArray, deps: PageResolver): PageDefinition

}

interface PageResolver {

    fun resolvePage(key: String): PageDefinition

}

class AdapterSet {

    private val adapters: List<PageAdapter> = listOf(
            HtmlAdapter()
    )

    fun adapterForPath(path: Path): PageAdapter? {
        val mimetype = Files.probeContentType(path)
        return adapters.firstOrNull() { it.canParse(path, mimetype) }
    }

}
