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
import static com.folva.moderneastculture.model.Repository.loadImage;

/**
 * Підменю редагування коміксів. Відповідає за зміну та добавлення нових
 * коміксів
 */
@SuppressWarnings("DuplicatedCode")
public class EditComicsController implements Initializable {

    private static final Logger logger = LogManager.getLogger(EditComicsController.class.getName());

    @FXML
    private TextArea taName;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextArea taAltNames;
    @FXML
    private TextField tbChapterCount;
    @FXML
    private TextField tbPremiereYear;
    @FXML
    private ImageView ivFirstImage;
    @FXML
    private ListView<String> lvSources;
    @FXML
    private ListView<String> lvTypes;
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

    private final ObjectProperty<ObjectProperty<OpenPair<Boolean, Comics>>> editingObjectReference =
            new SimpleObjectProperty<>();
    private final ArrayList<String> currentComicsImagePaths = new ArrayList<>();
    private final Map<String, Image> cachedGalleryImages = new HashMap<>();
    private int currentImageIndex = 0;

    private boolean isNew = false;

    /**
     * Встановлює посилання на об'єкт-властивість стану змінення об'єкту та самого об'єкту коміксу
     * @param reference посилання на об'єкт-властивість стану змінення об'єкту та самого об'єкту коміксу
     */
    public void setEditingObjectReference(SimpleObjectProperty<OpenPair<Boolean, Comics>> reference) {
        editingObjectReference.set(reference);
        currentComicsImagePaths.clear();
        setComicsDetails();
    }

    private void setComicsDetails() {
        Comics currentComics = editingObjectReference.get().get().getValue();

        if (currentComics == null) {
            isNew = true;
            ivFirstImage.setImage(Repository.getNoImageImage());
            return;
        }

        ArrayList<Pair<Integer, String>> comicsImagePaths = Repository.instance.getComicsImagePaths(true);

        ArrayList<String> currentComicsImagePaths = comicsImagePaths
                .stream()
                .filter(comicsImage -> comicsImage.getKey() == currentComics.getId())
                .map(Pair::getValue)
                .collect(Collectors.toCollection(ArrayList::new));

        this.currentComicsImagePaths.addAll(currentComicsImagePaths);

        if (currentComicsImagePaths.isEmpty()) {
            ivFirstImage.setImage(Repository.getNoImageImage());
        } else {
            String firstImageName = currentComicsImagePaths.get(0);

            Image image = loadImage(firstImageName);

            if (image == null) {
                image = Repository.getNoImageImage();
            }

            ivFirstImage.setImage(image);
        }
        centerImage(ivFirstImage);

        taName.setText(currentComics.getName());
        taDescription.setText(currentComics.getDescription());
        String concatenatedString = currentComics.getAltNames()
                .stream()
                .reduce("", (s, s2) -> s.concat(";").concat(s2));
        String altNamesString = (concatenatedString.length() > 0) ? concatenatedString.substring(1) : "";
        taAltNames.setText(altNamesString); // remove 1 symbol because it will be ";"
        tbChapterCount.setText(String.valueOf(currentComics.getChapterCount()));
        tbPremiereYear.setText(String.valueOf(currentComics.getPremiereYear()));
        lvSources.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentComics.getSource().name));
        lvTypes.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentComics.getType().name));
        ArrayList<Genre> currentComicsGenres = Repository.instance.getComicsGenreMap(true).stream()
                .filter(comicsGenrePair -> comicsGenrePair.getKey().getId() == currentComics.getId())
                .map(Pair::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Integer> genresIdsList = currentComicsGenres
                .stream()
                .map(genre -> genre.id - 1)
                .collect(Collectors.toCollection(ArrayList::new));
        int[] genresIds = new int[genresIdsList.size()];
        for (int i = 0; i < genresIdsList.size(); i++) {
            genresIds[i] = genresIdsList.get(i);
        }
        lvGenres.getSelectionModel().clearSelection();
        lvGenres.getSelectionModel().selectIndices(-1, genresIds);
        lvStatus.getSelectionModel().select(Repository.instance.getNamesBundleValue(currentComics.getStatus().name));
        lvAuthors.getSelectionModel().select(currentComics.getAuthor().getName());

        bNextImage.setDisable(false);
        bPreviousImage.setDisable(false);
        bDeleteImage.setDisable(false);

        if (currentComicsImagePaths.size() <= 1) {
            bNextImage.setDisable(true);
            bPreviousImage.setDisable(true);
        }
        if (currentComicsImagePaths.size() == 0) {
            bDeleteImage.setDisable(true);
            ivCurrentGalleryImage.setImage(Repository.getNoImageImage());
            return;
        }

        String firstImageName = currentComicsImagePaths.get(0);
        Image galleryImage = Repository.loadImage(firstImageName);

        if (galleryImage != null) {
            ivCurrentGalleryImage.setImage(galleryImage);
            centerImage(ivCurrentGalleryImage);
        } else {
            ivCurrentGalleryImage.setImage(Repository.getNoImageImage());
        }
    }

    @FXML
    private void onBExitEditWindowClick() {
        removeRedundantImages();

        ObjectProperty<OpenPair<Boolean, Comics>> reference = editingObjectReference.get();
        reference.set(new OpenPair<>(false, null));
    }

    @SuppressWarnings("ConstantConditions")
    private void removeRedundantImages() {
        ArrayList<String> dbComicsImagePaths = Repository.instance.getComicsImagePaths(true)
                .stream()
                .map(Pair::getValue)
                .collect(Collectors.toCollection(ArrayList::new));
        Repository.instance.handleDirectoryExistence();
        String[] list = Paths.get(DB_IMAGES_FOLDER).toFile().list();
        ArrayList<String> folderComicsImagePaths = new ArrayList<>(Arrays.asList(list));
        folderComicsImagePaths.forEach(folderComicsImagePath -> {
            if (!dbComicsImagePaths.contains(folderComicsImagePath)) {
                try {
                    Files.delete(Paths.get(DB_IMAGES_FOLDER, folderComicsImagePath));
                } catch (IOException e) {
                    logger.error("Error while deleting file " + folderComicsImagePath + ": ", e);
                }
            }
        });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @FXML
    private void onBSaveClick() {
        if (!fieldsAreValidated()) return;

        String newName = taName.getText();
        String newDescription = taDescription.getText();
        ArrayList<String> newAltNames = Arrays.stream(taAltNames.getText().split(";"))
                .filter(s -> !s.matches("[ ]*")).collect(Collectors.toCollection(ArrayList::new));
        int newChapterCount = Integer.parseInt(tbChapterCount.getText());
        int newPremiereYear = Integer.parseInt(tbPremiereYear.getText());

        String newSourceString = lvSources.getSelectionModel().getSelectedItem();
        Optional<Comics.Source> optionalSource = Arrays.stream(Comics.Source.values())
                .filter(source -> Repository.instance.getNamesBundleValue(source.name).equalsIgnoreCase(newSourceString))
                .findFirst();
        Comics.Source newSource = optionalSource.orElse(Comics.Source.OTHER);

        String newTypeString = lvTypes.getSelectionModel().getSelectedItem();
        Optional<Comics.Type> optionalType = Arrays.stream(Comics.Type.values())
                .filter(type -> Repository.instance.getNamesBundleValue(type.name).equalsIgnoreCase(newTypeString))
                .findFirst();
        Comics.Type newType = optionalType.orElse(Comics.Type.MANGA);

        String newAuthorString = lvAuthors.getSelectionModel().getSelectedItem();
        Optional<Author> optionalAuthor = Repository.instance.getAuthors(false).stream()
                .filter(author -> author.getName().equalsIgnoreCase(newAuthorString))
                .findFirst();
        Author newAuthor;
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
            Optional<Genre> optionalGenre = genres.stream()
                    .filter(genre -> {
                        Optional<String> anyGenreWithTheSameName = newGenresStringsLeftToFind.stream()
                                .filter(localNewGenreStringLeft -> localNewGenreStringLeft.equalsIgnoreCase(genre.name))
                                .findFirst();
                        return anyGenreWithTheSameName.isPresent();
                    })
                    .findFirst();
            newGenres.add(optionalGenre.get());
            newGenresStringsLeftToFind.remove(newGenreString);
        }

        String newStatusString = lvStatus.getSelectionModel().getSelectedItem();
        Optional<Status> optionalStatus = Arrays.stream(Status.values())
                .filter(status -> Repository.instance.getNamesBundleValue(status.name).equalsIgnoreCase(newStatusString))
                .findFirst();
        Status newStatus = optionalStatus.orElse(Status.ONGOING);

        if (isNew) {
            Optional<Comics> comicsWithMaxId = Repository.instance.getComics(true)
                    .stream()
                    .max(Comparator.comparingInt(Comics::getId));
            int nextId = comicsWithMaxId.get().getId() + 1;
            logger.trace(nextId);
            Comics newComics = Comics.Builder.newBuilder()
                    .setId(nextId)
                    .setAuthor(newAuthor)
                    .setName(newName)
                    .setDescription(newDescription)
                    .setChapterCount(newChapterCount)
                    .setPremiereYear(newPremiereYear)
                    .setSource(newSource)
                    .setType(newType)
                    .setAltNames(newAltNames)
                    .setStatus(newStatus)
                    .build();
            Repository.instance.insertNewComics(newComics);
            editingObjectReference.get().get().setValue(newComics);
        } else {
            Comics currentComics = editingObjectReference.get().get().getValue();
            currentComics.setName(newName);
            currentComics.setAuthor(newAuthor);
            currentComics.setDescription(newDescription);
            currentComics.setAltNames(newAltNames);
            currentComics.setChapterCount(newChapterCount);
            currentComics.setPremiereYear(newPremiereYear);
            currentComics.setSource(newSource);
            currentComics.setType(newType);
            currentComics.setStatus(newStatus);
            Repository.instance.updateComics(currentComics);
            currentComics.invalidate();
        }

        int currentComicsId = editingObjectReference.get().get().getValue().getId();
        Pair<Comics, ArrayList<Genre>> comicsGenres = new Pair<>(
                editingObjectReference.get().get().getValue(), newGenres);
        Pair<Comics, ArrayList<String>> comicsImages = new Pair<>(
                editingObjectReference.get().get().getValue(), currentComicsImagePaths);
        Repository.instance.updateComicsImages(comicsImages);
        Repository.instance.updateComicsGenres(comicsGenres);
        Repository.instance.updateComicsCache();

        logger.info(String.format("Comics with id %d was altered", currentComicsId));
    }

    private boolean fieldsAreValidated() {
        String newName = taName.getText();
        int newPremiereYear = Integer.parseInt(tbPremiereYear.getText());
        String selectedSource = lvSources.getSelectionModel().getSelectedItem();
        String selectedType = lvTypes.getSelectionModel().getSelectedItem();
        String selectedStatus = lvStatus.getSelectionModel().getSelectedItem();
        String selectedAuthor = lvAuthors.getSelectionModel().getSelectedItem();

        boolean nameIsNotEmpty = !newName.matches("[ ]*");
        boolean correctPremiereYear = newPremiereYear >= Repository.FIRST_ANIME_PREMIERE_YEAR;
        boolean correctSourceSelected = (selectedSource != null);
        boolean correctTypeSelected = (selectedType != null);
        boolean correctStatusSelected = (selectedStatus != null);
        boolean correctAuthorSelected = (selectedAuthor != null);

        StringBuilder warningBuilder = new StringBuilder();
        if (!nameIsNotEmpty) warningBuilder.append(Repository.instance.getNamesBundleValue("nameIsEmptyError"));
        if (!correctPremiereYear)
            warningBuilder.append(Repository.instance.getNamesBundleValue("animePremiereYearIsWrongError"));
        if (!correctSourceSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("sourceIsNotSelectedError"));
        if (!correctTypeSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("typeIsNotSelectedError"));
        if (!correctStatusSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("statusIsNotSelectedError"));
        if (!correctAuthorSelected)
            warningBuilder.append(Repository.instance.getNamesBundleValue("authorIsNotSelectedError"));

        warningBuilder.trimToSize();

        if (warningBuilder.length() > 0) {
            Main.warningAlert.setContentText(warningBuilder.toString());
            Main.warningAlert.show();
        }

        return nameIsNotEmpty && correctPremiereYear &&
                correctSourceSelected && correctTypeSelected &&
                correctStatusSelected && correctAuthorSelected;
    }

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

        Path destinationImagePath = Repository.instance.imageWasCopiedToDbImgFolder(file);
        if (destinationImagePath == null) return;

        InputStream fileStream = Main.getFileStream(destinationImagePath.toFile());
        Image newImage = new Image(fileStream);
        ivFirstImage.setImage(newImage);
        centerImage(ivFirstImage);

        currentComicsImagePaths.add(destinationImagePath.getFileName().toString());
        if (currentComicsImagePaths.size() == 1) {
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

    private void setGalleryImage() {
        String imageName = currentComicsImagePaths.get(currentImageIndex);
        Image image;

        if (cachedGalleryImages.containsKey(imageName)) {
            image = cachedGalleryImages.get(imageName);
        } else {
            image = Repository.loadImage(imageName);
            cachedGalleryImages.put(imageName, image);
        }

        if (image == null) {
            image = Repository.getNoImageImage();
        }

        ivCurrentGalleryImage.setImage(image);
        centerImage(ivCurrentGalleryImage);
    }

    @FXML
    private void onBPreviousImageClick() {
        if (currentImageIndex == 0) {
            currentImageIndex = currentComicsImagePaths.size() - 1;
        } else {
            currentImageIndex--;
        }

        setGalleryImage();
    }

    @FXML
    private void onBNextImageClick() {
        if (currentImageIndex == currentComicsImagePaths.size() - 1) {
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

        Path destinationImagePath = Repository.instance.imageWasCopiedToDbImgFolder(newImage);
        if (destinationImagePath == null) return;

        String newImageFileName = destinationImagePath.getFileName().toString();
        currentComicsImagePaths.add(newImageFileName);

        currentImageIndex = currentComicsImagePaths.indexOf(newImageFileName);

        setGalleryImage();

        if (currentComicsImagePaths.size() == 1) {
            bDeleteImage.setDisable(false);
        } else if (currentComicsImagePaths.size() == 2) {
            bPreviousImage.setDisable(false);
            bNextImage.setDisable(false);
        }

        logger.info("Image '" + newImageFileName + "' was added");
    }

    @FXML
    private void onBDeleteImageClick() {
        String imageNameToDelete = currentComicsImagePaths.get(currentImageIndex);

        currentComicsImagePaths.remove(imageNameToDelete);

        if (currentComicsImagePaths.size() == 0) {
            bDeleteImage.setDisable(true);
            bNextImage.setDisable(true);
            bPreviousImage.setDisable(true);
            ivCurrentGalleryImage.setImage(Repository.getNoImageImage());
            ivFirstImage.setImage(Repository.getNoImageImage());
            centerImage(ivCurrentGalleryImage);
            return;
        } else if (currentImageIndex == currentComicsImagePaths.size()) {
            currentImageIndex--;
        }

        setGalleryImage();

        logger.info("Image '" + imageNameToDelete + "' was deleted");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUpTextBoxes();
        setUpListViews();
    }

    private void setUpListViews() {
        lvSources.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvSources.getItems().clear();
        lvSources.getItems().addAll(Arrays.stream(Comics.Source.values())
                .map(source -> Repository.instance.getNamesBundleValue(source.name))
                .collect(Collectors.toCollection(ArrayList::new)));

        lvTypes.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvTypes.getItems().clear();
        lvTypes.getItems().addAll(Arrays.stream(Comics.Type.values())
                .map(type -> Repository.instance.getNamesBundleValue(type.name))
                .collect(Collectors.toCollection(ArrayList::new)));

        lvGenres.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lvGenres.getItems().clear();
        lvGenres.getItems().addAll(Repository.instance.getGenres(false).stream()
                .map(genre -> genre.name).collect(Collectors.toCollection(ArrayList::new)));

        lvStatus.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvStatus.getItems().clear();
        lvStatus.getItems().addAll(Arrays.stream(Status.values())
                .map(status -> Repository.instance.getNamesBundleValue(status.name))
                .collect(Collectors.toCollection(ArrayList::new)));

        lvAuthors.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvAuthors.getItems().clear();
        lvAuthors.getItems().addAll(Repository.instance.getAuthors(false).stream()
                .map(Author::getName)
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
        tbChapterCount.textProperty().addListener(tbChangeListener);
        tbPremiereYear.textProperty().addListener(tbChangeListener);
    }
}
