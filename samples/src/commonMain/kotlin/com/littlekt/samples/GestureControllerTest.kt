package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Color
import com.littlekt.graphics.Fonts
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.Key
import com.littlekt.input.gesture.gestureController
import com.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 10/24/2022
 */
class GestureControllerTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera

        var inputStatus = "TBD"
        var gestureStatus = "TBD"

        input.inputProcessor {
            onTouchDown { screenX, screenY, pointer ->
                inputStatus = "Pointer ${pointer.index} touch down at $screenX, $screenY"
            }

            onTouchDragged { screenX, screenY, pointer ->
                inputStatus = "Pointer ${pointer.index} touch dragged at $screenX, $screenY"
            }

            onTouchUp { screenX, screenY, pointer ->
                inputStatus = "Pointer ${pointer.index} touch up at $screenX, $screenY"
            }
        }

        input.gestureController {
            onTouchDown { screenX, screenY, pointer ->
                gestureStatus = "Pointer ${pointer.index} touch down at $screenX, $screenY"
            }

            onFling { velocityX, velocityY, pointer ->
                gestureStatus = "Pointer ${pointer.index} fling at a velocity of $velocityX, $velocityY"
            }

            onPinch { initialPos1, initialPos2, pos1, pos2 ->
                gestureStatus = "Pinching!"
            }

            onTap { screenX, screenY, count, pointer ->
                gestureStatus = "Pointer ${pointer.index} is tapping at $screenX,$screenY for a total of $count taps."
            }

            onZoom { initialDistance, distance ->
                gestureStatus = "Zooming a distance of $distance"
            }

            onPinchStop {
                gestureStatus = "Pinch stopped!"
            }

            onLongPress { screenX, screenY ->
                gestureStatus = "Long pressing at $screenX, $screenY"
                input.vibrate(100.milliseconds)
            }

            onPan { screenX, screenY, dx, dy ->
                gestureStatus = "Panning at $screenX,$screenY with a delta of $dx,$dy"
            }

            onPanStop { screenX, screenY, pointer ->
                gestureStatus = "Panning stopped at $screenX,$screenY"
            }
        }

        onResize { width, height ->
            viewport.update(width, height, this, true)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            batch.use(camera.viewProjection) { batch ->
                Fonts.default.draw(batch, gestureStatus, 100f, 450f)
                Fonts.default.draw(batch, inputStatus, 100f, 250f)
            }

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}