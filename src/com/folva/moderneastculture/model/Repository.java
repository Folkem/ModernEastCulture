package com.folva.moderneastculture.model;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.dto.*;
import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
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

/**
 * Сховище, яке містить всю головну інформацію та можливості роботи з базою даних. Також містить деякі
 * статичні методи-утиліти
 */
@SuppressWarnings({"RedundantIfStatement", "DuplicatedCode"})
public class Repository {

    private static final Logger logger = LogManager.getLogger(Repository.class);
    private static final Preferences prefs = Preferences.userRoot().node("com/folva/moderneastculture/config");
    private static final String PREFS_ADMIN_LOGIN = "superadmin_login";
    private static final String PREFS_ADMIN_PASSWORD = "superadmin_password";
    private static final String DEFAULT_ADMIN_LOGIN = "somebody@oncetold.me";
    private static final String DEFAULT_ADMIN_PASSWORD = "simplePassword";
    private static final long MAX_CACHED_TIME;

    /**
     * Екземпляр "сховища", захищений від породження копій
     */
    public static final Repository instance;
    /**
     * Рік виходу першого аніме
     */
    public static final int FIRST_ANIME_PREMIERE_YEAR = 1958;
    /**
     * Рік виходу першого коміксу-манги
     */
    public static final int FIRST_MANGA_PREMIERE_YEAR = 1900;
    /**
     * Об'єкт з ресурсами до магатомовного інтерфейсу - в залежності від працюючої локалі або вказаних
     * параметрів (в командній строці) щодо локалі буде використовуватися та чи інша мова в інтерфейсі.
     * Наразі доступні - українська, російська (по замовчуванню) та англійська
     */
    public static final ResourceBundle namesBundle;
    /**
     * Властивість щодо того, чи авторизувався адміністратор зараз.
     * Є можливість підписатися на нього
     */
    public static final SimpleBooleanProperty adminIsAuthorizedProperty;
    /**
     * Шлях до каталогу з зображеннями бази даних
     */
    public static final String DB_IMAGES_FOLDER = "db_img";

    private static String ADMIN_LOGIN;
    private static String ADMIN_PASSWORD;

    private final DbRepository dbRepository;

    private Pair<ArrayList<Genre>, Long> cachedGenres;
    private Pair<ArrayList<Author>, Long> cachedAuthors;
    private Pair<ArrayList<Anime>, Long> cachedAnime;
    private Pair<ArrayList<Comics>, Long> cachedComics;
    private Pair<ArrayList<Pair<Anime, Genre>>, Long> cachedAnimeGenreMap;
    private Pair<ArrayList<Pair<Integer, String>>, Long> cachedAnimeImagePaths;
    private Pair<ArrayList<Pair<Comics, Genre>>, Long> cachedComicsGenreMap;
    private Pair<ArrayList<Pair<Integer, String>>, Long> cachedComicsImagePaths;

    static {
        instance = new Repository();

        ADMIN_LOGIN = prefs.get(PREFS_ADMIN_LOGIN, DEFAULT_ADMIN_LOGIN);
        ADMIN_PASSWORD = prefs.get(PREFS_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD);

        if (ADMIN_LOGIN.equalsIgnoreCase(DEFAULT_ADMIN_LOGIN))
            prefs.put(PREFS_ADMIN_LOGIN, DEFAULT_ADMIN_LOGIN);
        if (ADMIN_PASSWORD.equalsIgnoreCase(DEFAULT_ADMIN_PASSWORD))
            prefs.put(PREFS_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD);

        namesBundle = ResourceBundle.getBundle("ui_names", new ResourceBundle.Control() {
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

    /**
     * Метод під'єднання до бази даних. Потрібно виконувати його перед
     * роботою з базою даних, інакше робота з базою даних буде
     * неможлива
     */
    public void connectToDatabase() {
        dbRepository.connect();
    }

    /**
     * Метод від'єднання від бази даних. Потрібно виконувати після роботи
     * з базою даних
     */
    public void disconnectFromDatabase() {
        dbRepository.disconnect();
    }

    /**
     * @param login строка логіну для перевірки
     * @return true, якщо логін корректний (враховуючи регістр). Інакше false
     */
    public boolean loginIsCorrect(String login) {
        if (ADMIN_LOGIN.equals(login)) return true;

        return false;
    }

    /**
     * @param password строка паролю для перевірки
     * @return true, якщо пароль корректний (враховуючи регістр). Інакше false
     */
    public boolean passwordIsCorrect(String password) {
        if (ADMIN_PASSWORD.equals(password)) return true;

        return false;
    }

    /**
     * Оновлює логін адміністратору
     * @param newLogin новий логін. Перед оновленням проходить валідацію та в разі
     *                 її непроходження НЕ встановлюється
     */
    public void updateAdminLogin(String newLogin) {
        if (loginIsValid(newLogin)) {
            ADMIN_LOGIN = newLogin;
            prefs.put(PREFS_ADMIN_LOGIN, ADMIN_LOGIN);
        }
    }

    /**
     * Оновлює пароль адміністратору
     * @param newPassword новий пароль. Перед оновленням проходить валідацію та в разі
     *                 її непроходження НЕ встановлюється
     */
    public void updateAdminPassword(String newPassword) {
        if (passwordIsValid(newPassword)) {
            ADMIN_PASSWORD = newPassword;
            prefs.put(PREFS_ADMIN_PASSWORD, ADMIN_PASSWORD);
        }
    }

    /**
     * @param login строка логіну для валідації
     * @return true, якщо логін можна встановити як новий. Інакше false
     */
    public boolean loginIsValid(String login) {
        if (login == null || !login.matches("[a-zA-Z0-9@.]{3,20}")) return false;

        return true;
    }

    /**
     * @param password строка паролю для валідації
     * @return true, якщо пароль можна встановити як новий. Інакше false
     */
    public boolean passwordIsValid(String password) {
        if (password == null || !password.matches("[a-zA-Z0-9]{3,20}")) return false;

        return true;
    }

    /**
     * @param key "ключ" до значення, яке потрібно отримати в поточній локалі
     *            ресурсів. Наприклад, якщо значення <code>key</code> буде
     *            <code>title</code>, то в залежності від локалі повернеться
     *            титулка або на російській, або на українській, або на
     *            англійській мові (або більше, якщо буде добавлено більше
     *            файлів ресурсів)
     * @return значення ключа в залежності від локалізації
     */
    public String getNamesBundleValue(String key) {
        if (key == null) return "";

        return namesBundle.getString(key);
    }

    /**
     * @param sourceFile Файл, який потрібно скопіювати у каталог зображень
     *                   бази даних.
     * @return об'єкт шляху до нового файлу, якщо він був скопійований, або
     * null, якщо ні
     */
    public Path imageWasCopiedToDbImgFolder(File sourceFile) {
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

    /**
     * Забезпечує існування папки {@value #DB_IMAGES_FOLDER} - якщо її нема,
     * то створює її. Викликається перед копіюванням файлу до папки
     */
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

    /**
     * @param updateCacheAndGetNew true, якщо потрібно отримати найновіші
     *                             дані, та false, якщо можливо отримати
     *                             дані, які були актуальні максимум 15
     *                             хвилин тому
     * @return колекцію жанрів
     */
    public ArrayList<Genre> getGenres(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedGenres == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedGenres.getValue() > MAX_CACHED_TIME) {
            cachedGenres = new Pair<>(dbRepository.getGenres(), currentTimeMillis);
        }

        return cachedGenres.getKey();
    }

    /**
     * @param updateCacheAndGetNew true, якщо потрібно отримати найновіші
     *                             дані, та false, якщо можливо отримати
     *                             дані, які були актуальні максимум 15
     *                             хвилин тому
     * @return колекцію авторів
     */
    public ArrayList<Author> getAuthors(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedAuthors == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedAuthors.getValue() > MAX_CACHED_TIME) {
            cachedAuthors = new Pair<>(dbRepository.getAuthors(), currentTimeMillis);
        }

        return cachedAuthors.getKey();
    }

    /**
     * @param updateCacheAndGetNew true, якщо потрібно отримати найновіші
     *                             дані, та false, якщо можливо отримати
     *                             дані, які були актуальні максимум 15
     *                             хвилин тому
     * @return колекцію аніме
     */
    public ArrayList<Anime> getAnimes(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedAnime == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedAnime.getValue() > MAX_CACHED_TIME) {
            cachedAnime = new Pair<>(dbRepository.getAnimes(), currentTimeMillis);
        }

        return cachedAnime.getKey();
    }

    /**
     * @param updateCacheAndGetNew true, якщо потрібно отримати найновіші
     *                             дані, та false, якщо можливо отримати
     *                             дані, які були актуальні максимум 15
     *                             хвилин тому
     * @return колекцію коміксів
     */
    public ArrayList<Comics> getComics(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedComics == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedComics.getValue() > MAX_CACHED_TIME) {
            cachedComics = new Pair<>(dbRepository.getComics(), currentTimeMillis);
        }

        return cachedComics.getKey();
    }

    /**
     * @param updateCacheAndGetNew true, якщо потрібно отримати найновіші
     *                             дані, та false, якщо можливо отримати
     *                             дані, які були актуальні максимум 15
     *                             хвилин тому
     * @return колекцію пар "аніме-жанр". Зв'язок від багатьох до багатьох
     */
    public ArrayList<Pair<Anime, Genre>> getAnimeGenreMap(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedAnimeGenreMap == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedAnimeGenreMap.getValue() > MAX_CACHED_TIME) {
            cachedAnimeGenreMap = new Pair<>(dbRepository.getAnimeGenreMap(), currentTimeMillis);
        }

        return cachedAnimeGenreMap.getKey();
    }

    /**
     * @param updateCacheAndGetNew true, якщо потрібно отримати найновіші
     *                             дані, та false, якщо можливо отримати
     *                             дані, які були актуальні максимум 15
     *                             хвилин тому
     * @return колекцію пар "комікс-жанр". Зв'язок від багатьох до багатьох
     */
    public ArrayList<Pair<Comics, Genre>> getComicsGenreMap(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedComicsGenreMap == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedComicsGenreMap.getValue() > MAX_CACHED_TIME) {
            cachedComicsGenreMap = new Pair<>(dbRepository.getComicsGenreMap(), currentTimeMillis);
        }

        return cachedComicsGenreMap.getKey();
    }

    /**
     * @param updateCacheAndGetNew true, якщо потрібно отримати найновіші
     *                             дані, та false, якщо можливо отримати
     *                             дані, які були актуальні максимум 15
     *                             хвилин тому
     * @return колекцію пар "індекс аніме-назва зображення". Зв'язок від
     * багатьох до багатьох
     */
    public ArrayList<Pair<Integer, String>> getAnimeImagePaths(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedAnimeImagePaths == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedAnimeImagePaths.getValue() > MAX_CACHED_TIME) {
            cachedAnimeImagePaths = new Pair<>(dbRepository.getAnimeImagePaths(), currentTimeMillis);
        }

        return cachedAnimeImagePaths.getKey();
    }

    /**
     * @param updateCacheAndGetNew true, якщо потрібно отримати найновіші
     *                             дані, та false, якщо можливо отримати
     *                             дані, які були актуальні максимум 15
     *                             хвилин тому
     * @return колекцію пар "індекс коміксу-назва зображення". Зв'язок від
     * багатьох до багатьох
     */
    public ArrayList<Pair<Integer, String>> getComicsImagePaths(boolean updateCacheAndGetNew) {
        long currentTimeMillis = System.currentTimeMillis();

        if (cachedComicsImagePaths == null || updateCacheAndGetNew ||
                currentTimeMillis - cachedComicsImagePaths.getValue() > MAX_CACHED_TIME) {
            cachedComicsImagePaths = new Pair<>(dbRepository.getComicsImagePaths(), currentTimeMillis);
        }

        return cachedComicsImagePaths.getKey();
    }

    /**
     * Метод-утиліта для оновнення кешу всього аніме підрозділу
     */
    public void updateAnimeCache() {
        getGenres(true);
        getAnimes(true);
        getAnimeGenreMap(true);
        getAnimeImagePaths(true);
    }

    /**
     * Добавляє нове аніме до бази даних або, в разі невдачі, виводить помилку
     * користувачу
     * @param newAnime нове аніме для добавлення
     */
    public void insertNewAnime(Anime newAnime) {
        dbRepository.insertNewAnime(newAnime);
    }

    /**
     * Оновлює аніме в базі даних або, в разі невдачі, виводить помилку користувачу
     * @param anime комікс для оновлення
     */
    public void updateAnime(Anime anime) {
        dbRepository.updateAnime(anime);
    }

    /**
     * Видаляє старе аніме з бази даних або, в разі невдачі, виводить помилку
     * користувачу
     * @param oldAnime старе аніме для видалення
     */
    public void deleteAnimeFromDb(Anime oldAnime) {
        dbRepository.deleteAnime(oldAnime);
    }

    /**
     * Оновлює жанри щодо вказанного аніме
     * @param animeGenres нові жанри до вказаного аніме
     */
    public void updateAnimeGenres(Pair<Anime, ArrayList<Genre>> animeGenres) {
        dbRepository.updateAnimeGenres(animeGenres);
    }

    /**
     * Оновлює шляхи зображень щодо вказанного аніме
     * @param animeImages нові імена зображень до вказаного аніме
     */
    public void updateAnimeImages(Pair<Anime, ArrayList<String>> animeImages) {
        dbRepository.updateAnimeImages(animeImages);
    }

    /**
     * Метод-утиліта для оновнення кешу всього коміксового підрозділу
     */
    public void updateComicsCache() {
        getGenres(true);
        getComics(true);
        getComicsGenreMap(true);
        getComicsImagePaths(true);
    }

    /**
     * Добавляє новий комікс до бази даних або, в разі невдачі, виводить помилку
     * користувачу
     * @param newComics новий комікс для добавлення
     */
    public void insertNewComics(Comics newComics) {
        dbRepository.insertNewComics(newComics);
    }

    /**
     * Оновлює комікс в базі даних або, в разі невдачі, виводить помилку користувачу
     * @param comics комікс для оновлення
     */
    public void updateComics(Comics comics) {
        dbRepository.updateComics(comics);
    }

    /**
     * Видаляє старий комікс з бази даних або, в разі невдачі, виводить помилку
     * користувачу
     * @param oldComics старий комікс для видалення
     */
    public void deleteComicsFromDb(Comics oldComics) {
        dbRepository.deleteComics(oldComics);
    }

    /**
     * Оновлює жанри щодо вказанного коміксу
     * @param comicsGenres нові жанри до вказаного коміксу
     */
    public void updateComicsGenres(Pair<Comics, ArrayList<Genre>> comicsGenres) {
        dbRepository.updateComicsGenres(comicsGenres);
    }

    /**
     * Оновлює шляхи зображень щодо вказанного коміксу
     * @param comicsImages нові імена зображень до вказаного коміксу
     */
    public void updateComicsImages(Pair<Comics, ArrayList<String>> comicsImages) {
        dbRepository.updateComicsImages(comicsImages);
    }

    /**
     * @param file файл, розширення якого потрібно отримати
     * @return розширення файлу. Наприклад, з файлу "somebody.txt"
     * поверне строку "txt", а з файлу "heh" вилучить пусту строку
     */
    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else return "";
    }

    /**
     * @param imageName назва зображення каталогу бази даних, яке
     *                  потрібно завантажати в об'єкт Image
     * @return об'єкт Image вказаного зображення або null, якщо вказаного
     * зображення нема або не вдалося вилучити
     */
    public static Image loadImage(String imageName) {
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

    /**
     * @return зображення з текстом "NO IMAGE FOUND". Потрібне
     * зазвичай для випадків, коли у аніме/коміксу нема навіть
     * одного зображення, щоб поставити на "титульне" віконце
     * зображення
     */
    public static Image getNoImageImage() {
        URL notFoundImageResource = Main.getResource("/res/img/no_image.jpg");
        return new Image(notFoundImageResource.toString());
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

        private enum ComicsFields {
            ID("id", 1),
            ID_AUTHOR("id_author", 2),
            TYPE("type", 3),
            NAME("name", 4),
            DESCRIPTION("description", 5),
            CHAPTER_COUNT("chapter_count", 6),
            SOURCE("source", 7),
            PREMIERE_YEAR("premiere_year", 8),
            STATUS("status", 9);

            public final String columnName;
            public final int columnIndex;

            ComicsFields(String columnName, int columnIndex) {
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

        private enum ComicsGenreFields {
            ID_COMICS("id_comics", 1),
            ID_GENRE("id_genre", 2);

            public final String columnName;
            public final int columnIndex;

            ComicsGenreFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        private enum ComicsAltNameFields {
            ID_COMICS("id_comics", 1),
            ALTERNATIVE_NAME("alternative_name", 2);

            public final String columnName;
            public final int columnIndex;

            ComicsAltNameFields(String columnName, int columnIndex) {
                this.columnName = columnName;
                this.columnIndex = columnIndex;
            }

            @Override
            public String toString() {
                return columnName;
            }
        }

        private enum ComicsGalleryFields {
            ID_COMICS("id_comics", 1),
            IMG_PATH("img_path", 2);

            public final String columnName;
            public final int columnIndex;

            ComicsGalleryFields(String columnName, int columnIndex) {
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

        public ArrayList<Comics> getComics() {
            ArrayList<Comics> comicsList = new ArrayList<>();

            ArrayList<Author> authors = getAuthors();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from comics");
                while (resultSet.next()) {
                    int comicsId = resultSet.getInt(ComicsFields.ID.columnName);

                    int comicsAuthorId = resultSet.getInt(ComicsFields.ID_AUTHOR.columnName);
                    Stream<Author> authorStream = authors.stream().filter(
                            author -> author.getId() == comicsAuthorId);
                    Optional<Author> optionalAuthor = authorStream.findFirst();
                    Author comicsAuthor = optionalAuthor.orElse(null);

                    String comicsTypeString = resultSet.getString(ComicsFields.TYPE.columnName);
                    Comics.Type comicsType = Comics.Type.valueOf(comicsTypeString.toUpperCase());

                    String comicsName = resultSet.getString(ComicsFields.NAME.columnName);
                    String comicsDescription = resultSet.getString(ComicsFields.DESCRIPTION.columnName);
                    int comicsChapterCount = resultSet.getInt(ComicsFields.CHAPTER_COUNT.columnName);

                    String comicsSourceString = resultSet.getString(ComicsFields.SOURCE.columnName);
                    Comics.Source comicsSource = Comics.Source.valueOf(comicsSourceString.toUpperCase());

                    int comicsPremiereYear = resultSet.getInt(ComicsFields.PREMIERE_YEAR.columnName);

                    String comicsStatusString = resultSet.getString(ComicsFields.STATUS.columnName);
                    Status comicsStatus = Status.valueOf(comicsStatusString.toUpperCase());

                    Comics newAnime = Comics.Builder.newBuilder()
                            .setId(comicsId)
                            .setAuthor(comicsAuthor)
                            .setType(comicsType)
                            .setName(comicsName)
                            .setDescription(comicsDescription)
                            .setChapterCount(comicsChapterCount)
                            .setSource(comicsSource)
                            .setPremiereYear(comicsPremiereYear)
                            .setStatus(comicsStatus)
                            .build();

                    comicsList.add(newAnime);
                }
                resultSet.close();
            } catch (SQLException | IllegalArgumentException e) {
                logger.error("Error while getting comics from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            setComicsAltNames(comicsList);

            return comicsList;
        }

        private void setComicsAltNames(ArrayList<Comics> comicsList) {
            ArrayList<Pair<Integer, String>> allAltNames = new ArrayList<>();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from comics_alt_names");
                while (resultSet.next()) {
                    int idComics = resultSet.getInt(ComicsAltNameFields.ID_COMICS.columnName);
                    String altName = resultSet.getString(ComicsAltNameFields.ALTERNATIVE_NAME.columnName);
                    Pair<Integer, String> newAltNameObj = new Pair<>(idComics, altName);
                    allAltNames.add(newAltNameObj);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting comics alt names from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            comicsList.forEach(comics -> allAltNames.forEach(altName -> {
                if (comics.getId() == altName.getKey())
                    comics.getAltNames().add(altName.getValue());
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

        public ArrayList<Pair<Anime, Genre>> getAnimeGenreMap() {
            ArrayList<Pair<Anime, Genre>> animeGenres = new ArrayList<>();
            ArrayList<Anime> allAnimes = getAnimes();
            ArrayList<Genre> allGenres = getGenres();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from anime_genres");
                while (resultSet.next()) {
                    int idAnime = resultSet.getInt(AnimeGenreFields.ID_ANIME.columnName);
                    int idGenre = resultSet.getInt(AnimeGenreFields.ID_GENRE.columnName);
                    Optional<Anime> optionalAnime = allAnimes.stream().filter(anime ->
                            anime.getId() == idAnime).findFirst();
                    Optional<Genre> optionalGenre = allGenres.stream().filter(genre ->
                            genre.id == idGenre).findFirst();
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

        public ArrayList<Pair<Comics, Genre>> getComicsGenreMap() {
            ArrayList<Pair<Comics, Genre>> comicsGenres = new ArrayList<>();
            ArrayList<Comics> allComics = getComics();
            ArrayList<Genre> allGenres = getGenres();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from comics_genres");
                while (resultSet.next()) {
                    int idComics = resultSet.getInt(ComicsGenreFields.ID_COMICS.columnName);
                    int idGenre = resultSet.getInt(ComicsGenreFields.ID_GENRE.columnName);
                    Optional<Comics> optionalComics = allComics.stream().filter(comics ->
                            comics.getId() == idComics).findFirst();
                    Optional<Genre> optionalGenre = allGenres.stream().filter(genre ->
                            genre.id == idGenre).findFirst();
                    if (!optionalComics.isPresent() || !optionalGenre.isPresent()) continue;
                    Comics comics = optionalComics.get();
                    Genre genre = optionalGenre.get();
                    Pair<Comics, Genre> pair = new Pair<>(comics, genre);
                    comicsGenres.add(pair);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting comics genre connections: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            return comicsGenres;
        }

        public ArrayList<Pair<Integer, String>> getComicsImagePaths() {
            ArrayList<Pair<Integer, String>> comicsImagePaths = new ArrayList<>();

            try {
                ResultSet resultSet = connection.createStatement().executeQuery("select * from comics_gallery");
                while (resultSet.next()) {
                    int comicsId = resultSet.getInt(ComicsGalleryFields.ID_COMICS.columnName);
                    String imgPath = resultSet.getString(ComicsGalleryFields.IMG_PATH.columnName);
                    Pair<Integer, String> newPair = new Pair<>(comicsId, imgPath);
                    comicsImagePaths.add(newPair);
                }
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while getting comics image paths from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseImportError"));
                Main.errorAlert.show();
            }

            return comicsImagePaths;
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

        public void deleteComics(Comics comics) {
            try {
                int comicsId = comics.getId();
                PreparedStatement preparedStatement = connection.prepareStatement("delete from comics where id = ?");
                preparedStatement.setInt(ComicsFields.ID.columnIndex, comicsId);
                preparedStatement.execute();
            } catch (SQLException e) {
                logger.error("Error while deleting comics from db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseDeleteError"));
                Main.errorAlert.show();
            }
        }

        /*
         * Methods which add data in the database
         */

        @SuppressWarnings("DuplicatedCode")
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

        @SuppressWarnings("DuplicatedCode")
        public void insertNewComics(Comics newComics) {
            try {
                int comicsId = newComics.getId();
                int comicsAuthorId = newComics.getAuthor().getId();
                String comicsType = newComics.getType().name;
                String comicsName = newComics.getName();
                String comicsDescription = newComics.getDescription();
                int comicsChapterCount = newComics.getChapterCount();
                String comicsSource = newComics.getSource().name;
                int comicsPremiereYear = newComics.getPremiereYear();
                String comicsStatus = newComics.getStatus().name;

                String insertString = "insert into comics values (?,?,?,?,?,?,?,?,?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertString);

                insertStatement.setInt(1, comicsId);
                insertStatement.setInt(2, comicsAuthorId);
                insertStatement.setString(3, comicsType);
                insertStatement.setString(4, comicsName);
                insertStatement.setString(5, comicsDescription);
                insertStatement.setInt(6, comicsChapterCount);
                insertStatement.setString(7, comicsSource);
                insertStatement.setInt(8, comicsPremiereYear);
                insertStatement.setString(9, comicsStatus);

                insertStatement.execute();
            } catch (SQLException e) {
                logger.error("Error while inserting comics in db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseInsertError"));
                Main.errorAlert.show();
            }
        }

        /*
         * Methods which update (specifically "update" or drop & add) data in the database
         */

        public void updateAnime(Anime anime) {
            try {
                int animeId = anime.getId();
                int animeAuthorId = anime.getAuthor().getId();
                String animeType = anime.getType().name;
                String animeName = anime.getName();
                String animeDescription = anime.getDescription();
                int animeEpisodeCount = anime.getEpisodeCount();
                String animeSource = anime.getSource().name;
                int animeAgeRatingId = anime.getAgeRating().id;
                int animePremiereYear = anime.getPremiereYear();
                String animePremiereSeason = anime.getPremiereSeason().name;
                String animeStatus = anime.getStatus().name;

                String updateString = "update anime set id_author=?, type=?, name=?, description=?, " +
                        "episode_count=?, source=?, id_rating=?, premiere_year=?, premiere_season=?, " +
                        "status=? where id=?";
                PreparedStatement updateStatement = connection.prepareStatement(updateString);

                updateStatement.setInt(1, animeAuthorId);
                updateStatement.setString(2, animeType);
                updateStatement.setString(3, animeName);
                updateStatement.setString(4, animeDescription);
                updateStatement.setInt(5, animeEpisodeCount);
                updateStatement.setString(6, animeSource);
                updateStatement.setInt(7, animeAgeRatingId);
                updateStatement.setInt(8, animePremiereYear);
                updateStatement.setString(9, animePremiereSeason);
                updateStatement.setString(10, animeStatus);
                updateStatement.setInt(11, animeId);

                updateStatement.execute();
            } catch (SQLException e) {
                logger.error("Error while updating anime in db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseUpdateError"));
                Main.errorAlert.show();
            }
        }

        public void updateComics(Comics comics) {
            try {
                int animeId = comics.getId();
                int animeAuthorId = comics.getAuthor().getId();
                String animeType = comics.getType().name;
                String animeName = comics.getName();
                String animeDescription = comics.getDescription();
                int animeChapterCount = comics.getChapterCount();
                String animeSource = comics.getSource().name;
                int animePremiereYear = comics.getPremiereYear();
                String animeStatus = comics.getStatus().name;

                String updateString = "update comics set id_author=?, type=?, name=?, description=?, " +
                        "episode_count=?, source=?, premiere_year=?, status=? where id=?";
                PreparedStatement updateStatement = connection.prepareStatement(updateString);

                updateStatement.setInt(1, animeAuthorId);
                updateStatement.setString(2, animeType);
                updateStatement.setString(3, animeName);
                updateStatement.setString(4, animeDescription);
                updateStatement.setInt(5, animeChapterCount);
                updateStatement.setString(6, animeSource);
                updateStatement.setInt(7, animePremiereYear);
                updateStatement.setString(8, animeStatus);
                updateStatement.setInt(9, animeId);

                updateStatement.execute();
            } catch (SQLException e) {
                logger.error("Error while updating comics in db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseUpdateError"));
                Main.errorAlert.show();
            }
        }

        @SuppressWarnings("unchecked")
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

        @SuppressWarnings("unchecked")
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

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        public void updateComicsGenres(Pair<Comics, ArrayList<Genre>> comicsGenres) {
            try {
                ArrayList<Genre> currentComicsGenres = getComicsGenreMap().stream().filter(
                        comicsGenrePair -> comicsGenrePair.getKey().getId() == comicsGenres.getKey().getId())
                        .map(Pair::getValue)
                        .collect(Collectors.toCollection(ArrayList::new));
                ArrayList<Genre> genresToAdd = (ArrayList<Genre>) comicsGenres.getValue().clone();
                genresToAdd.removeIf(genre -> currentComicsGenres.stream().anyMatch(localGenre ->
                        localGenre.name.equalsIgnoreCase(genre.name)));
                ArrayList<Genre> genresToDrop = (ArrayList<Genre>) currentComicsGenres.clone();
                genresToDrop.removeIf(genre -> comicsGenres.getValue().stream().anyMatch(localGenre ->
                        localGenre.name.equalsIgnoreCase(genre.name)));

                if (genresToDrop.size() > 0) {
                    StringBuilder dropGenresBuilder = new StringBuilder("delete from comics_genres where id_comics = ? " +
                            "and id_genre in (?");
                    for (int i = 1; i < genresToDrop.size(); i++) {
                        dropGenresBuilder.append(",?");
                    }
                    dropGenresBuilder.append(")");
                    PreparedStatement preparedDropStatement = connection.prepareStatement(dropGenresBuilder.toString());
                    preparedDropStatement.setInt(1, comicsGenres.getKey().getId());
                    for (int i = 1; i < genresToDrop.size() + 1; i++) {
                        preparedDropStatement.setInt(i + 1, genresToDrop.get(i - 1).id);
                    }
                    preparedDropStatement.execute();
                }

                if (genresToAdd.size() > 0) {
                    StringBuilder addGenresBuilder = new StringBuilder("insert into comics_genres values (?,?)");
                    for (int i = 1; i < genresToAdd.size(); i++) {
                        addGenresBuilder.append(",(?,?)");
                    }
                    PreparedStatement preparedAddStatement = connection.prepareStatement(addGenresBuilder.toString());
                    for (int i = 1, j = 1; i < genresToAdd.size() + 1; i++, j += 2) {
                        preparedAddStatement.setInt(j, comicsGenres.getKey().getId());
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
        public void updateComicsImages(Pair<Comics, ArrayList<String>> comicsImages) {
            try {
                ArrayList<String> currentComicsImages = getComicsImagePaths().stream().filter(
                        comicsImagePair -> comicsImagePair.getKey() == comicsImages.getKey().getId())
                        .map(Pair::getValue)
                        .collect(Collectors.toCollection(ArrayList::new));
                ArrayList<String> imagesToAdd = (ArrayList<String>) comicsImages.getValue().clone();
                imagesToAdd.removeIf(image -> currentComicsImages.stream().anyMatch(localImage ->
                        localImage.equalsIgnoreCase(image)));
                ArrayList<String> imagesToDrop = (ArrayList<String>) currentComicsImages.clone();
                imagesToDrop.removeIf(image -> comicsImages.getValue().stream().anyMatch(localImage ->
                        localImage.equalsIgnoreCase(image)));

                if (imagesToDrop.size() > 0) {
                    StringBuilder dropImagesBuilder = new StringBuilder("delete from comics_gallery where id_comics = ? " +
                            "and img_path in (?");
                    for (int i = 1; i < imagesToDrop.size(); i++) {
                        dropImagesBuilder.append(",?");
                    }
                    dropImagesBuilder.append(")");
                    PreparedStatement preparedDropStatement = connection.prepareStatement(dropImagesBuilder.toString());
                    preparedDropStatement.setInt(1, comicsImages.getKey().getId());
                    for (int i = 1; i < imagesToDrop.size() + 1; i++) {
                        preparedDropStatement.setString(i + 1, imagesToDrop.get(i - 1));
                    }
                    preparedDropStatement.execute();
                }

                if (imagesToAdd.size() > 0) {
                    StringBuilder addImagesBuilder = new StringBuilder("insert into comics_gallery values (?,?)");
                    for (int i = 1; i < imagesToAdd.size(); i++) {
                        addImagesBuilder.append(",(?,?)");
                    }
                    PreparedStatement preparedAddStatement = connection.prepareStatement(addImagesBuilder.toString());
                    for (int i = 1, j = 1; i < imagesToAdd.size() + 1; i++, j += 2) {
                        preparedAddStatement.setInt(j, comicsImages.getKey().getId());
                        preparedAddStatement.setString(j + 1, imagesToAdd.get(i - 1));
                    }
                    preparedAddStatement.execute();
                }
            } catch (Exception e) {
                logger.error("Error while updating comics images in db: ", e);
                Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("databaseUpdateError"));
                Main.errorAlert.show();
            }
        }
    }
}
