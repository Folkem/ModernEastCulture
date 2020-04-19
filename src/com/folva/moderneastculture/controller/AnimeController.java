package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.AgeRating;
import com.folva.moderneastculture.model.dto.Anime;
import com.folva.moderneastculture.model.dto.Genre;
import com.folva.moderneastculture.model.dto.Status;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AnimeController implements Initializable {

    private static final int MAX_ANIME_PER_PAGE = 20;

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

    private final ArrayList<Anime> animeList = new ArrayList<>();

    private ObservableListWrapper<Anime> filteredAnimeList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        importAnime();
        setUpGenres();
        setAnimeTypes();
        setAnimeStatuses();
        setAnimeYearBoxes();
        setAgeRatings();
        setAnimeDisplay();
    }

    private void setAnimeDisplay() {
        filteredAnimeList.addListener((InvalidationListener)c -> {

        });
    }

    private void importAnime() {
        animeList.addAll(Repository.instance.getAnimes());
        filteredAnimeList = new ObservableListWrapper<>(new ArrayList<>());
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

        filteredAnimeList.addAll(animeList.stream().filter(anime -> {
            boolean containGenre = false;
            boolean correctType = false;
            boolean correctStatus = false;
            boolean correctYear = false;
            boolean correctAgeRating = false;

            if (animeGenreMap.stream().anyMatch(animeGenrePair -> {
                boolean currentAnime = (animeGenrePair.getKey().equals(anime));
                boolean containsAtLeastOneSelectedGenre = selectedGenres.contains(animeGenrePair.getValue().name);
                return currentAnime && containsAtLeastOneSelectedGenre; }))
                containGenre = true;

            if (selectedType == null ||
                selectedType.equals(Repository.instance.getNamesBundleValue("ignoreValue")) ||
                selectedType.equals(Repository.instance.getNamesBundleValue(anime.getType().type)))
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
        }).collect(Collectors.toCollection(ArrayList::new)));
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
            boolean nameContainsSearchedSymbols = anime.getName().contains(animeName);
            boolean anyAltNameContainsSearchedSymbols = anime.getAltNames().stream().anyMatch(altName -> altName.contains(animeName));
            return nameContainsSearchedSymbols || anyAltNameContainsSearchedSymbols;
        }).collect(Collectors.toCollection(ArrayList::new)));

        filteredAnimeList.forEach(anime -> System.out.println(anime.getName()));
    }
}
