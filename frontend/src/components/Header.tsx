import React from 'react';
import { ShieldCheck, Bell, UserCircle, Activity } from 'lucide-react';
import type { AuthSession } from '../types';

interface HeaderProps {
  session: AuthSession | null;
  gatewayStatus: string;
  latencyMs: number;
  unreadCount: number;
  onOpenNotifications: () => void;
  onOpenAuth: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  session,
  gatewayStatus,
  latencyMs,
  unreadCount,
  onOpenNotifications,
  onOpenAuth
}) => {
  const isOnline = gatewayStatus === 'ONLINE' || gatewayStatus === 'DEMO_STANDALONE';

  return (
    <header className="header glass-card" style={{
      margin: '16px auto',
      width: 'calc(100% - 32px)',
      maxWidth: '1320px',
      padding: '14px 28px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      position: 'sticky',
      top: '16px',
      zIndex: 50,
      backdropFilter: 'blur(20px)',
      borderRadius: '16px'
    }}>
      {/* Brand & Platform Identity */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div style={{
          width: '42px',
          height: '42px',
          borderRadius: '12px',
          background: 'linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 15px rgba(14, 165, 233, 0.4)'
        }}>
          <ShieldCheck size={24} color="#ffffff" strokeWidth={2.5} />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '18px', fontWeight: '800', letterSpacing: '-0.02em', color: '#ffffff' }}>
              APEX
            </span>
            <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--accent-cyan)', background: 'rgba(56, 189, 248, 0.1)', padding: '2px 8px', borderRadius: '6px', border: '1px solid rgba(56, 189, 248, 0.25)' }}>
              CORE BANKING
            </span>
          </div>
          <p style={{ fontSize: '11px', color: 'var(--text-subtle)', marginTop: '1px' }}>
            High-Throughput Double-Entry Ledger & Edge Gateway Platform
          </p>
        </div>
      </div>

      {/* Edge Gateway Status Indicator */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '18px' }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '6px 14px',
          borderRadius: '9999px',
          background: isOnline ? 'rgba(16, 185, 129, 0.1)' : 'rgba(244, 63, 94, 0.1)',
          border: `1px solid ${isOnline ? 'rgba(16, 185, 129, 0.3)' : 'rgba(244, 63, 94, 0.3)'}`,
        }}>
          <span className="pulse-dot" style={{ color: isOnline ? 'var(--color-success)' : 'var(--color-danger)' }}></span>
          <Activity size={14} color={isOnline ? 'var(--color-success)' : 'var(--color-danger)'} />
          <span style={{ fontSize: '12px', fontWeight: '700', color: isOnline ? 'var(--color-success)' : 'var(--color-danger)', letterSpacing: '0.04em' }}>
            GATEWAY: 8080
          </span>
          <span style={{ fontSize: '11px', color: 'var(--text-subtle)', fontFamily: 'var(--font-mono)' }}>
            ({latencyMs}ms)
          </span>
        </div>

        {/* In-App Notification Bell */}
        <button
          onClick={onOpenNotifications}
          className="btn-secondary"
          style={{
            position: 'relative',
            padding: '8px',
            borderRadius: '10px',
            cursor: 'pointer',
            border: '1px solid var(--border-subtle)',
            background: 'rgba(30, 41, 59, 0.6)'
          }}
          title="In-App Notifications"
        >
          <Bell size={18} color="var(--text-main)" />
          {unreadCount > 0 && (
            <span style={{
              position: 'absolute',
              top: '-4px',
              right: '-4px',
              background: 'linear-gradient(135deg, #f43f5e 0%, #e11d48 100%)',
              color: '#ffffff',
              fontSize: '10px',
              fontWeight: '800',
              padding: '2px 6px',
              borderRadius: '9999px',
              border: '2px solid var(--bg-base)',
              boxShadow: '0 0 10px rgba(244, 63, 94, 0.5)'
            }}>
              {unreadCount}
            </span>
          )}
        </button>

        {/* Active Customer Profile Badge */}
        <button
          onClick={onOpenAuth}
          className="btn-secondary"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '6px 14px',
            borderRadius: '10px',
            cursor: 'pointer',
            border: '1px solid var(--border-subtle)',
            background: 'rgba(30, 41, 59, 0.6)'
          }}
        >
          <UserCircle size={18} color="var(--accent-cyan)" />
          <div style={{ textAlign: 'left' }}>
            <div style={{ fontSize: '12px', fontWeight: '700', color: '#ffffff' }}>
              {session?.username || 'customer1'}
            </div>
            <div style={{ fontSize: '10px', color: 'var(--accent-cyan)', fontWeight: '600' }}>
              {session?.userType || 'CUSTOMER'}
            </div>
          </div>
        </button>
      </div>
    </header>
  );
};
