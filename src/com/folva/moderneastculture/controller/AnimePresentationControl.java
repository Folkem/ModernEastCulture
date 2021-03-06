package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.Anime;
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
 * Контроллер елементу керування, який має в собі коротку інформацію про аніме
 */
public class AnimePresentationControl extends Pane implements Initializable {

    @FXML
    private Label lAnimePremiereYear;
    @FXML
    private Label lAnimeName;
    @FXML
    private Button bDeleteAnime;
    @FXML
    private Button bEditAnime;
    @FXML
    private ImageView ivAnimeImage;

    /**
     * Об'єкт-властивість, відповідаючий за стан видалення елементу керування
     */
    public final BooleanProperty isDeleted = new SimpleBooleanProperty(false);
    /**
     * Об'єкт-властивість, відповідаючий за те, чи оброблений виклик меню інформації про аніме,
     * яке носить в собі даний елемент керування
     */
    public final BooleanProperty infoCallHandled = new SimpleBooleanProperty(true);

    /**
     * Об'єкт (логер), який виводить різного роду інформацію відносно цього елементу керування
     */
    public Logger logger;

    private SimpleObjectProperty<OpenPair<Boolean, Anime>> editingObjectReference;
    private boolean isLoaded = false;
    private Anime currentAnime = null;

    /**
     * Завантажує даний елемент керування та встановлює залежності від об'єкту-властивості
     * стану авторизації адміністратора
     */
    public void load() {
        if (!isLoaded) {
            FXMLLoader loader = new FXMLLoader(Main.getResource("/res/fxml_views/AnimePresentation_Control.fxml"));
            loader.setController(this);
            try {
                getChildren().add(loader.load());
            } catch (IOException e) {
                logger.error("Error while loading one of the anime presentation controls", e);
            }
            bDeleteAnime.visibleProperty().bind(Repository.adminIsAuthorizedProperty);
            bEditAnime.visibleProperty().bind(Repository.adminIsAuthorizedProperty);
            isLoaded = true;
        }

    }

    /**
     * Встановлює аніме, яке потрібно відображати в цьому елементі керування
     * @param anime аніме для відображення
     */
    public void setAnime(Anime anime) {
        currentAnime = anime;
        lAnimeName.setText(anime.getName());
        lAnimePremiereYear.setText(String.valueOf(anime.getPremiereYear()));
    }

    /**
     * @return аніме для відображення
     */
    public Anime getAnime() {
        return currentAnime;
    }

    /**
     * Встановлює посилання на об'єкт-властивість стану змінення об'єкту та самого об'єкту аніме
     * @param reference посилання на об'єкт-властивість стану змінення об'єкту та самого об'єкту аніме
     */
    public void setEditingObjectReference(SimpleObjectProperty<OpenPair<Boolean, Anime>> reference) {
        editingObjectReference = reference;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bDeleteAnime.setVisible(false);
    }

    @FXML
    private void onBDeleteAnimeClick() {
        try {
            Repository.instance.deleteAnimeFromDb(currentAnime);
            isDeleted.setValue(true);
        } catch (Exception e) {
            logger.error("Error while deleting anime '" + lAnimeName.getText() + "': ", e);
        }
    }

    @FXML
    private void onBEditAnimeClick() {
        editingObjectReference.setValue(new OpenPair<>(true, currentAnime));
    }

    @FXML
    private void onBShowAnimeInfoClick() {
        infoCallHandled.setValue(false);
    }

    @SuppressWarnings("DuplicatedCode")
    private void centerImage() {
        Image img = ivAnimeImage.getImage();
        if (img != null) {
            double ratioX = ivAnimeImage.getFitWidth() / img.getWidth();
            double ratioY = ivAnimeImage.getFitHeight() / img.getHeight();

            double reducCoeff = Math.min(ratioX, ratioY);

            double width = img.getWidth() * reducCoeff;
            double height = img.getHeight() * reducCoeff;

            ivAnimeImage.setX((ivAnimeImage.getFitWidth() - width) / 2);
            ivAnimeImage.setY((ivAnimeImage.getFitHeight() - height) / 2);
        }
    }

    /**
     * Встановлює зображення, якщо воно є в базі даних та якщо вдалося завантажити в об'єкт Image
     */
    @SuppressWarnings("DuplicatedCode")
    public void loadImage() {
        ArrayList<Pair<Integer, String>> animeImages = Repository.instance.getAnimeImagePaths(true);
        Optional<Pair<Integer, String>> optionalAnimeImagePair = animeImages.stream()
                .filter(animeImagePair -> animeImagePair.getKey() == currentAnime.getId()).findFirst();
        if (optionalAnimeImagePair.isPresent() &&
                Files.exists(Paths.get(DB_IMAGES_FOLDER, optionalAnimeImagePair.get().getValue()))) {
            Path imagePath = Paths.get(DB_IMAGES_FOLDER, optionalAnimeImagePair.get().getValue());
            File file = imagePath.toFile();
            InputStream fileStream = Main.getFileStream(file);
            ivAnimeImage.setImage(new Image(fileStream));
            centerImage();
            try {
                fileStream.close();
            } catch (IOException e) {
                logger.error("Error while closing the file stream: ", e);
            }
        }
    }
}
