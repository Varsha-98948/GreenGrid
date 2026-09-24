# GreenGrid Chrome Extension

A Chrome browser extension (Manifest V3) that detects accepted coding solutions on **LeetCode** and saves them directly to your **GreenGrid** account with one click.

## Features

- **Automatic Detection**: Watches LeetCode submissions using a non-intrusive `MutationObserver` and triggers only upon confirmed `Accepted` status.
- **Accurate Code Extraction**: Pulls actual submitted solution code and language metadata directly from the submission and editor DOM.
- **Smart Deduplication**: Prevents duplicate prompts across page reloads and DOM re-renders using local storage fingerprints.
- **Non-Intrusive In-Page UI**: Clean, floating confirmation card rendered in an isolated Shadow DOM so it never conflicts with LeetCode styling.
- **Backend & GitHub Sync**: Automatically connects with GreenGrid's existing `ProblemService`, persisting revisions and triggering GitHub commits server-side.
- **Environment Toggle**: Easily switch between Localhost (`http://localhost:8080`) and Production (`https://greengrid-byh0.onrender.com`) via the extension popup.

---

## Build Instructions

If you modify extension source files, rebuild the extension using:

```bash
cd extension
npm install
npm run build
```

This compiles TypeScript, bundles React components, and outputs the production extension into the `dist/` directory.

---

## Loading the Extension in Google Chrome

1. Open Google Chrome and navigate to:
   ```text
   chrome://extensions
   ```
2. Enable **Developer mode** using the toggle switch in the top-right corner.
3. Click the **Load unpacked** button in the top-left corner.
4. Browse to and select the `dist` folder:
   ```text
   d:\GreenGrid\extension\dist
   ```
5. The **GreenGrid — Save Accepted Solutions** extension will now be loaded and active.

---

## Usage Workflow

1. **Connect Account**:
   - Click the GreenGrid 🌱 icon in your Chrome toolbar.
   - Enter your GreenGrid email and password (or switch backend API environment if testing locally).
   - Click **Connect to GreenGrid**.
2. **Solve on LeetCode**:
   - Go to any problem on [LeetCode](https://leetcode.com/problems/).
   - Write and submit your solution.
3. **Save Solution**:
   - Once LeetCode reports **Accepted**, the GreenGrid confirmation card appears in the bottom right corner.
   - Click **Yes, Save**.
   - GreenGrid saves the solution (creating a new problem or adding a revision if already tracked) and commits the code to your configured GitHub repository.
