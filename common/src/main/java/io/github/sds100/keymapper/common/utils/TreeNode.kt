package io.github.sds100.keymapper.common.utils

data class TreeNode<T>(val value: T, val children: MutableList<TreeNode<T>> = mutableListOf())

inline fun <T> TreeNode<T>.breadFirstTraversal(
    action: (T) -> Unit,
) {
    val queue = ArrayDeque<TreeNode<T>>()
    queue.add(this)

    while (queue.isNotEmpty()) {
        val currentNode = queue.removeFirst()
        action(currentNode.value)
        queue.addAll(currentNode.children)
    }
}
