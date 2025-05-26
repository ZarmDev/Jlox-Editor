package com.example;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class GUIutils {
	public static void fadeInOutAnimation(Node node) {
		// Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.7), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Fade out
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.7), node);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        // How long the text stays visible before fading out
        fadeOut.setDelay(Duration.seconds(1.2));

        // Chain the transitions
        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
	}
}
