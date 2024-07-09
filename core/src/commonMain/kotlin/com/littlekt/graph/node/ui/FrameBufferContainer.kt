package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.FrameBufferNode
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max

/**
 * Adds a [FrameBufferContainer] to the current [Node] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.frameBufferContainer(callback: @SceneGraphDslMarker FrameBufferContainer.() -> Unit = {}): FrameBufferContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return FrameBufferContainer().also(callback).addTo(this)
}

/**
 * Adds a [FrameBufferContainer] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.frameBufferContainer(callback: @SceneGraphDslMarker FrameBufferContainer.() -> Unit = {}): FrameBufferContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.frameBufferContainer(callback)
}

/**
 * A [Container] node that holds a [FrameBufferNode]. It uses the [FrameBufferNode] minimum size unless
 * [stretch] is enabled.
 * @author Colton Daily
 * @date 03/17/2022
 */
open class FrameBufferContainer : Container() {

    private val temp = MutableVec2f()
    private var dirty = true
    var offsetX = 0f
    var offsetY = 0f
    var margin = 0f

    /**
     * If `true`, the frame buffer will be scaled to this [Control] size.
     */
    var stretch = false
        set(value) {
            if (field == value) return
            field = value
            onMinimumSizeChanged()
            queueSort()
        }

    /**
     * Divide the viewport's effective resolution by this value while preserving its scale. This can be used to
     * speed up rendering.
     *
     * For example, a 1280x720 frame buffer with [shrink] set to `2` will be rendered at 640x360 while occupying
     * the same size in the container.
     *
     * [stretch] must be set to `true` for this to take effect.
     */
    var shrink: Int = 1
        set(value) {
            if (field == value) return
            check(shrink >= 1) { "Stretch must be >= 1" }
            field = value
            if (!stretch) return
            dirty = true
        }

    override fun onChildAdded(child: Node) {
        if (child is FrameBufferNode) {
            dirty = true
        }
    }

    override fun onResized() {
        nodes.forEach {
            if (it is FrameBufferNode) {
                dirty = true
                return
            }
        }
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        if (dirty) {
            nodes.forEach {
                if (it is FrameBufferNode) {
                    it.resizeFbo(
                        (width / shrink.toFloat()).toInt(),
                        (height / shrink.toFloat()).toInt()
                    )
                }
            }
            dirty = false
        }
        nodes.forEach { node ->
            if (node is FrameBufferNode) {
                node.fboTexture?.let {
                    batch.draw(
                        it,
                        globalX - margin / shrink + offsetX,
                        globalY - margin / shrink + offsetY,
                        width = if (stretch) width + margin * 2 / shrink else it.width.toFloat() + margin * 2 / shrink,
                        height = if (stretch) height + margin * 2 / shrink else it.height.toFloat() + margin * 2 / shrink,
                        scaleX = globalScaleX,
                        scaleY = globalScaleY,
                        rotation = globalRotation,
                        flipY = true
                    )
                }
            }
        }
    }

    override fun calculateMinSize() {
        if (stretch) {
            _internalMinWidth = 0f
            _internalMinHeight = 0f
        } else {
            nodes.forEach {
                if (it is FrameBufferNode) {
                    _internalMinWidth = max(_internalMinWidth, it.width.toFloat())
                    _internalMinHeight = max(_internalMinHeight, it.height.toFloat())
                }
            }
        }
        minSizeInvalid = false
    }

    override fun callInput(event: InputEvent<*>) {
        if (!enabled || !insideTree) return

        event.apply {
            val localCoords = toLocal(event.canvasX, event.canvasY, tempVec2f)
            localX = localCoords.x
            localY = localCoords.y
        }
        onInput.emit(event) // signal is first due to being able to handle the event
        if (event.handled) {
            return
        }
        input(event)
    }

    override fun propagateHit(hx: Float, hy: Float): Control? {
        val canvas = canvas ?: return null
        temp.set(hx, hy)
        if (stretch) {
            canvas.canvasToScreenCoordinates(temp)
            temp.scale(1f / shrink.toFloat())
        }
        canvas.screenToCanvasCoordinates(temp)
        nodes.forEachReversed {
            val target = if (it is FrameBufferNode) it.propagateHit(temp.x, temp.y) else it.propagateHit(hx, hy)
            if (target != null) {
                return target
            }
        }

        return null
    }

    override fun propagateInput(event: InputEvent<*>): Boolean {
        scene ?: return false
        val canvas = canvas ?: return false
        if (!enabled || isDestroyed) return false
        temp.set(event.sceneX, event.sceneY)
        if (stretch) {
            canvas.canvasToScreenCoordinates(temp)
            temp.scale(1f / shrink.toFloat())
        }
        canvas.screenToCanvasCoordinates(temp)
        val prevCanvasX = event.canvasX
        val prevCanvasY = event.canvasY
        nodes.forEachReversed {
            // we set canvas coords every iteration just in case a child CanvasLayer changes it
            if (it is FrameBufferNode) {
                event.canvasX = temp.x
                event.canvasY = temp.y
            } else {
                event.canvasX = prevCanvasX
                event.canvasY = prevCanvasY
            }
            it.propagateInput(event)
            if (event.handled) {
                return true
            }
        }
        callInput(event)
        return event.handled
    }

    override fun propagateUnhandledInput(event: InputEvent<*>): Boolean {
        scene ?: return false
        val canvas = canvas ?: return false
        if (!enabled || isDestroyed) return false
        temp.set(event.sceneX, event.sceneY)
        if (stretch) {
            canvas.canvasToScreenCoordinates(temp)
            temp.scale(1f / shrink.toFloat())
        }
        canvas.screenToCanvasCoordinates(temp)
        val prevCanvasX = event.canvasX
        val prevCanvasY = event.canvasY
        nodes.forEachReversed {
            // we set canvas coords every iteration just in case a child CanvasLayer changes it
            if (it is FrameBufferNode) {
                event.canvasX = temp.x
                event.canvasY = temp.y
            } else {
                event.canvasX = prevCanvasX
                event.canvasY = prevCanvasY
            }
            it.propagateUnhandledInput(event)
            if (event.handled) {
                return true
            }
        }
        callUnhandledInput(event)
        return event.handled
    }

    companion object {
        private val tempVec2f = MutableVec2f()
    }
}