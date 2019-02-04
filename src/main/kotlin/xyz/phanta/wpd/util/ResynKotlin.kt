package xyz.phanta.wpd.util

import io.github.phantamanta44.resyn.parser.token.TokenContainer
import io.github.phantamanta44.resyn.parser.token.TokenNode

fun TokenContainer.childNode(n: Int): String = (children[n] as TokenNode).content
