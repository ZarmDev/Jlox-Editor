package com.example;

import java.util.Optional;
import java.util.function.Function;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class GUIutils {
	public static void fadeInOutAnimation(Node node, double duration) {
		// Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(duration), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Fade out
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(duration), node);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        // How long the text stays visible before fading out
        fadeOut.setDelay(Duration.seconds(1.2));

        // Chain the transitions
        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
	}
	
	public static String createDialog(HBox content, Function<HBox, String> resultExtractor) {
		// Create a custom dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Alert");
        
        dialog.getDialogPane().setContent(content);
        
		ButtonType customButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(customButtonType, ButtonType.CANCEL);
        
        // The return value of the function below is passed to the ifPresent function and it basically does something when the dialog is over
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == customButtonType) {
            	return resultExtractor.apply(content);
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            System.out.println("Result: " + input);
        });
        
        // Because it is optional, first ensure that the result is present and not empty
        return result.orElse(null);
	}
}
