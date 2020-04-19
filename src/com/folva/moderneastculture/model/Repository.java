package com.folva.moderneastculture.model;

import com.folva.moderneastculture.model.dto.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

@SuppressWarnings({"RedundantIfStatement", "UnnecessaryLocalVariable"})
public class Repository {

    private static final Logger logger = LogManager.getLogger(Repository.class);
    private static final Preferences prefs = Preferences.userRoot().node("com/folva/moderneastculture/config");
    private static final String CONNECTION_URL = "jdbc:sqlite:modern_east_culture.db3";
    private static final String PREFS_ADMIN_LOGIN = "superadmin_login";
    private static final String PREFS_ADMIN_PASSWORD = "superadmin_password";
    private static final String DEFAULT_ADMIN_LOGIN = "somebody@oncetold.me";
    private static final String DEFAULT_ADMIN_PASSWORD = "simplePassword";

    public static final Repository instance;
    public static final int FIRST_ANIME_PREMIERE_YEAR = 1958;
    public static final int FIRST_MANGA_PREMIERE_YEAR = 1900;
    public static final ResourceBundle namesBundle;
    public static final SimpleBooleanProperty adminIsAuthorizedProperty;

    private static String ADMIN_LOGIN;
    private static String ADMIN_PASSWORD;

    private final DbRepository dbRepository;

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

    public ArrayList<Genre> getGenres() {
        return dbRepository.getGenres();
    }

    public ArrayList<Anime> getAnimes() {
        return dbRepository.getAnimes();
    }

    public ArrayList<Pair<Anime, Genre>> getAnimeGenreMap() {
        return dbRepository.getAnimeGenreMap();
    }

    private static final class DbRepository {

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

        public Connection connection;

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
                            .setRating(animeAgeRating)
                            .setPremiereYear(animePremiereYear)
                            .setPremiereSeason(animePremiereSeason)
                            .setStatus(animeStatus)
                            .build();

                    animeList.add(newAnime);
                }
                resultSet.close();
            } catch (SQLException | IllegalArgumentException e) {
                logger.error("Error while getting anime from db: ", e);
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
                // TODO: 17.04.2020 warning window to user
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
                    Anime anime = optionalAnime.get();
                    Genre genre = optionalGenre.get();
                    Pair<Anime, Genre> pair = new Pair<>(anime, genre);
                    animeGenres.add(pair);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting anime genre connections: ", e);
            }

            return animeGenres;
        }
    }
}
