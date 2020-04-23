package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.folva.moderneastculture.model.Repository.DB_IMAGES_FOLDER;

public class EditAnimeController implements Initializable {

    private static final Logger logger = LogManager.getLogger(EditAnimeController.class.getName());

    @FXML
    private TextArea taName;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextArea taAltNames;
    @FXML
    private TextField tbEpisodeCount;
    @FXML
    private TextField tbPremiereYear;
    @FXML
    private ImageView ivFirstImage;
    @FXML
    private ListView<String> lvAgeRatings;
    @FXML
    private ListView<String> lvSources;
    @FXML
    private ListView<String> lvTypes;
    @FXML
    private ListView<String> lvPremiereSeasons;
    @FXML
    private ListView<String> lvGenres;
    @FXML
    private ListView<String> lvStatus;

    private final ObjectProperty<ObjectProperty<Pair<Boolean, Anime>>> editingObjectReference =
            new SimpleObjectProperty<>();
    private boolean isNew = false;

    public void setEditingObjectReference(SimpleObjectProperty<Pair<Boolean, Anime>> reference) {
        editingObjectReference.set(reference);
        setAnimeDetails();
        reference.addListener((observable, oldValue, newValue) -> {

        });
    }

    private void setAnimeDetails() {
        Anime currentAnime = editingObjectReference.get().get().getValue();

        if (currentAnime == null) {
            isNew = true;
            URL resource = Main.getResource("/res/img/no_image.jpg");
            ivFirstImage.setImage(new Image(resource.toString()));
            return;
        }

        ArrayList<Pair<Integer, String>> animeImagePaths = Repository.instance.getAnimeImagePaths();

        ArrayList<Pair<Integer, String>> currentAnimeImagePaths = animeImagePaths.stream().filter(animeImage ->
                animeImage.getKey() == currentAnime.getId()).collect(Collectors.toCollection(ArrayList::new));

        if (currentAnimeImagePaths.isEmpty()) {
            URL resource = Main.getResource("/res/img/no_image.jpg");
            ivFirstImage.setImage(new Image(resource.toString()));
        } else {
            String firstImageName = currentAnimeImagePaths.get(0).getValue();
            Path firstImagePath = Paths.get(DB_IMAGES_FOLDER, firstImageName);
            File firstImage = firstImagePath.toFile();
            ivFirstImage.setImage(new Image(Main.getFileStream(firstImage)));
        }
        centerImage();

        taName.setText(currentAnime.getName());
        taDescription.setText(currentAnime.getDescription());
        taAltNames.setText(currentAnime.getAltNames().stream()
                .reduce("\b", (s, s2) -> s.concat(";").concat(s2)));
        tbEpisodeCount.setText(String.valueOf(currentAnime.getEpisodeCount()));
        tbPremiereYear.setText(String.valueOf(currentAnime.getPremiereYear()));
        lvAgeRatings.getSelectionModel().select(currentAnime.getRating().name);
        lvSources.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentAnime.getSource().name));
        lvTypes.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentAnime.getType().name));
        lvPremiereSeasons.getSelectionModel().select(
                Repository.instance.getNamesBundleValue(currentAnime.getPremiereSeason().name));
        ArrayList<Genre> currentAnimeGenres = Repository.instance.getAnimeGenreMap().stream()
                .filter(animeGenrePair -> animeGenrePair.getKey().getId() == currentAnime.getId())
                .map(Pair::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Integer> genresIdsList = currentAnimeGenres.stream().map(genre ->
                genre.id).collect(Collectors.toCollection(ArrayList::new));
        int[] genresIds = new int[genresIdsList.size()];
        for (int i = 0; i < genresIds.length; i++) {
            genresIds[i] = genresIdsList.get(i);
        }
        lvGenres.getSelectionModel().clearSelection();
        lvGenres.getSelectionModel().selectIndices(0, genresIds);
        lvStatus.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentAnime.getStatus().type));
    }

    @FXML
    private void onBExitEditWindowClick() {
        ObjectProperty<Pair<Boolean, Anime>> reference = editingObjectReference.get();
        reference.set(new Pair<>(false, null));
    }

    @FXML
    private void onBSaveClick() {
        Anime currentAnime = editingObjectReference.get().get().getValue();
        if (isNew) {

        } else {
//            currentAnime.set
        }
        logger.info(String.format("Anime with id %d was altered", currentAnime.getId()));
    }

    @SuppressWarnings("DuplicatedCode")
    private void centerImage() {
        Image img = ivFirstImage.getImage();
        if (img != null) {
            double ratioX = ivFirstImage.getFitWidth() / img.getWidth();
            double ratioY = ivFirstImage.getFitHeight() / img.getHeight();

            double reducCoeff = Math.min(ratioX, ratioY);

            double width = img.getWidth() * reducCoeff;
            double height = img.getHeight() * reducCoeff;

            ivFirstImage.setX((ivFirstImage.getFitWidth() - width) / 2);
            ivFirstImage.setY((ivFirstImage.getFitHeight() - height) / 2);

        }
    }

    @FXML
    private void onBChooseFirstImageClick() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(Main.stage);
        System.out.println(file.getAbsolutePath());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUpTextBoxes();
        setUpListViews();
    }

    private void setUpListViews() {
        lvAgeRatings.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvAgeRatings.getItems().clear();
        lvAgeRatings.getItems().addAll(Arrays.stream(AgeRating.values()).map(ageRating ->
                ageRating.name).collect(Collectors.toCollection(ArrayList::new)));

        lvSources.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvSources.getItems().clear();
        lvSources.getItems().addAll(Arrays.stream(Anime.Source.values()).map(source ->
                Repository.instance.getNamesBundleValue(source.name))
                .collect(Collectors.toCollection(ArrayList::new)));

        lvTypes.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvTypes.getItems().clear();
        lvTypes.getItems().addAll(Arrays.stream(Anime.Type.values()).map(type ->
                Repository.instance.getNamesBundleValue(type.name))
                .collect(Collectors.toCollection(ArrayList::new)));

        lvPremiereSeasons.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvPremiereSeasons.getItems().clear();
        lvPremiereSeasons.getItems().addAll(Arrays.stream(YearSeason.values()).map(season ->
                Repository.instance.getNamesBundleValue(season.name))
                .collect(Collectors.toCollection(ArrayList::new)));

        lvGenres.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lvGenres.getItems().clear();
        lvGenres.getItems().addAll(Repository.instance.getGenres().stream().map(genre ->
                genre.name).collect(Collectors.toCollection(ArrayList::new)));

        lvStatus.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvStatus.getItems().clear();
        lvStatus.getItems().addAll(Arrays.stream(Status.values()).map(status ->
                Repository.instance.getNamesBundleValue(status.type))
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    private void setUpTextBoxes() {
        ChangeListener<String> tbChangeListener = (observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                ((StringProperty) observable).setValue(String.valueOf(0));
            } else if (!newValue.matches("[0-9]{0,4}")) {
                ((StringProperty) observable).setValue(oldValue);
            }
        };
        tbEpisodeCount.textProperty().addListener(tbChangeListener);
        tbPremiereYear.textProperty().addListener(tbChangeListener);
    }
}
