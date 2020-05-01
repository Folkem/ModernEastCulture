package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.Anime;
import com.folva.moderneastculture.model.dto.Comics;
import com.folva.moderneastculture.model.dto.Genre;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Контроллер, який відповідає за меню відображення повної інформації про комікс
 */
@SuppressWarnings("DuplicatedCode")
public class ComicsInfoController implements Initializable {

    @FXML
    private Label lComicsName;
    @FXML
    private Label lComicsAltNames;
    @FXML
    private Label lStatus;
    @FXML
    private Label lYear;
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
    private Label lChapterCount;
    @FXML
    private Label lDescription;
    @FXML
    private ImageView ivFirstImage;

    private ComicsPresentationControl comicsPresentationControl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lGenres.maxWidthProperty().bind(new DoubleBinding() {
            private static final int MAX_LEFT_PART_WIDTH = 517;
            private static final int COLON_WIDTH = 10;

            { bind(lGenresTitle.widthProperty()); }

            @SuppressWarnings("UnnecessaryLocalVariable")
            @Override
            protected double computeValue() {
                double newWidth = MAX_LEFT_PART_WIDTH - COLON_WIDTH - lGenresTitle.getWidth();
                return newWidth;
            }
        });
    }

    /**
     * @param comicsPresentationControl посилання на об'єкт елементу керування, який викликав це меню.
     *                                  Коли дане меню буде закриватися, у властивості цього елементу
     *                                  керування змінять стан на true. Також дозволяє вилучити комікс
     *                                  для відображення
     */
    public void setComicsControllerReference(ComicsPresentationControl comicsPresentationControl) {
        this.comicsPresentationControl = comicsPresentationControl;

        Comics comics = comicsPresentationControl.getComics();

        lComicsName.setText(comics.getName());

        Optional<String> collectedAltNames = comics.getAltNames()
                .stream()
                .reduce((s, s2) -> s.concat("\n").concat(s2));
        if (!collectedAltNames.isPresent()) {
            Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("fatalError"));
            Main.errorAlert.showAndWait();
            System.exit(-1);
        }
        lComicsAltNames.setText(collectedAltNames.get());

        lStatus.setText(Repository.instance.getNamesBundleValue(comics.getStatus().name));

        lYear.setText(String.valueOf(comics.getPremiereYear()));

        ArrayList<Pair<Comics, Genre>> comicsGenreMap = Repository.instance.getComicsGenreMap(true);
        ArrayList<String> currentComicsGenreNames = comicsGenreMap
                .stream()
                .filter(comicsGenrePair -> comicsGenrePair.getKey().getId() == comics.getId())
                .map(Pair::getValue)
                .map(genre -> genre.name)
                .collect(Collectors.toCollection(ArrayList::new));
        String genresString = currentComicsGenreNames.stream()
                .reduce("", (s, s2) -> s.concat(", ".concat(s2)))
                .substring(2);
        lGenres.setText(genresString);

        lSource.setText(Repository.instance.getNamesBundleValue(comics.getSource().name));

        lAuthorTitle.setText(Repository.instance.getNamesBundleValue(comics
                .getAuthor().getType().name().toLowerCase()));

        lAuthor.setText(comics.getAuthor().getName());

        lType.setText(Repository.instance.getNamesBundleValue(comics.getType().name));

        lChapterCount.setText(String.valueOf(comics.getChapterCount()));

        lDescription.setText(comics.getDescription());

        ArrayList<String> imagePaths = Repository.instance.getComicsImagePaths(true)
                .stream()
                .filter(comicsImagePathPair -> comicsImagePathPair.getKey() == comics.getId())
                .map(Pair::getValue)
                .collect(Collectors.toCollection(ArrayList::new));

        ivFirstImage.setImage(Repository.getNoImageImage());
        if (imagePaths.size() != 0) {
            String firstImageName = imagePaths.get(0);
            Image firstImage = Repository.loadImage(firstImageName);
            if (firstImage != null) ivFirstImage.setImage(firstImage);
        }
    }

    @FXML
    private void onBExitEditWindowClick() {
        comicsPresentationControl.infoCallHandled.setValue(true);
    }
}
