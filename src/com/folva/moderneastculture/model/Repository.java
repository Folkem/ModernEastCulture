package com.folva.moderneastculture.model;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.dto.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"RedundantIfStatement"})
public class Repository {

    private static final Logger logger = LogManager.getLogger(Repository.class);
    private static final Preferences prefs = Preferences.userRoot().node("com/folva/moderneastculture/config");
    private static final String PREFS_ADMIN_LOGIN = "superadmin_login";
    private static final String PREFS_ADMIN_PASSWORD = "superadmin_password";
    private static final String DEFAULT_ADMIN_LOGIN = "somebody@oncetold.me";
    private static final String DEFAULT_ADMIN_PASSWORD = "simplePassword";
    private static final long MAX_CACHED_TIME;

    public static final Repository instance;
    public static final int FIRST_ANIME_PREMIERE_YEAR = 1958;
    public static final int FIRST_MANGA_PREMIERE_YEAR = 1900;
    public static final ResourceBundle namesBundle;
    public static final SimpleBooleanProperty adminIsAuthorizedProperty;
    public static final String DB_IMAGES_FOLDER = "db_img";

    private static String ADMIN_LOGIN;
    private static String ADMIN_PASSWORD;

    private final DbRepository dbRepository;

    private Pair<ArrayList<Genre>, Long> cachedGenres;
    private Pair<ArrayList<Anime>, Long> cachedAnime;
    private Pair<ArrayList<Author>, Long> cachedAuthors;
    private Pair<ArrayList<Pair<Anime, Genre>>, Long> cachedAnimeGenreMap;
    private Pair<ArrayList<Pair<Integer, String>>, Long> cachedAnimeImagePaths;

    static {
        instance = new Repository();

        ADMIN_LOGIN = prefs.get(PREFS_ADMIN_LOGIN, DEFAULT_ADMIN_LOGIN);
        ADMIN_PASSWORD = prefs.get(PREFS_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD);

        if (ADMIN_LOGIN.equalsIgnoreCase(DEFAULT_ADMIN_LOGIN))
            prefs.put(PREFS_ADMIN_LOGIN, DEFAULT_ADMIN_LOGIN);
        if (ADMIN_PASSWORD.equalsIgnoreCase(DEFAULT_ADMIN_PASSWORD))
            prefs.put(PREFS_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD);

        namesBundle = ResourceBundle.getBundle("ui_names", Locale.getDefault(), new ResourceBundle.Control() {
            @Override
            public ResourceBundle newBundle
                    (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                    throws IOException {
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, "properties");
                ResourceBundle bundle = null;
                InputStream stream = null;
                if (reload) {
                    URL url = loader.getResource(resourceName);
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        if (connection != null) {
                            connection.setUseCaches(false);
                            stream = connection.getInputStream();
                        }
                    }
                } else {
                    stream = loader.getResourceAsStream(resourceName);
                }
                if (stream != null) {
                    try {
                        // Only this line is changed to make it to read properties files as UTF-8.
                        bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    } finally {
                        stream.close();
                    }
                }
                return bundle;
            }
        });

        adminIsAuthorizedProperty = new SimpleBooleanProperty(false);

        MAX_CACHED_TIME = Duration.ofMinutes(15).toMillis();
    }

    private Repository() {
        dbRepository = new DbRepository();
    }

    public void connectToDatabase() {
        dbRepository.connect();
    }

    public void disconnectFromDatabase() {
        dbRepository.disconnect();
    }

    public boolean loginIsCorrect(String login) {
        if (ADMIN_LOGIN.equals(login)) return true;

        return false;
    }

    public boolean passwordIsCorrect(String password) {
        if (ADMIN_PASSWORD.equals(password)) return true;

        return false;
    }

    public void updateAdminLogin(String newLogin) {
        if (loginIsValid(newLogin)) {
            ADMIN_LOGIN = newLogin;
            prefs.put(PREFS_ADMIN_LOGIN, ADMIN_LOGIN);
        }
    }

    public void updateAdminPassword(String newPassword) {
        if (passwordIsValid(newPassword)) {
            ADMIN_PASSWORD = newPassword;
            prefs.put(PREFS_ADMIN_PASSWORD, ADMIN_PASSWORD);
        }
    }

    public boolean loginIsValid(String login) {
        if (login == null || !login.matches("[a-zA-Z0-9@.]{3,20}")) return false;

        return true;
    }

    public boolean passwordIsValid(String password) {
        if (password == null || !password.matches("[a-zA-Z0-9]{3,20}")) return false;

        return true;
    }

    public String getNamesBundleValue(String key) {
        if (key == null) return "";

        return namesBundle.getString(key);
    }

    public Path imageWasCopied(File sourceFile) {
        Path destinationFilePath = null;

        String extension = getFileExtension(sourceFile);
        String randomName = String.valueOf(System.currentTimeMillis())
                .substring(0, 10).concat(".").concat(extension);
        File destinationFile = Paths.get(DB_IMAGES_FOLDER, randomName).toFile();

        try {
            handleDirectoryExistence();
            destinationFilePath = Files.copy(sourceFile.toPath(), destinationFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Error while copying image file: ", e);
            Main.warningAlert.setContentText(Repository.instance.getNamesBundleValue("imageCopyError"));
            Main.warningAlert.show();
        }
        return destinationFilePath;
    }

    public void handleDirectoryExistence() {
        File directory = Paths.get(DB_IMAGES_FOLDER).toFile();
        if (!directory.exists()) {
            try {
                Files.createDirectory(directory.toPath());
            } catch (IOException e) {
                logger.fatal("Error while creating db_img folder: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("imageCopyError"));
                Main.errorAlert.showAndWait();
                System.exit(-1);
            }
        }
    }

    public ArrayList<Genre> getGenres(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedGenres == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedGenres.getValue() > MAX_CACHED_TIME) {
            cachedGenres = new Pair<>(dbRepository.getGenres(), currentTimeMillis);
        }

        return cachedGenres.getKey();
    }

    public ArrayList<Anime> getAnimes(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedAnime == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedAnime.getValue() > MAX_CACHED_TIME) {
            cachedAnime = new Pair<>(dbRepository.getAnimes(), currentTimeMillis);
        }

        return cachedAnime.getKey();
    }

    public ArrayList<Author> getAuthors(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedAuthors == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedAuthors.getValue() > MAX_CACHED_TIME) {
            cachedAuthors = new Pair<>(dbRepository.getAuthors(), currentTimeMillis);
        }

        return cachedAuthors.getKey();
    }

    public ArrayList<Pair<Anime, Genre>> getAnimeGenreMap(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedAnimeGenreMap == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedAnimeGenreMap.getValue() > MAX_CACHED_TIME) {
            cachedAnimeGenreMap = new Pair<>(dbRepository.getAnimeGenreMap(), currentTimeMillis);
        }

        return cachedAnimeGenreMap.getKey();
    }

    public ArrayList<Pair<Integer, String>> getAnimeImagePaths(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedAnimeImagePaths == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedAnimeImagePaths.getValue() > MAX_CACHED_TIME) {
            cachedAnimeImagePaths = new Pair<>(dbRepository.getAnimeImagePaths(), currentTimeMillis);
        }

        return cachedAnimeImagePaths.getKey();
    }

    public void updateAnimeCache() {
        getGenres(true);
        getAnimes(true);
        getAnimeGenreMap(true);
        getAnimeImagePaths(true);
    }

    public void deleteAnimeFromDb(Anime key) {
        dbRepository.deleteAnime(key);
    }

    public void updateAnimeGenres(Pair<Anime, ArrayList<Genre>> animeGenres) {
        dbRepository.updateAnimeGenres(animeGenres);
    }

    public void updateAnimeImages(Pair<Anime, ArrayList<String>> animeImages) {
        dbRepository.updateAnimeImages(animeImages);
    }

    private static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else return "";
    }

    public void insertNewAnime(Anime newAnime) {
        dbRepository.insertNewAnime(newAnime);
    }

    private static final class DbRepository {

        private static final String CONNECTION_URL = "jdbc:sqlite:modern_east_culture.db3";

        private enum GenreFields {
            ID("id", 1),
            NAME("name", 2);

            public final String columnName;
            public final int columnIndex;

            GenreFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        private enum AnimeFields {
            ID("id", 1),
            ID_AUTHOR("id_author", 2),
            TYPE("type", 3),
            NAME("name", 4),
            DESCRIPTION("description", 5),
            EPISODE_COUNT("episode_count", 6),
            SOURCE("source", 7),
            ID_RATING("id_rating", 8),
            PREMIERE_YEAR("premiere_year", 9),
            PREMIERE_SEASON("premiere_season", 10),
            STATUS("status", 11);

            public final String columnName;
            public final int columnIndex;

            AnimeFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        private enum AuthorFields {
            ID("id", 1),
            TYPE("type", 2),
            NAME("name", 3);

            public final String columnName;
            public final int columnIndex;

            AuthorFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        private enum AgeRatingFields {
            ID("id", 1),
            NAME("name", 2),
            DESCRIPTION("description", 3);

            public final String columnName;
            public final int columnIndex;

            AgeRatingFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        private enum AnimeGenreFields {
            ID_ANIME("id_anime", 1),
            ID_GENRE("id_genre", 2);

            public final String columnName;
            public final int columnIndex;

            AnimeGenreFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        private enum AnimeAltNameFields {
            ID_ANIME("id_anime", 1),
            ALTERNATIVE_NAME("alternative_name", 2);

            public final String columnName;
            public final int columnIndex;

            AnimeAltNameFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        private enum AnimeGalleryFields {
            ID_ANIME("id_anime", 1),
            IMG_PATH("img_path", 2);

            public final String columnName;
            public final int columnIndex;

            AnimeGalleryFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        public Connection connection;

        /*
         * Connection/disconnection
         */

        public void connect() {
            try {
                connection = DriverManager.getConnection(CONNECTION_URL);
                logger.info("Database connection is successful.");
            } catch (SQLException e) {
                logger.error("Database connection was refused: ", e);
            }
        }

        public void disconnect() {
            try {
                if (connection != null)
                    connection.close();
                logger.info("Database was successfully disconnected.");
            } catch (SQLException e) {
                logger.error("Database wasn't disconnected: ", e);
            }
        }

        /*
         * Methods which return the output of "select" statements
         */

        public ArrayList<Anime> getAnimes() {
            ArrayList<Anime> animeList = new ArrayList<>();

            ArrayList<Author> authors = getAuthors();
            ArrayList<AgeRating> ageRatings = getAgeRatings();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from anime");
                while (resultSet.next()) {
                    int animeId = resultSet.getInt(AnimeFields.ID.columnName);

                    int animeAuthorId = resultSet.getInt(AnimeFields.ID_AUTHOR.columnName);
                    Stream<Author> authorStream = authors.stream().filter(
                            author -> author.getId() == animeAuthorId);
                    Optional<Author> optionalAuthor = authorStream.findFirst();
                    Author animeAuthor = optionalAuthor.orElse(null);

                    String animeTypeString = resultSet.getString(AnimeFields.TYPE.columnName);
                    Anime.Type animeType = Anime.Type.valueOf(animeTypeString.toUpperCase());

                    String animeName = resultSet.getString(AnimeFields.NAME.columnName);
                    String animeDescription = resultSet.getString(AnimeFields.DESCRIPTION.columnName);
                    int animeEpisodeCount = resultSet.getInt(AnimeFields.EPISODE_COUNT.columnName);

                    String animeSourceString = resultSet.getString(AnimeFields.SOURCE.columnName);
                    Anime.Source animeSource = Anime.Source.valueOf(animeSourceString.toUpperCase());

                    int animeAgeRatingId = resultSet.getInt(AnimeFields.ID_RATING.columnName);
                    Stream<AgeRating> ageRatingStream = ageRatings.stream().filter(
                            ageRating -> ageRating.id == animeAgeRatingId);
                    Optional<AgeRating> optionalAgeRating = ageRatingStream.findFirst();
                    AgeRating animeAgeRating = optionalAgeRating.orElse(null);

                    int animePremiereYear = resultSet.getInt(AnimeFields.PREMIERE_YEAR.columnName);

                    String animePremiereSeasonString = resultSet.getString(AnimeFields.PREMIERE_SEASON.columnName);
                    YearSeason animePremiereSeason = YearSeason.valueOf(animePremiereSeasonString.toUpperCase());

                    String animeStatusString = resultSet.getString(AnimeFields.STATUS.columnName);
                    Status animeStatus = Status.valueOf(animeStatusString.toUpperCase());

                    Anime newAnime = Anime.Builder.newBuilder()
                            .setId(animeId)
                            .setAuthor(animeAuthor)
                            .setType(animeType)
                            .setName(animeName)
                            .setDescription(animeDescription)
                            .setEpisodeCount(animeEpisodeCount)
                            .setSource(animeSource)
                            .setAgeRating(animeAgeRating)
                            .setPremiereYear(animePremiereYear)
                            .setPremiereSeason(animePremiereSeason)
                            .setStatus(animeStatus)
                            .build();

                    animeList.add(newAnime);
                }
                resultSet.close();
            } catch (SQLException | IllegalArgumentException e) {
                logger.error("Error while getting anime from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            setAnimeAltNames(animeList);

            return animeList;
        }

        private void setAnimeAltNames(ArrayList<Anime> animeList) {
            ArrayList<Pair<Integer, String>> allAltNames = new ArrayList<>();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from anime_alt_names");
                while (resultSet.next()) {
                    int idAnime = resultSet.getInt(AnimeAltNameFields.ID_ANIME.columnName);
                    String altName = resultSet.getString(AnimeAltNameFields.ALTERNATIVE_NAME.columnName);
                    Pair<Integer, String> newAltNameObj = new Pair<>(idAnime, altName);
                    allAltNames.add(newAltNameObj);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting anime alt names from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            animeList.forEach(anime -> allAltNames.forEach(altName -> {
                if (anime.getId() == altName.getKey())
                    anime.getAltNames().add(altName.getValue());
            }));
        }

        public ArrayList<Author> getAuthors() {
            ArrayList<Author> authors = new ArrayList<>();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from authors");
                while (resultSet.next()) {
                    int authorId = resultSet.getInt(AuthorFields.ID.columnName);
                    String authorType = resultSet.getString(AuthorFields.TYPE.columnName);
                    String authorName = resultSet.getString(AuthorFields.NAME.columnName);
                    Author newAuthor = Author.Builder.newBuilder()
                            .setId(authorId)
                            .setType(Author.Type.valueOf(authorType.toUpperCase()))
                            .setName(authorName)
                            .build();
                    authors.add(newAuthor);
                }
                resultSet.close();
            } catch (SQLException | IllegalArgumentException e) {
                logger.error("Error while getting authors from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            return authors;

        }

        public ArrayList<AgeRating> getAgeRatings() {
            ArrayList<AgeRating> ageRatings = new ArrayList<>();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from age_ratings");
                while (resultSet.next()) {
                    int ageRatingId = resultSet.getInt(AgeRatingFields.ID.columnName);
                    AgeRating ageRating = AgeRating.valueOfId(ageRatingId);
                    ageRatings.add(ageRating);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting age ratings from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            return ageRatings;
        }

        public ArrayList<Genre> getGenres() {
            ArrayList<Genre> genres = new ArrayList<>();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from genres");
                while (resultSet.next()) {
                    int genreId = resultSet.getInt(GenreFields.ID.columnName);
                    String genreName = resultSet.getString(GenreFields.NAME.columnName);
                    Genre newGenre = new Genre(genreId, genreName);
                    genres.add(newGenre);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting genres from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            return genres;
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        public ArrayList<Pair<Anime, Genre>> getAnimeGenreMap() {
            ArrayList<Pair<Anime, Genre>> animeGenres = new ArrayList<>();
            ArrayList<Anime> animes = getAnimes();
            ArrayList<Genre> genres = getGenres();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from anime_genres");
                while (resultSet.next()) {
                    int idAnime = resultSet.getInt(AnimeGenreFields.ID_ANIME.columnName);
                    int idGenre = resultSet.getInt(AnimeGenreFields.ID_GENRE.columnName);
                    Optional<Anime> optionalAnime = animes.stream().filter(anime -> anime.getId() == idAnime).findFirst();
                    Optional<Genre> optionalGenre = genres.stream().filter(genre -> genre.id == idGenre).findFirst();
                    if (!optionalAnime.isPresent() || !optionalGenre.isPresent()) continue;
                    Anime anime = optionalAnime.get();
                    Genre genre = optionalGenre.get();
                    Pair<Anime, Genre> pair = new Pair<>(anime, genre);
                    animeGenres.add(pair);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting anime genre connections: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            return animeGenres;
        }

        public ArrayList<Pair<Integer, String>> getAnimeImagePaths() {
            ArrayList<Pair<Integer, String>> animeImagePaths = new ArrayList<>();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from anime_gallery");
                while (resultSet.next()) {
                    int animeId = resultSet.getInt(AnimeGalleryFields.ID_ANIME.columnName);
                    String imgPath = resultSet.getString(AnimeGalleryFields.IMG_PATH.columnName);
                    Pair<Integer, String> newPair = new Pair<>(animeId, imgPath);
                    animeImagePaths.add(newPair);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting anime image paths from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            return animeImagePaths;
        }

        /*
         * Methods which drop data in the database
         */

        public void deleteAnime(Anime anime) {
            try {
                int animeId = anime.getId();
                PreparedStatement preparedStatement = connection.prepareStatement("delete from anime where id = ?");
                preparedStatement.setInt(AnimeFields.ID.columnIndex, animeId);
                preparedStatement.execute();
            } catch (SQLException e) {
                logger.error("Error while deleting anime from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseDeleteError"));
                Main.errorAlert.show();
            }
        }

        /*
        * Methods which add data in the database
        */

        public void insertNewAnime(Anime newAnime) {
            try {
                int animeId = newAnime.getId();
                int animeAuthorId = newAnime.getAuthor().getId();
                String animeType = newAnime.getType().name;
                String animeName = newAnime.getName();
                String animeDescription = newAnime.getDescription();
                int animeEpisodeCount = newAnime.getEpisodeCount();
                String animeSource = newAnime.getSource().name;
                int animeAgeRatingId = newAnime.getAgeRating().id;
                int animePremiereYear = newAnime.getPremiereYear();
                String animePremiereSeason = newAnime.getPremiereSeason().name;
                String animeStatus = newAnime.getStatus().name;

                String insertString = "insert into anime values (?,?,?,?,?,?,?,?,?,?,?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertString);

                insertStatement.setInt(1, animeId);
                insertStatement.setInt(2, animeAuthorId);
                insertStatement.setString(3, animeType);
                insertStatement.setString(4, animeName);
                insertStatement.setString(5, animeDescription);
                insertStatement.setInt(6, animeEpisodeCount);
                insertStatement.setString(7, animeSource);
                insertStatement.setInt(8, animeAgeRatingId);
                insertStatement.setInt(9, animePremiereYear);
                insertStatement.setString(10, animePremiereSeason);
                insertStatement.setString(11, animeStatus);

                insertStatement.execute();
            } catch (SQLException e) {
                logger.error("Error while inserting anime in db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseInsertError"));
                Main.errorAlert.show();
            }
        }

        /*
        * Methods which update (specifically "update" or drop & add) data in the database
        */

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        public void updateAnimeGenres(Pair<Anime, ArrayList<Genre>> animeGenres) {
            try {
                ArrayList<Genre> currentAnimeGenres = getAnimeGenreMap().stream().filter(
                        animeGenrePair -> animeGenrePair.getKey().getId() == animeGenres.getKey().getId())
                        .map(Pair::getValue)
                        .collect(Collectors.toCollection(ArrayList::new));
                ArrayList<Genre> genresToAdd = (ArrayList<Genre>) animeGenres.getValue().clone();
                genresToAdd.removeIf(genre -> currentAnimeGenres.stream().anyMatch(localGenre ->
                        localGenre.name.equalsIgnoreCase(genre.name)));
                ArrayList<Genre> genresToDrop = (ArrayList<Genre>) currentAnimeGenres.clone();
                genresToDrop.removeIf(genre -> animeGenres.getValue().stream().anyMatch(localGenre ->
                        localGenre.name.equalsIgnoreCase(genre.name)));

                if (genresToDrop.size() > 0) {
                    StringBuilder dropGenresBuilder = new StringBuilder("delete from anime_genres where id_anime = ? " +
                            "and id_genre in (?");
                    for (int i = 1; i < genresToDrop.size(); i++) {
                        dropGenresBuilder.append(",?");
                    }
                    dropGenresBuilder.append(")");
                    PreparedStatement preparedDropStatement = connection.prepareStatement(dropGenresBuilder.toString());
                    preparedDropStatement.setInt(1, animeGenres.getKey().getId());
                    for (int i = 1; i < genresToDrop.size() + 1; i++) {
                        preparedDropStatement.setInt(i + 1, genresToDrop.get(i - 1).id);
                    }
                    preparedDropStatement.execute();
                }

                if (genresToAdd.size() > 0) {
                    StringBuilder addGenresBuilder = new StringBuilder("insert into anime_genres values (?,?)");
                    for (int i = 1; i < genresToAdd.size(); i++) {
                        addGenresBuilder.append(",(?,?)");
                    }
                    PreparedStatement preparedAddStatement = connection.prepareStatement(addGenresBuilder.toString());
                    for (int i = 1, j = 1; i < genresToAdd.size() + 1; i++, j += 2) {
                        preparedAddStatement.setInt(j, animeGenres.getKey().getId());
                        preparedAddStatement.setInt(j + 1, genresToAdd.get(i - 1).id);
                    }
                    preparedAddStatement.execute();
                }

            } catch (Exception e) {
                logger.error("Error while updating anime genres in db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseUpdateError"));
                Main.errorAlert.show();
            }
        }

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        public void updateAnimeImages(Pair<Anime, ArrayList<String>> animeImages) {
            try {
                ArrayList<String> currentAnimeImages = getAnimeImagePaths().stream().filter(
                        animeImagePair -> animeImagePair.getKey() == animeImages.getKey().getId())
                        .map(Pair::getValue)
                        .collect(Collectors.toCollection(ArrayList::new));
                ArrayList<String> imagesToAdd = (ArrayList<String>) animeImages.getValue().clone();
                imagesToAdd.removeIf(image -> currentAnimeImages.stream().anyMatch(localImage ->
                        localImage.equalsIgnoreCase(image)));
                ArrayList<String> imagesToDrop = (ArrayList<String>) currentAnimeImages.clone();
                imagesToDrop.removeIf(image -> animeImages.getValue().stream().anyMatch(localImage ->
                        localImage.equalsIgnoreCase(image)));

                if (imagesToDrop.size() > 0) {
                    StringBuilder dropImagesBuilder = new StringBuilder("delete from anime_gallery where id_anime = ? " +
                            "and img_path in (?");
                    for (int i = 1; i < imagesToDrop.size(); i++) {
                        dropImagesBuilder.append(",?");
                    }
                    dropImagesBuilder.append(")");
                    PreparedStatement preparedDropStatement = connection.prepareStatement(dropImagesBuilder.toString());
                    preparedDropStatement.setInt(1, animeImages.getKey().getId());
                    for (int i = 1; i < imagesToDrop.size() + 1; i++) {
                        preparedDropStatement.setString(i + 1, imagesToDrop.get(i - 1));
                    }
                    preparedDropStatement.execute();
                }

                if (imagesToAdd.size() > 0) {
                    StringBuilder addImagesBuilder = new StringBuilder("insert into anime_gallery values (?,?)");
                    for (int i = 1; i < imagesToAdd.size(); i++) {
                        addImagesBuilder.append(",(?,?)");
                    }
                    PreparedStatement preparedAddStatement = connection.prepareStatement(addImagesBuilder.toString());
                    for (int i = 1, j = 1; i < imagesToAdd.size() + 1; i++, j += 2) {
                        preparedAddStatement.setInt(j, animeImages.getKey().getId());
                        preparedAddStatement.setString(j + 1, imagesToAdd.get(i - 1));
                    }
                    preparedAddStatement.execute();
                }
            } catch (Exception e) {
                logger.error("Error while updating anime images in db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseUpdateError"));
                Main.errorAlert.show();
            }
        }
    }
}
