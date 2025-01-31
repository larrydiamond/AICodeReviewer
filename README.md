# AI Code Reviewer README

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=larrydiamond_AICodeReviewer&metric=bugs)](https://sonarcloud.io/summary/new_code?id=larrydiamond_AICodeReviewer)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=larrydiamond_AICodeReviewer&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=larrydiamond_AICodeReviewer)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=larrydiamond_AICodeReviewer&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=larrydiamond_AICodeReviewer)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=larrydiamond_AICodeReviewer&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=larrydiamond_AICodeReviewer)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=larrydiamond_AICodeReviewer&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=larrydiamond_AICodeReviewer)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=larrydiamond_AICodeReviewer&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=larrydiamond_AICodeReviewer)

Welcome to the **AI Code Reviewer** repository! This project provides an AI-powered tool for reviewing code files and providing actionable feedback to fix bugs, improve application speed, and fix security problems.

---

## Features
- Automated code review for various programming languages.
- In-depth suggestions for improving code quality.
- Simple command-line interface for ease of use.

---

## Demos
- Fix Calculate Circumference - https://youtu.be/gjapO5D0guk
- Fix copy and paste bug - https://youtu.be/YdeEu7ofzV4

---

## Examples
- Used the wrong collection [java code](https://github.com/larrydiamond/AICodeReviewer/blob/main/src/main/java/com/ldiamond/dli/examples/Usedthewrongcollection.java)
[output](https://github.com/larrydiamond/AICodeReviewer/blob/main/src/main/java/com/ldiamond/dli/examples/Usedthewrongcollection.output)
- Buggy algorithm [java code](https://github.com/larrydiamond/AICodeReviewer/blob/main/src/main/java/com/ldiamond/dli/examples/Previousreference.java) [output](https://github.com/larrydiamond/AICodeReviewer/blob/main/src/main/java/com/ldiamond/dli/examples/Previousreference.output)

---

## Requirements
Before running the AI Code Reviewer, you need to install and set up the following dependencies:

1. **Java**  
   Ensure you have Java installed on your system. The tool requires Java to run the `.jar` file.

2. **Ollama**  
   The AI Code Reviewer depends on **Ollama**, which needs to be installed locally.

3. **AI Models**  
   Install the following AI models via Ollama:
   - `gemma2`
   - `llama3.1`
   - `llama3.2`
   - `qwen2.5-coder`
   - `falcon3`

---

## Installation

### 1. Install Java
Download and install Java from [https://www.java.com](https://www.java.com) if it is not already installed on your system.

### 2. Install Ollama
Follow the instructions at [Ollama's official site](https://www.ollama.ai) to install it locally.

### 3. Install AI Models
Once Ollama is installed, add the required models:
```bash
ollama pull gemma2
ollama pull llama3.1
ollama pull llama3.2
ollama pull qwen2.5-coder
ollama pull falcon3
```

### 4. Clone the Repository
Clone this repository to your local machine:
```bash
git clone https://github.com/yourusername/aicodereviewer.git
cd aicodereviewer
```

---

## Usage

Run the AI Code Reviewer using the following command:

```bash
java -jar dli-0.0.2-SNAPSHOT.jar <filename_to_review>
```

Replace `<filename_to_review>` with the path to the file you want to review. For example:
```bash
java -jar dli-0.0.2-SNAPSHOT.jar src/MyCodeFile.java
```

---

## Example Output

The AI Code Reviewer will analyze the provided file and output suggestions directly in the terminal, highlighting areas for improvement, potential bugs, and optimization tips.

---

## Contributing

We welcome contributions! If you would like to contribute to the AI Code Reviewer project:
1. Fork the repository.
2. Create a feature branch: `git checkout -b feature-name`.
3. Commit your changes: `git commit -m "Add feature"`.
4. Push to the branch: `git push origin feature-name`.
5. Open a pull request.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Support

If you encounter any issues or have questions, please open an issue in the repository

---

Happy coding and reviewing! 🚀
