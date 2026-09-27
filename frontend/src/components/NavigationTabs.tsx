import { LayoutDashboard, ArrowLeftRight, BellRing, Network, KeyRound, ShieldCheck } from 'lucide-react';

export type TabType = 'dashboard' | 'transfer' | 'audit' | 'notifications' | 'topology' | 'auth';

interface NavigationTabsProps {
  activeTab: TabType;
  onSelectTab: (tab: TabType) => void;
  unreadCount: number;
}

export const NavigationTabs: React.FC<NavigationTabsProps> = ({
  activeTab,
  onSelectTab,
  unreadCount,
}) => {
  const tabs = [
    { id: 'dashboard', label: 'Dashboard & Balances', icon: LayoutDashboard },
    { id: 'transfer', label: 'Idempotent Transfer Engine', icon: ArrowLeftRight },
    { id: 'audit', label: 'Audit & Compliance', icon: ShieldCheck },
    { id: 'notifications', label: 'In-App Alerts', icon: BellRing, badge: unreadCount },
    { id: 'topology', label: 'Gateway & Architecture', icon: Network },
    { id: 'auth', label: 'Identity & Tokens', icon: KeyRound },
  ];

  return (
    <nav style={{
      maxWidth: '1320px',
      margin: '0 auto 24px auto',
      width: 'calc(100% - 32px)',
      display: 'flex',
      gap: '8px',
      padding: '6px',
      background: 'rgba(15, 23, 42, 0.65)',
      borderRadius: '14px',
      border: '1px solid var(--border-subtle)',
      backdropFilter: 'blur(12px)',
      overflowX: 'auto'
    }}>
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onSelectTab(tab.id as TabType)}
            style={{
              flex: 1,
              minWidth: '170px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px',
              padding: '10px 16px',
              borderRadius: '10px',
              border: isActive ? '1px solid rgba(56, 189, 248, 0.4)' : '1px solid transparent',
              background: isActive
                ? 'linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(37, 99, 235, 0.15) 100%)'
                : 'transparent',
              color: isActive ? '#ffffff' : 'var(--text-muted)',
              fontSize: '13px',
              fontWeight: isActive ? '700' : '500',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              boxShadow: isActive ? '0 4px 12px rgba(14, 165, 233, 0.2)' : 'none'
            }}
          >
            <Icon size={16} color={isActive ? 'var(--accent-cyan)' : 'var(--text-subtle)'} />
            <span>{tab.label}</span>
            {tab.badge !== undefined && tab.badge > 0 && (
              <span style={{
                background: 'var(--color-danger)',
                color: '#ffffff',
                fontSize: '10px',
                fontWeight: '800',
                padding: '1px 6px',
                borderRadius: '9999px',
                marginLeft: '4px'
              }}>
                {tab.badge}
              </span>
            )}
          </button>
        );
      })}
    </nav>
  );
};
