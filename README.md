# KU Royale

A Clash Royale clone game built with Java and JavaFX.

## Prerequisites

- **Java 25 LTS** - The latest Long-Term Support release
- **Maven is NOT required** - this project uses Maven Wrapper (`mvnw`)

> **Note:** This project uses Java 25 LTS and JavaFX 21 LTS - all LTS versions for maximum stability!

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/yourusername/KuRoyale.git
cd KuRoyale
```

### 2. Build the project
On Windows:
```bash
.\mvnw.cmd clean install
```

On Linux/Mac:
```bash
./mvnw clean install
```

### 3. Run the application
On Windows:
```bash
.\mvnw.cmd javafx:run
```

On Linux/Mac:
```bash
./mvnw javafx:run
```

## Project Structure

```
KuRoyale/
├── .mvn/                  # Maven wrapper files
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── kuroyale/
│       │           └── Main.java
│       └── resources/     # Images, sounds, FXML files
├── documentation/         # Project documentation
├── mvnw                   # Maven wrapper script (Unix)
├── mvnw.cmd              # Maven wrapper script (Windows)
├── pom.xml               # Maven project configuration
└── README.md
```

## Technologies

- **Java 25 LTS** - Latest Long-Term Support release
- **JavaFX 21.0.5 LTS** - Latest stable UI framework
- **Maven 3.9.11** - Build automation (via wrapper)

## Development

The Maven wrapper automatically downloads the correct Maven version, so you don't need to install Maven manually.

### Useful Commands

- `.\mvnw.cmd clean` - Clean build artifacts
- `.\mvnw.cmd compile` - Compile the project
- `.\mvnw.cmd test` - Run tests
- `.\mvnw.cmd package` - Create JAR package
- `.\mvnw.cmd javafx:run` - Run the application