package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.bool
import io.github.sds100.keymapper.util.resourceId
import kotlinx.android.synthetic.main.expandable_card.view.*

/**
 * Created by sds100 on 24/05/2019.
 */

class ExpandableCardView(
        context: Context?,
        attrs: AttributeSet
) : MaterialCardView(context, attrs) {

    var expanded = false
        set(value) {
            field = value

            buttonExpand.expanded = expanded
        }

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.expandable_card, this)

        context?.resourceId(
                attrs,
                R.styleable.ExpandableCardView,
                R.styleable.ExpandableCardView_expandedLayout
        )?.let { expandedLayoutId ->
            inflater.inflate(expandedLayoutId, layoutExpanded)

        }

        context?.resourceId(
                attrs,
                R.styleable.ExpandableCardView,
                R.styleable.ExpandableCardView_collapsedLayout
        )?.let { collapsedLayoutId ->
            inflater.inflate(collapsedLayoutId, layoutCollapsed)
        }

        expanded = context!!.bool(
                attrs,
                R.styleable.ExpandableCardView,
                R.styleable.ExpandableCardView_layoutExpanded,
                defaultValue = false
        )

        buttonExpand.onExpandClick = {
            layoutCollapsed.isVisible = false

            expandableLayout.expand()
            true
        }

        buttonExpand.onCollapseClick = {
            expandableLayout.collapse()

            val transition = Fade()
            TransitionManager.beginDelayedTransition(layoutCollapsed, transition)
            layoutCollapsed.isVisible = true
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        buttonExpand.expanded = expanded
    }
}