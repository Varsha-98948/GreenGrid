# 🌿 GreenGrid

> **A Git-powered coding journal that transforms every solved problem into a structured GitHub repository.**

GreenGrid is a full-stack developer platform that helps programmers organize, track, and version-control their coding journey. Instead of storing solutions in scattered folders or online judges, GreenGrid automatically structures every solved problem and syncs it to GitHub, creating a clean, searchable, and portfolio-worthy repository.

---

## ✨ Features

### 📚 Problem Management
- Save coding problems from any platform
- Rich code editor with syntax highlighting
- Notes, observations, and learning points
- Time & space complexity tracking
- Favorite important problems
- Custom tags and categorization

### 🐙 GitHub Integration
- Secure GitHub OAuth authentication
- Connect an existing repository or create a new one
- Automatic repository synchronization
- Structured folder organization
- Commit history for every solved problem

### 📊 Dashboard & Analytics
- Daily progress tracking
- Current & longest solving streaks
- GitHub-style contribution calendar
- Difficulty distribution
- Topic-wise breakdown
- Programming language usage
- Recent activity overview

### 🔍 Search & Revision
- Powerful filtering
- Search by title, tag, language, or difficulty
- Revision status tracking
- Bookmark important questions

### 🎨 Modern Interface
- Responsive design
- Dark theme
- Monaco Code Editor
- Smooth animations
- Clean dashboard experience

---

# 🏗️ Tech Stack

## Backend

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT Authentication
- GitHub OAuth
- Maven

## Frontend

- HTML5
- CSS3
- Vanilla JavaScript
- Bootstrap 5
- Monaco Editor

## APIs

- GitHub REST API
- GitHub OAuth

---

# 📁 Project Structure

```
GreenGrid
│
├── backend
│   ├── src
│   ├── pom.xml
│   └── ...
│
├── frontend
│   ├── css
│   ├── js
│   ├── assets
│   └── index.html
│
└── README.md
```

---

# 🚀 Getting Started

## Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/GreenGrid.git
cd GreenGrid
```

---

## Backend Setup

Navigate to the backend directory.

```bash
cd backend
```

Configure your environment variables.

```properties
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=

JWT_SECRET=
TOKEN_ENCRYPTION_KEY=

GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
GITHUB_REDIRECT_URI=

FRONTEND_URL=
```

Run the application.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway will automatically create the database schema during startup.

---

## Frontend Setup

Navigate to the frontend directory.

```bash
cd frontend
```

Update the backend URL inside:

```
js/config.js
```

Run any static web server.

Example:

```bash
python -m http.server 5500
```

or

```bash
npx serve .
```

---

# 🔒 Security

GreenGrid follows several security best practices:

- JWT Authentication
- GitHub OAuth 2.0
- AES Encryption for GitHub Access Tokens
- Password Hashing (BCrypt)
- Spring Security
- Protected REST APIs
- Database validation using Flyway migrations

---

# 🛣️ Roadmap

## ✅ Version 1

- User Authentication
- GitHub OAuth
- Repository Management
- Problem Management
- Dashboard
- Search & Filters
- Contribution Calendar
- Revision Tracker

## 🚧 Planned Improvements

- Single Git commit per problem (Git Data API)
- AI-generated solution summaries
- AI hints
- Multi-platform import (LeetCode, Codeforces, GFG)
- Daily challenge reminders
- Repository statistics
- Team workspaces
- Public profile pages

---

# 🤝 Contributing

Contributions, suggestions, and feature requests are welcome.

If you'd like to improve GreenGrid:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Open a Pull Request

---

# 👩‍💻 Author

**Varsha Jairam**

GitHub: https://github.com/Varsha-98948

---

> **GreenGrid aims to make every solved problem part of your developer portfolio—not just another submission.**
