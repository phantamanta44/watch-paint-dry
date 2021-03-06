package xyz.phanta.wpd.renderer

import org.jsoup.Jsoup
import org.jsoup.nodes.Entities
import org.jsoup.parser.Parser
import xyz.phanta.wpd.WpdArgs
import xyz.phanta.wpd.model.*
import xyz.phanta.wpd.renderer.adapter.HtmlAdapter
import xyz.phanta.wpd.renderer.adapter.JsonAdapter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.Comparator

class Renderer(private val args: WpdArgs, pathIn: Path, private val pathOut: Path) {

    private val adapters: AssetAdapterResolver = DefaultAdapters()

    private val dirPages: Path = pathIn.resolve("pages")
    private val dirTemplates: Path = pathIn.resolve("templates")
    private val dirStatic: Path = pathIn.resolve("assets")

    val inputDirectories: List<Path>
        get() = listOf(dirPages, dirTemplates, dirStatic)

    fun render() {
        val deps = AssetDependencyTree(adapters)

        if (Files.exists(pathOut)) {
            println("\n> Cleaning up output dir...")
            Files.walk(pathOut)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }

        println("\n> Searching for assets...")
        Files.walk(dirTemplates).filter { !Files.isDirectory(it) }.forEach {
            deps.registerTemplate(dirTemplates.relativize(it).toString(), it)
        }
        Files.walk(dirPages).filter { !Files.isDirectory(it) }.forEach {
            deps.registerPage(dirPages.relativize(it).toString(), it)
        }

        println("\n> Rendering pages...")
        val baked = deps.resolvePages().map {
            it.first to it.second.bake(NilNameResolver, deps).render()
        }

        println("\n> Writing rendered pages...")
        baked.forEach {
            val path = pathOut.resolve(dirPages.relativize(it.first))
            println("Writing '$path'")
            Files.createDirectories(path.parent)
            val doc = Jsoup.parse(it.second, "", Parser.htmlParser())
            doc.outputSettings().apply {
                charset(StandardCharsets.US_ASCII)
                escapeMode(Entities.EscapeMode.extended)
                if (args.reformat) {
                    prettyPrint(true)
                    indentAmount(args.indent)
                } else {
                    prettyPrint(false)
                    indentAmount(0)
                }
            }
            Files.write(path, doc.outerHtml().toByteArray())
        }

        println("\n> Copying static assets...")
        Files.walk(dirStatic).filter { !Files.isDirectory(it) }.forEach {
            val path = pathOut.resolve(dirStatic.relativize(it))
            println("Writing '$path'")
            Files.createDirectories(path.parent)
            Files.copy(it, path, StandardCopyOption.REPLACE_EXISTING)
        }

        println("\n> Done!")
        deps.printUnresolved()
    }

}

private class AssetDependencyTree(private val adapters: AssetAdapterResolver) : AssetResolver {

    private val keyMapping: MutableMap<String, Path> = mutableMapOf()
    private val pageMapping: MutableMap<String, Path> = mutableMapOf()
    private val cache: MutableMap<String, Asset> = mutableMapOf()
    private val resolutionStack: Deque<String> = LinkedList()

    fun registerTemplate(key: String, file: Path) {
        keyMapping[key]?.let { throw DuplicateAssetException(key, it, file) }
        println("Found '$key'")
        keyMapping[key] = file
    }

    fun registerPage(key: String, file: Path) {
        registerTemplate(key, file)
        pageMapping[key] = file
    }

    fun resolvePages(): List<Pair<Path, Asset>> = pageMapping.map { entry ->
        entry.value to (cache[entry.key] ?: parsePath(entry.key, entry.value))
    }

    fun printUnresolved() {
        val unused = keyMapping.filter { !cache.containsKey(it.key) }
        if (unused.isNotEmpty()) {
            println("! Some assets were discovered but unused:")
            unused.forEach { println("- ${it.key} (${it.value})") }
        }
    }

    override fun resolveAsset(key: String): Asset = cache[key] ?: parse(key)

    private fun parse(key: String): Asset = parsePath(key, keyMapping[key] ?: throw UnresolvableAssetException(key))

    private fun parsePath(key: String, path: Path): Asset {
        if (resolutionStack.contains(key)) {
            throw RenderingStateException("Circular dependency: $key (${resolutionStack.joinToString(" -> ")})")
        }
        resolutionStack.push(key)
        val adapter = adapters.adapterFor(key, path)
        println("Parsing '$key'")
        val page = adapter.parse(key, Files.readAllBytes(path), this)
        resolutionStack.pop()
        cache[key] = page
        return page
    }

}

private class DefaultAdapters : AssetAdapterResolver {

    private val adapters: List<AssetAdapter> = listOf(
        HtmlAdapter(), JsonAdapter()
    )

    override fun adapterFor(key: String, path: Path): AssetAdapter {
        val mimetype = Files.probeContentType(path)
        return adapters.firstOrNull { it.canParse(path, mimetype) } ?: throw UnrenderableAssetException(key, path)
    }

}
