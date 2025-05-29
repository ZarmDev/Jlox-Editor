package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Scanner;

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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import one.jpro.platform.mdfx.MarkdownView;

public class App extends Application {
    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    private final double width = screenBounds.getWidth();
    private final double height = screenBounds.getHeight() - 100;
    private StackPane root = new StackPane();
    private VBox mainScreen = new VBox(10);
    private String currentFilePath = null;
    private FileWriter fw;
    private TextArea textArea = new TextArea();
    // TODO: Why is filename.txt here?
    private File readTextArea = new File("filename.txt");
    private FileChooser fileChooser = new FileChooser();
    private TextField filterField = new TextField();
    // Reuse this for different prompts
    private TextInputDialog userPrompt = new TextInputDialog();
    private Stage primaryStage;
    private Text alert = new Text();
    // private Text aiResults = new Text();
    MarkdownView aiResults = new MarkdownView();
    ScrollPane scrollPane = new ScrollPane();

    public App() {
        // aiResults.setWrappingWidth(width - 100);
    }

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
    
    public void runCode() {
    	if (currentFilePath == null) {
    		alertUser("Please save the file first!");
    		return;
    	}
    	System.out.println(currentFilePath);
    	try {
    	// TODO: May be possible to optimize and reuse getRuntime?
    	Process compile = Runtime.getRuntime().exec("javac " + currentFilePath);
    	BufferedReader reader = new BufferedReader(new InputStreamReader(compile.getInputStream()));
    	String line;
    	while ((line = reader.readLine()) != null) {
    		System.out.println(line);
    		updateMarkdown(line);
//    		GUIutils.fadeInOutAnimation(aiResults, 0.2);
    	}
    	int exitCode = compile.waitFor();
    	if (exitCode == 0) {
        	Process run = Runtime.getRuntime().exec("java " + currentFilePath);
        	BufferedReader reader2 = new BufferedReader(new InputStreamReader(run.getInputStream()));
        	while ((line = reader2.readLine()) != null) {
        		System.out.println(line);
        		updateMarkdown("Result: \\n" + line);
        	}
    	} else {
    		System.out.println("Exited with error code : " + exitCode);
        	updateMarkdown(line + "\nExited with error code : " + exitCode);
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
                    StringBuilder fileContent = new StringBuilder();
                    while (myReader.hasNextLine()) {
                        String data = myReader.nextLine();
                        fileContent.append(data).append("\n");
                    }
                    textArea.setText(fileContent.toString());
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
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.S) {
                System.out.println("Ctrl+S pressed!");
                saveToFile();
                event.consume(); // Prevent default behavior
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.F) {
                System.out.println("Ctrl+F pressed!" + filterField.isManaged());
                boolean shouldShow = !filterField.isVisible();
                filterField.setVisible(shouldShow);
                filterField.setManaged(shouldShow);
                event.consume(); // Prevent default behavior
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.Q) {
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
                event.consume(); // Prevent default behavior
            } else if (event.getCode() == javafx.scene.input.KeyCode.F5) {
                javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                	@Override
                    protected Void call() throws Exception {
                        runCode();
                        return null;
                    }
                };
                new Thread(task).start();
                event.consume(); // Prevent default behavior
            }
        });
        // Add event listener for filterField
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            String content = textArea.getText();
            // If the textArea is empty then don't select anything (deselect in case any
            // text is already selected)
            if (newVal.isEmpty()) {
                textArea.deselect();
                return;
            }
            // Otherwise, look for the value in the filterField
            int index = content.indexOf(newVal);
            // If the index exists, then use selectRange to select it
            if (index >= 0) {
                textArea.selectRange(index, index + newVal.length());
            } else {
                // Otherwise, ensure nothing is selected
                textArea.deselect();
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
    	        scrollPane = new ScrollPane(aiResults);
    	        mainScreen.getChildren().add(scrollPane); // Add new MarkdownView at the top
    	    }
    	});
    }
    
    public void initialization() {
        // Set the title of the window
        primaryStage.setTitle("Jlox Code Editor");
        userPrompt.setTitle("Input Required");
        scrollPane.setFitToWidth(true); // Makes sure it fits the width
        scrollPane.setPannable(true); // Allows panning with the mouse
        textArea.setPrefHeight(500); // Set preferred height in pixels
        textArea.setMinHeight(400); // Minimum height
        
        // ## FILTER CODE ##
        filterField.setVisible(false);
        filterField.setManaged(false);

        // ## SET DEFAULT VALUES ##
        filterField.setPromptText("Type to filter code...");
        textArea.setText("public class Main {\r\n" + //
                "\tpublic static void main(String[] args) {\r\n" + //
                "\r\n" + //
                "\t}\r\n" + //
                "}\r\n" + //
                "");
        
        // ## ADD ALL CHILDREN TO THE SCENE ##
        mainScreen.getChildren().add(alert);
        
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
        primaryStage.setScene(new Scene(root, width, height));

        // ## EVENT HANDLERS ##
        initalizeEventHandlers();
        primaryStage.show();
    }
}
