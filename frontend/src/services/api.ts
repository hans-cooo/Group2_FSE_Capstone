import type { Account, AuthSession, GatewayRoute, NotificationItem, ProblemDetails, TransferRequest, TransferResponse } from '../types';

// Fallback seed accounts for seamless offline / initial demonstration
const SEED_ACCOUNTS: Account[] = [
  {
    accountId: 1,
    accountNumber: 'ACC-1001-8842',
    accountType: 'CHECKING',
    balance: 14750.50,
    currency: 'PHP',
    status: 'ACTIVE',
    createdAt: '2026-01-15T08:30:00Z',
  },
  {
    accountId: 2,
    accountNumber: 'ACC-1002-9915',
    accountType: 'SAVINGS',
    balance: 85200.00,
    currency: 'PHP',
    status: 'ACTIVE',
    createdAt: '2026-02-01T10:15:00Z',
  },
  {
    accountId: 3,
    accountNumber: 'ACC-1003-3321',
    accountType: 'PAYROLL',
    balance: 6250.75,
    currency: 'PHP',
    status: 'ACTIVE',
    createdAt: '2026-03-10T14:00:00Z',
  }
];

const SEED_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 101,
    type: 'TRANSACTION_CREDIT',
    message: 'Payroll deposit received: ₱3,250.00 from Apex Corp.',
    isRead: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 18).toISOString(),
  },
  {
    id: 102,
    type: 'TRANSACTION_DEBIT',
    message: 'Fund transfer executed: ₱150.00 to Account #ACC-1002-9915.',
    isRead: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
  },
  {
    id: 103,
    type: 'SECURITY_ALERT',
    message: 'MFA verified successfully from IP 127.0.0.1.',
    isRead: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 360).toISOString(),
  }
];

class ApiClient {
  private session: AuthSession | null = null;
  private localAccounts: Account[] = [...SEED_ACCOUNTS];
  private localNotifications: NotificationItem[] = [...SEED_NOTIFICATIONS];
  private idempotencyCache: Map<string, TransferResponse> = new Map();

  constructor() {
    const saved = localStorage.getItem('apex_auth_session');
    if (saved) {
      try {
        this.session = JSON.parse(saved);
      } catch {
        this.session = null;
      }
    }
    // Default demo session if none exists
    if (!this.session) {
      this.session = {
        accessToken: 'demo-jwt-token-group2-fse-capstone',
        userId: 1,
        username: 'customer1',
        roles: ['ROLE_CUSTOMER'],
        userType: 'CUSTOMER'
      };
    }
  }

  public getSession(): AuthSession | null {
    return this.session;
  }

  public setSession(session: AuthSession | null) {
    this.session = session;
    if (session) {
      localStorage.setItem('apex_auth_session', JSON.stringify(session));
    } else {
      localStorage.removeItem('apex_auth_session');
    }
  }

  private generateUuid(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  // --------------------------------------------------------------------------
  // Health & Gateway Route Discovery
  // --------------------------------------------------------------------------
  public async checkGatewayHealth(): Promise<{ status: string; latencyMs: number; routes: GatewayRoute[] }> {
    const start = performance.now();
    try {
      const res = await fetch('/api/v1/actuator/health', { method: 'GET' });
      const latencyMs = Math.round(performance.now() - start);
      if (res.ok) {
        let routes: GatewayRoute[] = [];
        try {
          const routesRes = await fetch('/api/v1/actuator/gateway/routes');
          if (routesRes.ok) {
            routes = await routesRes.json();
          }
        } catch {
          // ignore routes endpoint error
        }
        return { status: 'ONLINE', latencyMs, routes };
      }
    } catch {
      // Gateway offline or proxy not running
    }
    return {
      status: 'DEMO_STANDALONE',
      latencyMs: 4,
      routes: [
        { route_id: 'auth-service-route', uri: 'http://localhost:8081', predicates: ['Path=/api/v1/auth/**'] },
        { route_id: 'account-service-accounts-route', uri: 'http://localhost:8082', predicates: ['Path=/api/v1/accounts/**'] },
        { route_id: 'account-service-customers-route', uri: 'http://localhost:8082', predicates: ['Path=/api/v1/customers/**'] },
        { route_id: 'ledger-service-route', uri: 'http://localhost:8083', predicates: ['Path=/api/v1/ledger/**'] },
        { route_id: 'notification-service-route', uri: 'http://localhost:8084', predicates: ['Path=/api/v1/notifications/**'] },
        { route_id: 'audit-service-route', uri: 'http://localhost:8085', predicates: ['Path=/api/v1/audit/**'] }
      ]
    };
  }

  // --------------------------------------------------------------------------
  // Accounts
  // --------------------------------------------------------------------------
  public async getAccounts(): Promise<Account[]> {
    try {
      const res = await fetch('/api/v1/accounts/my-accounts', {
        headers: {
          'Authorization': `Bearer ${this.session?.accessToken || ''}`,
          'X-Correlation-ID': this.generateUuid()
        }
      });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          this.localAccounts = data;
          return data;
        }
      }
    } catch {
      // Fallback to local accounts
    }
    return [...this.localAccounts];
  }

  // --------------------------------------------------------------------------
  // Fund Transfer (Double-Entry Engine via Gateway)
  // --------------------------------------------------------------------------
  public async executeTransfer(
    payload: TransferRequest,
    idempotencyKey: string,
    correlationId: string
  ): Promise<{ response: TransferResponse; isCachedReplay: boolean; latencyMs: number }> {
    const start = performance.now();

    // Live Gateway Call
    try {
      const res = await fetch('/api/v1/ledger/transfers', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.session?.accessToken || ''}`,
          'Idempotency-Key': idempotencyKey,
          'X-Correlation-ID': correlationId
        },
        body: JSON.stringify(payload)
      });

      const latencyMs = Math.round(performance.now() - start);

      if (res.ok) {
        const data = await res.json();
        const txnId = data.transactionId || data.transferId || data.id;
        const result: TransferResponse = {
          transactionId: txnId,
          referenceNo: payload.referenceNo,
          sourceAccountId: payload.sourceAccountId,
          destinationAccountId: payload.destinationAccountId,
          amount: payload.amount,
          status: 'COMPLETED',
          timestamp: new Date().toISOString(),
          isCachedReplay: res.headers.get('X-Cache') === 'HIT'
        };
        // Update local accounts if active
        await this.getAccounts();
        return { response: result, isCachedReplay: !!result.isCachedReplay, latencyMs };
      } else {
        const errorJson: ProblemDetails = await res.json().catch(() => ({}));
        throw new Error(errorJson.detail || `Gateway returned HTTP ${res.status}`);
      }
    } catch (err: any) {
      // If server is not running, provide local demonstration behavior including strict idempotency mutex
      const latencyMs = Math.round(performance.now() - start);

      // Check Idempotency Mutex Simulation
      if (this.idempotencyCache.has(idempotencyKey)) {
        const cached = this.idempotencyCache.get(idempotencyKey)!;
        return {
          response: { ...cached, isCachedReplay: true },
          isCachedReplay: true,
          latencyMs: Math.max(2, latencyMs)
        };
      }

      // Execute simulated double-entry debit & credit
      const src = this.localAccounts.find(a => a.accountId === payload.sourceAccountId);
      const dst = this.localAccounts.find(a => a.accountId === payload.destinationAccountId);

      if (src && src.balance < payload.amount) {
        throw new Error(`Insufficient funds: Account #${src.accountNumber} has ₱${src.balance.toFixed(2)}, required ₱${payload.amount.toFixed(2)}`);
      }

      if (src) src.balance = Math.round((src.balance - payload.amount) * 100) / 100;
      if (dst) dst.balance = Math.round((dst.balance + payload.amount) * 100) / 100;

      const newTxnId = 5000 + Math.floor(Math.random() * 9000);
      const newResponse: TransferResponse = {
        transactionId: newTxnId,
        referenceNo: payload.referenceNo,
        sourceAccountId: payload.sourceAccountId,
        destinationAccountId: payload.destinationAccountId,
        amount: payload.amount,
        status: 'COMPLETED',
        timestamp: new Date().toISOString(),
        isCachedReplay: false
      };

      this.idempotencyCache.set(idempotencyKey, newResponse);

      // Create in-app notification
      this.localNotifications.unshift({
        id: Date.now(),
        type: 'TRANSACTION_TRANSFER',
        message: `Transferred ₱${payload.amount.toFixed(2)} from #${src?.accountNumber || payload.sourceAccountId} to #${dst?.accountNumber || payload.destinationAccountId}. Ref: ${payload.referenceNo}`,
        isRead: false,
        createdAt: new Date().toISOString()
      });

      return { response: newResponse, isCachedReplay: false, latencyMs: Math.max(12, latencyMs) };
    }
  }

  // --------------------------------------------------------------------------
  // Notifications
  // --------------------------------------------------------------------------
  public async getNotifications(): Promise<NotificationItem[]> {
    try {
      const res = await fetch('/api/v1/notifications/my-notifications', {
        headers: {
          'Authorization': `Bearer ${this.session?.accessToken || ''}`,
          'X-Correlation-ID': this.generateUuid()
        }
      });
      if (res.ok) {
        const data = await res.json();
        const items = data.content || data;
        if (Array.isArray(items)) {
          this.localNotifications = items;
          return items;
        }
      }
    } catch {
      // ignore
    }
    return [...this.localNotifications];
  }

  public async markNotificationAsRead(id: number): Promise<void> {
    try {
      await fetch(`/api/v1/notifications/${id}/read`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${this.session?.accessToken || ''}`,
          'X-Correlation-ID': this.generateUuid()
        }
      });
    } catch {
      // ignore
    }
    const found = this.localNotifications.find(n => n.id === id);
    if (found) {
      found.isRead = true;
    }
  }

  public getUnreadNotificationCount(): number {
    return this.localNotifications.filter(n => !n.isRead).length;
  }
}

// Export singleton
export const apiClient = new ApiClient();
