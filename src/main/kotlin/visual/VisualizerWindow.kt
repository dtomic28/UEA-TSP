package com.dtomic.visual

import javax.swing.JFrame

class VisualizerWindow(title: String, panel: TspVisualizer) : JFrame(title) {

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        contentPane = panel
        setSize(900, 700)
        setLocationRelativeTo(null)
        isVisible = true
    }
}
