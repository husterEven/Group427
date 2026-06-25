import { createBrowserRouter, Navigate } from 'react-router-dom'
import MainLayout from '@/components/MainLayout'
import AdminLayout from '@/components/AdminLayout'
import AuthGuard from '@/components/AuthGuard'

// 懒加载页面
import { lazy } from 'react'

const LoginPage = lazy(() => import('@/pages/LoginPage'))
const RegisterPage = lazy(() => import('@/pages/RegisterPage'))
const HomePage = lazy(() => import('@/pages/HomePage'))
const PostDetailPage = lazy(() => import('@/pages/PostDetailPage'))
const PostEditorPage = lazy(() => import('@/pages/PostEditorPage'))
const ProfilePage = lazy(() => import('@/pages/ProfilePage'))
const EditProfilePage = lazy(() => import('@/pages/EditProfilePage'))
const VerificationPage = lazy(() => import('@/pages/VerificationPage'))
const PreferencePage = lazy(() => import('@/pages/PreferencePage'))
const PrivacyPage = lazy(() => import('@/pages/PrivacyPage'))
const AchievementPage = lazy(() => import('@/pages/AchievementPage'))
const RiskAssessmentPage = lazy(() => import('@/pages/RiskAssessmentPage'))
const FollowingPage = lazy(() => import('@/pages/FollowingPage'))
const MessagesPage = lazy(() => import('@/pages/MessagesPage'))
const ChatPage = lazy(() => import('@/pages/ChatPage'))
const GroupsPage = lazy(() => import('@/pages/GroupsPage'))
const GroupDetailPage = lazy(() => import('@/pages/GroupDetailPage'))
const DynamicsPage = lazy(() => import('@/pages/DynamicsPage'))
const NotificationsPage = lazy(() => import('@/pages/NotificationsPage'))
const SearchPage = lazy(() => import('@/pages/SearchPage'))
const CollectionsPage = lazy(() => import('@/pages/CollectionsPage'))

// 管理后台
const AdminDashboard = lazy(() => import('@/pages/admin/AdminDashboard'))
const AuditQueuePage = lazy(() => import('@/pages/admin/AuditQueuePage'))
const ReportsPage = lazy(() => import('@/pages/admin/ReportsPage'))
const PunishmentsPage = lazy(() => import('@/pages/admin/PunishmentsPage'))
const UserMonitorPage = lazy(() => import('@/pages/admin/UserMonitorPage'))

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/',
    element: (
      <AuthGuard>
        <MainLayout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <HomePage /> },
      { path: 'section/:sectionId', element: <HomePage /> },
      { path: 'zone/:zoneId', element: <HomePage /> },
      { path: 'post/:postId', element: <PostDetailPage /> },
      { path: 'editor', element: <PostEditorPage /> },
      { path: 'editor/:postId', element: <PostEditorPage /> },
      { path: 'profile/:userId', element: <ProfilePage /> },
      { path: 'profile/:userId/dynamics', element: <DynamicsPage /> },
      { path: 'settings/profile', element: <EditProfilePage /> },
      { path: 'settings/verification', element: <VerificationPage /> },
      { path: 'settings/preference', element: <PreferencePage /> },
      { path: 'settings/privacy', element: <PrivacyPage /> },
      { path: 'settings/achievement', element: <AchievementPage /> },
      { path: 'settings/risk', element: <RiskAssessmentPage /> },
      { path: 'following', element: <FollowingPage /> },
      { path: 'messages', element: <MessagesPage /> },
      { path: 'messages/:userId', element: <ChatPage /> },
      { path: 'groups', element: <GroupsPage /> },
      { path: 'groups/:groupId', element: <GroupDetailPage /> },
      { path: 'dynamics', element: <DynamicsPage /> },
      { path: 'notifications', element: <NotificationsPage /> },
      { path: 'search', element: <SearchPage /> },
      { path: 'collections', element: <CollectionsPage /> },
      { path: 'admin', element: <Navigate to="/admin/dashboard" replace /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <AuthGuard requireAdmin>
        <AdminLayout />
      </AuthGuard>
    ),
    children: [
      { path: 'dashboard', element: <AdminDashboard /> },
      { path: 'audit', element: <AuditQueuePage /> },
      { path: 'reports', element: <ReportsPage /> },
      { path: 'punishments', element: <PunishmentsPage /> },
      { path: 'users', element: <UserMonitorPage /> },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
])

export default router
