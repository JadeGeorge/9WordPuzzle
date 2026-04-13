/*
 * File:     Dictionary.kt
 * Created:  2026-04-13
 * Modified: 2026-04-13
 * Author:   Jade
 * Purpose:  Trie-based dictionary for fast anagram lookups. Stores all valid words
 *           and finds every word that can be formed from a given set of letters.
 * Notes:    Ported from the original anagram-solver.js trie implementation.
 *           The trie structure means lookups are much faster than scanning a word list.
 */

package com.au.lightlytwisted.nineletters

// A single node in the trie — holds child nodes for each possible next letter
class TrieNode {
    val children = HashMap<Char, TrieNode>()
    var isWord = false
}

// The dictionary itself — wraps the trie root and exposes add/search operations
class Dictionary {
    val root = TrieNode()
    var wordCount = 0

    // Inserts a word into the trie one letter at a time
    fun addWord(word: String) {
        var node = root
        for (c in word) {
            node = node.children.getOrPut(c) { TrieNode() }
        }
        node.isWord = true
        wordCount++
    }

    // Returns all words that can be formed using some or all of the given letters, longest first
    fun findMatches(letters: String): List<String> {
        val results = mutableSetOf<String>()
        findRecursive(root, letters.toMutableList(), StringBuilder(), results)
        return results.sortedByDescending { it.length }
    }

    // Recursively walks the trie, trying each remaining letter at each step
    private fun findRecursive(
        node: TrieNode,
        remaining: MutableList<Char>,
        current: StringBuilder,
        results: MutableSet<String>
    ) {
        if (node.isWord && current.isNotEmpty()) {
            results.add(current.toString())
        }
        for (i in remaining.indices) {
            val c = remaining[i]
            val child = node.children[c] ?: continue
            current.append(c)
            remaining.removeAt(i)
            findRecursive(child, remaining, current, results)
            remaining.add(i, c)
            current.deleteCharAt(current.length - 1)
        }
    }
}