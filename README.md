# AR Education Android App

An Android AR (Augmented Reality) education app for physics, biology, and chemistry lessons with interactive 3D models, guided lab steps, and quiz functionality.

## Features

### 🎯 Core Features
- **AR Marker-Based Launch**: Scan markers to start specific lessons
- **Interactive 3D Models**: Touch, rotate, and interact with 3D models
- **Guided Lab Steps**: Step-by-step instructions with visual guidance
- **Offline Mode**: All content available without internet connection
- **Quiz System**: Multiple-choice quizzes with explanations
- **Progress Tracking**: Track completion, scores, and time spent
- **Low-End Device Support**: Optimized for Android 10+ devices

### 📚 Subjects
- **Physics**: Newton's Laws, Motion, Forces, Energy
- **Biology**: Cell Structure, Human Anatomy, Biology Processes
- **Chemistry**: Atomic Structure, Chemical Reactions, Elements

## Architecture

### 🏗️ Project Structure
```
app/
├── src/main/
│   ├── java/com/ar/education/
│   │   ├── data/           # Data models and repository
│   │   ├── ui/             # UI Activities and ViewModels
│   │   ├── ar/             # AR-related classes
│   │   ├── quiz/           # Quiz components
│   │   └── progress/       # Progress tracking
│   ├── res/
│   │   ├── layout/         # XML layouts
│   │   ├── values/         # Colors, strings, themes
│   │   └── drawable/       # Icons and graphics
│   ├── assets/
│   │   ├── lessons/        # Lesson data (JSON)
│   │   ├── markers/        # AR marker images
│   │   └── models/         # 3D models (GLB/GLTF)
│   └── AndroidManifest.xml
├── build.gradle           # App-level dependencies
└── proguard-rules.pro     # ProGuard configuration
```

### 🧩 Key Components

#### Data Layer
- `LessonRepository`: Manages lesson data and ARCore compatibility
- `ProgressRepository`: Tracks user progress using Room database
- `LessonModels`: Data classes for lessons, quizzes, and progress

#### UI Layer
- `MainActivity`: Entry point with subject filtering
- `ARViewerActivity`: AR camera view with 3D model interactions
- `QuizActivity`: Quiz taking interface
- `ProgressActivity`: Progress dashboard

#### AR Layer
- `ARViewerViewModel`: Manages AR session state
- Sceneform integration for 3D rendering
- Marker detection and model placement

#### Database
- Room database for progress tracking
- DataStore for preferences
- Optimized for offline storage

## Dependencies

### Core Libraries
- **ARCore**: Google's AR platform
- **SceneView**: 3D rendering framework (modern Sceneform alternative)
- **Room**: Local database
- **Navigation Component**: In-app navigation
- **ViewModel & LiveData**: Architecture components
- **Material Design**: UI components

### Key Dependencies
```gradle
implementation 'com.google.ar:core:1.41.0'
implementation 'io.github.sceneview:sceneview:2.0.0'
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.5'
```

## Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 29+ (Android 10)
- ARCore compatible device
- Physical markers for lesson launch

### Building the App
1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Build and run on device

**Note on SceneView**: This project uses SceneView (`io.github.sceneview:sceneview:2.0.0`) which is a modern, actively maintained fork of the deprecated Google Sceneform library. SceneView provides better compatibility with modern Android versions and is available on Maven Central.

### Gradle Sync Instructions

If you encounter Gradle sync issues, try these steps:

1. **Force Gradle Sync**
   - In Android Studio: File → Sync Project with Gradle Files
   - Or click the "Sync Project with Gradle Files" button in the toolbar

2. **Clean and Rebuild**
   ```
   ./gradlew clean
   ./gradlew build
   ```

3. **Invalidate Caches (Android Studio)**
   - File → Invalidate Caches → Invalidate and Restart

4. **Common Issues**
   - **"Cannot resolve external dependency"**: Ensure all repositories are defined in `settings.gradle` and `build.gradle`
   - **"Plugin not found"**: Check `pluginManagement.repositories` in `settings.gradle`
   - **Gradle version mismatch**: Verify Gradle version in `gradle/wrapper/gradle-wrapper.properties`

5. **Repository Configuration**
   - The project uses these repositories:
     - `google()` - For Android and Google libraries
     - `mavenCentral()` - For most third-party libraries
     - `gradlePluginPortal()` - For Gradle plugins
   - These are configured in:
     - `settings.gradle` (pluginManagement and dependencyResolutionManagement)
     - `build.gradle` (buildscript.repositories)

### Adding New Lessons
1. Create lesson JSON file in `assets/lessons/`
2. Add 3D model file in `assets/models/`
3. Create AR marker image in `assets/markers/`
4. Update lesson repository with new content

## Usage

### Starting a Lesson
1. Launch the app
2. Select subject (Physics/Biology/Chemistry)
3. Choose lesson from list
4. Point camera at lesson marker
5. Follow guided AR instructions

### Taking Quizzes
1. Complete lab steps or start from lesson details
2. Tap "Take Quiz" button
3. Answer multiple-choice questions
4. View results and explanations
5. Retry if needed

### Tracking Progress
1. View completed lessons
2. Check quiz scores
3. See time spent on lessons
4. Bookmark favorite lessons

## Optimization for Low-End Devices

### Performance Optimizations
- Compressed 3D models (512-1024px textures)
- Limited FPS on older devices
- Efficient memory management
- Background processing for heavy tasks

### AR Compatibility
- Graceful fallback for non-ARCore devices
- 2D model viewer when AR unavailable
- Hardware compatibility checking

### Offline Features
- All content bundled in APK
- No runtime network requirements
- Local progress storage
- Asset compression for size optimization

## Development Notes

### AR Marker Requirements
- High contrast, feature-rich images
- Minimum 500x500px resolution
- Flat, rigid surfaces for scanning

### 3D Model Guidelines
- GLB/GLTF format preferred
- Under 10MB per model
- Optimized polygon count
- Standardized material naming

### Quiz Design
- Multiple choice format
- Clear, concise questions
- Educational explanations
- 70% passing score threshold

## Future Enhancements

### Potential Features
- Multi-user collaboration
- Voice-guided instructions
- Real-time collaboration
- Advanced analytics
- Content management system
- Teacher dashboard
- Student assessment tools

### Technical Improvements
- WebXR support
- Advanced physics simulation
- Machine learning integration
- Cloud synchronization
- Push notifications

## Contributing

### Development Guidelines
1. Follow Kotlin coding standards
2. Use MVVM architecture pattern
3. Implement proper error handling
4. Add comprehensive unit tests
5. Document new features

### Code Style
- Use meaningful variable names
- Follow Material Design principles
- Implement accessibility features
- Optimize for performance
- Maintain offline-first approach

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For technical support or feature requests, please contact the development team or create an issue in the repository.