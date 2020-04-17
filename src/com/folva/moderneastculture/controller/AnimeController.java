package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.AgeRating;
import com.folva.moderneastculture.model.dto.Anime;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

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
        filteredAnimeList = new ObservableListWrapper<>(animeList);
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
        lvGenres.getItems().addAll(Repository.instance.getGenres());
    }

    @FXML
    private void onBFilter() {

    }
}
