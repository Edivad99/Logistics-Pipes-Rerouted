package network.rs485.logisticspipes.gui

import logisticspipes.api.gui.Margin
import logisticspipes.api.gui.HorizontalAlignment
import logisticspipes.api.gui.VerticalAlignment
import logisticspipes.api.gui.Size

import logisticspipes.api.property.Property
import logisticspipes.api.property.layer.PropertyLayer

@DslMarker
annotation class GuiComponentMarker

@GuiComponentMarker
abstract class GuiComponent {
    var width: Int = -1
    var height: Int = -1
    val children = arrayListOf<GuiComponent>()
    var margin: Margin = Margin.NONE
    var horizontalAlignment: HorizontalAlignment = HorizontalAlignment.LEFT
    var verticalAlignment: VerticalAlignment = VerticalAlignment.TOP
    var horizontalSize: Size = Size.GROW
    var verticalSize: Size = Size.GROW
    var enabled: Boolean = true

    open fun <T : GuiComponent> initComponent(component: T, init: T.() -> Unit): T {
        component.apply(init)
        children.add(component)
        return component
    }
}


abstract class ComponentContainer : GuiComponent() {

    var gap: Int = 1

    /**
     * Arranges contained widgets left-to-right horizontally
     */
    fun horizontal(init: HContainer.() -> Unit) = initComponent(HContainer(), init)

    /**
     * Arranges contained widgets top-to-bottom vertically
     */
    fun vertical(init: VContainer.() -> Unit) = initComponent(VContainer(), init)

    fun optionalComponent(init: OptionalComponent.() -> Unit) = initComponent(OptionalComponent(), init)

    /**
     * Represents a label with the text value based on a property.
     */
    fun <V : Any, P : Property<V>> label(init: PropertyLabel<V, P>.() -> Unit) = initComponent(PropertyLabel(), init)

    /**
     * Represents a button with the text value based on a property.
     */
    fun <V : Any, P : Property<V>> propertyButton(init: PropertyButton<V, P>.() -> Unit) =
        initComponent(PropertyButton(), init)

    /**
     * Represents a static button.
     */
    fun button(init: Button.() -> Unit) = initComponent(Button(), init)

    /**
     * Represents a static piece of text.
     */
    fun staticLabel(init: Label.() -> Unit) = initComponent(Label(), init)
}

/**
 * Used to construct a base container component.
 */
fun widgetContainer(init: VContainer.() -> Unit): VContainer =
    VContainer().apply(init)

class HContainer : ComponentContainer() {
    var alignment: HorizontalAlignment = HorizontalAlignment.LEFT
}

class VContainer : ComponentContainer() {
    var alignment: VerticalAlignment = VerticalAlignment.TOP
}

/**
 * Adds component to hierarchy if the predicate returns true.
 */
class OptionalComponent : ComponentContainer() {
    var predicate: () -> Boolean = { false }
    var vertical: Boolean = true
    private var addComponents: Boolean = false

    override fun <T : GuiComponent> initComponent(component: T, init: T.() -> Unit): T {
        if (addComponents) {
            component.apply(init)
            children.add(component)
        }
        return component
    }

    fun activeComponents(init: OptionalComponent.() -> Unit): OptionalComponent {
        if (predicate.invoke()) {
            addComponents = true
        }
        init(this)
        addComponents = false
        return this
    }

    fun inactiveComponents(init: OptionalComponent.() -> Unit): OptionalComponent {
        if (!predicate.invoke()) {
            addComponents = true
        }
        init(this)
        addComponents = false
        return this
    }
}

open class Label : GuiComponent() {
    var text: String = ""
    var textAlignment: HorizontalAlignment = HorizontalAlignment.LEFT
    var textColor: Int = 0
    var extendable: Boolean = false
    var backgroundColor: Int = 0
}

interface PropertyAware {
    fun onPropertyUpdate(callback: (String) -> Unit)
}

class PropertyLabel<V : Any, P : Property<V>> : Label(), PropertyAware {
    lateinit var propertyLayer: PropertyLayer
    lateinit var property: P
    var propertyToText: (V) -> String = Any::toString

    override fun onPropertyUpdate(callback: (String) -> Unit) {
        propertyLayer.addObserver(property) {
            callback.invoke(propertyToText.invoke(it.copyValue()))
        }
    }
}

open class Button : GuiComponent() {
    var text: String = ""
    var action: () -> Unit = {}
}

class PropertyButton<V : Any, P : Property<V>> : Button(), PropertyAware {
    lateinit var propertyLayer: PropertyLayer
    lateinit var property: P
    var propertyToText: (V) -> String = Any::toString

    override fun onPropertyUpdate(callback: (String) -> Unit) {
        propertyLayer.addObserver(property) {
            callback.invoke(propertyToText.invoke(it.copyValue()))
        }
    }
}
