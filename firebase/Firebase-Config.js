// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getMessaging } from "firebase/messaging";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyDMxR4Xvug2sGmbHGo3fYeJCb9d0WorVSE",
  authDomain: "lms-project-b8489.firebaseapp.com",
  projectId: "lms-project-b8489",
  storageBucket: "lms-project-b8489.firebasestorage.app",
  messagingSenderId: "46298862215",
  appId: "1:46298862215:web:62d4fa4f39af2fca2cde22",
  measurementId: "G-RMFS6V42HN"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Messaging 서비스 초기화
export const messaging = getMessaging(app);
export default app;