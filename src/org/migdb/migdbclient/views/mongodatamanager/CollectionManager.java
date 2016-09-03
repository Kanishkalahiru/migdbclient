package org.migdb.migdbclient.views.mongodatamanager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import org.bson.Document;
import org.json.simple.JSONObject;
import org.migdb.migdbclient.config.FxmlPath;
import org.migdb.migdbclient.main.MainApp;
import org.migdb.migdbclient.models.dao.SqliteDAO;
import org.migdb.migdbclient.resources.CenterLayout;
import org.migdb.migdbclient.resources.MongoDBResource;
import org.migdb.migdbclient.tablegen.CustomCellFactory;
import org.migdb.migdbclient.tablegen.TableBean;
import org.migdb.migdbclient.utils.MigDBNotifier;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import tray.animations.AnimationType;
import tray.notification.NotificationType;

public class CollectionManager implements Initializable {

	@FXML
	private AnchorPane collectionAncPane;
	@FXML
	private TableView<TableBean> collectionTable;
	@FXML
	private Label collectionNameLabel;
	@FXML
	private Button backButton;
	@FXML
	private Button insertNewDocButton;

	@FXML
	private TextField keyTextField;
	@FXML
	private TextField valueTextField;
	@FXML
	private TextField limitTextField;
	@FXML
	private Button searchButton;
	@FXML
	private ComboBox<String> operatorsComboBox;

	MongoCollection<Document> collection;
	private String collectionName;
	private MongoDatabase db;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		db = MongoDBResource.INSTANCE.getDatabase();
		keyTextField.setPromptText("Key");
		valueTextField.setPromptText("Value");
		operatorsComboBox.setPromptText("Operators");
		SqliteDAO dao = new SqliteDAO();
		operatorsComboBox.getItems().addAll(dao.getQueryOperators());

	}

	public void setCollection(String collectionName) {
		this.collectionName = collectionName;
		collection = db.getCollection(collectionName);
		long documentCount = collection.count();
		collectionNameLabel.setText(collectionName + " (" + documentCount + " Documents)");

		// System.out.println(collectionName);

		List<Document> foundDocument = collection.find().into(new ArrayList<Document>());

		// Document document = collection.find().first();
		// System.out.println(document.toJson());
		// JSONObject jsonObject = new JSONObject(document);

		// System.out.println("aaaaaaaaaaa" + jsonObject);
		// System.out.println(document.keySet().toString());
		if (!foundDocument.isEmpty()) {
			setTable(foundDocument, getCommonColumns(foundDocument));
		} else {
			String title = "Attention";
			String message = "The Collection you selected is Empty";
			AnimationType animationType = AnimationType.FADE;
			NotificationType notificationType = NotificationType.WARNING;
			int showTime = 6;

			MigDBNotifier notification = new MigDBNotifier(title, message, animationType, notificationType, showTime);
			notification.createDefinedNotification();
		}

	}

	public Set<String> getCommonColumns(List<Document> doc) {
		JSONObject commonColumns = new JSONObject();
		for (Document document : doc) {
			JSONObject object = new JSONObject(document);

			for (Object key : object.keySet()) {
				// System.out.println(key);
				document.containsKey(key);
				if (commonColumns.containsKey(key)) {
					int cnt = Integer.parseInt(commonColumns.get(key).toString());
					cnt++;
					commonColumns.put(key, cnt);
				} else {
					commonColumns.put(key, "1");
				}
			}

		}
		// System.out.println(commonColumns);
		Set<String> set = null;
		Iterator<?> keys = commonColumns.keySet().iterator();
		int[] arr = new int[commonColumns.keySet().size()];
		int cnt = 0;
		while (keys.hasNext()) {
			// System.out.println(commonColumns.get(keys.next()));
			arr[cnt++] = Integer.parseInt(commonColumns.get(keys.next()).toString());
		}
		for (int i = 0; i < arr.length; i++) {

			System.out.println(arr[i]);
		}
		Document commonDoc = new Document();
		commonDoc.put("_id", "1");
		int commonCount = getMostPopularElement(arr);
		Iterator<?> keyset = commonColumns.keySet().iterator();
		while (keyset.hasNext()) {
			Object key = keyset.next();
			String st = commonColumns.get(key).toString();
			int value = Integer.parseInt(st);
			if (value == commonCount) {
				commonDoc.append((String) key, value);
			}

		}
		System.out.println("common doc " + commonDoc);

		return commonDoc.keySet();
	}

	public void setTable(List<Document> documents, Set<String> keySet) {

		double tableWidth = 0;
		for (String col : keySet) {
			TableColumn<TableBean, String> column = new TableColumn<>();
			column.setCellValueFactory(new CustomCellFactory<TableBean, String>(col));
			column.setText(col);
			collectionTable.getItems().clear();
			collectionTable.getColumns().add(column);
		}
		ObservableList<TableBean> tableList = FXCollections.observableArrayList();

		for (Document doc : documents) {
			TableBean bean = new TableBean();
			for (String key : keySet) {
				// if(key.equals("loc")) continue;
				bean.setCellData(key, String.valueOf(doc.get(key)));
			}

			tableList.add(bean);
		}

		collectionTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		collectionTable.getItems().addAll(tableList);
		collectionTable.getColumns().stream().forEach(cell -> {
			// System.out.println("width " + cell.getPrefWidth());
		});
		// collectionTable.setPrefWidth(tableWidth);

	}

	@FXML
	public void deleteDocument() {
		SimpleStringProperty st = (SimpleStringProperty) collectionTable.getSelectionModel().getSelectedItem()
				.getCellValue("_id");
		String selectedId = st.getValue();
		int selectedIndex = collectionTable.getSelectionModel().getSelectedIndex();
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Confirmation");
		alert.setHeaderText("Delete Document");
		alert.setContentText(
				"Command = db." + collectionName + ".deleteMany({ _id : \"" + selectedId + "\" })\n Are you sure?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			// ... user chose OK
			DeleteResult MongoResult = db.getCollection(collectionName).deleteMany(new Document("_id", selectedId));
			collectionTable.getItems().remove(selectedIndex);
			// System.out.println(MongoResult.toString());
		} else {
			// ... user chose CANCEL or closed the dialog
		}

	}

	@FXML
	public void showDocument() {
		SimpleStringProperty st = (SimpleStringProperty) collectionTable.getSelectionModel().getSelectedItem()
				.getCellValue("_id");
		String selectedId = st.getValue();
		AnchorPane root;
		root = CenterLayout.INSTANCE.getRootContainer();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainApp.class.getResource(FxmlPath.DOCUMENTMANAGER.getPath()));
		AnchorPane documentManagerAncPane;
		try {
			documentManagerAncPane = loader.load();
			DocumentManager documentManager = (DocumentManager) loader.getController();
			documentManager.setDocument(collection, collectionName, selectedId);
			root.getChildren().clear();
			root.getChildren().add(documentManagerAncPane);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	public void showMongoDataManager() throws Exception {
		AnchorPane root;
		root = CenterLayout.INSTANCE.getRootContainer();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainApp.class.getResource(FxmlPath.DATAMANAGER.getPath()));
		AnchorPane mongoDataManagerAncPane;
		try {
			mongoDataManagerAncPane = loader.load();
			MongoDataManager dataManager = (MongoDataManager) loader.getController();
			dataManager.setDatabase(MongoDBResource.INSTANCE.getDatabaseName());
			root.getChildren().clear();
			root.getChildren().add(mongoDataManagerAncPane);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@FXML
	public void insertNewDocument() {
		AnchorPane root = CenterLayout.INSTANCE.getRootContainer();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainApp.class.getResource(FxmlPath.NEW_DOC.getPath()));
		AnchorPane mongoDataManagerAncPane;
		try {
			mongoDataManagerAncPane = loader.load();
			root.getChildren().clear();
			root.getChildren().add(mongoDataManagerAncPane);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	public void Search() {
		if (validateSearch(operatorsComboBox)) {
			String key = keyTextField.getText();
			String value = valueTextField.getText();
			String operator = operatorsComboBox.getSelectionModel().getSelectedItem();
			System.out.println(key + " " + value + " " + operator + " " + collectionName);

			List<Document> foundDocument = new ArrayList<>();
			if (operator.equals("=")) {
				BasicDBObject query = new BasicDBObject();
				query.put(key, value);
				FindIterable<Document> iterable = db.getCollection(collectionName).find(query);
				foundDocument = iterableToList(iterable);
			} else if (operator.equals("!=")) {
				/* Step 4 : Create Query object */
				BasicDBObject query = new BasicDBObject();
				query.put(key, new BasicDBObject("$ne", value));

				/* Step 5 : Get all documents */
				FindIterable<Document> iterable = db.getCollection(collectionName).find(query);
				foundDocument = iterableToList(iterable);
			} 
			else if (operator.equals("<")) {
				/* Step 4 : Create Query object */
				BasicDBObject query = new BasicDBObject();
				query.put(key, new BasicDBObject("$gt", value));

				/* Step 5 : Get all documents */
				FindIterable<Document> iterable = db.getCollection(collectionName).find(query);
				foundDocument = iterableToList(iterable);
			}
			else if (operator.equals("<=")) {
				/* Step 4 : Create Query object */
				BasicDBObject query = new BasicDBObject();
				query.put(key, new BasicDBObject("$gte", value));
				
				/* Step 5 : Get all documents */
				FindIterable<Document> iterable = db.getCollection(collectionName).find(query);
				foundDocument = iterableToList(iterable);
			}
			else if (operator.equals(">")) {
				/* Step 4 : Create Query object */
				BasicDBObject query = new BasicDBObject();
				query.put(key, new BasicDBObject("$lt", value));
				
				/* Step 5 : Get all documents */
				FindIterable<Document> iterable = db.getCollection(collectionName).find(query);
				foundDocument = iterableToList(iterable);
			}
			else if (operator.equals(">=")) {
				/* Step 4 : Create Query object */
				BasicDBObject query = new BasicDBObject();
				query.put(key, new BasicDBObject("$lte", value));
				
				/* Step 5 : Get all documents */
				FindIterable<Document> iterable = db.getCollection(collectionName).find(query);
				foundDocument = iterableToList(iterable);
			}

			if (!foundDocument.isEmpty()) {
				collectionTable.getColumns().clear();

				setTable(foundDocument, getCommonColumns(foundDocument));
			} else {
				collectionTable.getItems().clear();
				String title = "Attention";
				String message = "No mathing values with given parameters";
				AnimationType animationType = AnimationType.FADE;
				NotificationType notificationType = NotificationType.WARNING;
				int showTime = 6;

				MigDBNotifier notification = new MigDBNotifier(title, message, animationType, notificationType,
						showTime);
				notification.createDefinedNotification();
			}
		}
	}

	private boolean validateSearch(ComboBox<String> box) {
		if (box.getSelectionModel().isEmpty()) {
			String title = "Attention";
			String message = "Please select an Operator";
			AnimationType animationType = AnimationType.FADE;
			NotificationType notificationType = NotificationType.WARNING;
			int showTime = 6;

			MigDBNotifier notification = new MigDBNotifier(title, message, animationType, notificationType, showTime);
			notification.createDefinedNotification();
			return false;
		} else {
			return true;
		}
	}

	private List<Document> iterableToList(FindIterable<Document> iterable) {
		List<Document> foundDocument = new ArrayList<>();

		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				System.out.println(document);
				foundDocument.add(document);
			}
		});
		return foundDocument;
	}

	private static int getMostPopularElement(int[] a) {
		int maxElementIndex = getArrayMaximumElementIndex(a);
		int[] b = new int[a[maxElementIndex] + 1];
		for (int i = 0; i < a.length; i++) {
			++b[a[i]];
		}
		return getArrayMaximumElementIndex(b);
	}

	private static int getArrayMaximumElementIndex(int[] a) {
		int maxElementIndex = 0;
		for (int i = 1; i < a.length; i++) {
			if (a[i] >= a[maxElementIndex]) {
				maxElementIndex = i;
			}
		}
		return maxElementIndex;
	}

}
