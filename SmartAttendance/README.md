# Smart Attendance System (QR Code + Location Based)

Android app (Java) for taking class attendance. The teacher shows a QR code that changes every 30 seconds.
Students scan it, and the app checks that the student is inside the class (within 100 m) before marking attendance.

**Tech:** Java, Android Studio, Firebase Authentication, Cloud Firestore, ZXing (QR), Google Fused Location, Material Design 3.

## Features

- Teacher and Student roles (register / login / auto-login)
- Teacher: create courses (6-letter join code), start / end sessions, live present counter, report, share as CSV
- Student: join course with code, scan QR, attendance percentage per course, low attendance warning (below 75%)
- Security checks on every scan: valid QR, fresh token (30 s), enrolled student, no duplicate, no fake GPS, within 100 m

## Setup (PC par run karne ke steps)

### 1. Firebase (ek baar)

1. https://console.firebase.google.com par naya project banayein.
2. **Add app > Android**. Package name bilkul ye likhein: `com.smartattendance.app`
3. `google-services.json` download karein aur `app/` folder mein rakhein (`app/google-services.json`).
4. **Build > Authentication > Get started > Email/Password** enable karein.
5. **Build > Firestore Database > Create database**. Phir **Rules** tab mein `firestore.rules` file ka content paste karke **Publish** karein.

### 2. Android Studio

1. Android Studio (Koala ya naya) install karein.
2. **File > Open** aur ye project folder select karein.
3. Gradle sync ka wait karein (pehli baar internet chahiye, thora time lagta hai).
4. Phone mein Developer options > USB debugging on karein, USB se jodein, aur **Run** dabayein.
   (Emulator mein QR scan mushkil hai, real phone behtar hai.)

### 3. Test (2 phones behtar)

1. Phone A: Teacher account banayein, course banayein, join code note karein.
2. Phone B: Student account banayein, **Join course** se code dalein.
3. Phone A: **Start session**. QR dikhega.
4. Phone B: **Scan QR**. "Attendance marked" aana chahiye.

## Apna package name rakhna ho

Agar Firebase mein koi aur package name use karna hai: `app/build.gradle.kts` mein `namespace` aur `applicationId` badlein,
aur Java files ka `package` aur `AndroidManifest.xml` ke activity names usi hisaab se update karein
(Android Studio mein package par right click > Refactor > Rename sabse asaan hai).

## GitHub par upload

```
git init
git add .
git commit -m "Smart Attendance System"
git branch -M main
git remote add origin https://github.com/<username>/<repo>.git
git push -u origin main
```

Note: `google-services.json` is project mein commit hota hai taake PC par clone karke seedha chal jaye.
Agar repository public rakhni hai aur ye file nahi rakhni, to `.gitignore` mein `app/google-services.json` add kar dein,
aur PC par Firebase se dobara download karke `app/` mein rakh dein.

## Project structure

```
app/src/main/java/com/smartattendance/app/
  SplashActivity, LoginActivity, RegisterActivity
  TeacherDashboardActivity, QrSessionActivity, ReportActivity
  StudentDashboardActivity, ScanActivity
  Course, CourseAdapter, RoleRouter, Utils
app/src/main/res/layout/   (XML screens)
firestore.rules            (Firestore security rules)
```
