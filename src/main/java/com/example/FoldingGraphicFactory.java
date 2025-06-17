package com.example;

import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.fxmisc.richtext.CodeArea;
import java.util.function.IntFunction;

//FoldingGraphicFactory - A graphic factory that is applied on each line to give automatic line folding detection
public class FoldingGraphicFactory implements IntFunction<javafx.scene.Node> {
    private final CodeArea codeArea;

    public FoldingGraphicFactory(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

    // findMatchingLine - Given a line, it will get the text and then find the next line that has the same amount of whitespace before it.
    public int findMatchingLine(int startLine) {
        String startText = codeArea.getParagraph(startLine).getText();
        int startIndent = startText.indexOf(startText.trim());
        int totalLines = codeArea.getParagraphs().size();

        for (int i = startLine + 1; i < totalLines; i++) {
            String lineText = codeArea.getParagraph(i).getText();
            if (lineText.trim().isEmpty()) continue;
            int indent = lineText.indexOf(lineText.trim());
            if (indent == startIndent) {
                return i;
            }
        }
        return -1;
    }

    // apply - This function is run for each line in the document and it uses a regex to check whether the text is a class or function. If so, it will add an arrow that can be clicked to fold the function/class.
    @Override
    public javafx.scene.Node apply(int line) {
        String text = codeArea.getParagraph(line).getText();
        // Taken from online - I don't really know how to use regexes
        boolean isFoldable = text.matches("^\\s*(class\\s+\\w+|.*\\(.*\\)\\s*\\{)");

        // Arrow either exists or doesn't if regex matches
        Label arrow = new Label(isFoldable ? "▶" : "");
        arrow.setMinWidth(12);
        arrow.setPrefWidth(12);
        arrow.setMaxWidth(12);
        if (isFoldable) {
            arrow.setOnMouseClicked((MouseEvent e) -> {
                if (codeArea.isFolded(line + 1)) {
                    codeArea.unfoldParagraphs(line);
                    arrow.setText("▶");
                } else {
                	// Fold paragraphs from current line to next line with same whitespace
                    codeArea.foldParagraphs(line, findMatchingLine(line));
                    arrow.setText("▼");
                }
            });
        } else {
        	// Mouse events do not affect the arrow
            arrow.setMouseTransparent(true);
        }

        // Put the expected line number after folding (makes it look a bit wierd but works)
        Label lFold = new Label(String.valueOf(line + 1));

        HBox box = new HBox(0, arrow, lFold);
        // Just in case it takes padding
        box.setStyle("-fx-padding: 0; -fx-background-color: transparent;");
        // Set it aligned to the line numbers
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
    }

}