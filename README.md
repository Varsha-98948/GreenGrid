# 🧩 GreenGrid

> **Tracking the little pieces of a developer's journey, all in one place.**

GreenGrid is a developer growth dashboard built to bring different parts of a coding journey together.

Because progress isn't just a GitHub contribution graph.

It's the problems you solve, the code you write, the revisions you make, the contributions you build up, and the small improvements that happen along the way.

GreenGrid brings these pieces together so you can actually see your progress instead of having it scattered across different places.

---

## ✨ What is GreenGrid?

GreenGrid is a platform for tracking and visualizing your coding journey.

It currently focuses on **problem solving, GitHub activity, and progress tracking**, with social features that let you connect with friends and view their progress too.

### What you can do with GreenGrid

* 🧠 Add and organize coding problems
* 🏷️ Categorize problems using tags
* 📝 Create and manage problem revisions
* 🔄 Save changes as new revisions
* 💾 Commit problem changes to GitHub
* 📊 Track coding progress
* 📅 View contribution activity through a calendar
* 👥 Add friends and view their progress
* 🔐 Sign in using GitHub
* 🐙 Connect your GitHub identity with your GreenGrid profile

---

# 🚀 Features

## 🔐 GitHub Authentication

GreenGrid uses GitHub for authentication, making it easy to connect your developer identity with your account.

* GitHub OAuth 2.0
* JWT-based authentication
* Secure session handling
* GitHub profile integration

---

## 🧠 Problem Tracking

GreenGrid helps keep coding problems organized instead of letting them disappear into the endless pile of problems you've solved.

You can:

* Add coding problems
* Edit problems
* Set difficulty
* Add tags
* Track problem details
* Organize your problem-solving progress

---

## 📝 Problem Revisions

Problems can change over time, so GreenGrid keeps track of those changes through revisions.

You can:

* Edit an existing revision
* Save changes as a new revision
* Keep previous revisions
* Manage different versions of a problem
* Commit revision changes to GitHub

This makes the problem editor more than just a form that overwrites whatever was there before.

---

## 🐙 GitHub Integration

GitHub is a core part of GreenGrid.

The platform uses GitHub to connect your developer identity and coding activity with your GreenGrid profile.

GitHub integration includes:

* GitHub OAuth login
* GitHub profile information
* Contribution activity
* Repository-related integration
* Committing changes from GreenGrid to GitHub

---

## 📊 Progress Tracking

GreenGrid gives you a place to look at your coding activity as a whole.

Instead of focusing on a single metric, the dashboard brings together different parts of your progress.

You can keep track of:

* Problems solved
* Coding activity
* GitHub contributions
* Progress over time

The idea isn't to turn your entire developer journey into one magical number.

It's to make the different pieces visible.

---

## 👥 Friends & Progress

GreenGrid also lets you see how the people around you are progressing.

You can:

* Send friend requests
* Accept friend requests
* View your friends
* View a friend's progress
* See their contribution activity

Because sometimes checking what your friends are building is enough motivation to get back to your own code.

---

## 📅 Contribution Calendar

GreenGrid includes a contribution calendar to visualize coding activity over time.

It gives you a quick way to see when you've been active and how your contribution patterns change over time.

---

# 🏗️ System Architecture

At a high level, GreenGrid follows a frontend-backend architecture where the frontend communicates with the Spring Boot backend through REST APIs.

```text
                         User
                           │
                           ▼
                  GreenGrid Frontend
                    HTML / CSS / JS
                           │
                           │
                       REST APIs
                           │
                           ▼
                  Spring Boot Backend
                     Java + Maven
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
          Supabase      GitHub APIs   Application
          Database                     Services
```

The frontend handles the interface and user interactions, while the backend handles authentication, business logic, data access, GitHub integration, and API communication.

Supabase is used for the application's database layer.

---

# 🛠️ Tech Stack

## Frontend

| Technology   | Purpose                              |
| ------------ | ------------------------------------ |
| HTML5        | Application structure                |
| CSS3         | Styling and UI                       |
| JavaScript   | Frontend logic and API communication |
| GitHub Pages | Frontend hosting                     |

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

## Database & Backend Services

| Technology | Purpose                             |
| ---------- | ----------------------------------- |
| Supabase   | Database and backend infrastructure |
| PostgreSQL | Application database                |
| Flyway     | Database migrations                 |

---

## APIs & Integrations

| Technology   | Purpose                                                 |
| ------------ | ------------------------------------------------------- |
| GitHub API   | GitHub profile, repository and contribution integration |
| GitHub OAuth | Authentication                                          |
| REST APIs    | Frontend-backend communication                          |

---

## Deployment

| Technology     | Purpose          |
| -------------- | ---------------- |
| GitHub         | Version control  |
| GitHub Actions | CI/CD            |
| GitHub Pages   | Frontend hosting |
| Render         | Backend hosting  |
| Supabase       | Database hosting |

---

# 🔄 Authentication Flow

```text
User
 │
 ▼
GreenGrid Frontend
 │
 ▼
GitHub OAuth Login
 │
 ▼
GitHub Authorization
 │
 ▼
Spring Boot Callback API
 │
 ▼
JWT Authentication
 │
 ▼
Authenticated GreenGrid Session
 │
 ▼
GreenGrid Dashboard
```

---



# 🌐 Live Deployment

### Frontend

🔗 https://varsha-98948.github.io/GreenGrid/

### Backend API

🔗 https://greengrid-byh0.onrender.com

---

# 🤝 Contributors

- **[Varsha Jairam](https://github.com/Varsha-98948)** · Creator & Developer
- **[Atharva Deshmukh](https://github.com/DeadShotKira)** · Contributor · Proposed and implemented the **Friends & Progress** feature
