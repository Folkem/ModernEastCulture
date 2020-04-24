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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
    @FXML
    private ListView<String> lvAuthors;
    @FXML
    private ImageView ivCurrentGalleryImage;
    @FXML
    private Button bDeleteImage;
    @FXML
    private Button bPreviousImage;
    @FXML
    private Button bNextImage;

    private final ObjectProperty<ObjectProperty<OpenPair<Boolean, Anime>>> editingObjectReference =
            new SimpleObjectProperty<>();
    private final ArrayList<String> currentAnimeImagePaths = new ArrayList<>();
    private final Map<String, Image> cachedGalleryImages = new HashMap<>();
    private int currentImageIndex = 0;

    private boolean isNew = false;

    public void setEditingObjectReference(SimpleObjectProperty<OpenPair<Boolean, Anime>> reference) {
        editingObjectReference.set(reference);
        currentAnimeImagePaths.clear();
        setAnimeDetails();
    }

    private void setAnimeDetails() {
        Anime currentAnime = editingObjectReference.get().get().getValue();

        if (currentAnime == null) {
            isNew = true;
            URL resource = Main.getResource("/res/img/no_image.jpg");
            ivFirstImage.setImage(new Image(resource.toString()));
            return;
        }

        ArrayList<Pair<Integer, String>> animeImagePaths = Repository.instance.getAnimeImagePaths(true);

        ArrayList<String> currentAnimeImagePaths = animeImagePaths.stream().filter(animeImage ->
                animeImage.getKey() == currentAnime.getId())
                .map(Pair::getValue)
                .collect(Collectors.toCollection(ArrayList::new));

        this.currentAnimeImagePaths.addAll(currentAnimeImagePaths);

        if (currentAnimeImagePaths.isEmpty()) {
            URL resource = Main.getResource("/res/img/no_image.jpg");
            ivFirstImage.setImage(new Image(resource.toString()));
        } else {
            String firstImageName = currentAnimeImagePaths.get(0);
            Path firstImagePath = Paths.get(DB_IMAGES_FOLDER, firstImageName);
            File firstImage = firstImagePath.toFile();

            Image image;

            if (firstImage.exists()) {
                InputStream fileStream;
                fileStream = Main.getFileStream(firstImage);
                image = new Image(fileStream);
                try {
                    fileStream.close();
                } catch (Exception e) {
                    logger.error("Error while closing image file stream: ", e);
                }
            } else {
                image = getNoImageImage();
            }

            ivFirstImage.setImage(image);
        }
        centerImage(ivFirstImage);

        taName.setText(currentAnime.getName());
        taDescription.setText(currentAnime.getDescription());
        String concatenatedString = currentAnime.getAltNames().stream()
                .reduce("", (s, s2) -> s.concat(";").concat(s2));
        String altNamesString = (concatenatedString.length() > 0) ? concatenatedString.substring(1) : "";
        taAltNames.setText(altNamesString); // remove 1 symbol because it will be ";"
        tbEpisodeCount.setText(String.valueOf(currentAnime.getEpisodeCount()));
        tbPremiereYear.setText(String.valueOf(currentAnime.getPremiereYear()));
        lvAgeRatings.getSelectionModel().select(currentAnime.getAgeRating().name);
        lvSources.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentAnime.getSource().name));
        lvTypes.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentAnime.getType().name));
        lvPremiereSeasons.getSelectionModel().select(
                Repository.instance.getNamesBundleValue(currentAnime.getPremiereSeason().name));
        ArrayList<Genre> currentAnimeGenres = Repository.instance.getAnimeGenreMap(true).stream()
                .filter(animeGenrePair -> animeGenrePair.getKey().getId() == currentAnime.getId())
                .map(Pair::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Integer> genresIdsList = currentAnimeGenres.stream().map(genre ->
                genre.id - 1).collect(Collectors.toCollection(ArrayList::new));
        int[] genresIds = new int[genresIdsList.size()];
        for (int i = 0; i < genresIdsList.size(); i++) {
            genresIds[i] = genresIdsList.get(i);
        }
        lvGenres.getSelectionModel().clearSelection();
        lvGenres.getSelectionModel().selectIndices(-1, genresIds);
        lvStatus.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentAnime.getStatus().name));
        lvAuthors.getSelectionModel().select(currentAnime.getAuthor().getName());

        bNextImage.setDisable(false);
        bPreviousImage.setDisable(false);
        bDeleteImage.setDisable(false);

        if (currentAnimeImagePaths.size() <= 1) {
            bNextImage.setDisable(true);
            bPreviousImage.setDisable(true);
        }
        if (currentAnimeImagePaths.size() == 0) {
            bDeleteImage.setDisable(true);
            return;
        }

        String firstImageName = currentAnimeImagePaths.get(0);
        Image galleryImage = loadImage(firstImageName);

        if (galleryImage != null) {
            ivCurrentGalleryImage.setImage(galleryImage);
            centerImage(ivCurrentGalleryImage);
        } else {
            ivCurrentGalleryImage.setImage(getNoImageImage());
        }
    }

    @FXML
    private void onBExitEditWindowClick() {
        removeRedundantImages();

        ObjectProperty<OpenPair<Boolean, Anime>> reference = editingObjectReference.get();
        reference.set(new OpenPair<>(false, null));
    }

    @SuppressWarnings("ConstantConditions")
    private void removeRedundantImages() {
        ArrayList<String> dbAnimeImagePaths = Repository.instance.getAnimeImagePaths(true).stream()
                .map(Pair::getValue).collect(Collectors.toCollection(ArrayList::new));
        Repository.instance.handleDirectoryExistence();
        String[] list = Paths.get(DB_IMAGES_FOLDER).toFile().list();
        ArrayList<String> folderAnimeImagePaths = new ArrayList<>(Arrays.asList(list));
        folderAnimeImagePaths.forEach(folderAnimeImagePath -> {
            if (!dbAnimeImagePaths.contains(folderAnimeImagePath)) {
                try {
                    Files.delete(Paths.get(DB_IMAGES_FOLDER, folderAnimeImagePath));
                } catch (IOException e) {
                    logger.error("Error while deleting file " + folderAnimeImagePath + ": ", e);
                }
            }
        });
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "DuplicatedCode"})
    @FXML
    private void onBSaveClick() {
        if (!fieldsAreValidated()) return;

        String newName = taName.getText();
        String newDescription = taDescription.getText();
        ArrayList<String> newAltNames = Arrays.stream(taAltNames.getText().split(";"))
                .filter(s -> !s.matches("[ ]*")).collect(Collectors.toCollection(ArrayList::new));
        int newEpisodeCount = Integer.parseInt(tbEpisodeCount.getText());
        int newPremiereYear = Integer.parseInt(tbPremiereYear.getText());

        String newAgeRatingString = lvAgeRatings.getSelectionModel().getSelectedItem();
        Optional<AgeRating> optionalAgeRating = Arrays.stream(AgeRating.values()).filter(ageRating ->
                ageRating.name.equalsIgnoreCase(newAgeRatingString)).findFirst();
        AgeRating newAgeRating = optionalAgeRating.orElse(AgeRating.UN);

        String newSourceString = lvSources.getSelectionModel().getSelectedItem();
        Optional<Anime.Source> optionalSource = Arrays.stream(Anime.Source.values()).filter(source ->
                Repository.instance.getNamesBundleValue(source.name).equalsIgnoreCase(newSourceString)).findFirst();
        Anime.Source newSource = optionalSource.orElse(Anime.Source.OTHER);

        String newTypeString = lvTypes.getSelectionModel().getSelectedItem();
        Optional<Anime.Type> optionalType = Arrays.stream(Anime.Type.values()).filter(type ->
                Repository.instance.getNamesBundleValue(type.name).equalsIgnoreCase(newTypeString)).findFirst();
        Anime.Type newType = optionalType.orElse(Anime.Type.SERIES);

        String newPremiereSeasonString = lvPremiereSeasons.getSelectionModel().getSelectedItem();
        Optional<YearSeason> optionalYearSeason = Arrays.stream(YearSeason.values()).filter(yearSeason ->
                Repository.instance.getNamesBundleValue(yearSeason.name)
                        .equalsIgnoreCase(newPremiereSeasonString)).findFirst();
        YearSeason newPremiereSeason = optionalYearSeason.orElse(YearSeason.UNDEFINED);

        String newAuthorString = lvAuthors.getSelectionModel().getSelectedItem();
        Optional<Author> optionalAuthor = Repository.instance.getAuthors(false).stream().filter(author ->
                author.getName().equalsIgnoreCase(newAuthorString)).findFirst();
        Author newAuthor = null;
        try {
            newAuthor = optionalAuthor.get();
        } catch (Exception e) {
            logger.fatal("Error while finding author from repository: ", e);
            Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("fatalError"));
            Main.errorAlert.showAndWait();
            System.exit(-1);
            return;
        }

        ArrayList<String> newGenresStrings = new ArrayList<>(lvGenres.getSelectionModel().getSelectedItems());
        ArrayList<String> newGenresStringsLeftToFind = new ArrayList<>(lvGenres.getSelectionModel().getSelectedItems());
        ArrayList<Genre> genres = Repository.instance.getGenres(false);
        ArrayList<Genre> newGenres = new ArrayList<>();
        for (String newGenreString : newGenresStrings) {
            Optional<Genre> optionalGenre = genres.stream().filter(genre -> {
                Optional<String> anyGenreWithTheSameName = newGenresStringsLeftToFind.stream()
                        .filter(localNewGenreStringLeft -> localNewGenreStringLeft.equalsIgnoreCase(genre.name))
                        .findFirst();
                return anyGenreWithTheSameName.isPresent();
            }).findFirst();
            newGenres.add(optionalGenre.get());
            newGenresStringsLeftToFind.remove(newGenreString);
        }

        String newStatusString = lvStatus.getSelectionModel().getSelectedItem();
        Optional<Status> optionalStatus = Arrays.stream(Status.values()).filter(status ->
                Repository.instance.getNamesBundleValue(status.name).equalsIgnoreCase(newStatusString)).findFirst();
        Status newStatus = optionalStatus.orElse(Status.ONGOING);

        if (isNew) {
            Optional<Anime> animeWithMaxId = Repository.instance.getAnimes(true).stream()
                    .max(Comparator.comparingInt(Anime::getId));
            int nextId = animeWithMaxId.get().getId() + 1;
            logger.trace(nextId);
            Anime newAnime = Anime.Builder.newBuilder()
                    .setId(nextId)
                    .setAuthor(newAuthor)
                    .setName(newName)
                    .setDescription(newDescription)
                    .setEpisodeCount(newEpisodeCount)
                    .setPremiereYear(newPremiereYear)
                    .setAgeRating(newAgeRating)
                    .setSource(newSource)
                    .setType(newType)
                    .setPremiereSeason(newPremiereSeason)
                    .setAltNames(newAltNames)
                    .setStatus(newStatus)
                    .build();
            Repository.instance.insertNewAnime(newAnime);
            editingObjectReference.get().get().setValue(newAnime);
        } else {
            Anime currentAnime = editingObjectReference.get().get().getValue();
            currentAnime.setName(newName);
            currentAnime.setAuthor(newAuthor);
            currentAnime.setDescription(newDescription);
            currentAnime.setAltNames(newAltNames);
            currentAnime.setEpisodeCount(newEpisodeCount);
            currentAnime.setPremiereYear(newPremiereYear);
            currentAnime.setAgeRating(newAgeRating);
            currentAnime.setSource(newSource);
            currentAnime.setType(newType);
            currentAnime.setPremiereSeason(newPremiereSeason);
            currentAnime.setStatus(newStatus);
            currentAnime.invalidate();
        }

        int currentAnimeId = editingObjectReference.get().get().getValue().getId();
        Pair<Anime, ArrayList<Genre>> animeGenres = new Pair<>(
                editingObjectReference.get().get().getValue(), newGenres);
        Pair<Anime, ArrayList<String>> animeImages = new Pair<>(
                editingObjectReference.get().get().getValue(), currentAnimeImagePaths);
        Repository.instance.updateAnimeImages(animeImages);
        Repository.instance.updateAnimeGenres(animeGenres);
        Repository.instance.updateAnimeCache();

        logger.info(String.format("Anime with id %d was altered", currentAnimeId));
    }

    @SuppressWarnings("DuplicatedCode")
    private boolean fieldsAreValidated() {
        String newName = taName.getText();
        int newPremiereYear = Integer.parseInt(tbPremiereYear.getText());
        String selectedAgeRating = lvAgeRatings.getSelectionModel().getSelectedItem();
        String selectedSource = lvSources.getSelectionModel().getSelectedItem();
        String selectedType = lvTypes.getSelectionModel().getSelectedItem();
        String selectedPremiereSeason = lvPremiereSeasons.getSelectionModel().getSelectedItem();
        String selectedStatus = lvStatus.getSelectionModel().getSelectedItem();
        String selectedAuthor = lvAuthors.getSelectionModel().getSelectedItem();

        boolean nameIsNotEmpty = !newName.matches("[ ]*");
        boolean correctPremiereYear = newPremiereYear >= Repository.FIRST_ANIME_PREMIERE_YEAR;
        boolean correctAgeSelected = (selectedAgeRating != null);
        boolean correctSourceSelected = (selectedSource != null);
        boolean correctTypeSelected = (selectedType != null);
        boolean correctPremiereSeasonSelected = (selectedPremiereSeason != null);
        boolean correctStatusSelected = (selectedStatus != null);
        boolean correctAuthorSelected = (selectedAuthor != null);

        StringBuilder warningBuilder = new StringBuilder();
        if (!nameIsNotEmpty) warningBuilder.append(Repository.instance.getNamesBundleValue("nameIsEmptyError"));
        if (!correctPremiereYear)
            warningBuilder.append(Repository.instance.getNamesBundleValue("animePremiereYearIsWrongError"));
        if (!correctAgeSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("ageRatingIsNotSelectedError"));
        if (!correctSourceSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("sourceIsNotSelectedError"));
        if (!correctTypeSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("typeIsNotSelectedError"));
        if (!correctPremiereSeasonSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("premiereSeasonIsNotSelectedError"));
        if (!correctStatusSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("statusIsNotSelectedError"));
        if (!correctAuthorSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("authorIsNotSelectedError"));

        warningBuilder.trimToSize();

        if (warningBuilder.length() > 0) {
            Main.warningAlert.setContentText(warningBuilder.toString());
            Main.warningAlert.show();
        }

        return nameIsNotEmpty && correctPremiereYear && correctAgeSelected &&
                correctSourceSelected && correctTypeSelected && correctPremiereSeasonSelected &&
                correctStatusSelected && correctAuthorSelected;
    }

    @SuppressWarnings("DuplicatedCode")
    private void centerImage(ImageView imageView) {
        Image img = imageView.getImage();
        if (img != null) {
            double ratioX = imageView.getFitWidth() / img.getWidth();
            double ratioY = imageView.getFitHeight() / img.getHeight();

            double reducCoeff = Math.min(ratioX, ratioY);

            double width = img.getWidth() * reducCoeff;
            double height = img.getHeight() * reducCoeff;

            imageView.setX((imageView.getFitWidth() - width) / 2);
            imageView.setY((imageView.getFitHeight() - height) / 2);

        }
    }

    @FXML
    private void onBChooseFirstImageClick() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(Main.stage);
        if (file == null) return;

        Path destinationImagePath = Repository.instance.imageWasCopied(file);
        if (destinationImagePath == null) return;

        InputStream fileStream = Main.getFileStream(destinationImagePath.toFile());
        Image newImage = new Image(fileStream);
        ivFirstImage.setImage(newImage);
        centerImage(ivFirstImage);

        currentAnimeImagePaths.add(destinationImagePath.getFileName().toString());
        if (currentAnimeImagePaths.size() == 1) {
            ivCurrentGalleryImage.setImage(newImage);
            centerImage(ivCurrentGalleryImage);
            bDeleteImage.setDisable(false);
        }

        try {
            fileStream.close();
        } catch (IOException e) {
            logger.error("Error while closing the file stream: ", e);
        }
    }

    private Image getNoImageImage() {
        URL notFoundImageResource = Main.getResource("/res/img/no_image.jpg");
        return new Image(notFoundImageResource.toString());
    }

    private void setGalleryImage() {
        String imageName = currentAnimeImagePaths.get(currentImageIndex);
        Image image;

        if (cachedGalleryImages.containsKey(imageName)) {
            image = cachedGalleryImages.get(imageName);
        } else {
            image = loadImage(imageName);
            cachedGalleryImages.put(imageName, image);
        }

        if (image == null) {
            image = getNoImageImage();
        }

        ivCurrentGalleryImage.setImage(image);
        centerImage(ivCurrentGalleryImage);
    }

    @FXML
    private void onBPreviousImageClick() {
        if (currentImageIndex == 0) {
            currentImageIndex = currentAnimeImagePaths.size() - 1;
        } else {
            currentImageIndex--;
        }

        setGalleryImage();
    }

    @FXML
    private void onBNextImageClick() {
        if (currentImageIndex == currentAnimeImagePaths.size() - 1) {
            currentImageIndex = 0;
        } else {
            currentImageIndex++;
        }

        setGalleryImage();
    }

    @FXML
    private void onBAddImageClick() {
        FileChooser fileChooser = new FileChooser();
        File newImage = fileChooser.showOpenDialog(Main.stage);
        if (newImage == null) return;

        Path destinationImagePath = Repository.instance.imageWasCopied(newImage);
        if (destinationImagePath == null) return;

        String newImageFileName = destinationImagePath.getFileName().toString();
        currentAnimeImagePaths.add(newImageFileName);

        currentImageIndex = currentAnimeImagePaths.indexOf(newImageFileName);

        setGalleryImage();

        if (currentAnimeImagePaths.size() == 1) {
            bDeleteImage.setDisable(false);
        } else if (currentAnimeImagePaths.size() == 2) {
            bPreviousImage.setDisable(false);
            bNextImage.setDisable(false);
        }

        logger.info("Image '" + newImageFileName + "' was added");
    }

    @FXML
    private void onBDeleteImageClick() {
        String imageNameToDelete = currentAnimeImagePaths.get(currentImageIndex);

        currentAnimeImagePaths.remove(imageNameToDelete);

        if (currentAnimeImagePaths.size() == 0) {
            bDeleteImage.setDisable(true);
            bNextImage.setDisable(true);
            bPreviousImage.setDisable(true);
            ivCurrentGalleryImage.setImage(getNoImageImage());
            ivFirstImage.setImage(getNoImageImage());
            centerImage(ivCurrentGalleryImage);
            return;
        } else if (currentImageIndex == currentAnimeImagePaths.size()) {
            currentImageIndex--;
        }

        setGalleryImage();

        logger.info("Image '" + imageNameToDelete + "' was deleted");
    }

    private Image loadImage(String imageName) {
        Path imagePath = Paths.get(DB_IMAGES_FOLDER, imageName);

        if (Files.notExists(imagePath)) return null;

        InputStream fileStream = Main.getFileStream(imagePath.toFile());
        Image image = new Image(fileStream);
        try {
            fileStream.close();
        } catch (Exception e) {
            logger.error("Error closing image file stream: ", e);
        }

        return image;
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
        lvGenres.getItems().addAll(Repository.instance.getGenres(false).stream().map(genre ->
                genre.name).collect(Collectors.toCollection(ArrayList::new)));

        lvStatus.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvStatus.getItems().clear();
        lvStatus.getItems().addAll(Arrays.stream(Status.values()).map(status ->
                Repository.instance.getNamesBundleValue(status.name))
                .collect(Collectors.toCollection(ArrayList::new)));

        lvAuthors.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvAuthors.getItems().clear();
        lvAuthors.getItems().addAll(Repository.instance.getAuthors(false).stream().map(Author::getName)
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
