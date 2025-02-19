package org.nf2t.javafx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue; // Import this!

/**
 * JavaFX App
 */
public class App extends Application {
	 @Override
	    public void start(Stage primaryStage) {
	        final var statusLabel = new Label("Drag and drop file here");	        
	        final ObservableList<File> files = FXCollections.observableArrayList();
	        	      
	        final var table = new TableView<File>();
	        table.setItems(files);
	        
	        final var root = new VBox();
	        root.setSpacing(5);
	        root.setPadding(new Insets(10, 0, 0, 10));
	        root.getChildren().addAll(statusLabel, table);
	        
	        table.setEditable(true);
	        	        
	        final var firstNameCol = new TableColumn<File, String>("File Name");
	        firstNameCol.setCellValueFactory((x) -> {
	        	final var value = x.getValue().getName();
	        	return new SimpleStringProperty(value);
	        	
	        });
	        final var lastNameCol = new TableColumn<File, String>("Absolute Path");
	        lastNameCol.setCellValueFactory((x) -> {
	        	final var value = x.getValue().getAbsolutePath();
	        	return new SimpleStringProperty(value);
	        });
	        final var emailCol = new TableColumn<File, String>("Parent");
	        emailCol.setCellValueFactory((x) -> {
	        	final var value = x.getValue().getParent();
	        	return new SimpleStringProperty(value);
	        });
	        final var sizeCol = new TableColumn<File, Number>("Size");
	        sizeCol.setCellValueFactory(x -> {
	            final var value = x.getValue();	         
	            
	            return new SimpleLongProperty(value == null? 0L: value.length());	         
	        });
	        
	        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol, sizeCol);
	        
	        var scene = new Scene(root, 400, 200);
	        primaryStage.setScene(scene);
	        primaryStage.setTitle("File Drag and Drop");
	        primaryStage.show();

	        Platform.runLater(() -> {  // Ensure handlers are attached *after* stage is shown
	            scene.setOnDragOver(event -> {
	                if (event.getDragboard().hasFiles()) {
	                    event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
	                }
	                event.consume();
	            });

	            scene.setOnDragDropped(event -> {
	                boolean success = false;
	                if (event.getDragboard().hasFiles()) {
	                	files.addAll(event.getDragboard().getFiles());	 
	                	System.out.println(files.size());
	                    success = true;
	                }
	                event.setDropCompleted(success);
	                event.consume();
	            });
	        }); // End of Platform.runLater()
	    }

    public static void main(String[] args) {
        launch();
    }

}