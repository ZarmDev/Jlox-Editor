// THIS ENTIRE FILE WAS ADAPTED AND CHANGED FROM https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsDemo.java

package com.example;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.collection.ListModification;
import org.reactfx.Subscription;

// Extend as a Region because it's an area
public class CodeEditor extends Region {
	private final VirtualizedScrollPane<CodeArea> vsPane;
	public final CodeArea codeArea;

	// keywords to be highlighted
    private static final String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    // patterns/regexes used to find certain things like where brackets are, parentheses, etc
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"   // for whole text processing (text blocks)
    		                          + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)";  // for visible paragraph processing (line by line)

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<PAREN>" + PAREN_PATTERN + ")"
            + "|(?<BRACE>" + BRACE_PATTERN + ")"
            + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
            + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    private static final String sampleCode = String.join("\n", new String[] {
        "package com.example;",
        "",
        "import java.util.*;",
        "",
        "public class Foo extends Bar implements Baz {",
        "",
        "    /*",
        "     * multi-line comment",
        "     */",
        "    public static void main(String[] args) {",
        "        // single-line comment",
        "        for(String arg: args) {",
        "            if(arg.length() != 0)",
        "                System.out.println(arg);",
        "            else",
        "                System.err.println(\"Warning: empty string as argument\");",
        "        }",
        "    }",
        "",
        "}"
    });
    
    // Constructor - Adds graphic factories (classes that add styles to the editor) and adds some functionality like adding appropriate whitespace when you press enter
    public CodeEditor() {
    	codeArea = new CodeArea();
    	// Use a scroll pane to make the codeArea scrollable and have styles like padding
    	vsPane = new VirtualizedScrollPane<>(codeArea);
    	// Because this is a node you can call getChildren on the Region and add the scroll pane to contain the code editor
    	getChildren().add(vsPane);
    	
        // add line numbers to the left of area
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        // add feature to fold classes/functions
        codeArea.setParagraphGraphicFactory(new FoldingGraphicFactory(codeArea));
        // set the context menu (menu appearing on right clicking code) to the custom class below
        codeArea.setContextMenu( new DefaultContextMenu() );
        // recompute syntax highlighting only for visible paragraph changes
        // Note that this shows how it can be done but is not recommended for production where multi-
        // line syntax requirements are needed, like comment blocks without a leading * on each line. 
        codeArea.getVisibleParagraphs().addModificationObserver
        (
            new VisibleParagraphStyler<>( codeArea, this::computeHighlighting )
        );

        // auto-indent: insert previous line's indents on enter
        final Pattern whiteSpace = Pattern.compile( "^\\s+" );
        codeArea.addEventHandler( KeyEvent.KEY_PRESSED, KE ->
        {
            if ( KE.getCode() == KeyCode.ENTER ) {
            	// Caret is the line that blinks when you type (position you are typing at)
            	int caretPosition = codeArea.getCaretPosition();
            	int currentParagraph = codeArea.getCurrentParagraph();
            	// Get the paragraph before the one we are editing and then get the first "segment"
            	String amountOfWhitespace = codeArea.getParagraph(currentParagraph-1).getSegments().get(0);
            	// A regex matcher meant to detect whitespace (any amount of spaces)
                Matcher m0 = whiteSpace.matcher(amountOfWhitespace);
                // If the regex is matched, that means we can get the whitespace detected and add it to the caretPosition
                if ( m0.find() ) Platform.runLater( () -> codeArea.insertText( caretPosition, m0.group() ) );
            }
        });

        
        codeArea.replaceText(0, 0, sampleCode);
    }
    
    // layoutChildren - Resizes code editor
    @Override
    protected void layoutChildren() {
        vsPane.resizeRelocate(0, 0, getWidth(), getHeight());
    }

    // computeHighlighting - Finds keywords in a given text and returns the styles
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        // Just a collection (array) with a fancy name (to improve performance)
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        // while the regex matcher finds a pattern in the text, continue adding the styles to a collection (list)
        while(matcher.find()) {
        	// If the group is found, aka the patterns above (example: private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";) is found
        	// Then, set the styleClass to the corresponding class in java-keywords.css and the style will automatically be applied by JavaFX
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    matcher.group("SEMICOLON") != null ? "semicolon" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);

        return spansBuilder.create();
    }

    // This is added in the constructor as a modification observer - basically, it runs the accept function when lm is changed (new lines/text) and then uses the compute hightlighting function and applys the styles 
    private class VisibleParagraphStyler<PS, SEG, S> implements Consumer<ListModification<? extends Paragraph<PS, SEG, S>>>
    {
        private final GenericStyledArea<PS, SEG, S> area;
        private final Function<String,StyleSpans<S>> computeStyles;
        private int prevParagraph, prevTextLength;

        public VisibleParagraphStyler( GenericStyledArea<PS, SEG, S> area, Function<String,StyleSpans<S>> computeStyles )
        {
            this.computeStyles = computeStyles;
            this.area = area;
        }

        @Override
        public void accept( ListModification<? extends Paragraph<PS, SEG, S>> lm )
        {
            if ( lm.getAddedSize() > 0 ) Platform.runLater( () ->
            {
                int paragraph = Math.min( area.firstVisibleParToAllParIndex() + lm.getFrom(), area.getParagraphs().size()-1 );
                String text = area.getText( paragraph, 0, paragraph, area.getParagraphLength( paragraph ) );

                if ( paragraph != prevParagraph || text.length() != prevTextLength )
                {
                    if ( paragraph < area.getParagraphs().size()-1 )
                    {
                        int startPos = area.getAbsolutePosition( paragraph, 0 );
                        // This is the line that actually applies styles to the text (using compute highlighting)
                        area.setStyleSpans( startPos, computeStyles.apply( text ) );
                    }
                    prevTextLength = text.length();
                    prevParagraph = paragraph;
                }
            });
        }
    }
    
    // When you right click, this menu appears
    private class DefaultContextMenu extends ContextMenu
    {
        private MenuItem fold, unfold;

        public DefaultContextMenu()
        {
            fold = new MenuItem( "Fold selected text" );
            fold.setOnAction( AE -> { hide(); fold(); } );

            unfold = new MenuItem( "Unfold from cursor" );
            unfold.setOnAction( AE -> { hide(); unfold(); } );

//            print = new MenuItem( "Print" );
//            print.setOnAction( AE -> { hide(); print(); } );

            getItems().addAll( fold, unfold );
        }

        /**
         * Folds multiple lines of selected text, only showing the first line and hiding the rest.
         */
        private void fold() {
            ((CodeArea) getOwnerNode()).foldSelectedParagraphs();
        }

        /**
         * Unfold the CURRENT line/paragraph if it has a fold.
         */
        private void unfold() {
            CodeArea area = (CodeArea) getOwnerNode();
            area.unfoldParagraphs( area.getCurrentParagraph() );
        }

        private void print() {
            System.out.println( ((CodeArea) getOwnerNode()).getText() );
        }
    }
    
    
    /* GETTERS AND SETTERS */
    public String getText() {
        return codeArea.getText();
    }

    public void replaceText(String text) {
        codeArea.replaceText(text);
    }
}

/*
// recompute the syntax highlighting for all text, 500 ms after user stops editing area
// Note that this shows how it can be done but is not recommended for production with
// large files as it does a full scan of ALL the text every time there is a change !
Subscription cleanupWhenNoLongerNeedIt = codeArea

        // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
        // multi plain changes = save computation by not rerunning the code multiple times
        //   when making multiple changes (e.g. renaming a method at multiple parts in file)
        .multiPlainChanges()

        // do not emit an event until 500 ms have passed since the last emission of previous stream
        .successionEnds(Duration.ofMillis(500))

        // run the following code block when previous stream emits an event
        .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));

// when no longer need syntax highlighting and wish to clean up memory leaks
// run: `cleanupWhenNoLongerNeedIt.unsubscribe();`
*/