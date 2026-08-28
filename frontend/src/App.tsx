import React, { useState } from 'react';
import { Navbar } from './components/layout/Navbar';
import { Sidebar } from './components/layout/Sidebar';
import { CustomerPortal } from './pages/CustomerPortal';
import { AgentWorkspace } from './pages/AgentWorkspace';
import { KnowledgeConsole } from './pages/KnowledgeConsole';
import { AdminGovernance } from './pages/AdminGovernance';

export const App: React.FC = () => {
  const [currentRole, setCurrentRole] = useState<string>('AGENT');
  const [activeTab, setActiveTab] = useState<string>('my-queue');

  const handleRoleChange = (newRole: string) => {
    setCurrentRole(newRole);
    if (newRole === 'CUSTOMER') setActiveTab('create');
    else if (newRole === 'AGENT') setActiveTab('my-queue');
    else if (newRole === 'KNOWLEDGE_MANAGER') setActiveTab('articles');
    else setActiveTab('overview');
  };

  const renderContent = () => {
    if (currentRole === 'CUSTOMER') {
      return <CustomerPortal />;
    }
    if (currentRole === 'KNOWLEDGE_MANAGER' || activeTab === 'knowledge-search' || activeTab === 'knowledge') {
      return <KnowledgeConsole />;
    }
    if (currentRole === 'ADMIN' && (activeTab === 'governance' || activeTab === 'overview')) {
      return <AdminGovernance />;
    }
    return <AgentWorkspace />;
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Navbar currentRole={currentRole} onRoleChange={handleRoleChange} />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar currentRole={currentRole} activeTab={activeTab} onSelectTab={setActiveTab} />
        <div className="flex-1 overflow-y-auto">
          {renderContent()}
        </div>
      </div>
    </div>
  );
};

export default App;
