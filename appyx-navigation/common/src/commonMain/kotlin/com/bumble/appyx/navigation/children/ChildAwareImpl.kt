package com.bumble.appyx.navigation.children

import com.bumble.appyx.interactions.core.Element
import com.bumble.appyx.navigation.lifecycle.isDestroyed
import com.bumble.appyx.navigation.node.Node
import com.bumble.appyx.navigation.node.ParentNode
import com.bumble.appyx.navigation.platform.DefaultPlatformLifecycleObserver
import com.bumble.appyx.navigation.platform.PlatformLifecycle
import com.bumble.appyx.navigation.withPrevious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class ChildAwareImpl<N : Node> : ChildAware<N> {

    private val callbacks: MutableList<ChildAwareCallbackInfo> = ArrayList()

    private lateinit var children: StateFlow<Map<out Element<*>, ChildEntry<*>>>
    private lateinit var lifecycle: PlatformLifecycle
    private lateinit var coroutineScope: CoroutineScope

    override lateinit var node: N
        private set

    override fun init(node: N) {
        this.node = node
        lifecycle = node.lifecycle
        coroutineScope = lifecycle.coroutineScope
        if (node is ParentNode<*>) {
            children = node.children
            coroutineScope.launch { observeChanges() }
        } else {
            children = MutableStateFlow(emptyMap())
        }
    }

    private suspend fun observeChanges() {
        children
            .map { it.values.mapNotNullTo(HashSet()) { it.nodeOrNull } }
            .withPrevious()
            .collect { (previous, current) ->
                val newNodes = current - previous.orEmpty()
                val visitedSet = HashSet<Node>()
                newNodes.forEach { node ->
                    notifyWhenChanged(node, current, visitedSet)
                    visitedSet.add(node)
                }
            }
    }

    override fun <T : Any> whenChildAttached(
        child: KClass<T>,
        callback: ChildCallback<T>
    ) {
        if (lifecycle.isDestroyed) return
        val info = ChildAwareCallbackInfo.Single(child, callback, lifecycle)
        callbacks += info
        notifyWhenRegistered(info)
        lifecycle.removeWhenDestroyed(info)
    }

    override fun <T1 : Any, T2 : Any> whenChildrenAttached(
        child1: KClass<T1>,
        child2: KClass<T2>,
        callback: ChildrenCallback<T1, T2>
    ) {
        if (lifecycle.isDestroyed) return
        val info = ChildAwareCallbackInfo.Double(child1, child2, callback, lifecycle)
        callbacks += info
        notifyWhenRegistered(info)
        lifecycle.removeWhenDestroyed(info)
    }

    private fun notifyWhenRegistered(callback: ChildAwareCallbackInfo) {
        callback.onRegistered(getCreatedNodes(children.value))
    }

    private fun notifyWhenChanged(child: Node, nodes: Collection<Node>, ignore: Set<Node>) {
        for (callback in callbacks) {
            when (callback) {
                is ChildAwareCallbackInfo.Double<*, *> -> callback.onNewNodeAppeared(
                    activeNodes = nodes,
                    newNode = child,
                    ignoreNodes = ignore,
                )
                is ChildAwareCallbackInfo.Single<*> -> callback.onNewNodeAppeared(child)
            }
        }
    }

    private fun PlatformLifecycle.removeWhenDestroyed(info: ChildAwareCallbackInfo) {
        addObserver(object : DefaultPlatformLifecycleObserver {
            override fun onDestroy() {
                callbacks.remove(info)
            }
        })
    }

    private fun getCreatedNodes(childEntryMap: Map<out Element<*>, ChildEntry<*>>) =
        childEntryMap.values.mapNotNull { entry -> entry.nodeOrNull }

}
