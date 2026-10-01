import { useState, useEffect, useCallback } from 'react';
import { Header } from './components/Header';
import { NavigationTabs, type TabType } from './components/NavigationTabs';
import { DashboardView } from './components/DashboardView';
import { TransferView } from './components/TransferView';
import { NotificationView } from './components/NotificationView';
import { TopologyView } from './components/TopologyView';
import { AuditView } from './components/AuditView';
import { AuthModal } from './components/AuthModal';
import { apiClient } from './services/api';
import type { Account, AuthSession, GatewayRoute, NotificationItem, TransferRequest, TransferResponse } from './types';
import { CheckCircle2, AlertCircle, Info } from 'lucide-react';

interface Toast {
  type: 'success' | 'info' | 'error';
  message: string;
}

export function App() {
  const [session, setSession] = useState<AuthSession | null>(apiClient.getSession());
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [gatewayStatus, setGatewayStatus] = useState<string>('CONNECTING');
  const [latencyMs, setLatencyMs] = useState<number>(0);
  const [routes, setRoutes] = useState<GatewayRoute[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isAuthOpen, setIsAuthOpen] = useState<boolean>(false);
  const [transferSourceId, setTransferSourceId] = useState<number | undefined>(undefined);
  const [toast, setToast] = useState<Toast | null>(null);

  const showToast = (type: 'success' | 'info' | 'error', message: string) => {
    setToast({ type, message });
    setTimeout(() => {
      setToast(null);
    }, 4500);
  };

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      // 1. Check Gateway Actuator & Routes
      const health = await apiClient.checkGatewayHealth();
      setGatewayStatus(health.status);
      setLatencyMs(health.latencyMs);
      setRoutes(health.routes);

      // 2. Fetch Accounts
      const accList = await apiClient.getAccounts();
      setAccounts(accList);

      // 3. Fetch Notifications
      const notifList = await apiClient.getNotifications();
      setNotifications(notifList);
    } catch {
      // API fallback handles internal state
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
    const interval = setInterval(() => {
      apiClient.checkGatewayHealth().then(h => {
        setGatewayStatus(h.status);
        setLatencyMs(h.latencyMs);
        if (h.routes.length > 0) setRoutes(h.routes);
      }).catch(() => {});
    }, 20000);

    return () => clearInterval(interval);
  }, [loadData]);

  const handleTabChange = (tab: TabType) => {
    if (tab === 'auth') {
      setIsAuthOpen(true);
    } else {
      setActiveTab(tab);
    }
  };

  const handleSelectTransfer = (sourceId?: number) => {
    setTransferSourceId(sourceId);
    setActiveTab('transfer');
  };

  const handleExecuteTransfer = async (
    payload: TransferRequest,
    idempotencyKey: string,
    correlationId: string
  ): Promise<{ response: TransferResponse; isCachedReplay: boolean; latencyMs: number }> => {
    const result = await apiClient.executeTransfer(payload, idempotencyKey, correlationId);
    
    // Refresh accounts & notifications
    const accList = await apiClient.getAccounts();
    setAccounts(accList);
    const notifList = await apiClient.getNotifications();
    setNotifications(notifList);

    if (result.isCachedReplay) {
      showToast('info', `Idempotent Cache Hit: Key already executed. Safe replay verified.`);
    } else {
      showToast('success', `Transfer of ₱${payload.amount.toFixed(2)} posted to Master Ledger! Ref: ${payload.referenceNo}`);
    }

    return result;
  };

  const handleMarkAsRead = async (id: number) => {
    await apiClient.markNotificationAsRead(id);
    const updated = await apiClient.getNotifications();
    setNotifications([...updated]);
  };

  const handleSessionChange = (newSession: AuthSession | null) => {
    apiClient.setSession(newSession);
    setSession(newSession);
    loadData();
    showToast('info', newSession ? `Active identity: ${newSession.username}` : 'Logged out');
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      
      {/* Toast Notification Notification Pill */}
      {toast && (
        <div style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          zIndex: 999,
          padding: '14px 20px',
          borderRadius: '12px',
          background: toast.type === 'success' ? 'rgba(16, 185, 129, 0.95)' : (toast.type === 'error' ? 'rgba(244, 63, 94, 0.95)' : 'rgba(14, 165, 233, 0.95)'),
          color: '#ffffff',
          boxShadow: '0 10px 30px rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontSize: '13px',
          fontWeight: '600',
          backdropFilter: 'blur(10px)',
          animation: 'fadeIn 0.3s ease-out'
        }}>
          {toast.type === 'success' && <CheckCircle2 size={18} />}
          {toast.type === 'error' && <AlertCircle size={18} />}
          {toast.type === 'info' && <Info size={18} />}
          <span>{toast.message}</span>
        </div>
      )}

      {/* Global Application Header */}
      <Header
        session={session}
        gatewayStatus={gatewayStatus}
        latencyMs={latencyMs}
        unreadCount={unreadCount}
        onOpenNotifications={() => setActiveTab('notifications')}
        onOpenAuth={() => setIsAuthOpen(true)}
      />

      {/* Primary Navigation Tabs */}
      <NavigationTabs
        activeTab={activeTab}
        onSelectTab={handleTabChange}
        unreadCount={unreadCount}
      />

      {/* Main View Area */}
      <main style={{ flex: 1, paddingBottom: '48px' }}>
        {activeTab === 'dashboard' && (
          <DashboardView
            accounts={accounts}
            onSelectTransfer={handleSelectTransfer}
            onRefresh={loadData}
            isLoading={isLoading}
          />
        )}

        {activeTab === 'transfer' && (
          <TransferView
            accounts={accounts}
            initialSourceId={transferSourceId}
            onExecuteTransfer={handleExecuteTransfer}
          />
        )}

        {activeTab === 'audit' && (
          <AuditView
            accounts={accounts}
          />
        )}

        {activeTab === 'notifications' && (
          <NotificationView
            notifications={notifications}
            onMarkAsRead={handleMarkAsRead}
            onRefresh={loadData}
          />
        )}

        {activeTab === 'topology' && (
          <TopologyView
            routes={routes}
            gatewayStatus={gatewayStatus}
          />
        )}
      </main>

      {/* Auth & Identity Modal */}
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        currentSession={session}
        onSessionChange={handleSessionChange}
      />

      {/* Architectural Platform Footer */}
      <footer style={{
        marginTop: 'auto',
        borderTop: '1px solid var(--border-subtle)',
        background: 'rgba(2, 6, 23, 0.7)',
        padding: '24px 32px',
        fontSize: '12px',
        color: 'var(--text-subtle)',
        backdropFilter: 'blur(10px)'
      }}>
        <div style={{
          maxWidth: '1320px',
          margin: '0 auto',
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '16px'
        }}>
          <div>
            <span style={{ fontWeight: '700', color: '#ffffff' }}>CooBS Core Banking System</span> &mdash; Fullstack Capstone Architecture
            <div style={{ marginTop: '4px' }}>
              Microservices: Edge Gateway (8080) &bull; Auth (8081) &bull; Account (8082) &bull; Ledger (8083) &bull; Notification (8084) &bull; Audit (8085)
            </div>
          </div>
          <div style={{ display: 'flex', gap: '16px', fontFamily: 'var(--font-mono)' }}>
            <span>Oracle Free 23c (1522)</span>
            <span>&bull;</span>
            <span>PostgreSQL 16 (5434)</span>
            <span>&bull;</span>
            <span>Redis 7.4 (6379)</span>
            <span>&bull;</span>
            <span>Kafka KRaft (9092)</span>
          </div>
        </div>
      </footer>

    </div>
  );
}

export default App;
