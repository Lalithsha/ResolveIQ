import React from 'react';
import {
  Activity,
  BookOpen,
  Cpu,
  FileCheck2,
  FolderKanban,
  Inbox,
  LifeBuoy,
  PlusCircle,
  Settings,
  ShieldCheck,
  Users,
} from 'lucide-react';
import { Role } from '../../types';

interface SidebarProps {
  currentRole: Role;
  activeTab: string;
  onSelectTab: (tab: string) => void;
}

const roleNames: Record<Role, string> = {
  CUSTOMER: 'Customer portal',
  AGENT: 'Agent workspace',
  TEAM_LEAD: 'Team workspace',
  KNOWLEDGE_MANAGER: 'Knowledge operations',
  ADMIN: 'Administration',
  AUDITOR: 'Audit workspace',
};

export const Sidebar: React.FC<SidebarProps> = ({ currentRole, activeTab, onSelectTab }) => {
  const getNavItems = () => {
    switch (currentRole) {
      case 'CUSTOMER':
        return [
          { id: 'create', label: 'Create ticket', icon: PlusCircle },
          { id: 'my-tickets', label: 'My tickets', icon: Inbox },
          { id: 'help', label: 'Help center', icon: BookOpen },
        ];
      case 'AGENT':
        return [
          { id: 'my-queue', label: 'My queue', icon: Inbox },
          { id: 'team-queue', label: 'Team queue', icon: Users },
          { id: 'sla-risk', label: 'SLA risk', icon: Activity },
          { id: 'knowledge-search', label: 'Knowledge', icon: BookOpen },
        ];
      case 'TEAM_LEAD':
        return [
          { id: 'team-queue', label: 'Team queue', icon: Users },
          { id: 'sla-risk', label: 'SLA risk', icon: Activity },
          { id: 'knowledge-search', label: 'Knowledge', icon: BookOpen },
        ];
      case 'KNOWLEDGE_MANAGER':
        return [
          { id: 'articles', label: 'Articles & chunks', icon: BookOpen },
          { id: 'resolved-cases', label: 'Sanitized cases', icon: FileCheck2 },
          { id: 'embeddings', label: 'Vector indexes', icon: Cpu },
        ];
      case 'ADMIN':
        return [
          { id: 'overview', label: 'Overview', icon: Activity },
          { id: 'tickets', label: 'All tickets', icon: FolderKanban },
          { id: 'routing', label: 'Teams & routing', icon: Users },
          { id: 'knowledge', label: 'Knowledge base', icon: BookOpen },
          { id: 'governance', label: 'AI governance', icon: Cpu },
          { id: 'users', label: 'Users & roles', icon: Settings },
        ];
      case 'AUDITOR':
        return [
          { id: 'audit', label: 'Security audit', icon: ShieldCheck },
          { id: 'tickets', label: 'Ticket evidence', icon: FolderKanban },
          { id: 'workflows', label: 'Workflow audit', icon: Activity },
          { id: 'governance', label: 'AI governance', icon: Cpu },
        ];
      default:
        return [];
    }
  };

  const navItems = getNavItems();

  const navigation = navItems.map((item) => {
    const Icon = item.icon;
    const isActive = activeTab === item.id;
    return (
      <button
        key={item.id}
        onClick={() => onSelectTab(item.id)}
        aria-current={isActive ? 'page' : undefined}
        className={`group relative flex items-center gap-3 rounded-btn text-sm transition-colors lg:w-full lg:px-3 lg:py-2.5 ${
          isActive
            ? 'bg-primary-soft text-primary'
            : 'text-muted hover:bg-surface-muted hover:text-DEFAULT'
        }`}
      >
        <Icon className="h-[17px] w-[17px] flex-none" strokeWidth={isActive ? 2.2 : 1.8} />
        <span className="font-medium lg:block">{item.label}</span>
        {isActive && <span className="absolute left-0 hidden h-5 w-0.5 rounded-full bg-primary lg:block" />}
      </button>
    );
  });

  return (
    <>
      <aside className="hidden w-64 flex-none flex-col justify-between border-r border-border-subtle bg-surface px-3 py-5 lg:flex">
        <div>
          <div className="px-3 pb-4">
            <span className="eyebrow mb-1">Workspace</span>
            <p className="text-sm font-semibold text-DEFAULT">{roleNames[currentRole]}</p>
          </div>
          <nav aria-label="Primary navigation" className="space-y-1">{navigation}</nav>
        </div>

        <div className="mx-1 rounded-card border border-border-subtle bg-surface-muted p-3.5">
          <div className="mb-1.5 flex items-center gap-2 text-xs font-semibold text-DEFAULT">
            <LifeBuoy className="h-4 w-4 text-primary" />
            Need a hand?
          </div>
          <p className="text-[11px] leading-4 text-muted">Open the help center or contact your workspace administrator.</p>
        </div>
      </aside>

      <nav
        aria-label="Mobile navigation"
        className="fixed inset-x-0 bottom-0 z-50 flex h-[68px] items-stretch overflow-x-auto border-t border-border-subtle bg-surface/95 px-2 pb-[env(safe-area-inset-bottom)] backdrop-blur lg:hidden"
      >
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              aria-current={isActive ? 'page' : undefined}
              className={`flex min-w-[76px] flex-1 flex-col items-center justify-center gap-1 px-2 text-[10px] font-medium ${
                isActive ? 'text-primary' : 'text-muted'
              }`}
            >
              <Icon className="h-[18px] w-[18px]" strokeWidth={isActive ? 2.3 : 1.8} />
              <span className="max-w-[82px] truncate">{item.label}</span>
            </button>
          );
        })}
      </nav>
    </>
  );
};
