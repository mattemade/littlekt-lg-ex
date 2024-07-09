package com.littlekt

import com.littlekt.async.KtScope
import com.littlekt.async.MainDispatcher
import com.littlekt.async.mainThread
import com.littlekt.audio.OpenALAudioContext
import com.littlekt.file.Base64.decodeFromBase64
import com.littlekt.file.ByteBufferImpl
import com.littlekt.file.JvmVfs
import com.littlekt.file.vfs.VfsFile
import com.littlekt.file.vfs.readPixmap
import com.littlekt.graphics.GLVersion
import com.littlekt.graphics.internal.InternalResources
import com.littlekt.input.LwjglInput
import com.littlekt.log.Logger
import com.littlekt.util.fastForEach
import com.littlekt.util.internal.now
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.IntBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.lwjgl.opengl.GL as LWJGL


/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class LwjglContext(override val configuration: JvmConfiguration) : Context() {

    override val stats: AppStats = AppStats()
    override val graphics: LwjglGraphics = LwjglGraphics(this, stats.engineStats)
    override val logger: Logger = Logger(configuration.title)
    override val input: LwjglInput = LwjglInput(this)
    override val vfs = JvmVfs(this, logger, "./.storage", ".")
    override val resourcesVfs: VfsFile get() = vfs.root
    override val storageVfs: VfsFile get() = VfsFile(vfs, "./.storage")

    override val platform: Platform = Platform.DESKTOP

    override val clipboard: JvmClipboard by lazy { JvmClipboard(windowHandle) }

    internal var windowHandle: Long = 0
        private set

    internal val audioContext = OpenALAudioContext()

    private val windowShouldClose: Boolean
        get() = GLFW.glfwWindowShouldClose(windowHandle)

    private val tempBuffer: IntBuffer
    private val tempBuffer2: IntBuffer

    init {
        MemoryStack.stackPush().use { stack ->
            tempBuffer = stack.mallocInt(1) // int*
            tempBuffer2 = stack.mallocInt(1) // int*
        }
        KtScope.initiate()
        mainThread = Thread.currentThread()
    }


    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    override fun start(build: (app: Context) -> ContextListener) = runBlocking {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE) // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, configuration.resizeable.glfw) // the window will be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, configuration.maximized.glfw)

        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        if (isMac) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL30.GL_TRUE)
        }

        // Create the window
        windowHandle = GLFW.glfwCreateWindow(
            configuration.width,
            configuration.height,
            configuration.title,
            MemoryUtil.NULL,
            MemoryUtil.NULL
        )
        if (windowHandle == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")


        updateFramebufferInfo()

        // Get the resolution of the primary monitor
        val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())

        check(vidmode != null) { "Unable to retrieve GLFW video mode" }

        // Center the window
        GLFW.glfwSetWindowPos(
            windowHandle,
            configuration.windowPosX ?: ((vidmode.width() - graphics.width) / 2),
            configuration.windowPosY ?: ((vidmode.height() - graphics.height) / 2)
        )

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(windowHandle)

        LWJGL.createCapabilities()

        val versionString = GL11.glGetString(GL11.GL_VERSION) ?: ""
        val vendorString = GL11.glGetString(GL11.GL_VENDOR) ?: ""
        val rendererString = GL11.glGetString(GL11.GL_RENDERER) ?: ""
        graphics.gl.glVersion = GLVersion(platform, versionString, vendorString, rendererString)

        if (configuration.vSync) {
            // Enable v-sync
            GLFW.glfwSwapInterval(1)
        } else {
            GLFW.glfwSwapInterval(0)
        }


        // set window icon
        if (!isMac) {
            if (configuration.icons.isNotEmpty()) {
                val buffer = GLFWImage.malloc(configuration.icons.size)
                configuration.icons.forEach {
                    val pixmap = resourcesVfs[it].readPixmap()
                    val icon = GLFWImage.malloc()
                    icon.set(pixmap.width, pixmap.height, (pixmap.pixels as ByteBufferImpl).buffer)
                    buffer.put(icon)
                    icon.free()
                }
                buffer.position(0)
                GLFW.glfwSetWindowIcon(windowHandle, buffer)
                buffer.free()
            } else {
                val pixmap = ktHead32x32.decodeFromBase64().readPixmap()
                val icon = GLFWImage.malloc()
                icon.set(pixmap.width, pixmap.height, (pixmap.pixels as ByteBufferImpl).buffer)
                val buffer = GLFWImage.malloc(1)
                buffer.put(icon)
                icon.free()
                buffer.position(0)
                GLFW.glfwSetWindowIcon(windowHandle, buffer)
                buffer.free()
            }
        }

        // Make the window visible
        GLFW.glfwShowWindow(windowHandle)
        input.attachToWindow(windowHandle)

        // GLUtil.setupDebugMessageCallback()

        gl.clearColor(configuration.backgroundColor)

        InternalResources.createInstance(this@LwjglContext)
        InternalResources.INSTANCE.load()

        val listener: ContextListener = build(this@LwjglContext)

        GLFW.glfwSetFramebufferSizeCallback(windowHandle) { _, _, _ ->
            updateFramebufferInfo()
            graphics.gl.viewport(0, 0, graphics.backBufferWidth, graphics.backBufferHeight)

            resizeCalls.fastForEach { resize ->
                resize(
                    graphics.width,
                    graphics.height
                )
            }
        }

        listener.run { start() }
        listener.run {
            updateFramebufferInfo()
            resizeCalls.fastForEach { resize ->
                resize(
                    graphics.width,
                    graphics.height
                )
            }
        }


        while (!windowShouldClose) {
            calcFrameTimes(now().milliseconds)
            MainDispatcher.INSTANCE.executePending(available)
            update(dt)
        }

        disposeCalls.fastForEach { dispose -> dispose() }
    }

    private fun updateFramebufferInfo() {
        GLFW.glfwGetWindowSize(windowHandle, tempBuffer, tempBuffer2)

        graphics._logicalWidth = tempBuffer[0]
        graphics._logicalHeight = tempBuffer2[0]

        GLFW.glfwGetFramebufferSize(windowHandle, tempBuffer, tempBuffer2)

        graphics._backBufferWidth = tempBuffer[0]
        graphics._backBufferHeight = tempBuffer2[0]

    }

    private suspend fun update(dt: Duration) {
        audioContext.update()

        stats.engineStats.resetPerFrameCounts()

        invokeAnyRunnable()

        input.update()
        stats.update(dt)
        renderCalls.fastForEach { render -> render(dt) }
        postRenderCalls.fastForEach { postRender -> postRender(dt) }

        GLFW.glfwSwapBuffers(windowHandle)
        input.reset()
        GLFW.glfwPollEvents()
    }

    private fun invokeAnyRunnable() {
        if (postRunnableCalls.isNotEmpty()) {
            postRunnableCalls.fastForEach { postRunnable ->
                postRunnable.invoke()
            }
            postRunnableCalls.clear()
        }
    }

    override fun close() {
        GLFW.glfwSetWindowShouldClose(windowHandle, true)
    }

    override fun destroy() {
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowHandle)
        GLFW.glfwDestroyWindow(windowHandle)

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()

        audioContext.dispose()
    }

    private val Boolean.glfw: Int
        get() = if (this) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE
}