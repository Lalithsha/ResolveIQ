import React from 'react';
import { Bell, ChevronDown, LogOut, Sparkles } from 'lucide-react';
import { Role } from '../../types';

interface NavbarProps {
  currentRole: Role;
  availableRoles: Role[];
  userName: string;
  onRoleChange: (role: string) => void;
  onLogout: () => Promise<void>;
}

const roleLabels: Record<Role, string> = {
  CUSTOMER: 'Customer',
  AGENT: 'Support Agent',
  TEAM_LEAD: 'Team Lead',
  KNOWLEDGE_MANAGER: 'Knowledge Manager',
  ADMIN: 'Administrator',
  AUDITOR: 'Auditor',
};

export const Navbar: React.FC<NavbarProps> = ({
  currentRole,
  availableRoles,
  userName,
  onRoleChange,
  onLogout,
}) => {
  const initials = userName
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <header className="sticky top-0 z-40 h-16 border-b border-border-subtle bg-surface/95 backdrop-blur">
      <div className="flex h-full items-center justify-between px-4 sm:px-6">
        <div className="flex min-w-0 items-center gap-3">
          <div className="grid h-9 w-9 flex-none place-items-center rounded-[11px] bg-slate-950 text-white shadow-sm dark:bg-white dark:text-slate-950">
            <Sparkles className="h-[18px] w-[18px]" strokeWidth={2.2} />
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <span className="truncate text-[15px] font-semibold tracking-[-0.02em] text-DEFAULT">ResolveIQ</span>
              <span className="hidden rounded-full border border-border-subtle bg-surface-muted px-2 py-0.5 text-[9px] font-semibold uppercase tracking-[0.08em] text-muted sm:inline-flex">
                Alpha
              </span>
            </div>
            <span className="hidden text-[11px] text-muted sm:block">Support intelligence workspace</span>
          </div>
        </div>

        <div className="flex items-center gap-1 sm:gap-2">
          <label className="relative hidden items-center gap-2 rounded-btn border border-border-subtle bg-surface-muted/70 px-3 py-1.5 md:flex">
            <span className="text-[11px] font-medium text-muted">Viewing as</span>
            <select
              value={currentRole}
              onChange={(event) => onRoleChange(event.target.value)}
              aria-label="Select workspace role"
              className="appearance-none border-0 bg-transparent py-0 pl-0 pr-5 text-xs font-semibold text-DEFAULT focus:ring-0"
            >
              {availableRoles.map((role) => (
                <option key={role} value={role}>{roleLabels[role]}</option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute right-2.5 h-3.5 w-3.5 text-muted" />
          </label>

          <label className="relative md:hidden">
            <span className="sr-only">Select workspace role</span>
            <select
              value={currentRole}
              onChange={(event) => onRoleChange(event.target.value)}
              className="h-9 max-w-28 appearance-none rounded-btn border border-border-subtle bg-surface-muted py-0 pl-2.5 pr-7 text-[11px] font-semibold text-DEFAULT"
            >
              {availableRoles.map((role) => (
                <option key={role} value={role}>{roleLabels[role]}</option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute right-2 top-2.5 h-3.5 w-3.5 text-muted" />
          </label>

          <button aria-label="Notifications" className="icon-button relative">
            <Bell className="h-[17px] w-[17px]" />
            <span className="absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-full bg-primary ring-2 ring-surface" />
          </button>

          <div className="ml-1 flex items-center gap-2 border-l border-border-subtle pl-3">
            <div className="grid h-8 w-8 flex-none place-items-center rounded-full bg-primary-soft text-[11px] font-semibold text-primary">
              {initials || 'RI'}
            </div>
            <div className="hidden max-w-36 leading-tight lg:block">
              <p className="truncate text-xs font-semibold text-DEFAULT">{userName}</p>
              <p className="truncate text-[10px] text-muted">{roleLabels[currentRole]}</p>
            </div>
            <button
              onClick={() => void onLogout()}
              aria-label="Sign out"
              title="Sign out"
              className="icon-button h-8 w-8"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
