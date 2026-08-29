import React from 'react';
import { Sparkles, User, Bell, LogOut } from 'lucide-react';
import { Role } from '../../types';

interface NavbarProps {
  currentRole: string;
  availableRoles: Role[];
  userName: string;
  onRoleChange: (role: string) => void;
  onLogout: () => Promise<void>;
}

const roleLabels: Record<Role, string> = {
  CUSTOMER: 'Customer', AGENT: 'Support Agent', TEAM_LEAD: 'Team Lead',
  KNOWLEDGE_MANAGER: 'Knowledge Manager', ADMIN: 'Administrator', AUDITOR: 'Auditor'
};

export const Navbar: React.FC<NavbarProps> = ({ currentRole, availableRoles, userName, onRoleChange, onLogout }) => {
  return (
    <header className="h-16 bg-surface border-b border-border px-6 flex items-center justify-between sticky top-0 z-10">
      <div className="flex items-center space-x-3">
        <div className="w-8 h-8 rounded-btn bg-primary flex items-center justify-center text-white font-bold tracking-wider text-sm shadow-sm">
          RIQ
        </div>
        <div>
          <span className="font-bold text-lg text-DEFAULT tracking-tight">ResolveIQ</span>
          <span className="ml-2 text-xs font-mono text-muted bg-surface-muted px-2 py-0.5 rounded border border-border-subtle">
            v1.0-alpha
          </span>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-2 bg-surface-muted px-3 py-1.5 rounded-btn border border-border-subtle text-xs">
          <Sparkles className="w-4 h-4 text-ai" />
          <span className="text-muted font-medium">Workspace:</span>
          <select 
            value={currentRole} 
            onChange={(e) => onRoleChange(e.target.value)}
            aria-label="Select User Role"
            className="bg-surface text-DEFAULT font-semibold px-2 py-1 rounded border border-border text-xs focus:outline-none focus:ring-2 focus:ring-primary"
          >
            {availableRoles.map(role => <option key={role} value={role}>{roleLabels[role]}</option>)}
          </select>
        </div>

        <button aria-label="Notifications" className="p-2 text-muted hover:text-DEFAULT hover:bg-surface-muted rounded-btn transition-colors">
          <Bell className="w-4 h-4" />
        </button>

        <div className="flex items-center space-x-2 pl-2 border-l border-border-subtle">
          <div className="w-7 h-7 rounded-full bg-primary-soft text-primary flex items-center justify-center text-xs font-semibold">
            <User className="w-4 h-4" />
          </div>
          <span className="text-xs font-medium text-DEFAULT max-w-32 truncate">{userName}</span>
          <button onClick={() => void onLogout()} aria-label="Sign out" title="Sign out" className="p-1.5 text-muted hover:text-danger rounded-btn"><LogOut className="w-4 h-4" /></button>
        </div>
      </div>
    </header>
  );
};
