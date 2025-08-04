# 🌿 **TaskSprout**  
_A cozy, gamified task manager to grow your productivity one task at a time_

<img width="187" height="177" alt="taskSprout_logo" src="https://github.com/user-attachments/assets/24482e47-8308-411c-b360-541c05ce919a" />


---

## 📖 Description

**TaskSprout** transforms everyday task management into an engaging, collaborative experience.  
Complete tasks to grow XP, earn achievements 🏅, and plant virtual sprouts in your personal forest 🌱🌳

Built with collaboration in mind, users can create shared boards, assign tasks, and get motivated with a leaderboard and stats view. 
Whether you're a student, team player, or solo productivity warrior — TaskSprout grows with you.

---

## 🌟 Features

### 🗂️ **Board Management**
- Create, edit, delete, and share boards via email.
- “NEW” label on new, unopened boards (per device).
- All board info stored in **Firestore**.

### ✅ **Task Management**
- Tasks have 4 statuses: `TODO`, `IN PROGRESS`, `DONE`, `NEGLECTED`.
- Claim tasks to earn XP — only the assignee gets (or losses) XP for status updates.
- Drag & Drop tasks between statuses.
- Sound effects for status changes.
- Toast messages like “Task Added”, “Moved to DONE”, etc.

### 📊 **Board Statistics**
- XP configuration (available for board managers only).
- Leaderboard sorted by XP earned on the board.
- Five pie charts: Claimed, TODO, IN PROGRESS, DONE, NEGLECTED.

### 🧑‍💻 **Profile Page**
- Set your name and upload a profile picture.
- View XP total, task breakdown, achievements.
- Navigate to **Forest** and **Achievements**.
- Sign out of your user (firebase authorization).

### 🌲 **Forest Page**
- Every task you claim becomes a plant 🌱.
- Status determines the plant's appearance and stage of growth.
- Plants have permanent screen positions.
- "Sprouted!" toast and sound for new plants or status changes.
- DONE plants get automatically removed if the forest is full to make space for new plants.

### 🏆 **Achievements**
- 16 achievements with custom icons, XP, and progress tracking.
- Includes: first task done, 10 tasks done, join 5 boards, etc.
- In-app popup + confetti + sound on unlock.
- Visual progress bars and unlock indicators per achievement.

---

## 🧰 Tech Stack

- **Language**: Kotlin
- **IDE**: Android Studio

### 🔥 Firebase Services:
- Firebase Authentication (Email & Google login)
- Firebase Firestore (Boards, Tasks, Users, XP, Achievements)
- Firebase Storage (Profile Pictures)

### 📚 Libraries:
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)  
  `implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")`  
  → Used for pie charts in board stats

- [Konfetti by Daniel Martinus](https://github.com/DanielMartinus/Konfetti)  
  `implementation("nl.dionsegijn:konfetti-compose:2.0.2")`  
  `implementation("nl.dionsegijn:konfetti-xml:2.0.2")`  
  → Used for achievement confetti

- [Glide](https://github.com/bumptech/glide)  
  `implementation("com.github.bumptech.glide:glide:4.16.0")`  
  → Used for image loading (profile pics)

---

## 📸 Screenshots

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/42e11b7d-68ad-4da3-b4ef-7bf091891c0c" width="200"/><br>
      <b>Login & Register</b>
    </td>
  </tr>
</table>

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/97b892fa-8bf9-4acd-aec4-63ef9cd55f65" width="200"/><br>
      <b>Board List</b>
    </td>
  </tr>
</table>

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/699c8095-b093-478a-899d-0deba55bbac8" width="265"/><br>
      Task Board- TO DO
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/a486d212-bf23-4959-9d8b-835476334930" width="250"/><br>
      Task Board IN PROGRESS
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/2e68b134-6005-4b50-a1ad-2a29ff224249" width="550"/><br>
      Task Board DONE & NEGLECTED
    </td>
  </tr>
</table>

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/7cb0d3ea-7ec2-497c-9ee8-c2ef93f00b8f" width="200"/><br>
      Stats (XP Editor & Leaderboard)
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/29c3fbdc-3a40-49e4-9a70-27fd69f8eaa7" width="630"/><br>
      Stats (Charts)
    </td>
  </tr>
</table>

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/2e8f59d3-4c42-4c7a-a2e5-ae7419639c55" width="200"/><br>
      <b>Profile Page</b>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/092ee0d2-003b-4868-8a6f-ab658e20790a" width="210"/><br>
      <b>Forest Page</b>
    </td>
  </tr>
</table>

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/5bbc04f9-0bbe-4962-8d25-a71deb89c5bc" width="700"/><br>
      <b>Achievements</b>
    </td>
  </tr>
</table>


---

## ⚙️ Getting Started

### 1. Clone the repo:
```bash
git clone https://github.com/yourusername/tasksprout.git
```
### 🧑‍💻 2. Open the project in Android Studio

### 🔑 3. Add your Firebase configuration
Place your google-services.json file inside the /app folder

### 🔄 4. Sync & Run
Sync Gradle

Run the app on an Android emulator or physical android device (not suitable for apple users)

---

### 📝 Extra Documentation
For a complete breakdown of features, logic, and implementation decisions, check out the full presentation file below:

[TaskSprout.Documentaion.pdf](https://github.com/user-attachments/files/21576779/TaskSprout.Documentaion.pdf)


---

### 🌱 Let’s Grow!
Productivity doesn't have to be boring.
Level up your to-do list and grow your forest — one task at a time 🍀
