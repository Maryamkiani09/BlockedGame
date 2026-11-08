# BlockedGame

BlockedGame is a Java-based puzzle/block game built with Maven, featuring custom UI panels, sound management, game modes, and high-score tracking[cite: 1].

## Features
* **Game Engine & Modes**: Core engine supporting gameplay logic, multiple game modes (such as Classic and Time Attack), and dynamic block piece generation[cite: 1].
* **UI & Graphics**: Custom user interface components (`GameFrame`, `GamePanel`) built with Java Swing, styled with custom fonts (`Kenney Future Narrow.ttf`) and asset graphics[cite: 1].
* **Sound Effects**: Integrated audio support (`SoundManager`) with sound effects for clearing blocks, placing pieces, invalid moves, and selections[cite: 1].
* **Persistence**: Local score tracking and repository utilities to save and load best scores[cite: 1].

## Project Structure
* `src/main/java/com/blocked/data/`: Data loading and score repositories[cite: 1].
* `src/main/java/com/blocked/engine/`: Core game loop engine and sound management[cite: 1].
* `src/main/java/com/blocked/model/`: Game states, piece generators, boards, and block piece models[cite: 1].
* `src/main/java/com/blocked/ui/`: Swing UI frames and rendering panels[cite: 1].
* `src/main/resources/`: Images, icons, audio effects, and fonts[cite: 1].

## Build & Run
Make sure you have Java and Maven installed. 

1. Compile and build the project:
   ```bash
   mvn clean compile
2.Run the application via your preferred IDE or package it using Maven.