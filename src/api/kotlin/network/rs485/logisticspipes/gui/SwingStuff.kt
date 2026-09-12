package network.rs485.logisticspipes.gui

import java.awt.Container
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

interface WidgetRenderer<T : Any> {
    fun render(componentContainer: ComponentContainer): T
}

object SwingRenderer : WidgetRenderer<JPanel> {
    fun Container.addContainer(container: ComponentContainer) {
        container.children.forEach { child ->
            when (child) {
                is PropertyLabel<*, *> -> JLabel().apply {
                    text = child.text
                    child.onPropertyUpdate { newText ->
                        text = newText
                    }
                }

                is PropertyButton<*, *> -> JButton().apply {
                    text = child.text
                    addActionListener { child.action.invoke() }
                    child.onPropertyUpdate { newText ->
                        text = newText
                    }
                }

                is Label -> JLabel().apply {
                    text = child.text
                }

                is Button -> JButton().apply {
                    text = child.text
                    addActionListener { child.action.invoke() }
                }

                is ComponentContainer -> JPanel().apply {
                    addContainer(child)
                }

                else -> null
            }?.also(::add)
        }
    }

    override fun render(componentContainer: ComponentContainer): JPanel {
        return JPanel().apply {
            addContainer(componentContainer)
        }
    }
}

//fun main() {
//    SwingUtilities.invokeAndWait {
//        val frame = JFrame()
//        val gui = ProviderGui()
//        frame.add(SwingRenderer.render(gui.widgets))
//        frame.pack()
//        frame.isVisible = true
//    }
//}
