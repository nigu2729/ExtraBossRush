Source Installation Information for Modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It applies
some small patches to the vanilla MCP source code, giving you and it access
to the data and functions you need to build a successful mod.

⚖️ LICENSE NOTICE
This project is governed by a custom license (see LICENSE file).
Section 3 of the license must be inherited and remain unmodified. The Primary Author (tasogarerui) reserves all rights to the core logic.

Setup Process:
==============================

Step 1: Open your command-line and browse to the folder where you extracted the project.

Step 2: Choose your IDE.

If you prefer to use IntelliJ IDEA:
1. Open IDEA, and import the project.
2. Select your build.gradle file to begin the import.
3. Run the following command: `./gradlew genIntellijRuns`
4. Refresh the Gradle Project in IDEA if required.

If you prefer to use Eclipse:
1. Run the following command: `./gradlew genEclipseRuns`
2. Open Eclipse, then go to Import > Existing Gradle Project and select the folder.

Troubleshooting:
=============================
If you are missing libraries or encounter problems, run `./gradlew --refresh-dependencies` to refresh the local cache. Use `./gradlew clean` to reset the environment (this does not affect your code) and then restart the process.