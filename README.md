# 🌿 **TaskSprout**  
_A cozy, gamified task manager to grow your productivity one task at a time_

<img width="187" height="177" alt="taskSprout_logo" src="https://github.com/user-attachments/assets/24482e47-8308-411c-b360-541c05ce919a" />


---

## 📖 Description

**TaskSprout** transforms everyday task management into an engaging, collaborative experience.  
Built with students and teams in mind, it adds **gamification**, **XP**, **achievements**, and even a **virtual forest** to keep you motivated! 🌱

Complete tasks, earn XP, unlock achievements, and watch your to-dos turn into tiny trees in your personal forest.

---

## 🌟 Features

### 🗂️ **Board Management**
- Create, edit, delete, and share boards via email.
- “NEW” label on first-time viewed boards (per device).
- All board info stored in **Firestore**.

### ✅ **Task Management**
- Tasks have 5 statuses: `TODO`, `IN PROGRESS`, `DONE`, `NEGLECTED`, `CLAIMED`.
- Claim tasks to earn XP — only the assignee gets XP for status updates.
- Drag & Drop tasks between columns.
- Sound effects for status changes.
- Toast messages like “Task Added”, “Moved to DONE”, etc.

### 📊 **Board Statistics**
- XP configuration (for managers only).
- Leaderboard sorted by XP earned on the board.
- Five pie charts: Claimed, TODO, IN PROGRESS, DONE, NEGLECTED.

### 🧑‍💻 **Profile Page**
- Set your name and upload a profile picture.
- View XP total, task breakdown, achievements.
- Navigate to **Forest** and **Achievements**.

### 🌲 **Forest Page**
- Every task you claim becomes a plant 🌱.
- Status determines the plant's appearance.
- Plants have permanent screen positions.
- "Sprouted!" toast and sound for new plants.
- DONE plants get removed if the forest is full.

### 🏆 **Achievements**
- 16 achievements with custom icons, XP, and progress tracking.
- Includes: first task done, 10 tasks done, join 5 boards, etc.
- In-app popup + confetti + sound on unlock.
- Visual progress bars and unlock indicators.

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

### 🔐 Login & Register
![Login Page](insert_login_image_here.png)
<img width="302" height="556" alt="taskSprout_login" src="https://github.com/user-attachments/assets/42e11b7d-68ad-4da3-b4ef-7bf091891c0c" />

### 🗂️ Board List
![Board List Page](insert_board_list_image_here.png)
<img width="297" height="558" alt="taskSprout_boardList" src="https://github.com/user-attachments/assets/97b892fa-8bf9-4acd-aec4-63ef9cd55f65" />

### 📝 Task Board
![Task Board Page](insert_task_board_image_here.png)
<img width="305" height="556" alt="taskSprout_TaskBoard" src="https://github.com/user-attachments/assets/699c8095-b093-478a-899d-0deba55bbac8" />
<img width="298" height="561" alt="taskSprout_taskBoard2" src="https://github.com/user-attachments/assets/a486d212-bf23-4959-9d8b-835476334930" />
<img width="627" height="566" alt="taskSprout_taskBoard3" src="https://github.com/user-attachments/assets/2e68b134-6005-4b50-a1ad-2a29ff224249" />

### 📊 Board Stats
![Board Stats Page](insert_stats_image_here.png)
<img width="298" height="556" alt="taskSprout_stats1" src="https://github.com/user-attachments/assets/7cb0d3ea-7ec2-497c-9ee8-c2ef93f00b8f" />
<img width="992" height="563" alt="taskSprout_stats2" src="https://github.com/user-attachments/assets/29c3fbdc-3a40-49e4-9a70-27fd69f8eaa7" />

### 🌲 Profile Page
<img width="297" height="566" alt="taskSprout_profilePage" src="https://github.com/user-attachments/assets/2e8f59d3-4c42-4c7a-a2e5-ae7419639c55" />

### 🌲 Forest Page
![Forest Page](insert_forest_image_here.png)<img width="305" height="562" alt="taskSprout_forest" src="https://github.com/user-attachments/assets/092ee0d2-003b-4868-8a6f-ab658e20790a" />

### 🏆 Achievements
![Achievements Page](insert_achievements_image_here.png)
<img width="983" height="562" alt="taskSprout_achievements" src="https://github.com/user-attachments/assets/5bbc04f9-0bbe-4962-8d25-a71deb89c5bc" />

---

## ⚙️ Getting Started

### 1. Clone the repo:
git clone https://github.com/yourusername/tasksprout.git

### 🧑‍💻 2. Open the project in Android Studio
### 🔑 3. Add your Firebase configuration
Place your google-services.json file inside the /app folder

### 🔄 4. Sync & Run
Sync Gradle

Run the app on an Android emulator or physical device

---

### 📝 Extra Documentation
For a complete breakdown of features, logic, and implementation decisions, check out the full presentation file below:

📄 TaskSprout🌿 – Talya Benatar (2).pdf
