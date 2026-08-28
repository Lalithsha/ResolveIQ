import React from 'react';
import { Sparkles, User, Bell } from 'lucide-react';

interface NavbarProps {
  currentRole: string;
  onRoleChange: (role: string) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentRole, onRoleChange }) => {
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
          <span className="text-muted font-medium">Role Switcher:</span>
          <select 
            value={currentRole} 
            onChange={(e) => onRoleChange(e.target.value)}
            aria-label="Select User Role"
            className="bg-surface text-DEFAULT font-semibold px-2 py-1 rounded border border-border text-xs focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="AGENT">Support Agent</option>
            <option value="CUSTOMER">Customer Portal</option>
            <option value="KNOWLEDGE_MANAGER">Knowledge Manager</option>
            <option value="ADMIN">Lead / Admin</option>
          </select>
        </div>

        <button aria-label="Notifications" className="p-2 text-muted hover:text-DEFAULT hover:bg-surface-muted rounded-btn transition-colors">
          <Bell className="w-4 h-4" />
        </button>

        <div className="flex items-center space-x-2 pl-2 border-l border-border-subtle">
          <div className="w-7 h-7 rounded-full bg-primary-soft text-primary flex items-center justify-center text-xs font-semibold">
            <User className="w-4 h-4" />
          </div>
          <span className="text-xs font-medium text-DEFAULT">Demo User</span>
        </div>
      </div>
    </header>
  );
};
