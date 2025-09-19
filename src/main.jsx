import { createRoot } from 'react-dom/client';
import '../css/index.css';
import App from './App.jsx';

// PWA Service Worker
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js', { scope: '/' })
        .then(registration => {
            console.log('SW registered: ', registration)
        })
        .catch(registrationError => {
            console.log('SW registration failed: ', registrationError)
        })
    })
}

createRoot(document.getElementById('root')).render(
    <App />
);
