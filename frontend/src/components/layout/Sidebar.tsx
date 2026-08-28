import React from 'react';
import { 
  Inbox, 
  Users, 
  BookOpen, 
  Activity, 
  Settings, 
  PlusCircle, 
  FolderKanban,
  FileCheck2,
  Cpu
} from 'lucide-react';

interface SidebarProps {
  currentRole: string;
  activeTab: string;
  onSelectTab: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentRole, activeTab, onSelectTab }) => {
  const getNavItems = () => {
    switch (currentRole) {
      case 'CUSTOMER':
        return [
          { id: 'create', label: 'Create Ticket', icon: PlusCircle },
          { id: 'my-tickets', label: 'My Tickets', icon: Inbox },
          { id: 'help', label: 'Help Center', icon: BookOpen },
        ];
      case 'AGENT':
        return [
          { id: 'my-queue', label: 'My Queue', icon: Inbox },
          { id: 'team-queue', label: 'Team Queue', icon: Users },
          { id: 'sla-risk', label: 'SLA Risk', icon: Activity },
          { id: 'knowledge-search', label: 'Knowledge Search', icon: BookOpen },
        ];
      case 'KNOWLEDGE_MANAGER':
        return [
          { id: 'articles', label: 'Articles & Chunks', icon: BookOpen },
          { id: 'resolved-cases', label: 'Sanitized Cases', icon: FileCheck2 },
          { id: 'embeddings', label: 'Vector Indexes', icon: Cpu },
        ];
      case 'ADMIN':
      default:
        return [
          { id: 'overview', label: 'Operations Overview', icon: Activity },
          { id: 'tickets', label: 'All Tickets', icon: FolderKanban },
          { id: 'routing', label: 'Teams & Routing', icon: Users },
          { id: 'knowledge', label: 'Knowledge Base', icon: BookOpen },
          { id: 'governance', label: 'AI Governance & Eval', icon: Cpu },
          { id: 'settings', label: 'System Settings', icon: Settings },
        ];
    }
  };

  const navItems = getNavItems();

  return (
    <aside className="w-60 bg-surface border-r border-border min-h-[calc(100vh-4rem)] p-4 flex flex-col justify-between">
      <div className="space-y-1">
        <div className="px-3 py-2 text-xs font-semibold text-muted uppercase tracking-wider">
          Navigation ({currentRole})
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`w-full flex items-center space-x-3 px-3 py-2 rounded-btn text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-primary-soft text-primary font-semibold'
                  : 'text-muted hover:text-DEFAULT hover:bg-surface-muted'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-primary' : 'text-muted'}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>

      <div className="pt-4 border-t border-border-subtle text-xs text-muted">
        <p className="font-semibold text-DEFAULT">ResolveIQ Studio</p>
        <p className="text-[11px] mt-0.5">Answer with evidence</p>
      </div>
    </aside>
  );
};
