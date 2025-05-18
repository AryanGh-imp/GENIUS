//import models.account.Artist;
//import models.account.User;
//import models.music.Album;
//import models.music.Song;
//
//import services.SessionManager;
//import services.file.ArtistFileManager;
//import services.file.SongFileManager;
//import services.file.UserFileManager;
//
//import utils.ConfigLoader;
//import utils.FileUtil;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.time.Instant;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//public class TestDataSetup {
//
//    // File managers for handling data persistence
//    private static final SongFileManager songFileManager = new SongFileManager();
//    private static final ArtistFileManager artistFileManager = new ArtistFileManager();
//    private static final UserFileManager userFileManager = new UserFileManager();
//
//    // Constants for file paths
//    private static final String DATA_DIR = FileUtil.DATA_DIR;
//    private static final String FILE_SEPARATOR = File.separator;
//    private static final String DEFAULT_IMAGE_PATH = Paths.get("resources", "pics", "Genius.com_logo_yellow.png").toString();
//    private static final String BASE_ARTISTS_PATH = DATA_DIR + "artists" + FILE_SEPARATOR;
//    private static final String USER_DATA_PATH = DATA_DIR + "users" + FILE_SEPARATOR;
//    private static final String ADMIN_DATA_PATH = DATA_DIR + "admin" + FILE_SEPARATOR;
//    private static final String ARTIST_REQUESTS_PENDING_DIR = DATA_DIR + "admin/artist_requests/pending/";
//    private static final String ARTIST_REQUESTS_APPROVED_DIR = DATA_DIR + "admin/artist_requests/approved/";
//    private static final String ARTIST_REQUESTS_REJECTED_DIR = DATA_DIR + "admin/artist_requests/rejected/";
//    private static final String LYRICS_REQUESTS_PENDING_DIR = DATA_DIR + "lyrics_requests/pending/";
//    private static final String LYRICS_REQUESTS_APPROVED_DIR = DATA_DIR + "lyrics_requests/approved/";
//    private static final String LYRICS_REQUESTS_REJECTED_DIR = DATA_DIR + "lyrics_requests/rejected/";
//
//    // Utility objects
//    private static final Random random = new Random();
//    private static final List<User> testUsers = new ArrayList<>();
//
//    // Main method to set up test data
//    public static void setupTestData() throws Exception {
//        clearTestData();
//        initializeDirectories();
//        initializeIndexFiles();
//        setupTestUsers();
//        SessionManager.getInstance().setCurrentAccount(testUsers.getFirst());
//
//        // Set up artists with their singles and albums
//        setupKendrickLamarArtist();
//        setupEminemArtist();
//        setupTravisScottArtist();
//        setupMetroBoominArtist();
//        setupFutureArtist();
//        setupDrDreArtist();
//
//        setupArtistRequests();
//        setupLyricsEditRequests();
//    }
//
//    private static void initializeDirectories() {
//        FileUtil.ensureDataDirectoryExists(BASE_ARTISTS_PATH);
//        FileUtil.ensureDataDirectoryExists(USER_DATA_PATH);
//        FileUtil.ensureDataDirectoryExists(ADMIN_DATA_PATH);
//        FileUtil.ensureDataDirectoryExists(ARTIST_REQUESTS_PENDING_DIR);
//        FileUtil.ensureDataDirectoryExists(ARTIST_REQUESTS_APPROVED_DIR);
//        FileUtil.ensureDataDirectoryExists(ARTIST_REQUESTS_REJECTED_DIR);
//        FileUtil.ensureDataDirectoryExists(LYRICS_REQUESTS_PENDING_DIR);
//        FileUtil.ensureDataDirectoryExists(LYRICS_REQUESTS_APPROVED_DIR);
//        FileUtil.ensureDataDirectoryExists(LYRICS_REQUESTS_REJECTED_DIR);
//    }
//
//    private static void initializeIndexFiles() throws IOException {
//        String usersIndexPath = DATA_DIR + "users/index_users.txt";
//        String artistsIndexPath = DATA_DIR + "artists/index_artists.txt";
//        String adminsIndexPath = DATA_DIR + "admin/index_admins.txt";
//
//        if (!new File(usersIndexPath).exists()) {
//            FileUtil.writeFile(usersIndexPath, new ArrayList<>());
//        }
//        if (!new File(artistsIndexPath).exists()) {
//            FileUtil.writeFile(artistsIndexPath, new ArrayList<>());
//        }
//        if (!new File(adminsIndexPath).exists()) {
//            ConfigLoader configLoader = ConfigLoader.getInstance();
//            String adminEmail = configLoader.getAdminEmail();
//            String adminNickname = configLoader.getAdminNickname();
//            List<String> adminEntries = new ArrayList<>();
//            adminEntries.add(adminEmail + ":" + adminNickname);
//            FileUtil.writeFile(adminsIndexPath, adminEntries);
//        }
//    }
//
//    private static void clearTestData() {
//        FileUtil.deleteDirectory(new File(BASE_ARTISTS_PATH));
//        FileUtil.deleteDirectory(new File(USER_DATA_PATH));
//        FileUtil.deleteDirectory(new File(ADMIN_DATA_PATH));
//        FileUtil.deleteDirectory(new File(LYRICS_REQUESTS_PENDING_DIR));
//        FileUtil.deleteDirectory(new File(LYRICS_REQUESTS_APPROVED_DIR));
//        FileUtil.deleteDirectory(new File(LYRICS_REQUESTS_REJECTED_DIR));
//
//        String artistsIndexPath = DATA_DIR + "artists/index_artists.txt";
//        File artistsIndexFile = new File(artistsIndexPath);
//        if (artistsIndexFile.exists()) {
//            artistsIndexFile.delete();
//        }
//
//        artistFileManager.clearCache();
//        songFileManager.clearCache();
//    }
//
//    private static void setupTestUsers() throws Exception {
//        List<User> existingUsers = userFileManager.loadAllUsers();
//        if (existingUsers.isEmpty()) {
//            testUsers.add(new User("fan1@example.com", "MusicFan1", "FanPass2023!"));
//            testUsers.add(new User("fan2@example.com", "MusicFan2", "FanPass2023!"));
//            testUsers.add(new User("fan3@example.com", "MusicFan3", "FanPass2023!"));
//            for (User user : testUsers) {
//                userFileManager.saveAccount(user);
//            }
//        } else {
//            testUsers.addAll(existingUsers);
//            if (testUsers.size() < 3) {
//                User newUser = new User("fan" + (testUsers.size() + 1) + "@example.com",
//                        "MusicFan" + (testUsers.size() + 1), "FanPass2023!");
//                testUsers.add(newUser);
//                userFileManager.saveAccount(newUser);
//            }
//        }
//    }
//
//    private static void setupArtist(Artist artist, String basePath,
//                                    List<SingleInfo> singles, List<AlbumInfo> albums) throws Exception {
//        setupArtistBasics(artist, true);
//
//        if (artist.isApproved()) {
//            if (artist.getSingles().isEmpty()) {
//                for (SingleInfo single : singles) {
//                    addSingle(artist, single.title, single.lyrics, single.date,
//                            basePath + "singles/" + single.path + "/album_art.png");
//                }
//            }
//
//            for (AlbumInfo album : albums) {
//                Album albumObj = findOrCreateAlbum(artist, album.title, album.date,
//                        basePath + "albums/" + album.path + "/album_art.png");
//                if (albumObj != null && albumObj.getSongs().isEmpty()) {
//                    for (SongInfo song : album.songs) {
//                        addSongToAlbum(albumObj, song.title, song.lyrics, song.date,
//                                basePath + "albums/" + album.path + "/" + song.path + "/album_art.png");
//                    }
//                }
//            }
//        }
//    }
//
//    private static void setupKendrickLamarArtist() throws Exception {
//        Artist artist = getOrCreateArtist("KendrickLamar", "kendrick.lamar@example.com", "KDot2025!");
//        String basePath = BASE_ARTISTS_PATH + "KendrickLamar" + FILE_SEPARATOR;
//
//        List<SingleInfo> singles = List.of(
//                new SingleInfo("HUMBLE.", "Sit down, be humble...\nHol' up, that shit crazy...", "2017-04-14", "HUMBLE"),
//                new SingleInfo("Not Like Us", "They not like us, they not like us...\nWop, wop, wop, wop, wop...", "2024-05-04", "Not_Like_Us")
//        );
//
//        List<AlbumInfo> albums = List.of(
//                new AlbumInfo("DAMN.", "2017-04-14", "DAMN", List.of(
//                        new SongInfo("BLOOD.", "Is it wickedness? Is it weakness?...\nYou decide...", "2017-04-14", "BLOOD"),
//                        new SongInfo("HUMBLE.", "Sit down, be humble...\nHol' up, that shit crazy...", "2017-04-14", "HUMBLE")
//                )),
//                new AlbumInfo("Mr. Morale & The Big Steppers", "2022-05-13", "Mr_Morale", List.of(
//                        new SongInfo("United In Grief", "I hope you find some peace of mind in this lifetime...\nTell them, tell 'em...", "2022-05-13", "United_In_Grief"),
//                        new SongInfo("Savior", "Ain't nobody savin' me...\nI'm my own savior...", "2022-05-13", "Savior")
//                ))
//        );
//
//        setupArtist(artist, basePath, singles, albums);
//        followArtist("KendrickLamar", testUsers.get(0));
//        followArtist("KendrickLamar", testUsers.get(1));
//    }
//
//    private static void setupEminemArtist() throws Exception {
//        Artist artist = getOrCreateArtist("Eminem", "eminem@example.com", "SlimShady2025!");
//        String basePath = BASE_ARTISTS_PATH + "Eminem" + FILE_SEPARATOR;
//
//        List<SingleInfo> singles = List.of(
//                new SingleInfo("Lose Yourself", "Look, if you had one shot, or one opportunity...\nTo seize everything you ever wanted...", "2002-10-28", "Lose_Yourself"),
//                new SingleInfo("Houdini", "I wish I could say, 'What I wanna say'...\nBang, I'mma detonate...", "2024-05-31", "Houdini")
//        );
//
//        List<AlbumInfo> albums = List.of(
//                new AlbumInfo("The Marshall Mathers LP", "2000-05-23", "Marshall_Mathers_LP", List.of(
//                        new SongInfo("Stan", "My tea's gone cold, I'm wondering why...\nI got this feeling inside...", "2000-05-23", "Stan"),
//                        new SongInfo("The Real Slim Shady", "May I have your attention please?...\nWill the real Slim Shady please stand up?", "2000-05-23", "Real_Slim_Shady")
//                )),
//                new AlbumInfo("The Death of Slim Shady", "2024-07-12", "Death_of_Slim_Shady", List.of(
//                        new SongInfo("Tobey", "I'm back with a vengeance, Tobey Maguire...\nSwingin' like a spider...", "2024-07-12", "Tobey"),
//                        new SongInfo("Renaissance", "Reviving the art, renaissance...\nBack from the shadows...", "2024-07-12", "Renaissance")
//                ))
//        );
//
//        setupArtist(artist, basePath, singles, albums);
//        followArtist("Eminem", testUsers.get(0));
//        followArtist("Eminem", testUsers.get(2));
//    }
//
//    private static void setupTravisScottArtist() throws Exception {
//        Artist artist = getOrCreateArtist("TravisScott", "travisscott@example.com", "CactusJack2025!");
//        String basePath = BASE_ARTISTS_PATH + "TravisScott" + FILE_SEPARATOR;
//
//        List<SingleInfo> singles = List.of(
//                new SingleInfo("SICKO MODE", "Astro, yeah...\nSun is down, freezin' cold...", "2018-08-03", "SICKO_MODE"),
//                new SingleInfo("FE!N", "Feignin' for love, I'm feignin'...\nLost in the sauce...", "2023-10-20", "FE!N")
//        );
//
//        List<AlbumInfo> albums = List.of(
//                new AlbumInfo("ASTROWORLD", "2018-08-03", "ASTROWORLD", List.of(
//                        new SongInfo("STARGAZING", "Rollin', rollin', rollin', got me stargazin'...\nYeah, psychedelic...", "2018-08-03", "STARGAZING"),
//                        new SongInfo("SICKO MODE", "Astro, yeah...\nSun is down, freezin' cold...", "2018-08-03", "SICKO_MODE")
//                )),
//                new AlbumInfo("UTOPIA", "2023-07-28", "UTOPIA", List.of(
//                        new SongInfo("TELEKINESIS", "Telekinesis, movin' through the air...\nLost in the vision...", "2023-07-28", "TELEKINESIS"),
//                        new SongInfo("I KNOW ?", "I know, I know, I know...\nFeelin' the weight...", "2023-07-28", "I_KNOW")
//                ))
//        );
//
//        setupArtist(artist, basePath, singles, albums);
//        followArtist("TravisScott", testUsers.get(1));
//    }
//
//    private static void setupMetroBoominArtist() throws Exception {
//        Artist artist = getOrCreateArtist("MetroBoomin", "metroboomin@example.com", "Metro2025!");
//        String basePath = BASE_ARTISTS_PATH + "MetroBoomin" + FILE_SEPARATOR;
//
//        List<SingleInfo> singles = List.of(
//                new SingleInfo("Like That", "Why you hittin' me rippin' me...\nLike that, huh?", "2024-03-22", "Like_That"),
//                new SingleInfo("Creepin'", "Creepin', creepin', creepin'...\nThrough the night...", "2022-12-02", "Creepin")
//        );
//
//        List<AlbumInfo> albums = List.of(
//                new AlbumInfo("Heroes & Villains", "2022-12-02", "Heroes_Villains", List.of(
//                        new SongInfo("Superhero (Heroes & Villains)", "I'm a superhero, flyin' high...\nVillains in the back...", "2022-12-02", "Superhero"),
//                        new SongInfo("Creepin'", "Creepin', creepin', creepin'...\nThrough the night...", "2022-12-02", "Creepin")
//                )),
//                new AlbumInfo("WE DON'T TRUST YOU", "2024-03-22", "WE_DONT_TRUST_YOU", List.of(
//                        new SongInfo("Like That", "Why you hittin' me rippin' me...\nLike that, huh?", "2024-03-22", "Like_That"),
//                        new SongInfo("BBL Drizzy", "Drizzy got no BBL...\nWe don’t trust you...", "2024-05-03", "BBL_Drizzy")
//                ))
//        );
//
//        setupArtist(artist, basePath, singles, albums);
//        followArtist("MetroBoomin", testUsers.get(2));
//    }
//
//    private static void setupFutureArtist() throws Exception {
//        Artist artist = getOrCreateArtist("Future", "future@example.com", "Pluto2025!");
//        String basePath = BASE_ARTISTS_PATH + "Future" + FILE_SEPARATOR;
//
//        List<SingleInfo> singles = List.of(
//                new SingleInfo("Mask Off", "Mask on, fuck it, mask off...\nPercocet, molly, Percocet...", "2017-02-17", "Mask_Off"),
//                new SingleInfo("Type Shit", "Type shit, we on that type...\nTurn up the vibe...", "2024-04-19", "Type_Shit")
//        );
//
//        List<AlbumInfo> albums = List.of(
//                new AlbumInfo("FUTURE", "2017-02-17", "FUTURE", List.of(
//                        new SongInfo("Mask Off", "Mask on, fuck it, mask off...\nPercocet, molly, Percocet...", "2017-02-17", "Mask_Off"),
//                        new SongInfo("Draco", "Draco, draco, shootin' at your cameo...\nHit the block, I’m back again...", "2017-02-17", "Draco")
//                )),
//                new AlbumInfo("I NEVER LIKED YOU", "2022-04-29", "I_NEVER_LIKED_YOU", List.of(
//                        new SongInfo("Wait for U", "Wait for me, I’ll come back...\nHoldin' on tight...", "2022-04-29", "Wait_for_U"),
//                        new SongInfo("Puffin on Zooties", "Puffin' on Zooties, feelin' the high...\nLost in the sky...", "2022-04-29", "Puffin_on_Zooties")
//                ))
//        );
//
//        setupArtist(artist, basePath, singles, albums);
//        followArtist("Future", testUsers.getFirst());
//    }
//
//    private static void setupDrDreArtist() throws Exception {
//        Artist artist = getOrCreateArtist("DrDre", "drdre@example.com", "Dre2025!");
//        String basePath = BASE_ARTISTS_PATH + "DrDre" + FILE_SEPARATOR;
//
//        List<SingleInfo> singles = List.of(
//                new SingleInfo("Still D.R.E.", "Still, still doin' my thing...\nI'm representin' for the gang...", "1999-10-12", "Still_DRE"),
//                new SingleInfo("The Next Episode", "La-da-da, da-da-da...\nStill take a mug, still...", "1999-10-12", "Next_Episode")
//        );
//
//        List<AlbumInfo> albums = List.of(
//                new AlbumInfo("2001", "1999-11-16", "2001", List.of(
//                        new SongInfo("Still D.R.E.", "Still, still doin' my thing...\nI'm representin' for the gang...", "1999-11-16", "Still_DRE"),
//                        new SongInfo("The Next Episode", "La-da-da, da-da-da...\nStill take a mug, still...", "1999-11-16", "Next_Episode")
//                )),
//                new AlbumInfo("Compton", "2015-08-07", "Compton", List.of(
//                        new SongInfo("Talk About It", "Talk about it, let’s get it clear...\nWest coast vibes...", "2015-08-07", "Talk_About_It"),
//                        new SongInfo("Genocide", "Genocide, wipe 'em out...\nNo mercy here...", "2015-08-07", "Genocide")
//                ))
//        );
//
//        setupArtist(artist, basePath, singles, albums);
//        followArtist("DrDre", testUsers.get(1));
//    }
//
//    private static void setupArtistRequests() throws IOException {
//        createArtistRequest(ARTIST_REQUESTS_APPROVED_DIR, "eminem@example.com", "Eminem", "SlimShady2025!", "Approved");
//        createArtistRequest(ARTIST_REQUESTS_APPROVED_DIR, "future@example.com", "Future", "Pluto2025!", "Approved");
//        createArtistRequest(ARTIST_REQUESTS_APPROVED_DIR, "kendrick.lamar@example.com", "KendrickLamar", "KDot2025!", "Approved");
//        createArtistRequest(ARTIST_REQUESTS_APPROVED_DIR, "travisscott@example.com", "TravisScott", "CactusJack2025!", "Approved");
//        createArtistRequest(ARTIST_REQUESTS_APPROVED_DIR, "metroboomin@example.com", "MetroBoomin", "Metro2025!", "Approved");
//        createArtistRequest(ARTIST_REQUESTS_APPROVED_DIR, "drdre@example.com", "DrDre", "Dre2025!", "Approved");
//        createArtistRequest(ARTIST_REQUESTS_REJECTED_DIR, "newrapper@example.com", "NewRapper", "TestPass!", "Rejected");
//    }
//
//    private static void setupLyricsEditRequests() throws IOException {
//        createLyricsEditRequest("Eminem", "Houdini", "2025-05-13_20_51_00Z",
//                "I wish I could say, 'What I wanna say'..., detonate!", "The Death of Slim Shady");
//        createLyricsEditRequest("Future", "Type Shit", "2025-05-13_20_51_15Z",
//                "Type shit, we on that type...", null);
//        createLyricsEditRequest(LYRICS_REQUESTS_APPROVED_DIR, "KendrickLamar", "Not Like Us", "2025-05-13_20_51_30Z",
//                "They not like us, wop, wop...", "Single", "Approved");
//        createLyricsEditRequest(LYRICS_REQUESTS_REJECTED_DIR, "DrDre", "Talk About It", "2025-05-13_20_51_45Z",
//                "Talk about it, let’s skip...", "Compton", "Rejected");
//    }
//
//    private static void createArtistRequest(String email, String nickName, String password) throws IOException {
//        createArtistRequest(ARTIST_REQUESTS_PENDING_DIR, email, nickName, password, "Pending");
//    }
//
//    private static void createArtistRequest(String dirPath, String email, String nickName, String password, String status) throws IOException {
//        String safeNickName = nickName.replaceAll("[^a-zA-Z0-9]", "_");
//        String requestDir = dirPath + safeNickName + "/";
//        Path requestDirPath = Paths.get(requestDir);
//        if (!Files.exists(requestDirPath)) {
//            Files.createDirectories(requestDirPath);
//        }
//
//        String requestFile = requestDir + safeNickName + "-" + email + ".txt";
//        List<String> requestData = new ArrayList<>();
//        requestData.add("Email: " + email);
//        requestData.add("Nickname: " + safeNickName);
//        requestData.add("Password: " + password);
//        requestData.add("Status: " + status);
//        requestData.add("Timestamp: " + Instant.now().toString());
//        Files.write(Paths.get(requestFile), requestData);
//        System.out.println("Created artist request: " + requestFile);
//    }
//
//    private static void createLyricsEditRequest(String artistNickName, String songTitle, String timestamp, String suggestedLyrics, String albumName) throws IOException {
//        createLyricsEditRequest(LYRICS_REQUESTS_PENDING_DIR, artistNickName, songTitle, timestamp, suggestedLyrics, albumName, "Pending");
//    }
//
//    private static void createLyricsEditRequest(String dirPath, String artistNickName, String songTitle, String timestamp, String suggestedLyrics, String albumName, String status) throws IOException {
//        String safeArtistNickName = artistNickName.replaceAll("[^a-zA-Z0-9]", "_");
//        String safeSongTitle = songTitle.replaceAll("[^a-zA-Z0-9]", "_");
//
//        String requestDir = dirPath + safeArtistNickName + "/" + safeSongTitle + "/";
//        Path requestDirPath = Paths.get(requestDir);
//        if (!Files.exists(requestDirPath)) {
//            Files.createDirectories(requestDirPath);
//        }
//
//        String safeTimestamp = timestamp.replace(":", "_");
//        String requestFile = requestDir + safeSongTitle + "-" + safeTimestamp + ".txt";
//        List<String> requestData = new ArrayList<>();
//        requestData.add("Artist: " + artistNickName);
//        requestData.add("Song: " + songTitle);
//        requestData.add("Album: " + (albumName != null ? albumName : "Single"));
//        requestData.add("SuggestedLyrics: " + suggestedLyrics);
//        requestData.add("Requester: " + "testuser@example.com");
//        requestData.add("Status: " + status);
//        requestData.add("Timestamp: " + timestamp);
//        Files.write(Paths.get(requestFile), requestData);
//        System.out.println("Created lyrics edit request: " + requestFile);
//    }
//
//    private static Artist getOrCreateArtist(String nickname, String email, String password) throws Exception {
//        try {
//            Artist artist = (Artist) artistFileManager.loadAccountByNickName(nickname);
//            if (artist != null) {
//                return artist;
//            }
//        } catch (IllegalStateException e) {
//            System.err.println(e.getMessage());
//            e.printStackTrace();
//        }
//        return new Artist(email, nickname, password);
//    }
//
//    private static void setupArtistBasics(Artist artist, boolean approved) throws Exception {
//        artist.setApproved(approved);
//        artistFileManager.saveAccount(artist);
//
//        if (!approved) {
//            artist.getSingles().clear();
//            artist.getAlbums().clear();
//            artist.getFollowers().clear();
//            songFileManager.saveSongsAndAlbumsForArtist(artist);
//        }
//    }
//
//    private static void addSingle(Artist artist, String title, String lyrics, String date, String imagePath) throws IOException {
//        if (artist.isApproved()) {
//            Song song = new Song(title, lyrics, date);
//            song.addArtist(artist);
//            artist.addSingle(song);
//            song.setViews(random.nextInt(2000000) + 500000);
//            song.setLikes(random.nextInt(500000) + 100000);
//            song.setAlbumArtPath(copyImageToDestination(imagePath));
//            songFileManager.saveSongsAndAlbumsForArtist(artist);
//
//            String safeArtistNickName = artist.getNickName().replaceAll("[^a-zA-Z0-9]", "_");
//            String safeSongTitle = title.replaceAll("[^a-zA-Z0-9]", "_");
//            String singleDir = BASE_ARTISTS_PATH + safeArtistNickName + "/singles/";
//            Path singleDirPath = Paths.get(singleDir);
//            if (!Files.exists(singleDirPath)) {
//                Files.createDirectories(singleDirPath);
//            }
//
//            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
//            String timestamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
//            String prefix = "a1"; // پیشوند ثابت برای سازگاری با الگو
//            String metadataFile = singleDir + prefix + "-" + timestamp + "_metadata.txt";
//            String lyricsFile = singleDir + prefix + "-" + timestamp + "_lyrics.txt";
//
//            List<String> metadata = new ArrayList<>();
//            metadata.add("Title: " + title);
//            metadata.add("Artist: " + artist.getNickName());
//            metadata.add("Album: Single");
//            Files.write(Paths.get(metadataFile), metadata);
//            System.out.println("Created single metadata file: " + metadataFile);
//
//            List<String> lyricsData = new ArrayList<>();
//            lyricsData.add(lyrics);
//            Files.write(Paths.get(lyricsFile), lyricsData);
//            System.out.println("Created single lyrics file: " + lyricsFile);
//        }
//    }
//
//    private static Album findOrCreateAlbum(Artist artist, String title, String date, String imagePath) throws IOException {
//        if (artist.isApproved()) {
//            Album album = artist.getAlbums().stream()
//                    .filter(a -> a.getTitle().equals(title))
//                    .findFirst()
//                    .orElse(null);
//            if (album == null) {
//                album = new Album(title, date, artist);
//                album.setAlbumArtPath(copyImageToDestination(imagePath));
//                artist.addAlbum(album);
//            }
//            return album;
//        }
//        return null;
//    }
//
//    private static void addSongToAlbum(Album album, String title, String lyrics, String date, String imagePath) throws IOException {
//        if (album != null && album.getArtist().isApproved()) {
//            Song song = album.getSongs().stream()
//                    .filter(s -> s.getTitle().equals(title))
//                    .findFirst()
//                    .orElse(null);
//            if (song == null) {
//                song = new Song(title, lyrics, date);
//                song.addArtist(album.getArtist());
//                song.setAlbum(album);
//                album.addSong(song);
//                song.setViews(random.nextInt(1500000) + 300000);
//                song.setLikes(random.nextInt(400000) + 80000);
//                song.setAlbumArtPath(copyImageToDestination(imagePath));
//                songFileManager.saveSongsAndAlbumsForArtist(album.getArtist());
//
//                String safeArtistNickName = album.getArtist().getNickName().replaceAll("[^a-zA-Z0-9]", "_");
//                String safeAlbumName = album.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
//                String safeSongTitle = title.replaceAll("[^a-zA-Z0-9]", "_");
//                String albumDir = BASE_ARTISTS_PATH + safeArtistNickName + "/albums/" + safeAlbumName + "/";
//                Path albumDirPath = Paths.get(albumDir);
//                if (!Files.exists(albumDirPath)) {
//                    Files.createDirectories(albumDirPath);
//                }
//
//                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
//                String timestamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
//                String prefix = "a1"; // پیشوند ثابت برای سازگاری با الگو
//                String metadataFile = albumDir + prefix + "-" + timestamp + "_metadata.txt";
//                String lyricsFile = albumDir + prefix + "-" + timestamp + "_lyrics.txt";
//
//                List<String> metadata = new ArrayList<>();
//                metadata.add("Title: " + title);
//                metadata.add("Artist: " + album.getArtist().getNickName());
//                metadata.add("Album: " + album.getTitle());
//                Files.write(Paths.get(metadataFile), metadata);
//                System.out.println("Created album song metadata file: " + metadataFile);
//
//                List<String> lyricsData = new ArrayList<>();
//                lyricsData.add(lyrics);
//                Files.write(Paths.get(lyricsFile), lyricsData);
//                System.out.println("Created album song lyrics file: " + lyricsFile);
//            }
//        }
//    }
//
//    private static void followArtist(String artistNickname, User user) {
//        Artist artist = artistFileManager.loadAllArtists().stream()
//                .filter(a -> a.getNickName().equals(artistNickname))
//                .findFirst()
//                .orElse(null);
//        if (artist != null && artist.isApproved() && !user.getFollowingArtists().contains(artist)) {
//            user.followArtist(artist);
//            userFileManager.saveAccount(user);
//            artistFileManager.saveFollowers(artist, artist.getFollowers());
//            System.out.println(user.getNickName() + " followed " + artistNickname);
//        }
//    }
//
//    private static String copyImageToDestination(String destinationPath) throws IOException {
//        File sourceFile = new File(DEFAULT_IMAGE_PATH);
//        if (!sourceFile.exists()) {
//            System.out.println("Default image not found at " + DEFAULT_IMAGE_PATH + ", using placeholder path.");
//            return "placeholder_image.jpg";
//        }
//
//        File destinationFile = new File(destinationPath);
//        File destinationDir = destinationFile.getParentFile();
//
//        if (!destinationDir.exists()) {
//            boolean created = destinationDir.mkdirs();
//            if (!created) {
//                throw new IOException("Failed to create directory: " + destinationDir);
//            }
//        }
//
//        Files.copy(sourceFile.toPath(), destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
//        System.out.println("Copied image to: " + destinationPath);
//        return destinationPath;
//    }
//
//    public static void verifyTestData() {
//        List<Artist> artists = artistFileManager.loadAllArtists();
//        System.out.println("Total Artists Loaded: " + artists.size());
//        for (Artist artist : artists) {
//            System.out.println("Artist: " + artist.getNickName() + ", Approved: " + artist.isApproved());
//            if (artist.isApproved()) {
//                artist.loadSongsAndAlbums(songFileManager, artistFileManager);
//                List<Song> singles = artist.getSingles();
//                List<Album> albums = artist.getAlbums();
//                System.out.println("  Singles: " + singles.size());
//                for (Song single : singles) {
//                    System.out.println("    Single: " + single.getTitle() + " (Views: " + single.getViews() + ", Likes: " + single.getLikes() + ")");
//                }
//                System.out.println("  Albums: " + albums.size());
//                for (Album album : albums) {
//                    System.out.println("    Album: " + album.getTitle() + " (Songs: " + album.getSongs().size() + ")");
//                    for (Song song : album.getSongs()) {
//                        System.out.println("      Song: " + song.getTitle() + " (Views: " + song.getViews() + ", Likes: " + song.getLikes() + ")");
//                    }
//                }
//            } else {
//                System.out.println("  Singles and Albums: Not loaded (artist is unapproved)");
//            }
//            artist.loadFollowers(artistFileManager, userFileManager.loadAllUsers());
//            System.out.println("  Followers: " + artist.getFollowers().size());
//        }
//
//        List<User> users = userFileManager.loadAllUsers();
//        System.out.println("Total Users Loaded: " + users.size());
//        for (User user : users) {
//            user.loadFollowingArtistsFromFile(artistFileManager, userFileManager);
//            System.out.println("User " + user.getNickName() + " follows: " + user.getFollowingArtists().size() + " artists");
//            for (Artist followedArtist : user.getFollowingArtists()) {
//                System.out.println("  Follows: " + followedArtist.getNickName());
//            }
//        }
//
//        validateDataConsistency(artists, users);
//    }
//
//    private static void validateDataConsistency(List<Artist> artists, List<User> users) {
//        for (Artist artist : artists) {
//            int expectedFollowers = (int) users.stream()
//                    .filter(user -> user.getFollowingArtists().stream()
//                            .anyMatch(a -> a.getNickName().equals(artist.getNickName())))
//                    .count();
//            int actualFollowers = artist.getFollowers().size();
//            if (expectedFollowers != actualFollowers) {
//                System.out.println("Inconsistency detected: Artist " + artist.getNickName() +
//                        " has " + actualFollowers + " followers, but expected " + expectedFollowers);
//            }
//        }
//
//        for (Artist artist : artists) {
//            if (!artist.isApproved()) {
//                if (!artist.getSingles().isEmpty() || !artist.getAlbums().isEmpty()) {
//                    System.out.println("Inconsistency detected: Unapproved artist " + artist.getNickName() +
//                            " has songs or albums, which should not be allowed.");
//                }
//            }
//        }
//    }
//
//    public static void main(String[] args) {
//        try {
//            setupTestData();
//            verifyTestData();
//            System.out.println("Test data setup and verification completed successfully.");
//        } catch (Exception e) {
//            System.err.println("Failed to setup or verify test data: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    // Helper classes for organizing data
//    private static class SingleInfo {
//        String title;
//        String lyrics;
//        String date;
//        String path;
//
//        SingleInfo(String title, String lyrics, String date, String path) {
//            this.title = title;
//            this.lyrics = lyrics;
//            this.date = date;
//            this.path = path;
//        }
//    }
//
//    private static class AlbumInfo {
//        String title;
//        String date;
//        String path;
//        List<SongInfo> songs;
//
//        AlbumInfo(String title, String date, String path, List<SongInfo> songs) {
//            this.title = title;
//            this.date = date;
//            this.path = path;
//            this.songs = songs;
//        }
//    }
//
//    private static class SongInfo {
//        String title;
//        String lyrics;
//        String date;
//        String path;
//
//        SongInfo(String title, String lyrics, String date, String path) {
//            this.title = title;
//            this.lyrics = lyrics;
//            this.date = date;
//            this.path = path;
//        }
//    }
//}


// TODO : needs correction