package com.bumble.appyx.transitionmodel.backstack.operation

import com.bumble.appyx.interactions.Parcelize
import com.bumble.appyx.interactions.core.BaseOperation
import com.bumble.appyx.transitionmodel.backstack.BackStack
import com.bumble.appyx.transitionmodel.backstack.BackStackModel.State

/**
 * Operation:
 *
 * [A, B, C] + Pop = [A, B]
 */
@Parcelize
class Pop<NavTarget : Any> : BaseOperation<State<NavTarget>>() {
    override fun isApplicable(state: State<NavTarget>): Boolean =
        state.stashed.isNotEmpty()

    override fun createFromState(baseLineState: State<NavTarget>): State<NavTarget> =
        baseLineState


    override fun createTargetState(fromState: State<NavTarget>): State<NavTarget> =
        fromState.copy(
            active = fromState.stashed.first(),
            destroyed = fromState.destroyed + fromState.active,
            stashed = fromState.stashed.subList(1, fromState.stashed.size)
        )

    override fun equals(other: Any?): Boolean = this.javaClass == other?.javaClass

    override fun hashCode(): Int = this.javaClass.hashCode()
}

fun <NavTarget : Any> BackStack<NavTarget>.pop() {
    operation(Pop())
}
