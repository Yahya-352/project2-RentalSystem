import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './auth/AuthContext';
import { ProfileImageProvider } from './auth/ProfileImageContext';
import { NotificationProvider } from './notifications/NotificationContext';
import './styles.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ProfileImageProvider>
          <NotificationProvider>
            <App />
          </NotificationProvider>
        </ProfileImageProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
