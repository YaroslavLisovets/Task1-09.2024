package org.example

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File

class MyArgs(parser: ArgParser) {
    val root by parser.storing("--root", help = "The path to root project").default<String>("./temp")
    val output by parser.storing("--output", help = "The path to output project").default<String>("output.txt")
}


interface ITextChunk {
    fun renderText(): String
}

class TextChunk(text: String) : ITextChunk {
    private val text: String = ""
    override fun renderText(): String {
        return text
    }
}


class FileNode(filePath: String) : ITextChunk {

    val path: String = ""
    val children: MutableList<FileNode> = emptyList<FileNode>().toMutableList()
    val textChunks = mutableListOf<ITextChunk>()
    override fun renderText(): String {
        return textChunks.map { textChunks->textChunks.renderText() }.joinToString()
    }

}


fun main(args: Array<String>) {
    val myArgs = MyArgs(ArgParser(args))
    val root: String = myArgs.root
    val output = myArgs.output

    val nodeMap = hashMapOf<String, FileNode>()
    val resultingList = File(root)
        .walkTopDown()
        .filter { it.isFile }
        .sortedBy { it.name }
    val resultFile = File(output)
    resultFile.writer().use { writer -> resultingList.forEach { writer.write(it.readText()) } }

    val rootFile = File(root)
    for (file in rootFile.walkTopDown()) {
        if (file.isFile) {
            fillRequirements(file, nodeMap, rootFile)
        }
    }

    val sortedFiles = topologicalSort(nodeMap)

    if (sortedFiles != null) {
        println("Topologically sorted list:")
        sortedFiles.forEach {
            println(it)
            println(nodeMap[it]!!.renderText())
        }
    } else {
        println("A cyclic dependency was detected.")
    }


}

fun fillRequirements(file: File, nodeMap: HashMap<String, FileNode>, rootFile: File) {
    val filePath = file.relativeTo(rootFile).path.replace("\\", "/")
    val node = nodeMap.getOrPut(filePath) { FileNode(filePath) }

    for (line in file.readLines()) {
        if (!line.startsWith("require ")) {
            node.textChunks.add(TextChunk(line))
            continue
        }
        val requirement = line.trim().slice(9 until line.length - 1)
        val childNode = nodeMap.getOrPut(requirement) { FileNode(requirement) }
        node.children.add(childNode)
        node.textChunks.add(childNode)
    }
}

fun topologicalSort(nodeMap: HashMap<String, FileNode>): List<String>? {
    val sortedList = mutableListOf<String>()
    val visited = mutableMapOf<String, Boolean>()  // Track visited nodes
    val recStack = mutableMapOf<String, Boolean>()  // Track the recursion stack to detect cycles

    for (node in nodeMap.values) {
        if (visited[node.path] != true) {
            if (!dfs(node, visited, recStack, sortedList)) {
                return null  // A cycle was detected
            }
        }
    }

    return sortedList
}

fun dfs(
    node: FileNode,
    visited: MutableMap<String, Boolean>,
    recStack: MutableMap<String, Boolean>,
    sortedList: MutableList<String>
): Boolean {
    if (recStack[node.path] == true) {
        println("Cycle detected: ${getCyclePath(node.path, recStack)}")
        return false  // Cycle detected
    }

    if (visited[node.path] == true) {
        return true  // Already processed, skip
    }

    visited[node.path] = true
    recStack[node.path] = true

    for (child in node.children) {
        if (!dfs(child, visited, recStack, sortedList)) {
            return false  // Cycle detected in recursion
        }
    }

    recStack[node.path] = false
    sortedList.add(0, node.path)  // Add to the sorted list

    return true
}

fun getCyclePath(start: String, recStack: MutableMap<String, Boolean>): String {
    val cyclePath = mutableListOf<String>()
    var found = false
    for (node in recStack.keys) {
        if (recStack[node] == true) {
            if (node == start) found = true
            if (found) cyclePath.add(node)
        }
    }
    cyclePath.add(start)
    return cyclePath.joinToString(" -> ")
}

