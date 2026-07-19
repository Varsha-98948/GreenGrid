# 🧩 GreenGrid

> **A developer growth dashboard that brings together coding journeys, problem-solving progress, and contributions in one place.**

GreenGrid is a platform built to help developers track, organize, and visualize their complete growth journey across different coding platforms and development activities.

A developer's progress is not limited to GitHub commits alone. Every solved problem, every project built, every challenge completed, and every contribution made represents a piece of their journey.

GreenGrid brings these pieces together into one unified dashboard.

---

## ✨ Vision

Modern developers learn and grow across multiple platforms:

* 🧠 Solving problems on coding platforms
* 💻 Building personal and professional projects
* 🚀 Contributing to open-source
* 📚 Learning new technologies
* 📈 Tracking personal growth

However, this progress is scattered across different platforms.

**GreenGrid aims to create a single place where developers can understand, track, and showcase their journey.**

---

# 🚀 Features

## 🔐 Secure Authentication

* GitHub OAuth 2.0 authentication
* JWT-based session management
* Secure user identification through GitHub profiles

## 📊 Developer Dashboard

* Centralized view of developer activity
* Track coding progress and achievements
* Visual representation of growth over time

## 🧩 Unified Progress Tracking

Bring together different parts of development:

* Coding problems solved
* Projects completed
* Contributions made
* Learning milestones

## 🌐 Platform Integration

Designed to connect developer activity from multiple sources:

* GitHub
* Coding platforms
* Developer tools

*(More integrations coming soon)*

---

# 🏗️ System Architecture

```
                    User

                     |
                     ↓

          GitHub Pages Frontend
              HTML / CSS / JS

                     |
                     |
                REST APIs

                     ↓

             Spring Boot Backend
                 Java + Maven

                     |
                     ↓

              Data & Services
```

---

# 🛠️ Tech Stack

## Frontend

| Technology   | Purpose                              |
| ------------ | ------------------------------------ |
| HTML5        | Application structure                |
| CSS3         | Styling and UI design                |
| JavaScript   | Frontend logic and API communication |
| GitHub Pages | Frontend deployment                  |

---

## Backend

| Technology       | Purpose                          |
| ---------------- | -------------------------------- |
| Java             | Backend development              |
| Spring Boot      | REST API development             |
| Maven            | Dependency management            |
| JWT              | Authentication and authorization |
| GitHub OAuth 2.0 | User authentication              |

---

## Deployment

| Technology     | Purpose                   |
| -------------- | ------------------------- |
| GitHub         | Version control           |
| GitHub Actions | CI/CD deployment workflow |
| GitHub Pages   | Frontend hosting          |
| Render         | Backend cloud hosting     |

---

# 🔄 Authentication Flow

```
User
 |
 ↓
GreenGrid Frontend
 |
 ↓
GitHub OAuth Login
 |
 ↓
GitHub Authorization
 |
 ↓
Spring Boot Callback API
 |
 ↓
Generate JWT Tokens
 |
 ↓
Authenticated Dashboard
```

---

# 📂 Project Structure

```
GreenGrid

├── frontend
│   ├── html pages
│   ├── css
│   ├── javascript
│   └── assets
│
├── backend
│   ├── controllers
│   ├── services
│   ├── security
│   ├── models
│   └── configuration
│
└── README.md
```

---

# 🌐 Live Deployment

### Frontend

🔗 https://varsha-98948.github.io/GreenGrid/

### Backend API

🔗 https://greengrid-byh0.onrender.com

---

# 🤝 Contribution

GreenGrid is built around the idea that every contribution matters.

Whether it is solving a problem, fixing a bug, improving documentation, or adding a feature — every improvement becomes another piece of the bigger picture.

---

# 👩‍💻 Author

**Varsha Jairam**

## ⭐ Why GreenGrid?

A developer's journey cannot be measured by a single number.

It is built from thousands of small achievements.

**Every problem solved.
Every project built.
Every contribution made.**

🧩 Every piece matters.
