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

const DEFAULT_TAB: Record<Role, string> = {
  CUSTOMER: 'create', AGENT: 'my-queue', TEAM_LEAD: 'team-queue',
  KNOWLEDGE_MANAGER: 'articles', ADMIN: 'overview', AUDITOR: 'audit',
};

const ALLOWED_TABS: Record<Role, string[]> = {
  CUSTOMER: ['create', 'my-tickets', 'help'],
  AGENT: ['my-queue', 'team-queue', 'sla-risk', 'knowledge-search'],
  TEAM_LEAD: ['team-queue', 'sla-risk', 'knowledge-search'],
  KNOWLEDGE_MANAGER: ['articles', 'resolved-cases', 'embeddings'],
  ADMIN: ['overview', 'tickets', 'routing', 'knowledge', 'governance', 'users'],
  AUDITOR: ['audit', 'tickets', 'workflows', 'governance'],
};

export const App: React.FC = () => {
  const { user, activeRole, setActiveRole, isAuthenticated, isLoading, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<string>('create');

  useEffect(() => {
    if (!ALLOWED_TABS[activeRole].includes(activeTab)) setActiveTab(DEFAULT_TAB[activeRole]);
  }, [activeRole, activeTab]);

  if (isLoading) {
    return (
      <div className="grid min-h-screen place-items-center bg-background">
        <div className="flex items-center gap-3 text-sm font-medium text-muted">
          <span className="h-5 w-5 animate-spin rounded-full border-2 border-primary/20 border-t-primary" />
          Preparing your workspace…
        </div>
      </div>
    );
  }
  if (!isAuthenticated || !user) return <AuthPage />;

  const handleRoleChange = (newRole: string) => {
    const role = newRole as Role;
    setActiveRole(role);
    setActiveTab(DEFAULT_TAB[role]);
  };

  const renderContent = () => {
    if (activeRole === 'CUSTOMER') {
      return <CustomerPortal activeTab={activeTab} onSelectTab={setActiveTab} />;
    }
    if (activeRole === 'KNOWLEDGE_MANAGER' || activeTab === 'knowledge-search' || activeTab === 'knowledge') {
      return <KnowledgeConsole activeTab={activeTab} role={activeRole} />;
    }
    if (activeRole === 'ADMIN' || activeRole === 'AUDITOR') {
      if (activeTab === 'tickets') return <AgentWorkspace activeTab="all" role={activeRole} />;
      return <AdminGovernance activeTab={activeTab} role={activeRole} />;
    }
    return <AgentWorkspace activeTab={activeTab} role={activeRole} />;
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <Navbar currentRole={activeRole} availableRoles={user.roles} userName={user.fullName} onRoleChange={handleRoleChange} onLogout={logout} />
      <div className="flex min-h-0 flex-1">
        <Sidebar currentRole={activeRole} activeTab={activeTab} onSelectTab={setActiveTab} />
        <main className="min-w-0 flex-1 overflow-x-hidden pb-[68px] lg:pb-0">
          {renderContent()}
        </main>
      </div>
    </div>
  );
};

export default App;
