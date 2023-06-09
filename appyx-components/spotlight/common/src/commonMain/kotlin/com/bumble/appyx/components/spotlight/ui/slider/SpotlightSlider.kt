package com.bumble.appyx.components.spotlight.ui.slider

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.bumble.appyx.components.spotlight.SpotlightModel.State
import com.bumble.appyx.components.spotlight.SpotlightModel.State.ElementState.CREATED
import com.bumble.appyx.components.spotlight.SpotlightModel.State.ElementState.DESTROYED
import com.bumble.appyx.components.spotlight.SpotlightModel.State.ElementState.STANDARD
import com.bumble.appyx.components.spotlight.operation.Next
import com.bumble.appyx.components.spotlight.operation.Previous
import com.bumble.appyx.interactions.core.ui.context.TransitionBounds
import com.bumble.appyx.interactions.core.ui.context.UiContext
import com.bumble.appyx.interactions.core.ui.gesture.Drag
import com.bumble.appyx.interactions.core.ui.gesture.Gesture
import com.bumble.appyx.interactions.core.ui.gesture.GestureFactory
import com.bumble.appyx.interactions.core.ui.gesture.dragHorizontalDirection
import com.bumble.appyx.interactions.core.ui.gesture.dragVerticalDirection
import com.bumble.appyx.interactions.core.ui.property.impl.Alpha
import com.bumble.appyx.interactions.core.ui.property.impl.GenericFloatProperty
import com.bumble.appyx.interactions.core.ui.property.impl.Position
import com.bumble.appyx.interactions.core.ui.property.impl.Scale
import com.bumble.appyx.interactions.core.ui.state.MatchedTargetUiState
import com.bumble.appyx.transitionmodel.BaseMotionController

class SpotlightSlider<InteractionTarget : Any>(
    uiContext: UiContext,
    private val orientation: Orientation = Orientation.Horizontal, // TODO support RTL
) : BaseMotionController<InteractionTarget, State<InteractionTarget>, MutableUiState, TargetUiState>(
    uiContext = uiContext
) {
    private val width: Dp = uiContext.transitionBounds.widthDp
    private val height: Dp = uiContext.transitionBounds.heightDp
    private val scrollX = GenericFloatProperty(
        uiContext,
        GenericFloatProperty.Target(0f)
    ) // TODO sync this with the model's initial value rather than assuming 0
    override val viewpointDimensions: List<Pair<(State<InteractionTarget>) -> Float, GenericFloatProperty>> =
        listOf(
            { state: State<InteractionTarget> -> state.activeIndex } to scrollX
        )

    private val created: TargetUiState = TargetUiState(
        position = Position.Target(DpOffset(0.dp, width)),
        scale = Scale.Target(0f),
        alpha = Alpha.Target(1f),
    )

    private val standard: TargetUiState = TargetUiState(
        position = Position.Target(DpOffset.Zero),
        scale = Scale.Target(1f),
        alpha = Alpha.Target(1f),
    )

    private val destroyed: TargetUiState = TargetUiState(
        position = Position.Target(DpOffset(x = 0.dp, y = -height)),
        scale = Scale.Target(0f),
        alpha = Alpha.Target(0f),
    )

    override fun State<InteractionTarget>.toUiTargets(): List<MatchedTargetUiState<InteractionTarget, TargetUiState>> {
        return positions.flatMapIndexed { index, position ->
            position.elements.map {
                MatchedTargetUiState(
                    element = it.key,
                    targetUiState = TargetUiState(
                        base = when (it.value) {
                            CREATED -> created
                            STANDARD -> standard
                            DESTROYED -> destroyed
                        },
                        positionInList = index,
                        elementWidth = width
                    )
                )
            }
        }
    }

    override fun mutableUiStateFor(
        uiContext: UiContext,
        targetUiState: TargetUiState
    ): MutableUiState =
        targetUiState.toMutableState(uiContext, scrollX.renderValueFlow, width)


    class Gestures<InteractionTarget>(
        transitionBounds: TransitionBounds,
        private val orientation: Orientation = Orientation.Horizontal,
        private val reverseOrientation: Boolean = false,
    ) : GestureFactory<InteractionTarget, State<InteractionTarget>> {
        private val width = transitionBounds.widthPx.toFloat()
        private val height = transitionBounds.heightPx.toFloat()

        override fun createGesture(
            state: State<InteractionTarget>,
            delta: Offset,
            density: Density
        ): Gesture<InteractionTarget, State<InteractionTarget>> = when (orientation) {
            Orientation.Horizontal -> {
                when (dragHorizontalDirection(delta)) {
                    Drag.HorizontalDirection.LEFT -> Gesture(
                        operation = if (reverseOrientation) Previous() else Next(),
                        completeAt = Offset(-width, 0f)
                    )

                    else -> Gesture(
                        operation = if (reverseOrientation) Next() else Previous(),
                        completeAt = Offset(width, 0f)
                    )
                }
            }

            Orientation.Vertical -> {
                when (dragVerticalDirection(delta)) {
                    Drag.VerticalDirection.UP -> Gesture(
                        operation = if (reverseOrientation) Previous() else Next(),
                        completeAt = Offset(0f, -height)
                    )

                    else ->
                        Gesture(
                            operation = if (reverseOrientation) Next() else Previous(),
                            completeAt = Offset(0f, height)
                        )
                }
            }
        }
    }
}

