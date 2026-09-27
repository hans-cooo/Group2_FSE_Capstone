export interface User {
  userId: number;
  username: string;
  roles: string[];
  userType: 'CUSTOMER' | 'TELLER' | 'ADMIN';
}

export interface AuthSession {
  accessToken: string;
  refreshToken?: string;
  userId: number;
  username: string;
  roles: string[];
  userType: string;
}

export interface Account {
  accountId: number;
  accountNumber: string;
  accountType: 'SAVINGS' | 'CHECKING' | 'PAYROLL' | 'TIME_DEPOSIT';
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | 'DORMANT' | 'CLOSED';
  customerId?: number;
  customerName?: string;
  createdAt?: string;
}

export interface TransferRequest {
  sourceAccountId: number;
  destinationAccountId: number;
  amount: number;
  referenceNo: string;
  remarks?: string;
}

export interface TransferResponse {
  transactionId: number | string;
  referenceNo: string;
  sourceAccountId: number;
  destinationAccountId: number;
  amount: number;
  status: 'COMPLETED' | 'PENDING' | 'FAILED';
  timestamp?: string;
  isCachedReplay?: boolean;
}

export interface NotificationItem {
  id: number;
  type: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  correlationId?: string;
  timestamp?: string;
}

export interface GatewayRoute {
  route_id: string;
  uri: string;
  predicates: string[];
  order?: number;
}

export interface AuditRecord {
  auditId: number;
  transactionId: number;
  accountId: number;
  referenceNo?: string;
  transactionType: string;
  amount: number;
  oldBalance: number;
  newBalance: number;
  previousHash: string;
  currentHash: string;
  actorId?: number;
  clientIp?: string;
  eventTimestamp: string;
}

export interface ChainVerificationResult {
  accountId: number;
  totalRecordsVerified: number;
  isChainIntact: boolean;
  latestHash?: string;
  verifiedAt: string;
  message: string;
}

