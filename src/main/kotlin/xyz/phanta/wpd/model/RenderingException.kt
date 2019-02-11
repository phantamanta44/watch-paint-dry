package xyz.phanta.wpd.model

import java.nio.file.Path

open class RenderingException : Exception {

    constructor(msg: String, cause: Throwable) : super(msg, cause)

    constructor(msg: String) : super(msg)

}

class UnrenderableAssetException(key: String, val path: Path) : RenderingException("No adapter for asset: $key ($path)")

class DuplicateAssetException(key: String, oldPath: Path, newPath: Path)
    : RenderingException("Duplicate asset: $key ($oldPath -> $newPath)")

class UnresolvableAssetException(key: String) : RenderingException("Could not resolve asset: $key")

class AssetParsingException : RenderingException {

    val key: String
    val line: Int
    val pos: Int

    constructor (key: String, line: Int, pos: Int, cause: Throwable) : super("Parsing failed at $line/$pos for asset: $key", cause) {
        this.key = key
        this.line = line
        this.pos = pos
    }

    constructor(key: String, cause: Throwable) : super("Parsing failed for asset: $key", cause) {
        this.key = key
        this.line = -1
        this.pos = -1
    }

}

class UnresolvableReferenceException(identifier: String) : RenderingException("Could not resolve reference: $identifier")

class ReferenceTypeException(identifier: String, expectedType: Class<*>, actualType: Class<*>)
    : RenderingException("Expected ${expectedType.name} but found ${actualType.name}: $identifier")

class MalformationException(msg: String) : RenderingException(msg)

class RenderingStateException(msg: String) : RenderingException(msg)
