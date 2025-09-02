# Clipboard Monitor - User Manual

## Table of Contents
1. [Introduction](#introduction)
2. [Features](#features)
3. [Getting Started](#getting-started)
4. [Using the Application](#using-the-application)
   - [Main Screen](#main-screen)
   - [Clipboard History](#clipboard-history)
   - [Service Control](#service-control)
5. [Advanced Features](#advanced-features)
   - [Language Selection](#language-selection)
   - [Image Handling](#image-handling)
   - [URI Handling](#uri-handling)
6. [Configuration](#configuration)
7. [Troubleshooting](#troubleshooting)
8. [About](#about)

## Introduction
Clipboard Monitor is an Android application that keeps track of everything you copy to your clipboard, including text, images, and file references. It runs in the background and maintains a history of your clipboard items that you can access anytime.

## Features
- Monitors clipboard changes in real-time
- Stores history of copied text, images, and URIs
- Supports multiple languages (English, Russian, Ukrainian, Romanian)
- Image compression and preview
- Share or copy items from history
- Background service with notification

## Getting Started
1. **Installation**: Download and install the app from the RuStore
2. **Permissions**: Grant necessary permissions when prompted:
   - Clipboard access
   - Storage access (for images)
   - Notification permission (Android 13+)
3. **First Run**: The app will start monitoring your clipboard automatically if configured to do so

## Using the Application

### Main Screen
The main screen provides:
- Service status indicator (running/stopped)
- Button to view clipboard history
- Button to start/stop monitoring service
- Button to clear all history
- Menu button (top-right) for language selection and about information

### Clipboard History
Access your clipboard history by tapping "View Clipboard":
- Items are displayed in chronological order (newest first)
- Each entry shows:
   - Content preview (text or image thumbnail)
   - Type indicator (color-coded)
   - Timestamp
   - Action buttons (share, delete)

**Actions:**
- **Tap**: Copy item back to clipboard
- **Long-press**: Show context menu with additional options
- **Share**: Share the item with other apps
- **Delete**: Remove item from history

### Service Control
- **Start Service**: Begins monitoring clipboard (runs in background)
- **Stop Service**: Pauses monitoring (history remains)
- Status is shown at the top of main screen

## Advanced Features

### Language Selection
Change the app language through the menu:
1. Tap the menu button (top-right)
2. Select "Language"
3. Choose from available options
4. App will restart with new language

### Image Handling
- Images are compressed to save space
- Thumbnails are shown in history
- Full images can be copied back or shared
- On Android 15+, only image references are stored by default

### URI Handling
- File and content URIs are stored as references
- For images, you can choose to import the full image
- URIs can be copied or shared like regular items

## Configuration
The app can be configured via `config.toml` (advanced users):
- Maximum entries to keep (default: 100)
- Auto-cleanup (default: enabled)
- Image compression quality (default: 80%)
- Notification settings
- Database name and version
- UI theme and preview settings

## Troubleshooting
**Common Issues:**
1. **Clipboard not being monitored**:
   - Ensure service is running (check main screen status)
   - Grant all required permissions
   - Restart the app if needed

2. **Images not appearing**:
   - Check storage permissions
   - On Android 15+, images may be stored as references only

3. **Service stops unexpectedly**:
   - Disable battery optimization for the app
   - Ensure the app isn't being killed by system cleanup

## About
- Version: [shown in app header]
- Description: Clipboard monitoring utility
- Developer information available in About dialog

For additional support, please contact the developer through the app store listing.

---

*Note: This manual is based on version ${AppInfo.getFormattedVersion(context)} of Clipboard Monitor. Some features may vary depending on your device and Android version.*