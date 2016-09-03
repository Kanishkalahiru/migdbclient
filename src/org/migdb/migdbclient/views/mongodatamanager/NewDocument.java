package org.migdb.migdbclient.views.mongodatamanager;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.migdb.migdbclient.models.mongodatamanager.ArrayContainer;
import org.migdb.migdbclient.models.mongodatamanager.KeyValueContainer;
import org.migdb.migdbclient.models.mongodatamanager.ObjectContainer;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class NewDocument implements Initializable{
	@FXML
	private Button addKeyValue;
	@FXML
	private Button addArray;
	@FXML
	private Button addObject;
	
	@FXML private VBox container;
	
	private ObjectContainer objectContainer;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		objectContainer = new ObjectContainer();
	}
	
	@FXML
	public void addKeyValue(){
		Label label = new Label();
		label.setText("Key Value :  ");
		TextField key = new TextField();
		key.setPromptText("Key");
		TextField value = new TextField();
		value.setPromptText("Value");
		KeyValueContainer childContainer = new KeyValueContainer(key, value);
		objectContainer.getObjectContainer().add(childContainer);
		
		HBox box = new HBox();
		box.getChildren().add(label);
		box.getChildren().add(key);
		box.getChildren().add(value);
		
		container.getChildren().add(box);
	}
	@FXML
	public void addArray(){
		Label label = new Label();
		label.setText("Array List  :  ");
		TextField key = new TextField();
		key.setPromptText("Key");
		TextField value = new TextField();
		value.setPromptText("Value");
		ArrayContainer arrayContainer = new ArrayContainer(key, value);
		objectContainer.getObjectContainer().add(arrayContainer);
		
		HBox box = new HBox();
		box.getChildren().add(label);
		box.getChildren().add(key);
		box.getChildren().add(value);
		container.getChildren().add(box);
		
	}
	
	@FXML
	public void getJson() {
		System.out.println(objectContainer.generateJson());
	}

}