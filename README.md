# EcoSpotMapGoogle

Made By HUYNH THIEN AN NGUYEN

A native Android application that leverages Google Maps APIs to help users discover and navigate to eco-friendly locations in their area. The app combines location services with environmental awareness, providing a seamless experience for finding sustainable businesses, recycling centers, and green spaces.

## Features

- **Interactive Map Interface**: Real-time Google Maps integration with custom markers for eco-friendly locations
- **Location Discovery**: Search and filter eco-friendly spots including recycling centers, thrift stores, and farmers markets
- **Navigation Support**: Turn-by-turn directions using Google Directions API
- **User Profiles**: Personalized experience with activity tracking and achievements
- **Gamification**: Badge system and progress tracking to encourage sustainable behavior
- **Recent Activity**: History of visited locations and completed activities

## Technical Architecture

### Core Technologies
- **Language**: Kotlin
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 35 (Android 15)
- **Architecture**: Single Activity with multiple fragments
- **UI Framework**: Material Design 3

### Key Dependencies
- Google Maps SDK for Android (18.2.0)
- Google Places API (3.4.0)
- Google Directions API
- Retrofit 2.9.0 for network requests
- Glide 4.16.0 for image loading
- Gson 2.10.1 for JSON serialization

## Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Google Cloud Platform account
- Android device or emulator running API 24+

### Setup Process

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/EcoSpotMapGoogle.git
   cd EcoSpotMapGoogle
   ```

2. **Configure Google Maps API**
   - Navigate to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing project
   - Enable required APIs:
     - Maps SDK for Android
     - Places API
     - Directions API
   - Generate API credentials
   - Apply restrictions:
     - Restrict to Android applications
     - Add package name: `com.zaigame.ecospotmapgoogle`
     - Add your app's SHA-1 fingerprint

3. **Configure Application**
   - Open `app/src/main/res/values/strings.xml`
   - Replace `YOUR_API_KEY_HERE` with your actual API key:
   ```xml
   <string name="google_maps_key" translatable="false">YOUR_ACTUAL_API_KEY</string>
   ```

4. **Build and Run**
   - Sync project with Gradle files
   - Build project (Ctrl+F9)
   - Run on device or emulator

## Project Structure

```
app/src/main/java/com/zaigame/ecospotmapgoogle/
├── MainActivity.kt              # Main application entry point
├── ProfileActivity.kt           # User profile management
├── EcoSpot.kt                   # Data model for eco-friendly locations
├── DirectionsApiService.kt      # Google Directions API integration
├── adapters/
│   ├── BadgeAdapter.kt          # RecyclerView adapter for badges
│   └── RecentActivityAdapter.kt # RecyclerView adapter for activities
├── models/
│   └── UserProfile.kt           # User profile data model
└── services/
    └── GamificationService.kt   # Achievement and badge management
```

## API Integration

### Google Maps SDK
The application integrates multiple Google Maps services:

- **Maps SDK**: Displays interactive map with custom markers
- **Places API**: Searches for eco-friendly businesses and locations
- **Directions API**: Provides navigation routes between locations

### Security Considerations
- API keys are stored in `strings.xml` (excluded from version control)
- Keys are restricted to specific Android applications
- No sensitive data is transmitted without encryption

## Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Implement proper error handling
- Add comments for complex logic

### Testing
- Unit tests for business logic
- Instrumented tests for UI components
- API integration testing with mock responses

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For technical support or feature requests, please open an issue on GitHub or contact the development team.
nguyenan04102004@gmail.com

## Acknowledgments

- Google Maps Platform for providing the mapping and location services
- Material Design team for the UI components and guidelines
- Android developer community for best practices and resources

  Made By HUYNH THIEN AN NGUYEN
