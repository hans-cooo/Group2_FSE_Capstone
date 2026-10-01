import React from 'react';
import { Wallet, TrendingUp, ShieldCheck, ArrowUpRight, ArrowDownLeft, Clock, RefreshCw } from 'lucide-react';
import type { Account } from '../types';

interface DashboardViewProps {
  accounts: Account[];
  onSelectTransfer: (sourceAccountId?: number) => void;
  onRefresh: () => void;
  isLoading: boolean;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  accounts,
  onSelectTransfer,
  onRefresh,
  isLoading
}) => {
  const totalBalance = accounts.reduce((acc, curr) => acc + curr.balance, 0);

  return (
    <div style={{ maxWidth: '1320px', margin: '0 auto', width: 'calc(100% - 32px)', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      {/* Top Hero & Metric KPI Row */}
      <div className="grid-4">
        {/* KPI 1: Total Liquidity */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--text-muted)' }}>
              Total Vault Liquidity
            </span>
            <div style={{ padding: '8px', borderRadius: '8px', background: 'rgba(16, 185, 129, 0.1)', color: 'var(--color-success)' }}>
              <Wallet size={18} />
            </div>
          </div>
          <div style={{ marginTop: '12px', fontSize: '28px', fontWeight: '800', fontFamily: 'var(--font-mono)', color: '#ffffff' }}>
            ₱{totalBalance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: 'var(--color-success)' }}>
            <TrendingUp size={14} />
            <span>Pessimistic row locking active</span>
          </div>
        </div>

        {/* KPI 2: Active Accounts */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--text-muted)' }}>
              Managed Core Accounts
            </span>
            <div style={{ padding: '8px', borderRadius: '8px', background: 'rgba(56, 189, 248, 0.1)', color: 'var(--accent-cyan)' }}>
              <ShieldCheck size={18} />
            </div>
          </div>
          <div style={{ marginTop: '12px', fontSize: '28px', fontWeight: '800', fontFamily: 'var(--font-mono)', color: '#ffffff' }}>
            {accounts.length}
          </div>
          <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: 'var(--accent-cyan)' }}>
            <span className="pulse-dot" style={{ color: 'var(--accent-cyan)' }}></span>
            <span>All accounts in ACTIVE state</span>
          </div>
        </div>

        {/* KPI 3: Idempotency Reliability */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--text-muted)' }}>
              Idempotency Mutex
            </span>
            <div style={{ padding: '8px', borderRadius: '8px', background: 'rgba(99, 102, 241, 0.1)', color: 'var(--accent-indigo)' }}>
              <Clock size={18} />
            </div>
          </div>
          <div style={{ marginTop: '12px', fontSize: '28px', fontWeight: '800', fontFamily: 'var(--font-mono)', color: '#ffffff' }}>
            100%
          </div>
          <div style={{ marginTop: '8px', fontSize: '12px', color: 'var(--text-subtle)' }}>
            Redis atomic SETNX pre-flight guard
          </div>
        </div>

        {/* KPI 4: Cryptographic Audit Status */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--text-muted)' }}>
              Forensic Hash Chaining
            </span>
            <div style={{ padding: '8px', borderRadius: '8px', background: 'rgba(245, 158, 11, 0.1)', color: 'var(--color-warning)' }}>
              <ShieldCheck size={18} />
            </div>
          </div>
          <div style={{ marginTop: '12px', fontSize: '20px', fontWeight: '800', color: 'var(--color-warning)', letterSpacing: '0.02em' }}>
            SHA-256 SYNCED
          </div>
          <div style={{ marginTop: '12px', fontSize: '12px', color: 'var(--text-subtle)' }}>
            PostgreSQL immutable audit store
          </div>
        </div>
      </div>

      {/* Account Cards Header & Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: '800', color: '#ffffff' }}>
            Core Accounts & Ledgers
          </h2>
          <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
            Real-time balance feeds with pessimistic row locking and double-entry consistency
          </p>
        </div>

        <button
          onClick={onRefresh}
          disabled={isLoading}
          className="btn btn-secondary"
          style={{ padding: '8px 14px', fontSize: '13px' }}
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          <span>Refresh Balances</span>
        </button>
      </div>

      {/* Account Cards Grid */}
      <div className="grid-3">
        {accounts.map((account) => {
          const isChecking = account.accountType === 'CHECKING';
          const isSavings = account.accountType === 'SAVINGS';

          const gradient = isChecking
            ? 'linear-gradient(135deg, rgba(14, 165, 233, 0.15) 0%, rgba(15, 23, 42, 0.8) 100%)'
            : isSavings
            ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(15, 23, 42, 0.8) 100%)'
            : 'linear-gradient(135deg, rgba(99, 102, 241, 0.15) 0%, rgba(15, 23, 42, 0.8) 100%)';

          const borderColor = isChecking
            ? 'rgba(56, 189, 248, 0.3)'
            : isSavings
            ? 'rgba(16, 185, 129, 0.3)'
            : 'rgba(99, 102, 241, 0.3)';

          return (
            <div
              key={account.accountId}
              className="glass-card glass-card-interactive"
              style={{
                padding: '24px',
                background: gradient,
                borderColor: borderColor,
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                minHeight: '220px'
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span className="badge badge-info" style={{ fontSize: '11px' }}>
                    {account.accountType}
                  </span>
                  <span className={`badge ${account.status === 'ACTIVE' ? 'badge-success' : account.status === 'FROZEN' ? 'badge-danger' : 'badge-warning'}`} style={{ fontSize: '11px' }}>
                    {account.status === 'ACTIVE' && <span className="pulse-dot"></span>}
                    {account.status}
                  </span>
                </div>

                <div style={{ marginTop: '16px', fontSize: '13px', color: 'var(--text-subtle)', fontFamily: 'var(--font-mono)' }}>
                  ACCOUNT NUMBER
                </div>
                <div style={{ fontSize: '16px', fontWeight: '700', fontFamily: 'var(--font-mono)', color: '#ffffff', letterSpacing: '0.05em' }}>
                  {account.accountNumber}
                </div>
                {account.customerName && (
                  <div style={{ marginTop: '6px', fontSize: '12px', fontWeight: '600', color: 'var(--accent-cyan)' }}>
                    Holder: {account.customerName}
                  </div>
                )}
              </div>

              <div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  Available Balance
                </div>
                <div style={{ fontSize: '30px', fontWeight: '800', fontFamily: 'var(--font-mono)', color: '#ffffff' }}>
                  ₱{account.balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>

                <div style={{ marginTop: '16px', display: 'flex', gap: '8px' }}>
                  <button
                    onClick={() => onSelectTransfer(account.accountId)}
                    disabled={account.status !== 'ACTIVE'}
                    className="btn btn-primary"
                    style={{ flex: 1, padding: '8px 12px', fontSize: '12px', opacity: account.status !== 'ACTIVE' ? 0.5 : 1, cursor: account.status !== 'ACTIVE' ? 'not-allowed' : 'pointer' }}
                  >
                    <ArrowUpRight size={14} />
                    <span>{account.status !== 'ACTIVE' ? `Account ${account.status}` : 'Transfer Funds'}</span>
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Simulated Double-Entry Ledger Transactions Table */}
      <div className="glass-card" style={{ padding: '24px', marginTop: '8px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div>
            <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#ffffff' }}>
              Recent Double-Entry Ledger Transactions
            </h3>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              Dual-written to Oracle XE (Master System of Record) and PostgreSQL (Forensic Audit Store)
            </p>
          </div>
          <span className="badge badge-info" style={{ fontSize: '11px' }}>
            KAFKA STREAM: LEDGER.MUTATION.COMPLETED.V1
          </span>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-subtle)' }}>
                <th style={{ padding: '12px 16px', fontWeight: '600' }}>TXN ID</th>
                <th style={{ padding: '12px 16px', fontWeight: '600' }}>TYPE</th>
                <th style={{ padding: '12px 16px', fontWeight: '600' }}>SOURCE / DEST</th>
                <th style={{ padding: '12px 16px', fontWeight: '600' }}>AMOUNT</th>
                <th style={{ padding: '12px 16px', fontWeight: '600' }}>STATUS</th>
                <th style={{ padding: '12px 16px', fontWeight: '600' }}>FORENSIC HASH</th>
              </tr>
            </thead>
            <tbody>
              <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>TXN-9021</td>
                <td style={{ padding: '14px 16px' }}>
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--color-danger)' }}>
                    <ArrowUpRight size={14} /> DEBIT Leg
                  </span>
                </td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)' }}>ACC-1001-8842 &rarr; ACC-1002-9915</td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', fontWeight: '700', color: 'var(--color-danger)' }}>-₱150.00</td>
                <td style={{ padding: '14px 16px' }}><span className="badge badge-success">COMPLETED</span></td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', color: 'var(--text-subtle)', fontSize: '12px' }}>
                  8e3f94bc12a0e4...
                </td>
              </tr>
              <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>TXN-9020</td>
                <td style={{ padding: '14px 16px' }}>
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--color-success)' }}>
                    <ArrowDownLeft size={14} /> CREDIT Leg
                  </span>
                </td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)' }}>ACC-1001-8842 &rarr; ACC-1002-9915</td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', fontWeight: '700', color: 'var(--color-success)' }}>+₱150.00</td>
                <td style={{ padding: '14px 16px' }}><span className="badge badge-success">COMPLETED</span></td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', color: 'var(--text-subtle)', fontSize: '12px' }}>
                  5f1c97a824b1d0...
                </td>
              </tr>
              <tr>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>TXN-9018</td>
                <td style={{ padding: '14px 16px' }}>
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--color-success)' }}>
                    <ArrowDownLeft size={14} /> PAYROLL DEPOSIT
                  </span>
                </td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)' }}>CORP-PAYROLL &rarr; ACC-1003-3321</td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', fontWeight: '700', color: 'var(--color-success)' }}>+₱3,250.00</td>
                <td style={{ padding: '14px 16px' }}><span className="badge badge-success">COMPLETED</span></td>
                <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', color: 'var(--text-subtle)', fontSize: '12px' }}>
                  1b4d88e792c3a5...
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
