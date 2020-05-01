package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.Comics;
import com.folva.moderneastculture.model.dto.OpenPair;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.folva.moderneastculture.model.Repository.DB_IMAGES_FOLDER;

/**
 * Контроллер елементу керування, який має в собі коротку інформацію про комікс
 */
public class ComicsPresentationControl extends Pane implements Initializable {

    @FXML
    private Label lComicsPremiereYear;
    @FXML
    private Label lComicsName;
    @FXML
    private Button bDeleteComics;
    @FXML
    private Button bEditComics;
    @FXML
    private ImageView ivComicsImage;

    /**
     * Об'єкт-властивість, відповідаючий за стан видалення елементу керування
     */
    public final BooleanProperty isDeleted = new SimpleBooleanProperty(false);
    /**
     * Об'єкт-властивість, відповідаючий за те, чи оброблений виклик меню інформації про комікс,
     * яке носить в собі даний елемент керування
     */
    public final BooleanProperty infoCallHandled = new SimpleBooleanProperty(true);

    /**
     * Об'єкт (логер), який виводить різного роду інформацію відносно цього елементу керування
     */
    public Logger logger;

    private SimpleObjectProperty<OpenPair<Boolean, Comics>> editingObjectReference;
    private boolean isLoaded = false;
    private Comics currentComics = null;

    /**
     * Завантажує даний елемент керування та встановлює залежності від об'єкту-властивості
     * стану авторизації адміністратора
     */
    public void load() {
        if (!isLoaded) {
            FXMLLoader loader = new FXMLLoader(Main.getResource("/res/fxml_views/ComicsPresentation_Control.fxml"));
            loader.setController(this);
            try {
                getChildren().add(loader.load());
            } catch (IOException e) {
                logger.error("Error while loading one of the comics presentation controls", e);
            }
            bDeleteComics.visibleProperty().bind(Repository.adminIsAuthorizedProperty);
            bEditComics.visibleProperty().bind(Repository.adminIsAuthorizedProperty);
            isLoaded = true;
        }

    }

    /**
     * Встановлює комікс, яке потрібно відображати в цьому елементі керування
     * @param comics комік для відображення
     */
    public void setComics(Comics comics) {
        currentComics = comics;
        lComicsName.setText(comics.getName());
        lComicsPremiereYear.setText(String.valueOf(comics.getPremiereYear()));
    }

    /**
     * @return комікс для відображення
     */
    public Comics getComics() {
        return currentComics;
    }

    /**
     * Встановлює посилання на об'єкт-властивість стану змінення об'єкту та самого об'єкту коміксу
     * @param reference посилання на об'єкт-властивість стану змінення об'єкту та самого об'єкту коміксу
     */
    public void setEditingObjectReference(SimpleObjectProperty<OpenPair<Boolean, Comics>> reference) {
        editingObjectReference = reference;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bDeleteComics.setVisible(false);
    }

    @FXML
    private void onBDeleteComicsClick() {
        try {
            Repository.instance.deleteComicsFromDb(currentComics);
            isDeleted.setValue(true);
        } catch (Exception e) {
            logger.error("Error while deleting comics '" + lComicsName.getText() + "': ", e);
        }
    }

    @FXML
    private void onBEditComicsClick() {
        editingObjectReference.setValue(new OpenPair<>(true, currentComics));
    }

    @FXML
    private void onBShowComicsInfoClick() {
        infoCallHandled.setValue(false);
    }

    @SuppressWarnings("DuplicatedCode")
    private void centerImage() {
        Image img = ivComicsImage.getImage();
        if (img != null) {
            double ratioX = ivComicsImage.getFitWidth() / img.getWidth();
            double ratioY = ivComicsImage.getFitHeight() / img.getHeight();

            double reducCoeff = Math.min(ratioX, ratioY);

            double width = img.getWidth() * reducCoeff;
            double height = img.getHeight() * reducCoeff;

            ivComicsImage.setX((ivComicsImage.getFitWidth() - width) / 2);
            ivComicsImage.setY((ivComicsImage.getFitHeight() - height) / 2);
        }
    }

    /**
     * Встановлює зображення, якщо воно є в базі даних та якщо вдалося завантажити в об'єкт Image
     */
    @SuppressWarnings("DuplicatedCode")
    public void loadImage() {
        ArrayList<Pair<Integer, String>> comicsImages = Repository.instance.getComicsImagePaths(true);
        Optional<Pair<Integer, String>> optionalComicsImagePair = comicsImages.stream()
                .filter(comicsImagePair -> comicsImagePair.getKey() == currentComics.getId()).findFirst();
        if (optionalComicsImagePair.isPresent() &&
                Files.exists(Paths.get(DB_IMAGES_FOLDER, optionalComicsImagePair.get().getValue()))) {
            Path imagePath = Paths.get(DB_IMAGES_FOLDER, optionalComicsImagePair.get().getValue());
            File file = imagePath.toFile();
            Image image = Repository.loadImage(file.getName());
            if (image != null) {
                ivComicsImage.setImage(image);
                centerImage();
            }
        }
    }
}
