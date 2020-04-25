package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.Anime;
import com.folva.moderneastculture.model.dto.Genre;
import javafx.beans.binding.DoubleBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AnimeInfoController implements Initializable {

    @FXML
    private Label lAnimeName;
    @FXML
    private Label lAnimeAltNames;
    @FXML
    private Label lStatus;
    @FXML
    private Label lYear;
    @FXML
    private Label lPremiereSeason;
    @FXML
    private Label lAgeRating;
    @FXML
    private Label lGenresTitle;
    @FXML
    private Label lGenres;
    @FXML
    private Label lSource;
    @FXML
    private Label lAuthorTitle;
    @FXML
    private Label lAuthor;
    @FXML
    private Label lType;
    @FXML
    private Label lEpisodeCount;
    @FXML
    private Label lDescription;
    @FXML
    private ImageView ivFirstImage;

    private AnimePresentationControl animePresentationControl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lGenres.maxWidthProperty().bind(new DoubleBinding() {
            private static final int MAX_LEFT_PART_WIDTH = 517;
            private static final int COLON_WIDTH = 9;

            { bind(lGenresTitle.widthProperty()); }

            @SuppressWarnings("UnnecessaryLocalVariable")
            @Override
            protected double computeValue() {
                double newWidth = MAX_LEFT_PART_WIDTH - COLON_WIDTH - lGenresTitle.getWidth();
                return newWidth;
            }
        });
    }

    public void setAnimeControllerReference(AnimePresentationControl animePresentationControl) {
        this.animePresentationControl = animePresentationControl;

        Anime anime = animePresentationControl.getAnime();

        lAnimeName.setText(anime.getName());

        Optional<String> collectedAltNames = anime.getAltNames().stream()
                .reduce((s, s2) -> s.concat("\n").concat(s2));
        if (!collectedAltNames.isPresent()) {
            Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("fatalError"));
            Main.errorAlert.showAndWait();
            System.exit(-1);
        }
        lAnimeAltNames.setText(collectedAltNames.get());

        lStatus.setText(Repository.instance.getNamesBundleValue(anime.getStatus().name));

        lYear.setText(String.valueOf(anime.getPremiereYear()));

        lPremiereSeason.setText(Repository.instance.getNamesBundleValue(anime.getPremiereSeason().name));

        lAgeRating.setText(anime.getAgeRating().name);

        ArrayList<Pair<Anime, Genre>> animeGenreMap = Repository.instance.getAnimeGenreMap(true);
        ArrayList<String> currentAnimeGenreNames = animeGenreMap.stream().filter(animeGenrePair ->
                animeGenrePair.getKey().getId() == anime.getId()).map(Pair::getValue)
                .map(genre -> genre.name)
                .collect(Collectors.toCollection(ArrayList::new));
        String genresString = currentAnimeGenreNames.stream()
                .reduce("", (s, s2) -> s.concat(", ".concat(s2)))
                .substring(2);
        lGenres.setText(genresString);

        lSource.setText(Repository.instance.getNamesBundleValue(anime.getSource().name));

        lAuthorTitle.setText(Repository.instance.getNamesBundleValue(anime
                .getAuthor().getType().name().toLowerCase()));

        lAuthor.setText(anime.getAuthor().getName());

        lType.setText(Repository.instance.getNamesBundleValue(anime.getType().name));

        lEpisodeCount.setText(String.valueOf(anime.getEpisodeCount()));

        lDescription.setText(anime.getDescription());

        ArrayList<String> imagePaths = Repository.instance.getAnimeImagePaths(true)
                .stream()
                .filter(animeImagePathPair -> animeImagePathPair.getKey() == anime.getId())
                .map(Pair::getValue)
                .collect(Collectors.toCollection(ArrayList::new));

        ivFirstImage.setImage(Repository.getNoImageImage());
        if (imagePaths.size() != 0) {
            String firstImageName = imagePaths.get(0);
            Image firstImage = Repository.loadImage(firstImageName);
            if (firstImage != null) ivFirstImage.setImage(firstImage);
        }
    }

    public void onBExitEditWindowClick() {
        animePresentationControl.infoCallHandled.setValue(true);
    }
}
