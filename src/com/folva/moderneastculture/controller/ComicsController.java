package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import com.folva.moderneastculture.model.dto.*;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Контроллер відповідаючий за головне підменю коміксів - де можна продивитися увесь
 * список наявних коміксів та відфільтрувати, викликати меню редагування, додавання,
 * оглядання повної інформації якогось коміксу або видалити взагалі комікс
 */
public class ComicsController implements Initializable {

    private static final Logger logger = LogManager.getLogger(ComicsController.class.getName());

    private static final int COMICS_CONTROLS_PER_PAGE = 5;
    private static final int COMICS_CONTROL_HEIGHT = 100;

    @FXML
    private TextField tbComicsName;
    @FXML
    private ScrollPane spComicsList;
    @FXML
    private Pagination comicsPagination;
    @FXML
    private ListView<String> lvGenres;
    @FXML
    private ChoiceBox<String> cbComicsTypes;
    @FXML
    private ChoiceBox<String> cbComicsStatuses;
    @FXML
    private TextField tbYearFrom;
    @FXML
    private TextField tbYearTo;
    @FXML
    private Button bAddNewComics;

    private final ArrayList<OpenPair<Comics, ComicsPresentationControl>> comicsList = new ArrayList<>();
    private final SimpleObjectProperty<OpenPair<Boolean, Comics>> editingObject = new SimpleObjectProperty<>();

    private Tab tabComics;
    private ObservableListWrapper<OpenPair<Comics, ComicsPresentationControl>> filteredComicsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        importComics();
        setUpFiltrationData();
        setPagination();
        bindProperties();
        logger.info("ComicsController has been loaded!");
    }

    /**
     * Встановлює посилання на вкладку коміксів для переключення між підменю
     * @param tabComics посилання на вкладку коміксів
     */
    public void setTabComics(Tab tabComics) {
        this.tabComics = tabComics;
    }

    private void importComics() {
        ArrayList<Comics> comics = Repository.instance.getComics(true);
        comicsList.clear();
        comicsList.addAll(comics.stream().map(localComics ->
                new OpenPair<Comics, ComicsPresentationControl>(localComics, null))
                .collect(Collectors.toCollection(ArrayList::new)));
        filteredComicsList = new ObservableListWrapper<>(new ArrayList<>());
        filteredComicsList.addAll(comicsList);
        setComicsDisplay();
    }

    @SuppressWarnings("DuplicatedCode")
    private void setComicsDisplay() {
        filteredComicsList.addListener((InvalidationListener) c -> {
            int newPageCount = (int) Math.ceil(filteredComicsList.size() / (double) COMICS_CONTROLS_PER_PAGE);
            newPageCount = (newPageCount > 0) ? newPageCount : 1;
            comicsPagination.setPageCount(newPageCount + 2);
            comicsPagination.setPageCount(newPageCount);
        });
    }
    
    private void bindProperties() {
        editingObject.addListener((observable, oldValue, newValue) -> {
            boolean startEditing = newValue.getKey();

            if (startEditing) {
                Parent editPane = Main.getForm("EditComicsForm");
                try {
                    EditComicsController editController = (EditComicsController) Main
                            .getControllerForForm("EditComicsForm");
                    editController.setEditingObjectReference(editingObject);
                } catch (Exception e) {
                    logger.error("Error while setting called comics control for edit controller: ", e);
                    Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("problemOccurred"));
                    Main.errorAlert.showAndWait();
                    System.exit(-1);
                }
                tabComics.setContent(editPane);
            } else {
                Parent comicsContentPane = Main.getForm("ComicsForm");
                importComics();
                tabComics.setContent(comicsContentPane);
            }
        });
        bAddNewComics.visibleProperty().bind(Repository.adminIsAuthorizedProperty);
        tbComicsName.prefWidthProperty().bind(new DoubleBinding() {

            {
                bind(Repository.adminIsAuthorizedProperty);
            }

            @Override
            protected double computeValue() {
                if (Repository.adminIsAuthorizedProperty.get()) {
                    tbComicsName.setLayoutX(88);
                    return 700;
                } else {
                    tbComicsName.setLayoutX(22);
                    return 766;
                }
            }
        });
    }
    
    private void setPagination() {
        int newPageCount = (int) Math.ceil(comicsList.size() / (double) COMICS_CONTROLS_PER_PAGE);
        comicsPagination.setPageCount(newPageCount);
        comicsPagination.currentPageIndexProperty().addListener(observable -> spComicsList.setVvalue(0));
        comicsPagination.setPageFactory(pageIndex -> {
            final int controlCountOnTheLastPage = filteredComicsList.size() - pageIndex * COMICS_CONTROLS_PER_PAGE;
            final int startComicsIndex = pageIndex * COMICS_CONTROLS_PER_PAGE;
            final int endComicsIndex = startComicsIndex + Math.min(COMICS_CONTROLS_PER_PAGE, controlCountOnTheLastPage);
            ArrayList<OpenPair<Comics, ComicsPresentationControl>> animeListToPresent = new ArrayList<>(
                    filteredComicsList.subList(startComicsIndex, endComicsIndex));
            ArrayList<ComicsPresentationControl> comicsControls = new ArrayList<>();
            for (OpenPair<Comics, ComicsPresentationControl> comicsPair : animeListToPresent) {
                if (comicsPair.getValue() != null) {
                    comicsControls.add(comicsPair.getValue());
                    continue;
                }
                Comics comics = comicsPair.getKey();

                comics.addInvalidationListener(observable -> {
                    ComicsPresentationControl presentationControl = comicsPair.getValue();
                    presentationControl.setComics(comics);
                    presentationControl.loadImage();
                });

                ComicsPresentationControl comicsControl = new ComicsPresentationControl();
                comicsControl.logger = logger;
                comicsControl.load();
                comicsControl.setComics(comics);
                comicsControl.loadImage();
                comicsControl.isDeleted.addListener(observable -> {
                    logger.info("Comics: " + comics.getName() + " was deleted");
                    comicsList.remove(comicsPair);
                    filteredComicsList.remove(comicsPair);
                });
                comicsControl.infoCallHandled.addListener((observable, oldValue, newValue) -> {
                    boolean startLooking = !newValue;

                    if (startLooking) {
                        Parent editPane = Main.getForm("ComicsInfoForm");
                        try {
                            ComicsInfoController infoController = (ComicsInfoController) Main
                                    .getControllerForForm("ComicsInfoForm");
                            infoController.setComicsControllerReference(comicsControl);
                        } catch (Exception e) {
                            logger.error("Error while setting called comics control for info controller: ", e);
                            Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("problemOccurred"));
                            Main.errorAlert.showAndWait();
                            System.exit(-1);
                        }
                        tabComics.setContent(editPane);
                    } else {
                        Parent comicsContentPane = Main.getForm("ComicsForm");
                        importComics();
                        tabComics.setContent(comicsContentPane);
                    }
                });
                comicsControl.setEditingObjectReference(editingObject);
                comicsControls.add(comicsControl);

                comicsPair.setValue(comicsControl);
            }

            VBox pane = new VBox();
            pane.setPrefHeight(COMICS_CONTROL_HEIGHT * COMICS_CONTROLS_PER_PAGE);
            pane.getChildren().addAll(comicsControls);
            return pane;
        });
    }

    private void setUpFiltrationData() {
        setUpGenres();
        setComicsTypes();
        setComicsStatuses();
        setComicsYearBoxes();
    }

    private void setComicsYearBoxes() {
        ChangeListener<String> tbChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue.matches("[0-9]{0,4}")) {
                ((StringProperty) observable).setValue(oldValue);
            }
        };
        tbYearFrom.textProperty().addListener(tbChangeListener);
        tbYearTo.textProperty().addListener(tbChangeListener);
    }

    @SuppressWarnings("DuplicatedCode")
    private void setComicsStatuses() {
        ArrayList<String> comicsStatuses = new ArrayList<>();
        for (Status status : Status.values()) {
            comicsStatuses.add(Repository.instance.getNamesBundleValue(status.name));
        }
        comicsStatuses.add(Repository.instance.getNamesBundleValue("ignoreValue"));
        cbComicsStatuses.getItems().addAll(comicsStatuses);
    }

    private void setComicsTypes() {
        ArrayList<String> comicsTypes = new ArrayList<>();
        for (Comics.Type value : Comics.Type.values()) {
            comicsTypes.add(Repository.instance.getNamesBundleValue(value.name));
        }
        comicsTypes.add(Repository.instance.getNamesBundleValue("ignoreValue"));
        cbComicsTypes.getItems().addAll(comicsTypes);
    }

    private void setUpGenres() {
        lvGenres.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lvGenres.getItems().addAll(Repository.instance.getGenres(false).stream()
                .map(genre -> genre.name)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @SuppressWarnings("DuplicatedCode")
    @FXML
    private void onBFilter() {
        if (!filterFieldsAreValidated()) return;

        filteredComicsList.clear();

        ArrayList<Pair<Comics, Genre>> comicsGenreMap = Repository.instance.getComicsGenreMap(true);

        ArrayList<String> selectedGenres = new ArrayList<>(lvGenres.getSelectionModel().getSelectedItems());
        String selectedType = cbComicsTypes.getSelectionModel().getSelectedItem();
        String selectedStatus = cbComicsStatuses.getSelectionModel().getSelectedItem();
        int selectedYearFrom = tbYearFrom.getText().isEmpty() ? 0 : Integer.parseInt(tbYearFrom.getText());
        int selectedYearTo = tbYearTo.getText().isEmpty() ? 9999 : Integer.parseInt(tbYearTo.getText());

        filteredComicsList.addAll(
                comicsList
                        .stream()
                        .filter(comicsPair -> {
            Comics comics = comicsPair.getKey();

            boolean containGenre = false;
            boolean correctType = false;
            boolean correctStatus = false;
            boolean correctYear = false;

            if (selectedGenres.isEmpty() ||
                    comicsGenreMap.stream().anyMatch(comicsGenrePair -> {
                        boolean currentComics = (comicsGenrePair.getKey().equals(comics));
                        boolean containsAtLeastOneSelectedGenre = selectedGenres.contains(comicsGenrePair.getValue().name);
                        return currentComics && containsAtLeastOneSelectedGenre;
                    }))
                containGenre = true;

            if (selectedType == null ||
                    selectedType.equals(Repository.instance.getNamesBundleValue("ignoreValue")) ||
                    selectedType.equals(Repository.instance.getNamesBundleValue(comics.getType().name)))
                correctType = true;

            if (selectedStatus == null ||
                    selectedStatus.equals(Repository.instance.getNamesBundleValue("ignoreValue")) ||
                    selectedStatus.equals(Repository.instance.getNamesBundleValue(comics.getStatus().name)))
                correctStatus = true;

            if (comics.getPremiereYear() >= selectedYearFrom && comics.getPremiereYear() <= selectedYearTo)
                correctYear = true;

            return containGenre && correctType && correctStatus && correctYear;
        })
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @SuppressWarnings("DuplicatedCode")
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

    @SuppressWarnings("DuplicatedCode")
    @FXML
    private void onBSearchByNameClick() {
        String comicsName = tbComicsName.getText();

        if (comicsName.matches("[ ]+")) return;

        filteredComicsList.clear();

        filteredComicsList.addAll(comicsList.stream().filter(comics -> {
            boolean nameContainsSearchedSymbols = comics.getKey().getName().contains(comicsName);
            boolean anyAltNameContainsSearchedSymbols = comics.getKey().getAltNames().stream()
                    .anyMatch(altName -> altName.contains(comicsName));
            return nameContainsSearchedSymbols || anyAltNameContainsSearchedSymbols;
        })
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @FXML
    private void onBAddNewComicsClick() {
        editingObject.set(new OpenPair<>(true, null));
    }
}
