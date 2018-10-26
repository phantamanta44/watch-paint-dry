package xyz.phanta.wpd.renderer

import xyz.phanta.wpd.adapter.PageResolver

interface DefinitionProvider {

    fun resolveDef(key: String): PageToken?

}

interface PageDefinition : DefinitionProvider {

    val body: PageToken

}

class EmptyPageDefinition : PageDefinition {

    override val body: PageToken = EmptyToken()

    override fun resolveDef(key: String): PageToken? = null

}

class SimplePageDefinition(override val body: PageToken, private val defs: Map<String, PageToken>) : PageDefinition {

    override fun resolveDef(key: String): PageToken? = defs[key]

}

interface PageToken {

    fun render(defs: DefinitionProvider, pages: PageResolver): String

}

class EmptyToken : PageToken {

    override fun render(defs: DefinitionProvider, pages: PageResolver): String = ""

}

class ParentToken(private val children: List<PageToken>) : PageToken {

    override fun render(defs: DefinitionProvider, pages: PageResolver): String {
        return children.joinToString(separator = "") { it.render(defs, pages) }
    }

}

class LiteralToken(private val value: String) : PageToken {

    override fun render(defs: DefinitionProvider, pages: PageResolver): String = value

}

class ResolvableToken(private val key: String) : PageToken {

    override fun render(defs: DefinitionProvider, pages: PageResolver): String {
        val def = defs.resolveDef(key) ?: EmptyToken()
        return def.render(defs, pages)
    }

}

class ImportableToken(private val key: String) : PageToken {

    override fun render(defs: DefinitionProvider, pages: PageResolver): String {
        val page = pages.resolvePage(key)
        return page.body.render(ImportDefinitions(defs, page), pages)
    }

    private class ImportDefinitions(private val defs: DefinitionProvider, private val page: PageDefinition)
        : DefinitionProvider {

        override fun resolveDef(key: String): PageToken? = defs.resolveDef(key) ?: page.resolveDef(key)

    }

}
