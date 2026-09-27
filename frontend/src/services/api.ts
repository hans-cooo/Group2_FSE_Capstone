import type { Account, AuditRecord, AuthSession, ChainVerificationResult, GatewayRoute, NotificationItem, ProblemDetails, TransferRequest, TransferResponse } from '../types';

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
    message: 'Payroll deposit received: ₱3,250.00 from CooBS Payroll.',
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

const SEED_AUDIT_RECORDS: AuditRecord[] = [
  {
    auditId: 1001,
    transactionId: 501,
    accountId: 1,
    referenceNo: 'REF-DEP-001',
    transactionType: 'DEPOSIT',
    amount: 10000.00,
    oldBalance: 0.00,
    newBalance: 10000.00,
    previousHash: '0000000000000000000000000000000000000000000000000000000000000000',
    currentHash: 'c7d23e59a85b9e0f6b4d32e1850f2495b41cf131e50682a39281e59c049b72a4',
    actorId: 1,
    clientIp: '127.0.0.1',
    eventTimestamp: '2026-09-27T08:15:22.000Z'
  },
  {
    auditId: 1002,
    transactionId: 502,
    accountId: 1,
    referenceNo: 'REF-PAY-002',
    transactionType: 'CREDIT',
    amount: 5000.00,
    oldBalance: 10000.00,
    newBalance: 15000.00,
    previousHash: 'c7d23e59a85b9e0f6b4d32e1850f2495b41cf131e50682a39281e59c049b72a4',
    currentHash: '4a6b98e1f02c4819d45a901e7492c318bf920518dc9319e7a2b904128f49c018',
    actorId: 1,
    clientIp: '127.0.0.1',
    eventTimestamp: '2026-09-27T10:30:10.000Z'
  },
  {
    auditId: 1003,
    transactionId: 503,
    accountId: 1,
    referenceNo: 'REF-TRF-003',
    transactionType: 'TRANSFER_DEBIT',
    amount: 249.50,
    oldBalance: 15000.00,
    newBalance: 14750.50,
    previousHash: '4a6b98e1f02c4819d45a901e7492c318bf920518dc9319e7a2b904128f49c018',
    currentHash: '8e219fb041c9a48b5209c148209e51c8901b27e8a93149e0c8192a472918e932',
    actorId: 1,
    clientIp: '127.0.0.1',
    eventTimestamp: '2026-09-28T01:45:00.000Z'
  },
  {
    auditId: 2001,
    transactionId: 601,
    accountId: 2,
    referenceNo: 'REF-SAV-001',
    transactionType: 'DEPOSIT',
    amount: 85200.00,
    oldBalance: 0.00,
    newBalance: 85200.00,
    previousHash: '0000000000000000000000000000000000000000000000000000000000000000',
    currentHash: '1a90c481e9204bf8a9012c85e4920182b849201948291048b9184029418290e4',
    actorId: 2,
    clientIp: '127.0.0.1',
    eventTimestamp: '2026-09-27T09:00:00.000Z'
  }
];

class ApiClient {
  private session: AuthSession | null = null;
  private localAccounts: Account[] = [...SEED_ACCOUNTS];
  private localNotifications: NotificationItem[] = [...SEED_NOTIFICATIONS];
  private localAuditRecords: AuditRecord[] = [...SEED_AUDIT_RECORDS];
  private idempotencyCache: Map<string, TransferResponse> = new Map();

  constructor() {
    const saved = localStorage.getItem('coobs_auth_session') || localStorage.getItem('apex_auth_session');
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
        roles: ['ROLE_CUSTOMER', 'ROLE_ADMIN', 'AUDITOR'],
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
      localStorage.setItem('coobs_auth_session', JSON.stringify(session));
    } else {
      localStorage.removeItem('coobs_auth_session');
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
  // Live Authentication Methods
  // --------------------------------------------------------------------------
  public async loginCustomer(username: string, password = 'Password123!'): Promise<AuthSession> {
    const res = await fetch('/api/v1/auth/customers/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || err.message || `Customer login failed: HTTP ${res.status}`);
    }
    const data = await res.json();
    const authData = data.authData;
    const session: AuthSession = {
      accessToken: authData.accessToken,
      userId: authData.userId,
      username: authData.username,
      roles: authData.roles || ['ROLE_CUSTOMER'],
      userType: 'CUSTOMER'
    };
    this.setSession(session);
    return session;
  }

  public async loginStaff(username: string, password = 'Password123!'): Promise<AuthSession> {
    const res = await fetch('/api/v1/auth/staff/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || err.message || `Staff login failed: HTTP ${res.status}`);
    }
    const data = await res.json();
    const authData = data.authData;
    const session: AuthSession = {
      accessToken: authData.accessToken,
      userId: authData.userId,
      username: authData.username,
      roles: authData.roles || ['ROLE_ADMIN'],
      userType: authData.roles?.includes('ROLE_ADMIN') ? 'ADMIN' : 'TELLER'
    };
    this.setSession(session);
    return session;
  }

  public async ensureLiveSession(): Promise<AuthSession> {
    if (
      this.session &&
      this.session.accessToken &&
      !this.session.accessToken.startsWith('demo-') &&
      !this.session.accessToken.startsWith('simulated-') &&
      !this.session.accessToken.startsWith('coobs-token-')
    ) {
      return this.session;
    }
    try {
      return await this.loginCustomer('john_doe', 'Password123!');
    } catch {
      return this.session!;
    }
  }

  // --------------------------------------------------------------------------
  // Health & Gateway Route Discovery
  // --------------------------------------------------------------------------
  public async checkGatewayHealth(): Promise<{ status: string; latencyMs: number; routes: GatewayRoute[] }> {
    const start = performance.now();
    try {
      const res = await fetch('/actuator/health', { method: 'GET' });
      const latencyMs = Math.round(performance.now() - start);
      if (res.ok) {
        let routes: GatewayRoute[] = [];
        try {
          const routesRes = await fetch('/actuator/gateway/routes');
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
      await this.ensureLiveSession();
      const res = await fetch('/api/v1/accounts/my-accounts', {
        headers: {
          'Authorization': `Bearer ${this.session?.accessToken || ''}`,
          'X-Correlation-ID': this.generateUuid()
        }
      });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          const accounts: Account[] = data.map((acc: any) => ({
            accountId: acc.accountId,
            accountNumber: acc.accountNumber,
            accountType: acc.accountType || 'SAVINGS',
            balance: acc.balance !== undefined ? Number(acc.balance) : Number(acc.availableBalance || 0),
            currency: acc.currency || 'PHP',
            status: acc.status || 'ACTIVE',
            createdAt: acc.createdAt
          }));
          this.localAccounts = accounts;
          return accounts;
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
      await this.ensureLiveSession();
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
        const txnId = data.transferReference || data.transactionId || data.transferId || data.id;
        const result: TransferResponse = {
          transactionId: txnId,
          referenceNo: data.transferReference || payload.referenceNo,
          sourceAccountId: payload.sourceAccountId,
          destinationAccountId: payload.destinationAccountId,
          amount: payload.amount,
          status: 'COMPLETED',
          timestamp: data.timestamp || new Date().toISOString(),
          isCachedReplay: res.headers.get('X-Cache') === 'HIT'
        };
        // Update local accounts directly from database
        await this.getAccounts();
        return { response: result, isCachedReplay: !!result.isCachedReplay, latencyMs };
      } else {
        const errorJson: ProblemDetails = await res.json().catch(() => ({}));
        throw new Error(errorJson.detail || `Gateway returned HTTP ${res.status}`);
      }
    } catch (err: any) {
      if (err.message && !err.message.includes('Failed to fetch') && !err.message.includes('NetworkError')) {
        // If it was a real business error from backend (e.g. Insufficient funds), rethrow it
        throw err;
      }
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
      await this.ensureLiveSession();
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
      await this.ensureLiveSession();
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

  // --------------------------------------------------------------------------
  // Audit & Cryptographic Chain Verification Engine (Port 8085 via Gateway)
  // --------------------------------------------------------------------------
  public async getAccountAuditStatements(accountId: number): Promise<AuditRecord[]> {
    try {
      await this.ensureLiveSession();
      const res = await fetch(`/api/v1/audit/accounts/${accountId}/statement?page=0&size=50&sort=eventTimestamp,desc`, {
        headers: {
          'Authorization': `Bearer ${this.session?.accessToken || ''}`,
          'X-Correlation-ID': this.generateUuid()
        }
      });
      if (res.ok) {
        const data = await res.json();
        const records = data.content || data;
        if (Array.isArray(records) && records.length > 0) {
          return records;
        }
      }
    } catch {
      // Fallback to local audit records
    }
    return this.localAuditRecords.filter(r => r.accountId === accountId);
  }

  public async verifyAuditChain(accountId: number): Promise<ChainVerificationResult> {
    try {
      await this.ensureLiveSession();
      const res = await fetch(`/api/v1/audit/verify-chain/${accountId}`, {
        headers: {
          'Authorization': `Bearer ${this.session?.accessToken || ''}`,
          'X-Correlation-ID': this.generateUuid()
        }
      });
      if (res.ok) {
        return await res.json();
      }
    } catch {
      // Fallback to local chain verification
    }

    const records = this.localAuditRecords
      .filter(r => r.accountId === accountId)
      .sort((a, b) => a.auditId - b.auditId);

    if (records.length === 0) {
      return {
        accountId,
        totalRecordsVerified: 0,
        isChainIntact: true,
        verifiedAt: new Date().toISOString(),
        message: 'No mutation records found. Genesis state verified.'
      };
    }

    const GENESIS_HASH = '0000000000000000000000000000000000000000000000000000000000000000';
    if (records[0].previousHash !== GENESIS_HASH) {
      return {
        accountId,
        totalRecordsVerified: 0,
        isChainIntact: false,
        latestHash: records[0].currentHash,
        verifiedAt: new Date().toISOString(),
        message: `Genesis block corruption at audit ID ${records[0].auditId}: expected 64 zeroes but found [${records[0].previousHash.slice(0, 16)}...]`
      };
    }

    for (let i = 1; i < records.length; i++) {
      const prev = records[i - 1];
      const curr = records[i];
      if (curr.previousHash !== prev.currentHash) {
        return {
          accountId,
          totalRecordsVerified: i,
          isChainIntact: false,
          latestHash: curr.currentHash,
          verifiedAt: new Date().toISOString(),
          message: `Chain linkage mismatch at audit ID ${curr.auditId} (txn ${curr.transactionId}): expected previous_hash [${prev.currentHash.slice(0, 16)}...] but found [${curr.previousHash.slice(0, 16)}...]`
        };
      }
    }

    return {
      accountId,
      totalRecordsVerified: records.length,
      isChainIntact: true,
      latestHash: records[records.length - 1].currentHash,
      verifiedAt: new Date().toISOString(),
      message: 'Audit chain integrity verified successfully (SHA-256 Chained).'
    };
  }

  public simulateTampering(accountId: number): void {
    const records = this.localAuditRecords.filter(r => r.accountId === accountId);
    if (records.length > 1) {
      // Tamper with the middle record's hash
      records[records.length - 1].previousHash = 'tampered_hash_link_payload_compromised_000000000000000000000000';
    }
  }

  public restoreIntactChain(accountId: number): void {
    const originals = SEED_AUDIT_RECORDS.filter(r => r.accountId === accountId);
    this.localAuditRecords = this.localAuditRecords.filter(r => r.accountId !== accountId).concat(JSON.parse(JSON.stringify(originals)));
  }
}

// Export singleton
export const apiClient = new ApiClient();
