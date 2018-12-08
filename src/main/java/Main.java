import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import sam.books.BooksDBMinimal;
import sam.books.PathsMeta;
import sam.fx.clipboard.FxClipboard;
import sam.fx.helpers.FxCell;
import sam.fx.popup.FxPopupShop;
import sam.io.serilizers.LongSerializer;
import sam.io.serilizers.ObjectReader2;
import sam.io.serilizers.ObjectWriter2;
import sam.string.TextSearch;

public class Main extends Application{
	private static List<PathsImpl2> data;
	private static final Path SELF_DIR = Paths.get(System.getenv("SELF_DIR")); 
	
	public static void main(String[] args) throws SQLException, IOException {
		data = readData();
		launch(args);
	}
	private static List<PathsImpl2> readData() throws SQLException, IOException {
		
		Path cache = SELF_DIR.resolve("cache");
		Path modified = SELF_DIR.resolve("lastmodified");
		
		if(Files.exists(cache) && Files.exists(modified) && LongSerializer.read(modified) == BooksDBMinimal.ROOT.toFile().lastModified()){
			List<PathsImpl2> list = ObjectReader2.readList(cache, PathsImpl2::new);
			System.out.println("cache loaded");
			return list;
		}
		
		try(BooksDBMinimal db = new BooksDBMinimal()) {
			List<PathsImpl2> list = db.collectToList("SELECT * from "+PathsMeta.TABLE_NAME, PathsImpl2::new);
			ObjectWriter2.writeList(cache, list, (os, p) -> p.write(os));
			LongSerializer.write(BooksDBMinimal.ROOT.toFile().lastModified(), modified);
			return list;
		}
	}
	
	private final ListView<PathsImpl2> list = new ListView<>();
	private final TextField searchTF = new TextField();
	private final TextSearch<PathsImpl2> search = new TextSearch<>(p -> p.markerLowerCased, true, 500);
	private final Label fullpath = new Label();
	
	@Override
	public void start(Stage stage) throws Exception {
		FxPopupShop.setParent(stage);
		
		search.setAllData(data);
		searchTF.textProperty().addListener((p, o, n) ->  search.search(n));
		search.setOnChange(() -> Platform.runLater(this::update));
		list.setCellFactory(FxCell.listCell(p -> p.getMarker()));
		list.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> fullpath.setText(n  == null ? null : n.getPath()));
		list.getItems().setAll(data);
		
		list.setOnKeyReleased(this::copy);
		
		fullpath.setWrapText(true);
		searchTF.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(searchTF, Priority.ALWAYS);
		VBox root = new VBox(5, list, fullpath, new HBox(5, new Text("Find: "), searchTF));
		root.setPadding(new Insets(5));
		
		stage.setScene(new Scene(root));
		stage.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), searchTF::requestFocus);
		stage.show();
	}
	
	void copy(Event e){
		PathsImpl2 p = list.getSelectionModel().getSelectedItem();
		if(p == null) return;
		String s = p.getFullPath().toString();
		FxClipboard.copyToClipboard(s);
		FxPopupShop.showHidePopup(s, 2000);
	}
	void update() {
		list.getSelectionModel().clearSelection();
		search.process(list.getItems());
	}
}
