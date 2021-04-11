package xyz.phanta.wpd.util

import xyz.phanta.wird.parser.Grammar

fun loadGrammar(resource: String): Grammar =
    Grammar.create(Grammar::class.java.getResource(resource).readText())
