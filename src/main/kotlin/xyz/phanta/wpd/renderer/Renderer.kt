package xyz.phanta.wpd.renderer

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import xyz.phanta.wpd.WpdArgs
import xyz.phanta.wpd.adapter.AdapterSet
import xyz.phanta.wpd.adapter.PageResolver
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class Renderer(private val args: WpdArgs, pathIn: Path, private val pathOut: Path) {

    private val adapters: AdapterSet = AdapterSet()

    private val dirPages: Path = pathIn.resolve("pages")
    private val dirTemplates: Path = pathIn.resolve("templates")
    private val dirAssets: Path = pathIn.resolve("assets")

    val inputDirectories: List<Path>
        get() = listOf(dirPages, dirTemplates, dirAssets)

    fun render() {
        val pages = PageDependencyTree(adapters)

        if (Files.exists(pathOut)) {
            println("\n> Cleaning up output dir...")
            Files.walk(pathOut)
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::delete)
        }

        println("\n> Searching for page files...")
        Files.walk(dirTemplates).filter { !Files.isDirectory(it) }.forEach {
            pages.registerTemplate(dirTemplates.relativize(it).toString(), it)
        }
        Files.walk(dirPages).filter { !Files.isDirectory(it) }.forEach {
            pages.registerPage(dirPages.relativize(it).toString(), it)
        }

        println("\n> Rendering pages...")
        val rendered = pages.resolvePages().map {
            it.first to it.second.body.render(it.second, pages)
        }

        println("\n> Writing rendered pages...")
        rendered.forEach {
            val path = pathOut.resolve(dirPages.relativize(it.first))
            println("Writing '$path'")
            Files.createDirectories(path.parent)
            val doc = Jsoup.parse(it.second, "", Parser.htmlParser())
            if (args.ugly) {
                doc.outputSettings().prettyPrint(false)
            } else {
                doc.outputSettings().indentAmount(args.indent)
            }
            Files.write(path, doc.outerHtml().toByteArray())
        }

        println("\n> Copying assets...")
        Files.walk(dirAssets).filter { !Files.isDirectory(it) }.forEach {
            val path = pathOut.resolve(dirAssets.relativize(it))
            println("Writing '$path'")
            Files.createDirectories(path.parent)
            Files.copy(it, path, StandardCopyOption.REPLACE_EXISTING)
        }

        println("\n> Done!")
    }

}

private class PageDependencyTree(private val adapters: AdapterSet) : PageResolver {

    private val pathMapping: MutableMap<String, Path> = mutableMapOf()
    private val pageMapping: MutableMap<String, Path> = mutableMapOf()
    private val cache: MutableMap<String, PageDefinition> = mutableMapOf()

    fun registerTemplate(key: String, file: Path) {
        if (pathMapping.containsKey(key)) throw IllegalArgumentException("Duplicate page file '$key'")
        println("Found '$key'")
        pathMapping[key] = file
    }

    fun registerPage(key: String, file: Path) {
        registerTemplate(key, file)
        pageMapping[key] = file
    }

    fun resolvePages(): List<Pair<Path, PageDefinition>> = pageMapping.map { entry ->
        entry.value to cache.computeIfAbsent(entry.key) { parsePath(it, entry.value) }
    }

    override fun resolvePage(key: String): PageDefinition = cache.computeIfAbsent(key, ::parse)

    private fun parse(key: String): PageDefinition = parsePath(key, pathMapping[key]
            ?: throw NoSuchElementException(key))

    private fun parsePath(key: String, path: Path): PageDefinition {
        val adapter = adapters.adapterForPath(path)
        if (adapter == null) {
            println("No renderer available for $key ($path)!")
            return EmptyPageDefinition()
        }
        println("Rendering '$key'")
        val page = adapter.parse(Files.readAllBytes(path), this)
        cache[key] = page
        return page
    }

}
