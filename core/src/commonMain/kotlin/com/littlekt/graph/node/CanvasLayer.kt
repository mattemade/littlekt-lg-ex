package com.littlekt.graph.node

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.ui.Control
import com.littlekt.graphics.Camera
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.MutableVec3f
import com.littlekt.util.Signal
import com.littlekt.util.signal
import com.littlekt.util.viewport.Viewport
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [CanvasLayer] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [CanvasLayer] context in order to initialize any values
 * @return the newly created [CanvasLayer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.canvasLayer(callback: @SceneGraphDslMarker CanvasLayer.() -> Unit = {}): CanvasLayer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return CanvasLayer().also(callback).addTo(this)
}

/**
 * Adds a [CanvasLayer] to the current [SceneGraph.root] as a child and then triggers the [CanvasLayer]
 * @param callback the callback that is invoked with a [CanvasLayer] context in order to initialize any values
 * @return the newly created [CanvasLayer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.canvasLayer(callback: @SceneGraphDslMarker CanvasLayer.() -> Unit = {}): CanvasLayer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.canvasLayer(callback)
}

/**
 * A [Node] that uses a separate [OrthographicCamera] for rendering instead of the inherited camera from the [SceneGraph].
 * This is useful to render a list of nodes with a camera a certain way and another list of nodes another way.
 *
 * For example: When rendering certain nodes at a low resolution using a [Viewport] to render UI at a higher resolution.
 *
 * @author Colton Daily
 * @date 3/13/2022
 */
open class CanvasLayer : Node() {

    /**
     * Viewport instance that can be used for rendering children nodes in inherited classes. This is not used directly
     * in the base [CanvasLayer] class.
     * @see ViewportCanvasLayer
     */
    var viewport: Viewport = Viewport()
    val canvasCamera: OrthographicCamera get() = viewport.camera as OrthographicCamera
    val canvasCamera3d: PerspectiveCamera = PerspectiveCamera() // TODO refactor use 3d viewport

    /**
     * Signal that is emitted when the viewport dimensions are changed by the [CanvasLayer].
     */
    val onSizeChanged: Signal = signal()

    /**
     * The viewport virtual/world width
     */
    var virtualWidth: Float
        get() = viewport.virtualWidth
        set(value) {
            viewport.virtualWidth = value
        }

    /**
     * The viewport virtual/world height
     */
    var virtualHeight: Float
        get() = viewport.virtualHeight
        set(value) {
            viewport.virtualHeight = value
        }

    /**
     * Width of the viewport
     */
    var width: Int
        get() = viewport.width
        set(value) {
            viewport.width = value
        }

    /**
     * Height of the viewport
     */
    var height: Int
        get() = viewport.height
        set(value) {
            viewport.height = value
        }

    /**
     * Viewport x-coord
     */
    var x: Int
        get() = viewport.x
        set(value) {
            viewport.x = value
        }

    /**
     * Viewport y-coord
     */
    var y: Int
        get() = viewport.y
        set(value) {
            viewport.y = value
        }

    private val temp = MutableVec2f()

    override fun resize(width: Int, height: Int) {
        canvasCamera3d.virtualWidth = width.toFloat()
        canvasCamera3d.virtualHeight = height.toFloat()
        canvasCamera.ortho(width, height)
        viewport.width = width
        viewport.height = height
        onSizeChanged.emit()

        super.resize(width, height)
    }

    open fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (!enabled || isDestroyed) return
        val scene = scene ?: return

        val prevProjMatrix = batch.projectionMatrix
        canvasCamera.ortho(scene.context.graphics.width, scene.context.graphics.height)
        canvasCamera.update()
        canvasCamera3d.virtualWidth = scene.context.graphics.width.toFloat()
        canvasCamera3d.virtualHeight = scene.context.graphics.height.toFloat()
        canvasCamera3d.update()
        batch.projectionMatrix = canvasCamera.viewProjection
        nodes.forEach {
            it.propagateInternalRender(batch, canvasCamera, canvasCamera3d, shapeRenderer, renderCallback)
            if (scene.showDebugInfo) it.propagateInternalDebugRender(
                batch,
                canvasCamera,
                canvasCamera3d,
                shapeRenderer,
                renderCallback
            )
        }
        batch.projectionMatrix = prevProjMatrix
    }

    override fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        render(batch, shapeRenderer, renderCallback)
    }

    override fun propagateInternalDebugRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        // we override this and make it do nothing so that we don't make multiple calls
        // to debugRender with nested CanvasLayers.
    }


    override fun propagateHit(hx: Float, hy: Float): Control? {
        val scene = scene ?: return null
        if (!enabled || isDestroyed) return null
        canvas?.canvasToScreenCoordinates(temp.set(hx, hy))
        canvasCamera.screenToWorld(scene.context, temp, viewport, temp)
        nodes.forEachReversed {
            val target = it.propagateHit(temp.x, temp.y)
            if (target != null) {
                return target
            }
        }
        return null
    }


    override fun propagateInput(event: InputEvent<*>): Boolean {
        val scene = scene ?: return false
        if (!enabled || isDestroyed) return false
        temp.set(event.canvasX, event.canvasY)
        canvas?.canvasToScreenCoordinates(temp)
        canvasCamera.screenToWorld(scene.context, temp, viewport, temp)
        nodes.forEachReversed {
            // we set canvas coords every iteration just in case a child CanvasLayer changes it
            event.canvasX = temp.x
            event.canvasY = temp.y
            it.propagateInput(event)
            if (event.handled) {
                return true
            }
        }
        callInput(event)
        return event.handled
    }

    override fun propagateUnhandledInput(event: InputEvent<*>): Boolean {
        val scene = scene ?: return false
        if (!enabled || isDestroyed) return false
        temp.set(event.canvasX, event.canvasY)
        canvas?.canvasToScreenCoordinates(temp)
        canvasCamera.screenToWorld(scene.context, temp, viewport, temp)
        nodes.forEachReversed {
            // we set canvas coords every iteration just in case a child CanvasLayer changes it
            event.canvasX = temp.x
            event.canvasY = temp.y
            it.propagateUnhandledInput(event)
            if (event.handled) {
                return true
            }
        }
        callUnhandledInput(event)
        return event.handled
    }

    override fun onDestroy() {
        super.onDestroy()
        onSizeChanged.clear()
    }

    /**
     * Convert screen coordinates to local canvas coordinates.
     * @param vector2 the input screen coordinates. This is also used as the `out` vector.
     */
    fun screenToCanvasCoordinates(vector2: MutableVec2f): MutableVec2f {
        canvasCamera.screenToWorld(
            scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector2,
            viewport,
            vector2
        )
        return vector2
    }

    /**
     * Convert screen coordinates to local canvas coordinates.
     * @param vector3 the input screen coordinates. This is also used as the `out` vector.
     */
    fun screenToCanvasCoordinates(vector3: MutableVec3f): MutableVec3f {
        canvasCamera.screenToWorld(
            scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector3,
            viewport,
            vector3
        )
        return vector3
    }

    /**
     * Convert canvas coordinates to screen coordinates.
     * @param vector2 the input canvas coordinates. This is also used as the `out` vector.
     */
    fun canvasToScreenCoordinates(vector2: MutableVec2f): MutableVec2f {
        canvasCamera.worldToScreen(
            scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector2,
            viewport,
            vector2
        )
        return vector2
    }

    /**
     * Convert canvas coordinates to screen coordinates.
     * @param vector3 the input canvas coordinates. This is also used as the `out` vector.
     */
    fun canvasToScreenCoordinates(vector3: MutableVec3f): MutableVec3f {
        canvasCamera.worldToScreen(
            scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector3,
            viewport,
            vector3
        )
        return vector3
    }
}