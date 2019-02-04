package xyz.phanta.wpd.model

import java.nio.file.Path

open class RenderingException : Exception {

    constructor(msg: String, cause: Throwable) : super(msg, cause)

    constructor(msg: String) : super(msg)

}

class UnrenderableAssetException(val key: String, val path: Path) : RenderingException("No adapter for asset: $key ($path)")

class DuplicateAssetException(val key: String, val oldPath: Path, val newPath: Path)
    : RenderingException("Duplicate asset: $key ($oldPath -> $newPath)")

class UnresolvableAssetException(val key: String) : RenderingException("Could not resolve asset: $key")

class UnresolvableReferenceException(val identifier: String) : RenderingException("Could not resolve reference: $identifier")

class ReferenceTypeException(val identifier: String, val expectedType: Class<*>, val actualType: Class<*>)
    : RenderingException("Wanted ${expectedType.name} but referenced ${actualType.name}: $identifier")

class RenderingStateException(msg: String) : RenderingException(msg)
