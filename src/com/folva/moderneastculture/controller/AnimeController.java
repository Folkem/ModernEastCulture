package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.*;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.folva.moderneastculture.model.Repository.DB_IMAGES_FOLDER;

public class AnimeController implements Initializable {

    private static final Logger logger = LogManager.getLogger(AnimeController.class.getName());

    private static final int ANIME_CONTROLS_PER_PAGE = 5;
    private static final int ANIME_CONTROL_HEIGHT = 100;

    @FXML
    private ScrollPane spAnimeList;
    @FXML
    private ChoiceBox<String> cbAnimeStatuses;
    @FXML
    private ChoiceBox<String> cbAnimeTypes;
    @FXML
    private ChoiceBox<String> cbAgeRatings;
    @FXML
    private ListView<String> lvGenres;
    @FXML
    private TextField tbYearFrom;
    @FXML
    private TextField tbYearTo;
    @FXML
    private Pagination animePagination;
    @FXML
    private TextField tbAnimeName;

    private final ArrayList<AnimePair> animeList = new ArrayList<>();
    private final ArrayList<Pair<Integer, String>> animeImages = new ArrayList<>();
    private final SimpleObjectProperty<Pair<Boolean, Anime>> editingObject = new SimpleObjectProperty<>();

    private ObservableListWrapper<AnimePair> filteredAnimeList;
    private Tab tabAnime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        importAnime();
        setUpGenres();
        setAnimeTypes();
        setAnimeStatuses();
        setAnimeYearBoxes();
        setAgeRatings();
        setAnimeDisplay();
        setPagination();
        bindProperties();
    }

    private void bindProperties() {
        editingObject.addListener((observable, oldValue, newValue) -> {
            boolean startEditing = newValue.getKey();

            if (startEditing) {
                Parent editPane = Main.getForm("EditAnimeForm");
                try {
                    EditAnimeController editController = (EditAnimeController) Main
                            .getControllerForForm("EditAnimeForm");
                    editController.setEditingObjectReference(editingObject);
                } catch (Exception e) {
                    logger.error("Error while setting called anime control for edit controller: ", e);
                    Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("problemOccurred"));
                    Main.errorAlert.showAndWait();
                    System.exit(-1);
                }
                tabAnime.setContent(editPane);
            } else {
                Parent animeContentPane = Main.getForm("AnimeForm");
                tabAnime.setContent(animeContentPane);
            }
        });
    }

    private void setPagination() {
        int newPageCount = (int) Math.ceil(animeList.size() / (double) ANIME_CONTROLS_PER_PAGE);
        animePagination.setPageCount(newPageCount);
        animePagination.currentPageIndexProperty().addListener(observable -> spAnimeList.setVvalue(0));
        animePagination.setPageFactory(pageIndex -> {
            final int controlCountOnTheLastPage = filteredAnimeList.size() - pageIndex * ANIME_CONTROLS_PER_PAGE;
            final int startAnimeIndex = pageIndex * ANIME_CONTROLS_PER_PAGE;
            final int endAnimeIndex = startAnimeIndex + Math.min(ANIME_CONTROLS_PER_PAGE, controlCountOnTheLastPage);
            ArrayList<AnimePair> animeListToPresent = new ArrayList<>(
                    filteredAnimeList.subList(startAnimeIndex, endAnimeIndex));
            ArrayList<AnimePresentationControl> animeControls = new ArrayList<>();
            for (AnimePair animePair : animeListToPresent) {
                if (animePair.getValue() != null) {
                    animeControls.add(animePair.getValue());
                    continue;
                }
                Anime anime = animePair.getKey();

                AnimePresentationControl animeControl = new AnimePresentationControl();
                animeControl.logger = logger;
                animeControl.load();
                animeControl.setAnime(anime);
                Optional<Pair<Integer, String>> optionalAnimeImagePair = animeImages.stream()
                        .filter(animeImagePair -> animeImagePair.getKey() == anime.getId()).findFirst();
                if (optionalAnimeImagePair.isPresent() &&
                    Files.exists(Paths.get(DB_IMAGES_FOLDER, optionalAnimeImagePair.get().getValue()))) {
                    Path imagePath = Paths.get(DB_IMAGES_FOLDER, optionalAnimeImagePair.get().getValue());
                    File file = imagePath.toFile();
                    animeControl.ivAnimeImage.setImage(new Image(Main.getFileStream(file)));
                    animeControl.centerImage();
                }
                animeControl.isDeleted.addListener(observable -> {
                    logger.info("Anime: " + anime.getName() + " was deleted");
                    animeList.remove(animePair);
                    filteredAnimeList.remove(animePair);
                });
                animeControl.setEditingObjectReference(editingObject);
                animeControls.add(animeControl);

                animePair.setValue(animeControl);
            }

            VBox pane = new VBox();
            pane.setPrefHeight(ANIME_CONTROL_HEIGHT * ANIME_CONTROLS_PER_PAGE);
            pane.getChildren().addAll(animeControls);
            return pane;
        });
    }

    public void setTabAnime(Tab tabAnime) {
        this.tabAnime = tabAnime;
    }

    private void setAnimeDisplay() {
        filteredAnimeList.addListener((InvalidationListener) c -> {
            int newPageCount = (int) Math.ceil(filteredAnimeList.size() / (double) ANIME_CONTROLS_PER_PAGE);
            newPageCount = (newPageCount > 0) ? newPageCount : 1;
            animePagination.setPageCount(newPageCount + 2);
            animePagination.setPageCount(newPageCount);
        });
    }

    private void importAnime() {
        ArrayList<Anime> animes = Repository.instance.getAnimes();
        animeList.clear();
        animeList.addAll(animes.stream().map(anime -> new AnimePair(anime, null))
                .collect(Collectors.toCollection(ArrayList::new)));
        filteredAnimeList = new ObservableListWrapper<>(new ArrayList<>());
        filteredAnimeList.addAll(animeList);
        animeImages.clear();
        animeImages.addAll(Repository.instance.getAnimeImagePaths());
    }

    private void setAgeRatings() {
        ArrayList<String> ageRatings = new ArrayList<>();
        for (AgeRating ageRating : AgeRating.values()) {
            ageRatings.add(ageRating.toString());
        }
        ageRatings.add(Repository.instance.getNamesBundleValue("ignoreValue"));
        cbAgeRatings.getItems().addAll(ageRatings);
    }

    private void setAnimeYearBoxes() {
        ChangeListener<String> tbChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue.matches("[0-9]{0,4}")) {
                ((StringProperty) observable).setValue(oldValue);
            }
        };
        tbYearFrom.textProperty().addListener(tbChangeListener);
        tbYearTo.textProperty().addListener(tbChangeListener);
    }

    private void setAnimeStatuses() {
        ArrayList<String> animeStatuses = new ArrayList<>();
        for (Status status : Status.values()) {
            animeStatuses.add(Repository.instance.getNamesBundleValue(status.toString().toLowerCase()));
        }
        animeStatuses.add(Repository.instance.getNamesBundleValue("ignoreValue"));
        cbAnimeStatuses.getItems().addAll(animeStatuses);
    }

    private void setAnimeTypes() {
        ArrayList<String> animeTypes = new ArrayList<>();
        for (Anime.Type value : Anime.Type.values()) {
            animeTypes.add(Repository.instance.getNamesBundleValue(value.toString().toLowerCase()));
        }
        animeTypes.add(Repository.instance.getNamesBundleValue("ignoreValue"));
        cbAnimeTypes.getItems().addAll(animeTypes);
    }

    private void setUpGenres() {
        lvGenres.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lvGenres.getItems().addAll(Repository.instance.getGenres().stream().map(genre -> genre.name)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @FXML
    private void onBFilter() {
        if (!filterFieldsAreValidated()) return;

        filteredAnimeList.clear();

        ArrayList<Pair<Anime, Genre>> animeGenreMap = Repository.instance.getAnimeGenreMap();

        ArrayList<String> selectedGenres = new ArrayList<>(lvGenres.getSelectionModel().getSelectedItems());
        String selectedType = cbAnimeTypes.getSelectionModel().getSelectedItem();
        String selectedStatus = cbAnimeStatuses.getSelectionModel().getSelectedItem();
        int selectedYearFrom = tbYearFrom.getText().isEmpty() ? 0 : Integer.parseInt(tbYearFrom.getText());
        int selectedYearTo = tbYearTo.getText().isEmpty() ? 9999 : Integer.parseInt(tbYearTo.getText());
        String selectedAgeRating = cbAgeRatings.getSelectionModel().getSelectedItem();

        filteredAnimeList.addAll(animeList.stream().filter(animePair -> {
            Anime anime = animePair.getKey();

            boolean containGenre = false;
            boolean correctType = false;
            boolean correctStatus = false;
            boolean correctYear = false;
            boolean correctAgeRating = false;

            if (selectedGenres.isEmpty() ||
                animeGenreMap.stream().anyMatch(animeGenrePair -> {
                boolean currentAnime = (animeGenrePair.getKey().equals(anime));
                boolean containsAtLeastOneSelectedGenre = selectedGenres.contains(animeGenrePair.getValue().name);
                return currentAnime && containsAtLeastOneSelectedGenre;
            }))
                containGenre = true;

            if (selectedType == null ||
                    selectedType.equals(Repository.instance.getNamesBundleValue("ignoreValue")) ||
                    selectedType.equals(Repository.instance.getNamesBundleValue(anime.getType().name)))
                correctType = true;

            if (selectedStatus == null ||
                    selectedStatus.equals(Repository.instance.getNamesBundleValue("ignoreValue")) ||
                    selectedStatus.equals(Repository.instance.getNamesBundleValue(anime.getStatus().type)))
                correctStatus = true;

            if (anime.getPremiereYear() >= selectedYearFrom && anime.getPremiereYear() <= selectedYearTo)
                correctYear = true;

            if (selectedAgeRating == null ||
                    selectedAgeRating.equals(Repository.instance.getNamesBundleValue("ignoreValue")) ||
                    selectedAgeRating.equals(anime.getRating().name))
                correctAgeRating = true;

            return containGenre && correctType && correctStatus && correctYear && correctAgeRating;
        })
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    private boolean filterFieldsAreValidated() {
        int selectedYearFrom = tbYearFrom.getText().isEmpty() ? 0 : Integer.parseInt(tbYearFrom.getText());
        int selectedYearTo = tbYearTo.getText().isEmpty() ? 9999 : Integer.parseInt(tbYearTo.getText());

        if (selectedYearFrom > selectedYearTo) {
            Main.warningAlert.setContentText(Repository.instance.getNamesBundleValue("yearValidationIncorrect"));
            Main.warningAlert.show();
            return false;
        }

        return true;
    }

    @FXML
    private void onBSearchByNameClick() {
        String animeName = tbAnimeName.getText();

        if (animeName.matches("[ ]+")) return;

        filteredAnimeList.clear();

        filteredAnimeList.addAll(animeList.stream().filter(anime -> {
            boolean nameContainsSearchedSymbols = anime.getKey().getName().contains(animeName);
            boolean anyAltNameContainsSearchedSymbols = anime.getKey().getAltNames().stream()
                    .anyMatch(altName -> altName.contains(animeName));
            return nameContainsSearchedSymbols || anyAltNameContainsSearchedSymbols;
        })
                .collect(Collectors.toCollection(ArrayList::new)));

        filteredAnimeList.forEach(anime -> System.out.println(anime.getKey().getName()));
    }
}