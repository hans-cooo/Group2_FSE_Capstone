import React, { useState, useEffect, useCallback } from 'react';
import { ShieldCheck, AlertTriangle, RefreshCw, Database, Key, CheckCircle2, Lock, Unlock, Hash, Eye, Sparkles } from 'lucide-react';
import { apiClient } from '../services/api';
import type { Account, AuditRecord, ChainVerificationResult } from '../types';

interface AuditViewProps {
  accounts: Account[];
}

export const AuditView: React.FC<AuditViewProps> = ({ accounts }) => {
  const [selectedAccountId, setSelectedAccountId] = useState<number>(accounts[0]?.accountId || 1);
  const [auditRecords, setAuditRecords] = useState<AuditRecord[]>([]);
  const [verificationResult, setVerificationResult] = useState<ChainVerificationResult | null>(null);
  const [isVerifying, setIsVerifying] = useState<boolean>(false);
  const [selectedRecord, setSelectedRecord] = useState<AuditRecord | null>(null);

  const loadAuditData = useCallback(async (accId: number) => {
    const records = await apiClient.getAccountAuditStatements(accId);
    setAuditRecords(records);
    // Auto-run verification
    setIsVerifying(true);
    const result = await apiClient.verifyAuditChain(accId);
    setVerificationResult(result);
    setIsVerifying(false);
  }, []);

  useEffect(() => {
    if (selectedAccountId) {
      loadAuditData(selectedAccountId);
    }
  }, [selectedAccountId, loadAuditData]);

  const handleVerifyChain = async () => {
    setIsVerifying(true);
    const result = await apiClient.verifyAuditChain(selectedAccountId);
    setVerificationResult(result);
    setIsVerifying(false);
  };

  const handleSimulateTamper = async () => {
    apiClient.simulateTampering(selectedAccountId);
    const records = await apiClient.getAccountAuditStatements(selectedAccountId);
    setAuditRecords([...records]);
    setIsVerifying(true);
    const result = await apiClient.verifyAuditChain(selectedAccountId);
    setVerificationResult(result);
    setIsVerifying(false);
  };

  const handleRestoreChain = async () => {
    apiClient.restoreIntactChain(selectedAccountId);
    const records = await apiClient.getAccountAuditStatements(selectedAccountId);
    setAuditRecords([...records]);
    setIsVerifying(true);
    const result = await apiClient.verifyAuditChain(selectedAccountId);
    setVerificationResult(result);
    setIsVerifying(false);
  };

  const selectedAccount = accounts.find(a => a.accountId === selectedAccountId) || accounts[0];

  return (
    <div style={{ maxWidth: '1320px', margin: '0 auto', width: 'calc(100% - 32px)', display: 'flex', flexDirection: 'column', gap: '28px' }}>
      
      {/* Title & Architecture Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-start', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '10px',
              background: 'rgba(99, 102, 241, 0.15)',
              border: '1px solid rgba(99, 102, 241, 0.3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent-indigo)'
            }}>
              <ShieldCheck size={22} />
            </div>
            <h2 style={{ fontSize: '24px', fontWeight: '800', color: '#ffffff', letterSpacing: '-0.02em' }}>
              Cryptographic Audit & Compliance Engine
            </h2>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginTop: '6px' }}>
            Immutable SHA-256 tamper-evident hash chaining enforced by PostgreSQL 16 (<span style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>audit_store</span> on port 5434).
          </p>
        </div>

        {/* Regulatory Badges */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
          <span style={{
            fontSize: '11px',
            fontWeight: '600',
            padding: '5px 12px',
            borderRadius: '9999px',
            background: 'rgba(16, 185, 129, 0.1)',
            border: '1px solid rgba(16, 185, 129, 0.25)',
            color: 'var(--color-success)',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}>
            <Database size={12} />
            PostgreSQL 16 Isolated Store
          </span>
          <span style={{
            fontSize: '11px',
            fontWeight: '600',
            padding: '5px 12px',
            borderRadius: '9999px',
            background: 'rgba(56, 189, 248, 0.1)',
            border: '1px solid rgba(56, 189, 248, 0.25)',
            color: 'var(--accent-cyan)',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}>
            <Hash size={12} />
            SHA-256 Link Invariant
          </span>
          <span style={{
            fontSize: '11px',
            fontWeight: '600',
            padding: '5px 12px',
            borderRadius: '9999px',
            background: 'rgba(99, 102, 241, 0.1)',
            border: '1px solid rgba(99, 102, 241, 0.25)',
            color: 'var(--accent-indigo)',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}>
            <Lock size={12} />
            RBAC: ROLE_ADMIN / AUDITOR
          </span>
        </div>
      </div>

      {/* Account Selector & Verification Controls Toolbar */}
      <div style={{
        background: 'rgba(15, 23, 42, 0.75)',
        border: '1px solid var(--border-subtle)',
        borderRadius: '16px',
        padding: '20px 24px',
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '16px',
        backdropFilter: 'blur(16px)'
      }}>
        {/* Left: Account selector */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--text-muted)' }}>Target Account:</span>
          <div style={{ display: 'flex', gap: '8px' }}>
            {accounts.map(acc => (
              <button
                key={acc.accountId}
                onClick={() => setSelectedAccountId(acc.accountId)}
                style={{
                  padding: '8px 14px',
                  borderRadius: '10px',
                  fontSize: '12px',
                  fontWeight: '600',
                  border: selectedAccountId === acc.accountId ? '1px solid var(--accent-cyan)' : '1px solid var(--border-subtle)',
                  background: selectedAccountId === acc.accountId ? 'rgba(56, 189, 248, 0.15)' : 'rgba(2, 6, 23, 0.5)',
                  color: selectedAccountId === acc.accountId ? '#ffffff' : 'var(--text-muted)',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
              >
                #{acc.accountNumber} ({acc.accountType})
              </button>
            ))}
          </div>
        </div>

        {/* Right: Actions */}
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={handleVerifyChain}
            disabled={isVerifying}
            style={{
              padding: '9px 18px',
              borderRadius: '10px',
              fontSize: '13px',
              fontWeight: '700',
              border: 'none',
              background: 'linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%)',
              color: '#ffffff',
              cursor: isVerifying ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              boxShadow: '0 4px 15px rgba(14, 165, 233, 0.3)'
            }}
          >
            <RefreshCw size={15} className={isVerifying ? 'spin-icon' : ''} />
            <span>{isVerifying ? 'Verifying Hashes...' : 'Verify Chain Integrity'}</span>
          </button>

          <button
            onClick={handleSimulateTamper}
            title="Simulate retroactive unauthorized modification of a database record"
            style={{
              padding: '9px 14px',
              borderRadius: '10px',
              fontSize: '12px',
              fontWeight: '600',
              border: '1px solid rgba(244, 63, 94, 0.4)',
              background: 'rgba(244, 63, 94, 0.1)',
              color: 'var(--color-danger)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            <Unlock size={14} />
            <span>Simulate DB Tamper</span>
          </button>

          <button
            onClick={handleRestoreChain}
            title="Restore original pristine cryptographic hash chain"
            style={{
              padding: '9px 14px',
              borderRadius: '10px',
              fontSize: '12px',
              fontWeight: '600',
              border: '1px solid var(--border-subtle)',
              background: 'rgba(2, 6, 23, 0.5)',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            <Sparkles size={14} />
            <span>Restore Pristine</span>
          </button>
        </div>
      </div>

      {/* Verification Status Banner */}
      {verificationResult && (
        <div style={{
          background: verificationResult.isChainIntact ? 'rgba(16, 185, 129, 0.08)' : 'rgba(244, 63, 94, 0.1)',
          border: `1px solid ${verificationResult.isChainIntact ? 'rgba(16, 185, 129, 0.35)' : 'rgba(244, 63, 94, 0.4)'}`,
          borderRadius: '16px',
          padding: '24px',
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '20px',
          boxShadow: verificationResult.isChainIntact ? '0 10px 30px rgba(16, 185, 129, 0.08)' : '0 10px 30px rgba(244, 63, 94, 0.15)'
        }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '16px' }}>
            <div style={{
              width: '48px',
              height: '48px',
              borderRadius: '14px',
              background: verificationResult.isChainIntact ? 'rgba(16, 185, 129, 0.2)' : 'rgba(244, 63, 94, 0.2)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: verificationResult.isChainIntact ? 'var(--color-success)' : 'var(--color-danger)'
            }}>
              {verificationResult.isChainIntact ? <CheckCircle2 size={28} /> : <AlertTriangle size={28} />}
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <h3 style={{
                  fontSize: '18px',
                  fontWeight: '800',
                  color: verificationResult.isChainIntact ? 'var(--color-success)' : 'var(--color-danger)'
                }}>
                  {verificationResult.isChainIntact ? 'AUDIT CHAIN INTACT — ZERO TAMPERING DETECTED' : 'CRYPTOGRAPHIC TAMPER DETECTED! CHAIN BROKEN'}
                </h3>
                <span style={{
                  fontSize: '11px',
                  fontFamily: 'var(--font-mono)',
                  padding: '2px 8px',
                  borderRadius: '6px',
                  background: 'rgba(2, 6, 23, 0.6)',
                  color: '#ffffff',
                  border: '1px solid var(--border-subtle)'
                }}>
                  HTTP 200 OK (Gateway Verified)
                </span>
              </div>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginTop: '4px', maxWidth: '750px', lineHeight: '1.5' }}>
                {verificationResult.message}
              </p>
            </div>
          </div>

          {/* Quick Metrics */}
          <div style={{ display: 'flex', gap: '20px', fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
            <div>
              <div style={{ color: 'var(--text-subtle)', fontSize: '11px' }}>VERIFIED BLOCKS</div>
              <div style={{ fontSize: '16px', fontWeight: '700', color: '#ffffff', marginTop: '2px' }}>
                {verificationResult.totalRecordsVerified} records
              </div>
            </div>
            <div>
              <div style={{ color: 'var(--text-subtle)', fontSize: '11px' }}>LATEST SHA-256 DIGEST</div>
              <div style={{ fontSize: '13px', color: 'var(--accent-cyan)', marginTop: '4px' }}>
                {verificationResult.latestHash ? `${verificationResult.latestHash.slice(0, 16)}...` : 'N/A'}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Cryptographic Chain Visualizer */}
      <div>
        <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#ffffff', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Key size={18} color="var(--accent-cyan)" />
          <span>Sequential Cryptographic Hash Chain Continuity</span>
        </h3>
        
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '16px'
        }}>
          {auditRecords.map((record, index) => {
            const isGenesis = index === 0 && record.previousHash.startsWith('00000000');
            const isBroken = index > 0 && record.previousHash !== auditRecords[index - 1].currentHash;

            return (
              <div
                key={record.auditId}
                onClick={() => setSelectedRecord(record)}
                style={{
                  background: isBroken ? 'rgba(244, 63, 94, 0.08)' : 'rgba(15, 23, 42, 0.65)',
                  border: `1px solid ${isBroken ? 'rgba(244, 63, 94, 0.5)' : 'var(--border-subtle)'}`,
                  borderRadius: '14px',
                  padding: '18px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '12px',
                  cursor: 'pointer',
                  position: 'relative',
                  transition: 'all 0.2s ease',
                  backdropFilter: 'blur(10px)'
                }}
              >
                {/* Block header */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{
                      fontSize: '11px',
                      fontWeight: '700',
                      padding: '2px 8px',
                      borderRadius: '6px',
                      background: isGenesis ? 'rgba(56, 189, 248, 0.2)' : 'rgba(2, 6, 23, 0.6)',
                      color: isGenesis ? 'var(--accent-cyan)' : '#ffffff',
                      border: '1px solid var(--border-subtle)'
                    }}>
                      {isGenesis ? 'GENESIS BLOCK #1' : `BLOCK #${index + 1}`}
                    </span>
                    <span style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                      Audit ID: {record.auditId}
                    </span>
                  </div>

                  <span style={{
                    fontSize: '11px',
                    fontWeight: '700',
                    color: record.transactionType.includes('DEPOSIT') || record.transactionType.includes('CREDIT') ? 'var(--color-success)' : 'var(--accent-indigo)'
                  }}>
                    {record.transactionType}
                  </span>
                </div>

                {/* Amount and balance */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                  <span style={{ fontSize: '18px', fontWeight: '800', color: '#ffffff' }}>
                    ₱{record.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                  </span>
                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    Bal: ₱{record.newBalance.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                  </span>
                </div>

                {/* Hash Links */}
                <div style={{
                  background: 'rgba(2, 6, 23, 0.7)',
                  borderRadius: '8px',
                  padding: '10px',
                  fontFamily: 'var(--font-mono)',
                  fontSize: '11px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '6px'
                }}>
                  <div>
                    <span style={{ color: 'var(--text-subtle)' }}>Prev Hash: </span>
                    <span style={{ color: isBroken ? 'var(--color-danger)' : 'var(--text-muted)' }}>
                      {record.previousHash.slice(0, 16)}...
                    </span>
                  </div>
                  <div>
                    <span style={{ color: 'var(--text-subtle)' }}>Curr Hash: </span>
                    <span style={{ color: 'var(--accent-cyan)' }}>
                      {record.currentHash.slice(0, 16)}...
                    </span>
                  </div>
                </div>

                {/* Status indicator */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '11px', color: 'var(--text-subtle)' }}>
                  <span>{new Date(record.eventTimestamp).toLocaleTimeString()}</span>
                  <span style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    color: isBroken ? 'var(--color-danger)' : 'var(--color-success)',
                    fontWeight: '600'
                  }}>
                    {isBroken ? <AlertTriangle size={12} /> : <Lock size={12} />}
                    {isBroken ? 'Link Broken' : 'Chained & Sealed'}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Forensic Transaction Mutation Table */}
      <div style={{
        background: 'rgba(15, 23, 42, 0.65)',
        border: '1px solid var(--border-subtle)',
        borderRadius: '16px',
        overflow: 'hidden',
        backdropFilter: 'blur(16px)'
      }}>
        <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#ffffff' }}>
              Forensic Transaction Ledger Journal
            </h3>
            <p style={{ fontSize: '12px', color: 'var(--text-subtle)', marginTop: '2px' }}>
              Official immutable statement generated by Audit Service (<span style={{ fontFamily: 'var(--font-mono)' }}>/api/v1/audit/accounts/{selectedAccount?.accountId || 1}/statement</span>)
            </p>
          </div>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
            Showing {auditRecords.length} mutations
          </span>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ background: 'rgba(2, 6, 23, 0.5)', color: 'var(--text-muted)', borderBottom: '1px solid var(--border-subtle)' }}>
                <th style={{ padding: '14px 20px', fontWeight: '600' }}>Audit ID</th>
                <th style={{ padding: '14px 20px', fontWeight: '600' }}>Reference No</th>
                <th style={{ padding: '14px 20px', fontWeight: '600' }}>Type</th>
                <th style={{ padding: '14px 20px', fontWeight: '600' }}>Amount</th>
                <th style={{ padding: '14px 20px', fontWeight: '600' }}>Balance After</th>
                <th style={{ padding: '14px 20px', fontWeight: '600' }}>Current Hash</th>
                <th style={{ padding: '14px 20px', fontWeight: '600' }}>Timestamp</th>
                <th style={{ padding: '14px 20px', fontWeight: '600' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {auditRecords.map(record => (
                <tr
                  key={record.auditId}
                  style={{
                    borderBottom: '1px solid var(--border-subtle)',
                    transition: 'background 0.15s ease'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(56, 189, 248, 0.04)'}
                  onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                >
                  <td style={{ padding: '14px 20px', fontFamily: 'var(--font-mono)', color: 'var(--text-subtle)' }}>
                    #{record.auditId}
                  </td>
                  <td style={{ padding: '14px 20px', fontWeight: '600', color: '#ffffff' }}>
                    {record.referenceNo || `TXN-${record.transactionId}`}
                  </td>
                  <td style={{ padding: '14px 20px' }}>
                    <span style={{
                      fontSize: '11px',
                      fontWeight: '700',
                      padding: '3px 8px',
                      borderRadius: '6px',
                      background: record.transactionType.includes('DEPOSIT') || record.transactionType.includes('CREDIT') ? 'rgba(16, 185, 129, 0.15)' : 'rgba(99, 102, 241, 0.15)',
                      color: record.transactionType.includes('DEPOSIT') || record.transactionType.includes('CREDIT') ? 'var(--color-success)' : 'var(--accent-indigo)'
                    }}>
                      {record.transactionType}
                    </span>
                  </td>
                  <td style={{ padding: '14px 20px', fontWeight: '700', color: '#ffffff' }}>
                    ₱{record.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                  </td>
                  <td style={{ padding: '14px 20px', color: 'var(--text-muted)' }}>
                    ₱{record.newBalance.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                  </td>
                  <td style={{ padding: '14px 20px', fontFamily: 'var(--font-mono)', fontSize: '11px', color: 'var(--accent-cyan)' }}>
                    {record.currentHash.slice(0, 16)}...
                  </td>
                  <td style={{ padding: '14px 20px', fontSize: '12px', color: 'var(--text-subtle)' }}>
                    {new Date(record.eventTimestamp).toLocaleString()}
                  </td>
                  <td style={{ padding: '14px 20px' }}>
                    <button
                      onClick={() => setSelectedRecord(record)}
                      style={{
                        padding: '6px 10px',
                        borderRadius: '6px',
                        fontSize: '11px',
                        border: '1px solid var(--border-subtle)',
                        background: 'rgba(2, 6, 23, 0.6)',
                        color: 'var(--accent-cyan)',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px'
                      }}
                    >
                      <Eye size={12} />
                      Inspect
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Forensic Detail Modal */}
      {selectedRecord && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(2, 6, 23, 0.85)',
          backdropFilter: 'blur(12px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '20px'
        }}>
          <div style={{
            background: 'rgba(15, 23, 42, 0.95)',
            border: '1px solid var(--border-subtle)',
            borderRadius: '20px',
            width: '100%',
            maxWidth: '680px',
            padding: '28px',
            display: 'flex',
            flexDirection: 'column',
            gap: '20px',
            boxShadow: '0 25px 60px rgba(0, 0, 0, 0.8)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '14px' }}>
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: '800', color: '#ffffff' }}>
                  Forensic Mutation Block Details
                </h3>
                <span style={{ fontSize: '12px', color: 'var(--text-subtle)' }}>
                  Audit Record #{selectedRecord.auditId} &bull; Transaction ID #{selectedRecord.transactionId}
                </span>
              </div>
              <button
                onClick={() => setSelectedRecord(null)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: 'var(--text-muted)',
                  fontSize: '20px',
                  cursor: 'pointer'
                }}
              >
                &times;
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', fontSize: '13px' }}>
              <div>
                <div style={{ color: 'var(--text-subtle)', fontSize: '11px' }}>REFERENCE NUMBER</div>
                <div style={{ fontWeight: '600', color: '#ffffff', marginTop: '2px' }}>{selectedRecord.referenceNo || 'N/A'}</div>
              </div>
              <div>
                <div style={{ color: 'var(--text-subtle)', fontSize: '11px' }}>MUTATION TYPE</div>
                <div style={{ fontWeight: '600', color: 'var(--color-success)', marginTop: '2px' }}>{selectedRecord.transactionType}</div>
              </div>
              <div>
                <div style={{ color: 'var(--text-subtle)', fontSize: '11px' }}>MUTATION AMOUNT</div>
                <div style={{ fontWeight: '700', color: '#ffffff', marginTop: '2px' }}>₱{selectedRecord.amount.toFixed(2)}</div>
              </div>
              <div>
                <div style={{ color: 'var(--text-subtle)', fontSize: '11px' }}>NEW BALANCE</div>
                <div style={{ fontWeight: '700', color: 'var(--accent-cyan)', marginTop: '2px' }}>₱{selectedRecord.newBalance.toFixed(2)}</div>
              </div>
              <div>
                <div style={{ color: 'var(--text-subtle)', fontSize: '11px' }}>ACTOR ID / CLIENT IP</div>
                <div style={{ color: 'var(--text-muted)', marginTop: '2px' }}>Actor #{selectedRecord.actorId || 1} ({selectedRecord.clientIp || '127.0.0.1'})</div>
              </div>
              <div>
                <div style={{ color: 'var(--text-subtle)', fontSize: '11px' }}>RECORD TIMESTAMP</div>
                <div style={{ color: 'var(--text-muted)', marginTop: '2px' }}>{new Date(selectedRecord.eventTimestamp).toLocaleString()}</div>
              </div>
            </div>

            <div style={{
              background: 'rgba(2, 6, 23, 0.8)',
              borderRadius: '12px',
              padding: '16px',
              fontFamily: 'var(--font-mono)',
              fontSize: '11px',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
              border: '1px solid var(--border-subtle)'
            }}>
              <div>
                <div style={{ color: 'var(--text-subtle)', marginBottom: '4px' }}>PREVIOUS HASH (PREDECESSOR LINK)</div>
                <div style={{ color: 'var(--text-muted)', wordBreak: 'break-all' }}>{selectedRecord.previousHash}</div>
              </div>
              <div>
                <div style={{ color: 'var(--text-subtle)', marginBottom: '4px' }}>CURRENT SHA-256 HASH DIGEST</div>
                <div style={{ color: 'var(--accent-cyan)', wordBreak: 'break-all' }}>{selectedRecord.currentHash}</div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setSelectedRecord(null)}
                style={{
                  padding: '8px 20px',
                  borderRadius: '10px',
                  background: 'var(--border-subtle)',
                  border: 'none',
                  color: '#ffffff',
                  fontSize: '13px',
                  fontWeight: '600',
                  cursor: 'pointer'
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
