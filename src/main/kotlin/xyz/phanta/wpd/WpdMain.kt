package xyz.phanta.wpd

import com.sun.net.httpserver.spi.HttpServerProvider
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import xyz.phanta.wpd.model.RenderingException
import xyz.phanta.wpd.renderer.Renderer
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.Executors
import kotlin.concurrent.thread

private val MSG_BAD_PATH: ByteArray = "Bad path".toByteArray()

fun main(rawArgs: Array<String>) {
    mainBody {
        // parse args
        val args = ArgParser(rawArgs).parseInto(::WpdArgs)

        // prepare
        val fs = FileSystems.getDefault()
        val pathIn = fs.getPath(args.inputDir)
        val pathOut = fs.getPath(args.outputDir).toAbsolutePath()
        val renderer = Renderer(args, pathIn, pathOut)

        // do rendering
        println("# Rendering...")
        renderer.render()

        // launch observer if necessary
        if (args.observe) {
            println("\n# Starting observer...")
            thread(name = "WPD Observer Thread") {
                fs.newWatchService().use { ws ->
                    renderer.inputDirectories.forEach { inputDir ->
                        Files.walk(inputDir).filter { Files.isDirectory(it) }.forEach {
                            it.register(
                                ws,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY
                            )
                        }
                    }
                    while (!Thread.interrupted()) {
                        try {
                            try {
                                val key = ws.take()
                                println("$ Changed: ${key.pollEvents().joinToString { it.context().toString() }}")
                                key.reset()
                            } catch (e: InterruptedException) {
                                break
                            }
                            renderer.render()
                        } catch (e: Exception) {
                            println("$ Encountered exception while re-rendering:")
                            e.printStackTrace(System.out)
                        }
                    }
                }
            }
        }

        // launch server if necessary
        args.httpPort?.let { port ->
            println("# Starting server...")
            val server = HttpServerProvider.provider().createHttpServer(InetSocketAddress(port), 0)
            server.createContext("/") { req ->
                try {
                    var reqPath = pathOut.resolve(req.requestURI.path.substring(1))
                    if (Files.isDirectory(reqPath)) reqPath = reqPath.resolve("index.html")
                    try {
                        val data = Files.readAllBytes(reqPath)
                        req.responseHeaders.add("Content-Type", Files.probeContentType(reqPath) ?: "text/plain")
                        req.sendResponseHeaders(200, data.size.toLong())
                        req.responseBody.write(data)
                        println("${req.requestMethod} ${req.requestURI} - 200")
                    } catch (e: IOException) {
                        val msg = e.toString().toByteArray()
                        req.sendResponseHeaders(404, msg.size.toLong())
                        req.responseBody.write(msg)
                        println("${req.requestMethod} ${req.requestURI} - 404")
                    }
                } catch (e: InvalidPathException) {
                    req.sendResponseHeaders(400, MSG_BAD_PATH.size.toLong())
                    req.responseBody.write(MSG_BAD_PATH)
                    println("${req.requestMethod} ${req.requestURI} - 400")
                } finally {
                    req.responseBody.close()
                }
            }
            server.executor = Executors.newSingleThreadExecutor { task ->
                Thread(task, "WPD Server Thread").also { it.isDaemon = true }
            }
            server.start()
        }
    }
}

class WpdArgs(parser: ArgParser) {

    val httpPort: Int? by parser.storing("--serve", "-s", help = "HTTP server port.") { toInt() }.default { null }

    val observe: Boolean by parser.flagging(
        "--continuous",
        "-c",
        help = "Observe source files and update rendered files."
    ).default { false }

    val inputDir: String by parser.storing("--in", "-i", help = "Input directory.").default { "." }

    val outputDir: String by parser.storing("--out", "-o", help = "Output directory.").default { "out" }

    val reformat: Boolean by parser.flagging("--reformat", "-R", help = "Reformat output to prettify.")

    val indent: Int by parser.storing("--indent", "-n", help = "Indentation size for reformatting.") { toInt() }
        .default { 4 }

}
