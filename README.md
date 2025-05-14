# GENIUS

![GENIUS Logo](src/main/resources/pics/Genius.com_logo_yellow.png)

**GENIUS** is a comprehensive music management platform that allows users to explore songs, albums, and artists. It provides specialized interfaces for different user roles including regular users, artists, and administrators.

### Features

#### For Users
- Browse and search for songs and albums
- View song details including lyrics, release date, views, and likes
- Like songs and view song charts
- Request lyrics edits for songs
- Add comments to songs and albums

#### For Artists
- Manage your music catalog
- Add, edit, and delete songs
- Create, edit, and delete albums
- Upload album artwork
- Review and manage lyrics edit requests from users

#### For Administrators
- Approve or reject artist registration requests
- Manage lyrics edit requests
- Monitor platform activity

### User Interface
GENIUS features a modern, sleek interface with a distinctive yellow and black color scheme inspired by the original Genius.com branding.

#### Authentication Screens

**Sign Up Screen**  
![Sign Up Screen](shots/SignUp.png)

**Sign In Screen**  
![Sign In Screen](shots/SignIn.png)

**User Interface**  
![User Interface Screen](shots/UserProfile.png)

#### Song Management

**Add Song Interface**  
![Add Song Interface](shots/AddSong.png)

#### Song Details
![Song Details View](shots/SongP1.png)

#### Album Management

**Create Album Interface**  
![Create Album Interface](shots/CreateAlbum.png)

#### Album Details
![Album Details View](shots/AlbumP1.png)

### Technical Details
GENIUS is built using:

- **Java**
- **JavaFX** for the user interface
- **FXML** for layout definitions
- Custom file-based data storage system

### Installation Process
The GENIUS application can be installed either by running a pre-built executable or by building from source code.

#### Method 1: Using Pre-built Executable
1. Download the latest release JAR file from the project repository
2. Ensure you have Java JDK 17+ installed
3. Create a directory where you want to store the application data
4. Run the application using the command:
  
   ```
   java -jar GENIUS.jar
   ```
#### Method 2: Building from Source
1. Clone the repository:

   ```
   git clone https://github.com/AryanGh-imp/GENIUS.git
   cd GENIUS
   ```
2. Build the project using Gradle:

   ```
   ./gradlew build
   ```
3. Run the application:
 
   ```
   ./gradlew run
   ```

### Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

### License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
