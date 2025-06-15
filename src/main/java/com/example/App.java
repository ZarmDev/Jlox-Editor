package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Scanner;

import org.fxmisc.richtext.CodeArea;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import one.jpro.platform.mdfx.MarkdownView;

public class App extends Application {
    private final Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    private final double width = screenBounds.getWidth();
    private final double height = screenBounds.getHeight() - 100;
    private StackPane root = new StackPane();
    private VBox mainScreen = new VBox(10);
    private String currentFilePath = null;
    // TODO: Why is filename.txt here?
    private File readTextArea = new File("filename.txt");
    private FileChooser fileChooser = new FileChooser();
    private TextField filterField = new TextField();
    // Reuse this for different prompts
    private TextInputDialog userPrompt = new TextInputDialog();
    private Stage primaryStage;
    private Text alert = new Text();
    private MarkdownView aiResults = new MarkdownView();
    private ScrollPane scrollPane = new ScrollPane();
    private CodeEditor textArea = new CodeEditor();
    private double windowZoom = 1;
    // Reuse the Scale object rather than creating a new one everytime
    private Scale textAreaScale = new Scale(1, 1, 0, 0);

    public static void main(String[] args) {
        launch(args);
    }

    public void alertUser(String text) {
        alert.setText(text);
        
        GUIutils.fadeInOutAnimation(alert, 1);
    }

    // TODO: Document this
    public void saveToFile() {
        if (currentFilePath != null) {
        	// Unfortunately, you can not reuse a FileWriter object. You must recreate it everytime
            try (FileWriter fw = new FileWriter(currentFilePath)) {
                fw.write(textArea.getText());
                alertUser("File saved!");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            Text tip = new Text("Please enter the file name here:");
            Button chooseFolderBtn = new Button("Choose a folder to place the file");
            TextField inputField = new TextField();
            final File[] selectedDir = new File[1];
            HBox content = new HBox(10, tip, inputField, chooseFolderBtn);

            chooseFolderBtn.setOnAction(e -> {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select Folder");
                File dir = directoryChooser.showDialog(primaryStage);
                if (dir != null) {
                    selectedDir[0] = dir;
                }
            });

            // Use createDialog and pass a result extractor that returns the full file path
            String filePath = GUIutils.createDialog(content, box -> {
                String fileName = "";
                File dir = selectedDir[0];
                // Loop through the box children and get the text of the TextField (if it exists)
                for (javafx.scene.Node node : box.getChildren()) {
                    if (node instanceof TextField) {
                        fileName = ((TextField) node).getText();
                    }
                }
                if (dir != null && !fileName.isEmpty()) {
                    return dir.getAbsolutePath() + File.separator + fileName;
                }
                return null;
            });

            if (filePath != null) {
                File file = new File(filePath);
                if (!file.exists()) {
                    try (FileWriter fw = new FileWriter(file)) {
                        fw.write(textArea.getText());
                        alertUser("File saved!");
                        currentFilePath = filePath;
                    } catch (IOException err) {
                        err.printStackTrace();
                    }
                } else {
                    alertUser("File already exists!");
                }
            } else {
                alertUser("Please select a folder and enter a file name.");
            }
        }
    }

    public Void callAI() throws Exception {
        	// "https://openrouter.ai/api/v1/chat/completions"
        	String url = "https://ai.hackclub.com/chat/completions";
        	// "meta-llama/llama-3.3-8b-instruct:free"
        	String model = "llama-3.3-70b-versatile";
            String callResult = API.askAI(url, model,
                    "Find the error in the user's code", textArea.getText());
            if (callResult == null) {
            	alertUser("No API Key provided!");
            } else if (callResult.equals("")) {
            	alertUser("Failed to connect to the chatGPT API");
            } else {
            	updateMarkdown(callResult);
            }
            return null;
//          @Override
//          protected Void call() throws Exception {
//          	String callResult = API.askAI("https://openrouter.ai/api/v1/chat/completions", "meta-llama/llama-3.3-8b-instruct:free",
//                      "Find the error in the user's code", textArea.getText());
//              if (callResult == null) {
//              	alertUser("No API Key provided!");
//              } else if (callResult.equals("")) {
//              	alertUser("Failed to connect to the chatGPT API");
//              } else {
//              	updateMarkdown(callResult);
//              }
//              return null;
//          }
    }
    
    private String readStream(java.io.InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n\n");
        }
        return output.toString();
    }
    
    public void runCode() {
    	if (currentFilePath == null) {
    		alertUser("Please save the file first!");
    		return;
    	}
    	System.out.println(currentFilePath);
    	try {
    		updateMarkdown("Compiling...");
    		saveToFile();
    		// Taken from https://www.geeksforgeeks.org/measure-time-taken-function-java/
    		long startTime = System.nanoTime();
            Process compile = Runtime.getRuntime().exec("javac " + currentFilePath);
            String compileErrors = readStream(compile.getErrorStream());
            int compileExit = compile.waitFor();

            if (compileExit != 0) {
                updateMarkdown("Compilation failed:\n\n" + compileErrors);
                return;
            }

            // Remove .java extension for running
//            String className = new File(currentFilePath).getName().replaceFirst("[.][^.]+$", "");
            Process run = Runtime.getRuntime().exec("java " + currentFilePath);
            String runOutput = readStream(run.getInputStream());
            System.out.println(runOutput);
            String runErrors = readStream(run.getErrorStream());
            // Wait for the process so execution time is correct (and it runs smoothly)
            int runExit = run.waitFor();

            // Get current nano second time
            long endTime = System.nanoTime();
            // Get different between now and the startTime and divide by this number to get the milliseconds
            long executionTime = (endTime - startTime) / 1000000;
            
            // runExit returns an exit code from the command and 0 means that it ran without error.
            if (runExit != 0) {
                updateMarkdown("Runtime error (executed in " + executionTime + " ms):\n\n" + runErrors);
            } else {
                updateMarkdown("Result (executed in " + executionTime + " ms):\n\n" + runOutput);
            }
    	} catch (Exception e) {
    		alertUser("Failed to run code.");
    	}
    }
    
    public void initializeToolbarButtons() {
        // Create MenuBar
        MenuBar menuBar = new MenuBar();

        // --- File Menu ---
        Menu fileMenu = new Menu("File");

        MenuItem openItem = new MenuItem("Open new file");
        openItem.setOnAction(e -> {
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                currentFilePath = selectedFile.getAbsolutePath();
                try {
                    readTextArea = new File(currentFilePath);
                    Scanner myReader = new Scanner(readTextArea);
                    // Similar to reading a network request, or the System.in (input), you can read files using scanners
                    StringBuilder fileContent = new StringBuilder();
                    while (myReader.hasNextLine()) {
                        String data = myReader.nextLine();
                        // Append all the lines with a \n into the editor. There should be many other ways to do this.
                        fileContent.append(data).append("\n");
                    }
                    textArea.replaceText(fileContent.toString());
                    myReader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        MenuItem saveItem = new MenuItem("Save to file (CTRL+S)");
        saveItem.setOnAction(e -> saveToFile());

        MenuItem findItem = new MenuItem("Find in file (CTRL+F)");
        findItem.setOnAction(e -> {
            boolean shouldShow = !filterField.isVisible();
            filterField.setVisible(shouldShow);
            filterField.setManaged(shouldShow);
        });

        fileMenu.getItems().addAll(openItem, saveItem, findItem);

        // ## AI Menu ##
        Menu aiMenu = new Menu("AI");
        MenuItem aiAskItem = new MenuItem("Ask AI about your code (CTRL+Q)");
        aiAskItem.setOnAction(e -> {
            // Run askAI in a background thread to avoid freezing the UI (thread code from
            // online)
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
            	@Override
                protected Void call() throws Exception {
                    callAI();
                    return null;
                }
            };
            new Thread(task).start();
        });
        aiMenu.getItems().add(aiAskItem);
        
        // ## AI Menu ##
        Menu runMenu = new Menu("Run");
        MenuItem runBtn = new MenuItem("Run your code (F5)");
        runBtn.setOnAction(e -> {
        	// Use a thread to prevent the function from delaying the re-rendering of GUI
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                	runCode();
                	return null;
                }
            };
            new Thread(task).start();
        });
        runMenu.getItems().add(runBtn);

        // Add menus to the menu bar
        menuBar.getMenus().addAll(fileMenu, aiMenu, runMenu);

        // Add menuBar to your mainScreen at the top
        mainScreen.getChildren().add(0, menuBar);
    }

    public void initalizeEventHandlers() {
        primaryStage.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
        	if (event.isControlDown()) {
	            if (event.getCode() == javafx.scene.input.KeyCode.S) {
	                System.out.println("Ctrl+S pressed!");
	                saveToFile();
	            } else if (event.getCode() == javafx.scene.input.KeyCode.F) {
	                System.out.println("Ctrl+F pressed!" + filterField.isManaged());
	                boolean shouldShow = !filterField.isVisible();
	                filterField.setVisible(shouldShow);
	                filterField.setManaged(shouldShow);
	            } else if (event.getCode() == javafx.scene.input.KeyCode.Q) {
	            	// Run askAI in a background thread to avoid freezing the UI (thread code from
	                // online)
	                javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
	                	@Override
	                    protected Void call() throws Exception {
	                        callAI();
	                        return null;
	                    }
	                };
	                new Thread(task).start();
	            } else if (event.getCode() == javafx.scene.input.KeyCode.EQUALS) {
	                windowZoom *= 1.1;
	                textAreaScale.setX(windowZoom);
	                textAreaScale.setY(windowZoom);
	            } else if (event.getCode() == javafx.scene.input.KeyCode.MINUS) {
	                windowZoom *= 0.9;
	                textAreaScale.setX(windowZoom);
	                textAreaScale.setY(windowZoom);
	            }
        	} else if (event.getCode() == javafx.scene.input.KeyCode.F5) {
                javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                	@Override
                    protected Void call() throws Exception {
                        runCode();
                        return null;
                    }
                };
                new Thread(task).start();
            }
//        	event.consume(); // Prevent default behavior
        });
        
        // Add event listener for filterField
        // obs: ObservableValue<? extends String> (observes value like a listener in Javascript), oldVal: previous text, newVal: new text, 
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            String content = textArea.getText();
            // If the textArea is empty then don't select anything (deselect in case any
            // text is already being filtered/selected)
            if (newVal.isEmpty()) {
                textArea.codeArea.deselect();
                return;
            }
            // Otherwise, look for the value in the filterField
            int index = content.indexOf(newVal);
            // If the index exists, then use selectRange to select it
            if (index >= 0) {
            	// Select the range based on the index found
                textArea.codeArea.selectRange(index, index + newVal.length());
            } else {
                // Otherwise, ensure nothing is selected
                textArea.codeArea.deselect();
            }
        });
    }

    private void updateMarkdown(String newMarkdown) {
    	// https://stackoverflow.com/questions/21083945/how-to-avoid-not-on-fx-application-thread-currentthread-javafx-application-th
    	Platform.runLater(new Runnable() {
    	    @Override
    	    public void run() {
    	    	mainScreen.getChildren().remove(scrollPane); // Remove old MarkdownView
    	        aiResults = new MarkdownView(newMarkdown); // Create new instance
    	        // Add padding to make the text look better
    	        aiResults.setStyle("-fx-padding: 16;");
    	        scrollPane = new ScrollPane(aiResults);
    	        mainScreen.getChildren().add(scrollPane); // Add new MarkdownView at the top
    	    }
    	});
    }
    
    public void initialization() {
        // ## SET DEFAULT VALUES ##
        primaryStage.setTitle("Jlox Code Editor");
        userPrompt.setTitle("Input Required");
        
        scrollPane.setFitToWidth(true); // Makes sure it fits the width
        scrollPane.setPannable(true); // Allows panning with the mouse
        textArea.setPrefHeight(500); // Set preferred height in pixels
        textArea.setMinHeight(400); // Minimum height

        filterField.setVisible(false);
        filterField.setManaged(false);
        filterField.setPromptText("Type to filter code...");
        
        textArea.replaceText("public class Main {\r\n" + //
                "\tpublic static void main(String[] args) {\r\n" + //
                "\r\n" + //
                "\t}\r\n" + //
                "}\r\n" + //
                "");
        
        textArea.setPrefHeight(500);
        textArea.setMinHeight(400);
        textArea.setPrefWidth(width); // Optionally set width
        textArea.getTransforms().add(textAreaScale);
        
        // ## ADD CLASSES ##
//        textArea.getStyleClass().add("textArea");
        
        // ## ADD ALL CHILDREN TO THE SCENE ##
        HBox alertBox = new HBox(alert);
        // 8 pixels of padding on the left
		alertBox.setStyle("-fx-padding: 0 0 0 8;");
		mainScreen.getChildren().add(alertBox);
        // ## INITALIZE TOOLBAR ##
        initializeToolbarButtons();
        
        mainScreen.getChildren().add(filterField);
        mainScreen.getChildren().add(textArea);
        mainScreen.getChildren().add(aiResults);
    }
    
    @Override
    public void start(Stage primaryStage) {
    	// Ensure all functions can use this
        this.primaryStage = primaryStage;
    	// ## INITIALIZATION ##
        initialization();

        // Add the mainScreen and set a new scene
        root.getChildren().add(mainScreen);
        
        Scene scene = new Scene(root, width, height);
        // ## ADD CSS ##
        scene.getStylesheets().add(CodeEditor.class.getResource("java-keywords.css").toExternalForm());
        // TODO: ADD CSS
        scene.getStylesheets().add(CodeEditor.class.getResource("main.css").toExternalForm());
        primaryStage.setScene(scene);

        // ## EVENT HANDLERS ##
        initalizeEventHandlers();
        primaryStage.show();
    }
}
