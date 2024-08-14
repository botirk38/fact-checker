# TruthLens: Video Claim Analysis Platform

**TruthLens** is a cutting-edge video analysis platform that leverages artificial intelligence to scrutinize claims made in video content, providing a comprehensive fact-checking and content categorization solution.

## Table of Contents
1. [Features](#features)
2. [Prerequisites](#prerequisites)
3. [Installation](#installation)
4. [Usage](#usage)
5. [API Documentation](#api-documentation)
6. [Contributing](#contributing)
7. [License](#license)
8. [Contact](#contact)

## Features
- **Claim Extraction**: Automatically identifies and extracts claims made in video content using advanced natural language processing.
- **Fact-Checking**: Verifies extracted claims against a vast database of verified facts and reputable sources.
- **Truth Percentage**: Calculates an overall truth score for each video based on the veracity of its claims.
- **Content Categorization**: Groups videos based on their subject matter using state-of-the-art topic modeling algorithms.
- **Trend Analysis**: Identifies trending topics and recurring themes across multiple videos.
- **Interactive Visualizations**: Presents analysis results through intuitive, interactive charts and graphs.
- **API Integration**: Offers a robust API for seamless integration with other platforms and services.

## Prerequisites
- Java 21 or higher
- Node.js and npm (for Tailwind CSS)
- Gradle
- An OpenAI API key

## Installation

1. **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/truthlens.git
    cd truthlens
    ```

2. **Set up the OpenAI API key**:
    ```bash
    export OPENAI_API_KEY=your_api_key_here
    ```
   *Note: For persistent configuration, add this line to your `~/.bashrc` or `~/.zshrc` file.*

3. **Install Java dependencies**:
    ```bash
    ./gradlew build
    ```

4. **Install Node.js dependencies for Tailwind CSS**:
    ```bash
    npm install
    ```

5. **Build the Tailwind CSS**:
    ```bash
    npm run build-css
   
    ```
6. **Watch changes in template files**
    ```bash
   
   npm run watch-css
   
   ```

7. **Start the application**:
    ```bash
    ./gradlew bootRun
    ```
   
## Usage

1. **Upload a video** through the web interface or API.
2. **Processing**: TruthLens will process the video, extracting audio and transcribing it if necessary.
3. **Claim Identification and Fact-Checking**: Claims are identified and fact-checked against our extensive database.
4. **Results Presentation**:
    - Overall truth percentage
    - Breakdown of true, false, and unverified claims
    - Content categories and tags
    - Similar videos and trending topics


## API Documentation

TruthLens provides a RESTful API for integrating our analysis capabilities into your own applications. For detailed API documentation, including endpoints, request/response formats, and authentication, please visit our [API Documentation](#).

## Contributing

We welcome contributions from the community! If you'd like to contribute to TruthLens, please take a look at our [Contributing Guidelines](#). Here are some ways you can help:
- Report bugs and suggest features
- Improve documentation
- Write code and fix issues
- Help with testing and code reviews

