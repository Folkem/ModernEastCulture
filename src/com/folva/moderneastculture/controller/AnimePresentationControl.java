package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.Anime;
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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AnimePresentationControl extends Pane implements Initializable {

    @FXML
    public Label lAnimePremiereYear;
    @FXML
    public Label lAnimeName;
    @FXML
    public Button bDeleteAnime;
    @FXML
    public Button bEditAnime;
    @FXML
    public ImageView ivAnimeImage;

    public final BooleanProperty isDeleted = new SimpleBooleanProperty(false);

    public Logger logger;

    private SimpleObjectProperty<Pair<Boolean, Anime>> editingObjectReference;
    private boolean isLoaded = false;
    private Anime currentAnime = null;

    public void load() {
        if (isLoaded) return;

        FXMLLoader loader = new FXMLLoader(Main.getResource("/res/fxml_views/AnimePresentation_Control.fxml"));
        loader.setController(this);

        try {
            getChildren().add(loader.load());
        } catch (IOException e) {
            logger.error("Error while loading one of the anime presentation controls", e);
        }

        isLoaded = true;
        bDeleteAnime.visibleProperty().bind(Repository.adminIsAuthorizedProperty);
        bEditAnime.visibleProperty().bind(Repository.adminIsAuthorizedProperty);
    }

    public void setAnime(Anime anime) {
        currentAnime = anime;
        lAnimeName.setText(anime.getName());
        lAnimePremiereYear.setText(String.valueOf(anime.getPremiereYear()));
    }

    public void setEditingObjectReference(SimpleObjectProperty<Pair<Boolean, Anime>> reference) {
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
        editingObjectReference.setValue(new Pair<>(true, currentAnime));
    }

    @SuppressWarnings("DuplicatedCode")
    public void centerImage() {
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
}
