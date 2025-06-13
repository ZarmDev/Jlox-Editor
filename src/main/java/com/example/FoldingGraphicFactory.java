package com.example;

import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.fxmisc.richtext.CodeArea;
import java.util.function.IntFunction;

public class FoldingGraphicFactory implements IntFunction<javafx.scene.Node> {
    private final CodeArea codeArea;

    public FoldingGraphicFactory(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

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

    @Override
    public javafx.scene.Node apply(int line) {
        String text = codeArea.getParagraph(line).getText();
        boolean isFoldable = text.matches("^\\s*(class\\s+\\w+|.*\\(.*\\)\\s*\\{)");

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
                    codeArea.foldParagraphs(line, findMatchingLine(line));
                    arrow.setText("▼");
                }
            });
        } else {
            arrow.setMouseTransparent(true);
        }

        Label lFold = new Label(String.valueOf(line + 1));

        HBox box = new HBox(0, arrow, lFold);
        box.setStyle("-fx-padding: 0; -fx-background-color: transparent;");
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
    }

}