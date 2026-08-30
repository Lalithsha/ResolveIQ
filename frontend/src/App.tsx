import React, { useState, useEffect } from 'react';
import { Navbar } from './components/layout/Navbar';
import { Sidebar } from './components/layout/Sidebar';
import { CustomerPortal } from './pages/CustomerPortal';
import { AgentWorkspace } from './pages/AgentWorkspace';
import { KnowledgeConsole } from './pages/KnowledgeConsole';
import { AdminGovernance } from './pages/AdminGovernance';
import { AuthPage } from './pages/AuthPage';
import { useAuth } from './context/AuthContext';
import { Role } from './types';

export const App: React.FC = () => {
  const { user, activeRole, setActiveRole, isAuthenticated, isLoading, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<string>('create');

  useEffect(() => {
    if (activeRole === 'CUSTOMER' && !['create', 'my-tickets', 'help'].includes(activeTab)) {
      setActiveTab('create');
    } else if (activeRole === 'AGENT' && !['my-queue', 'team-queue', 'sla-risk', 'knowledge-search'].includes(activeTab)) {
      setActiveTab('my-queue');
    } else if (activeRole === 'KNOWLEDGE_MANAGER' && !['articles', 'resolved-cases', 'embeddings'].includes(activeTab)) {
      setActiveTab('articles');
    } else if (activeRole === 'ADMIN' && !['overview', 'tickets', 'routing', 'knowledge', 'governance', 'settings'].includes(activeTab)) {
      setActiveTab('overview');
    }
  }, [activeRole, activeTab]);

  if (isLoading) return <div className="min-h-screen bg-background grid place-items-center text-primary font-semibold">Loading secure workspace…</div>;
  if (!isAuthenticated || !user) return <AuthPage />;

  const handleRoleChange = (newRole: string) => {
    setActiveRole(newRole as Role);
    if (newRole === 'CUSTOMER') setActiveTab('create');
    else if (newRole === 'AGENT') setActiveTab('my-queue');
    else if (newRole === 'KNOWLEDGE_MANAGER') setActiveTab('articles');
    else setActiveTab('overview');
  };

  const renderContent = () => {
    if (activeRole === 'CUSTOMER') {
      return <CustomerPortal activeTab={activeTab} onSelectTab={setActiveTab} />;
    }
    if (activeRole === 'KNOWLEDGE_MANAGER' || activeTab === 'knowledge-search' || activeTab === 'knowledge') {
      return <KnowledgeConsole />;
    }
    if (activeRole === 'ADMIN' && (activeTab === 'governance' || activeTab === 'overview' || activeTab === 'routing' || activeTab === 'settings')) {
      return <AdminGovernance />;
    }
    return <AgentWorkspace />;
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Navbar currentRole={activeRole} availableRoles={user.roles} userName={user.fullName} onRoleChange={handleRoleChange} onLogout={logout} />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar currentRole={activeRole} activeTab={activeTab} onSelectTab={setActiveTab} />
        <div className="flex-1 overflow-y-auto">
          {renderContent()}
        </div>
      </div>
    </div>
  );
};

export default App;
