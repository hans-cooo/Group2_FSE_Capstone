import { useState, useEffect } from 'react';
import {
  FileCheck2,
  ArrowRightLeft,
  Banknote,
  PlusCircle,
  CheckCircle2,
  XCircle,
  Building2
} from 'lucide-react';
import type {
  Account,
  KycRequestResponse,
  PendingTransferItem
} from '../types';
import { apiClient } from '../services/api';

interface TellerWorkspaceProps {
  onRefreshGlobalData: () => void;
  showToast: (type: 'success' | 'info' | 'error', msg: string) => void;
}

export const TellerWorkspace: React.FC<TellerWorkspaceProps> = ({
  onRefreshGlobalData,
  showToast
}) => {
  const [activeTab, setActiveTab] = useState<'kyc' | 'transfers' | 'cash' | 'accounts'>('kyc');

  // KYC State
  const [pendingKyc, setPendingKyc] = useState<KycRequestResponse[]>([]);
  const [rejectKycModal, setRejectKycModal] = useState<{ id: number; reason: string } | null>(null);

  // Transfers State
  const [pendingTransfers, setPendingTransfers] = useState<PendingTransferItem[]>([]);
  const [rejectTransferModal, setRejectTransferModal] = useState<{ id: number; reason: string } | null>(null);

  // Cash Operations State
  const [cashAction, setCashAction] = useState<'DEPOSIT' | 'WITHDRAWAL'>('DEPOSIT');
  const [cashAccountId, setCashAccountId] = useState<string>('1');
  const [cashAmount, setCashAmount] = useState<string>('1000.00');
  const [cashRemarks, setCashRemarks] = useState<string>('Over-the-counter teller transaction');
  const [cashLoading, setCashLoading] = useState(false);

  // Open Account State
  const [newAccCustomerId, setNewAccCustomerId] = useState<string>('1');
  const [newAccType, setNewAccType] = useState<string>('SAVINGS');
  const [newAccInitialDeposit, setNewAccInitialDeposit] = useState<string>('500.00');
  const [newAccLoading, setNewAccLoading] = useState(false);

  // Accounts List & Status
  const [allAccounts, setAllAccounts] = useState<Account[]>([]);

  const loadKyc = async () => {
    try {
      const list = await apiClient.getPendingKycUpdateRequests();
      setPendingKyc(list);
    } catch {
      // fallback
    }
  };

  const loadTransfers = async () => {
    try {
      const list = await apiClient.getPendingTransfers();
      setPendingTransfers(list);
    } catch {
      // fallback
    }
  };

  const loadAccounts = async () => {
    try {
      const list = await apiClient.getAllAccounts();
      setAllAccounts(list);
    } catch {
      // fallback
    }
  };

  const loadAll = async () => {
    loadKyc();
    loadTransfers();
    loadAccounts();
  };

  useEffect(() => {
    loadAll();
  }, []);

  // KYC Actions
  const handleApproveKyc = async (id: number) => {
    try {
      await apiClient.approveKycUpdateRequest(id);
      showToast('success', `KYC request #${id} approved successfully.`);
      loadKyc();
      onRefreshGlobalData();
    } catch (err: any) {
      showToast('error', err.message || 'Failed to approve KYC.');
    }
  };

  const handleRejectKyc = async () => {
    if (!rejectKycModal) return;
    if (!rejectKycModal.reason.trim()) {
      showToast('error', 'Rejection reason is mandatory.');
      return;
    }
    try {
      await apiClient.rejectKycUpdateRequest(rejectKycModal.id, rejectKycModal.reason);
      showToast('info', `KYC request #${rejectKycModal.id} rejected.`);
      setRejectKycModal(null);
      loadKyc();
      onRefreshGlobalData();
    } catch (err: any) {
      showToast('error', err.message || 'Failed to reject KYC.');
    }
  };

  // Transfer Actions
  const handleApproveTransfer = async (id: number) => {
    try {
      await apiClient.approvePendingTransfer(id);
      showToast('success', `High-value transfer #${id} authorized and executed.`);
      loadTransfers();
      loadAccounts();
      onRefreshGlobalData();
    } catch (err: any) {
      showToast('error', err.message || 'Failed to authorize transfer.');
    }
  };

  const handleRejectTransfer = async () => {
    if (!rejectTransferModal) return;
    try {
      await apiClient.rejectPendingTransfer(rejectTransferModal.id, rejectTransferModal.reason || 'Declined by branch teller');
      showToast('info', `Transfer request declined.`);
      setRejectTransferModal(null);
      loadTransfers();
      onRefreshGlobalData();
    } catch (err: any) {
      showToast('error', err.message || 'Failed to decline transfer.');
    }
  };

  // Cash Deposit / Withdrawal Submit
  const handleCashSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const accId = parseInt(cashAccountId, 10);
    const amt = parseFloat(cashAmount);

    if (isNaN(accId) || accId <= 0) {
      showToast('error', 'Please enter a valid Account ID.');
      return;
    }
    if (isNaN(amt) || amt <= 0) {
      showToast('error', 'Please enter a valid cash amount greater than zero.');
      return;
    }

    setCashLoading(true);
    try {
      const ref = `OTC-${cashAction.substring(0, 3)}-${Math.floor(100000 + Math.random() * 900000)}`;
      if (cashAction === 'DEPOSIT') {
        await apiClient.depositCash(accId, amt, ref, cashRemarks);
        showToast('success', `Cash deposit of ₱${amt.toFixed(2)} completed for Account #${accId}. Ref: ${ref}`);
      } else {
        await apiClient.withdrawCash(accId, amt, ref, cashRemarks);
        showToast('success', `Cash withdrawal of ₱${amt.toFixed(2)} processed from Account #${accId}. Ref: ${ref}`);
      }
      loadAccounts();
      onRefreshGlobalData();
    } catch (err: any) {
      showToast('error', err.message || 'Cash operation failed.');
    } finally {
      setCashLoading(false);
    }
  };

  // Open New Customer Account Submit
  const handleOpenAccountSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const custId = parseInt(newAccCustomerId, 10);
    const deposit = parseFloat(newAccInitialDeposit) || 0;

    if (isNaN(custId) || custId <= 0) {
      showToast('error', 'Customer ID must be a positive integer.');
      return;
    }

    setNewAccLoading(true);
    try {
      const newAcc = await apiClient.openAccount(custId, newAccType, deposit);
      showToast('success', `Opened new ${newAccType} account ${newAcc.accountNumber} for Customer #${custId}.`);
      loadAccounts();
      onRefreshGlobalData();
    } catch (err: any) {
      showToast('error', err.message || 'Failed to create account.');
    } finally {
      setNewAccLoading(false);
    }
  };

  // Status Change Submit
  const handleStatusChangeSubmit = async (accountId: number, status: 'ACTIVE' | 'FROZEN' | 'DORMANT' | 'CLOSED') => {
    try {
      await apiClient.updateAccountStatus(accountId, status);
      showToast('info', `Account #${accountId} status updated to ${status}.`);
      loadAccounts();
      onRefreshGlobalData();
    } catch (err: any) {
      showToast('error', err.message || 'Failed to update account status.');
    }
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '32px 24px' }}>
      {/* Navigation Sub-Tabs */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        borderBottom: '1px solid #e2e8f0',
        marginBottom: '28px',
        paddingBottom: '12px'
      }}>
        <button
          type="button"
          onClick={() => setActiveTab('kyc')}
          style={{
            padding: '10px 18px',
            borderRadius: '8px',
            fontWeight: '600',
            fontSize: '14px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: activeTab === 'kyc' ? '#eff6ff' : 'transparent',
            color: activeTab === 'kyc' ? '#1d4ed8' : '#64748b',
            border: activeTab === 'kyc' ? '1px solid #bfdbfe' : '1px solid transparent'
          }}
        >
          <FileCheck2 size={18} />
          <span>Pending KYC Requests ({pendingKyc.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('transfers')}
          style={{
            padding: '10px 18px',
            borderRadius: '8px',
            fontWeight: '600',
            fontSize: '14px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: activeTab === 'transfers' ? '#eff6ff' : 'transparent',
            color: activeTab === 'transfers' ? '#1d4ed8' : '#64748b',
            border: activeTab === 'transfers' ? '1px solid #bfdbfe' : '1px solid transparent'
          }}
        >
          <ArrowRightLeft size={18} />
          <span>Pending Transfer Approvals ({pendingTransfers.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('cash')}
          style={{
            padding: '10px 18px',
            borderRadius: '8px',
            fontWeight: '600',
            fontSize: '14px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: activeTab === 'cash' ? '#eff6ff' : 'transparent',
            color: activeTab === 'cash' ? '#1d4ed8' : '#64748b',
            border: activeTab === 'cash' ? '1px solid #bfdbfe' : '1px solid transparent'
          }}
        >
          <Banknote size={18} />
          <span>Cash Deposit / Withdrawal</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('accounts')}
          style={{
            padding: '10px 18px',
            borderRadius: '8px',
            fontWeight: '600',
            fontSize: '14px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: activeTab === 'accounts' ? '#eff6ff' : 'transparent',
            color: activeTab === 'accounts' ? '#1d4ed8' : '#64748b',
            border: activeTab === 'accounts' ? '1px solid #bfdbfe' : '1px solid transparent'
          }}
        >
          <Building2 size={18} />
          <span>Open & Manage Accounts</span>
        </button>
      </div>

      {/* ========================================================================= */}
      {/* 1. PENDING KYC UPDATE REQUESTS                                             */}
      {/* ========================================================================= */}
      {activeTab === 'kyc' && (
        <div className="animate-fade">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <div>
              <h2 style={{ fontSize: '20px', fontWeight: '800', color: '#0f172a' }}>Customer KYC Verification Queue</h2>
              <p style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                Review and approve KYC identity updates requested by customers.
              </p>
            </div>
            <button type="button" onClick={loadKyc} className="banking-btn-secondary" style={{ fontSize: '13px' }}>
              Refresh Queue
            </button>
          </div>

          {pendingKyc.length === 0 ? (
            <div className="banking-card" style={{ textAlign: 'center', padding: '50px 20px' }}>
              <CheckCircle2 size={40} style={{ color: '#059669', margin: '0 auto 12px' }} />
              <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a' }}>Queue is Clear</h3>
              <p style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>
                There are no pending customer KYC update requests awaiting verification.
              </p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {pendingKyc.map((req) => (
                <div key={req.kycRequestId} className="banking-card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a' }}>
                          {req.customerName || `Customer #${req.customerId}`}
                        </span>
                        <span className="badge badge-pending">PENDING VERIFICATION</span>
                      </div>
                      <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                        Request ID #{req.kycRequestId} &bull; Customer #{req.customerId} &bull; Submitted {new Date(req.requestedAt).toLocaleString()}
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        type="button"
                        onClick={() => handleApproveKyc(req.kycRequestId)}
                        className="banking-btn-primary"
                        style={{ padding: '8px 14px', fontSize: '13px', background: '#059669' }}
                      >
                        <CheckCircle2 size={15} />
                        <span>Approve</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => setRejectKycModal({ id: req.kycRequestId, reason: '' })}
                        className="banking-btn-danger"
                        style={{ padding: '8px 14px', fontSize: '13px' }}
                      >
                        <XCircle size={15} />
                        <span>Reject</span>
                      </button>
                    </div>
                  </div>

                  {/* Field Diffs */}
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                    gap: '12px',
                    padding: '16px',
                    background: '#f8fafc',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    fontSize: '13px'
                  }}>
                    <div>
                      <span style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase', fontWeight: '600' }}>Requested Name:</span>
                      <div style={{ fontWeight: '600', color: '#0f172a' }}>
                        {req.newFirstName} {req.newMiddleInitial ? req.newMiddleInitial + '. ' : ''}{req.newLastName}
                      </div>
                    </div>

                    <div>
                      <span style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase', fontWeight: '600' }}>New Mobile:</span>
                      <div style={{ fontWeight: '600', color: '#0f172a' }}>{req.newMobileNumber || 'N/A'}</div>
                    </div>

                    <div>
                      <span style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase', fontWeight: '600' }}>New Civil Status:</span>
                      <div style={{ fontWeight: '600', color: '#0f172a' }}>{req.newCivilStatus || 'N/A'}</div>
                    </div>

                    <div>
                      <span style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase', fontWeight: '600' }}>New Occupation:</span>
                      <div style={{ fontWeight: '600', color: '#0f172a' }}>{req.newOccupation || 'N/A'}</div>
                    </div>

                    <div style={{ gridColumn: '1 / -1' }}>
                      <span style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase', fontWeight: '600' }}>New Address:</span>
                      <div style={{ fontWeight: '600', color: '#0f172a' }}>{req.newAddress || 'N/A'}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Rejection Modal */}
          {rejectKycModal && (
            <div style={{
              position: 'fixed',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              background: 'rgba(15, 23, 42, 0.6)',
              backdropFilter: 'blur(4px)',
              zIndex: 100,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '20px'
            }}>
              <div style={{
                background: '#ffffff',
                borderRadius: '16px',
                maxWidth: '440px',
                width: '100%',
                padding: '24px',
                boxShadow: 'var(--shadow-modal)'
              }}>
                <h3 style={{ fontSize: '17px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
                  Reject KYC Request #{rejectKycModal.id}
                </h3>
                <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '16px' }}>
                  Please supply the official bank compliance reason for rejecting this customer update.
                </p>

                <textarea
                  rows={3}
                  value={rejectKycModal.reason}
                  onChange={(e) => setRejectKycModal({ ...rejectKycModal, reason: e.target.value })}
                  placeholder="e.g. Identity document unreadable or mismatched address proof"
                  required
                />

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                  <button type="button" onClick={() => setRejectKycModal(null)} className="banking-btn-secondary">
                    Cancel
                  </button>
                  <button type="button" onClick={handleRejectKyc} className="banking-btn-danger">
                    Confirm Rejection
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* 2. PENDING FUND TRANSFER APPROVALS (HIGH TRANSACTION REVIEW)             */}
      {/* ========================================================================= */}
      {activeTab === 'transfers' && (
        <div className="animate-fade">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <div>
              <h2 style={{ fontSize: '20px', fontWeight: '800', color: '#0f172a' }}>High-Value Transfer Dual-Control Queue</h2>
              <p style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                Teller authorization required for high-volume transactions and risk threshold limits.
              </p>
            </div>
            <button type="button" onClick={loadTransfers} className="banking-btn-secondary" style={{ fontSize: '13px' }}>
              Refresh Transfers
            </button>
          </div>

          {pendingTransfers.length === 0 ? (
            <div className="banking-card" style={{ textAlign: 'center', padding: '50px 20px' }}>
              <CheckCircle2 size={40} style={{ color: '#059669', margin: '0 auto 12px' }} />
              <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a' }}>No High-Value Transfers Pending</h3>
              <p style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>
                All customer transactions are currently within auto-settlement limits.
              </p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {pendingTransfers.map((item) => (
                <div key={item.transferRequestId} className="banking-card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontSize: '22px', fontWeight: '800', color: '#0f172a' }}>
                          ₱{item.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                        </span>
                        <span className="badge badge-pending">NEEDS TELLER APPROVAL</span>
                      </div>
                      <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                        Ref: {item.referenceNo} &bull; Requested {new Date(item.requestedAt).toLocaleString()}
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        type="button"
                        onClick={() => handleApproveTransfer(item.transferRequestId)}
                        className="banking-btn-primary"
                        style={{ padding: '8px 14px', fontSize: '13px', background: '#059669' }}
                      >
                        <CheckCircle2 size={15} />
                        <span>Authorize</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => setRejectTransferModal({ id: item.transferRequestId, reason: '' })}
                        className="banking-btn-danger"
                        style={{ padding: '8px 14px', fontSize: '13px' }}
                      >
                        <XCircle size={15} />
                        <span>Decline</span>
                      </button>
                    </div>
                  </div>

                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: '1fr 1fr 1fr',
                    gap: '12px',
                    padding: '14px',
                    background: '#f8fafc',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    fontSize: '13px'
                  }}>
                    <div>
                      <span style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase' }}>Debit Source:</span>
                      <div style={{ fontWeight: '600', fontFamily: 'var(--font-mono)' }}>
                        {item.sourceAccountNumber || `Account #${item.sourceAccountId}`}
                      </div>
                    </div>

                    <div>
                      <span style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase' }}>Credit Destination:</span>
                      <div style={{ fontWeight: '600', fontFamily: 'var(--font-mono)' }}>
                        {item.destinationAccountNumber || `Account #${item.destinationAccountId}`}
                      </div>
                    </div>

                    <div>
                      <span style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase' }}>Remarks:</span>
                      <div style={{ color: '#334155' }}>{item.remarks || 'Standard Transfer'}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Transfer Rejection Modal */}
          {rejectTransferModal && (
            <div style={{
              position: 'fixed',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              background: 'rgba(15, 23, 42, 0.6)',
              backdropFilter: 'blur(4px)',
              zIndex: 100,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '20px'
            }}>
              <div style={{
                background: '#ffffff',
                borderRadius: '16px',
                maxWidth: '440px',
                width: '100%',
                padding: '24px',
                boxShadow: 'var(--shadow-modal)'
              }}>
                <h3 style={{ fontSize: '17px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
                  Decline Transfer Authorization #{rejectTransferModal.id}
                </h3>
                <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '16px' }}>
                  Specify the teller compliance reason for declining this high-value transfer.
                </p>

                <textarea
                  rows={3}
                  value={rejectTransferModal.reason}
                  onChange={(e) => setRejectTransferModal({ ...rejectTransferModal, reason: e.target.value })}
                  placeholder="e.g. Unverified beneficiary details or risk threshold exceeded"
                  required
                />

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                  <button type="button" onClick={() => setRejectTransferModal(null)} className="banking-btn-secondary">
                    Cancel
                  </button>
                  <button type="button" onClick={handleRejectTransfer} className="banking-btn-danger">
                    Decline Transfer
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* 3. CASH DEPOSIT / WITHDRAWAL (OVER-THE-COUNTER MUTATIONS)                  */}
      {/* ========================================================================= */}
      {activeTab === 'cash' && (
        <div className="animate-fade" style={{ maxWidth: '640px', margin: '0 auto' }}>
          <div className="banking-card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
              <div style={{
                width: '40px',
                height: '40px',
                borderRadius: '10px',
                background: '#eff6ff',
                color: '#2563eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Banknote size={20} />
              </div>
              <div>
                <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a' }}>Over-The-Counter Cash Desk</h2>
                <p style={{ fontSize: '13px', color: '#64748b' }}>Post direct cash deposits or cash withdrawals to customer accounts.</p>
              </div>
            </div>

            <form onSubmit={handleCashSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Action Type Toggle */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => setCashAction('DEPOSIT')}
                  style={{
                    padding: '12px',
                    borderRadius: '8px',
                    fontWeight: '700',
                    fontSize: '14px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    background: cashAction === 'DEPOSIT' ? '#ecfdf5' : '#f8fafc',
                    color: cashAction === 'DEPOSIT' ? '#059669' : '#64748b',
                    border: cashAction === 'DEPOSIT' ? '2px solid #059669' : '1px solid #e2e8f0'
                  }}
                >
                  <PlusCircle size={18} />
                  <span>Cash Deposit (Credit)</span>
                </button>

                <button
                  type="button"
                  onClick={() => setCashAction('WITHDRAWAL')}
                  style={{
                    padding: '12px',
                    borderRadius: '8px',
                    fontWeight: '700',
                    fontSize: '14px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    background: cashAction === 'WITHDRAWAL' ? '#fef2f2' : '#f8fafc',
                    color: cashAction === 'WITHDRAWAL' ? '#dc2626' : '#64748b',
                    border: cashAction === 'WITHDRAWAL' ? '2px solid #dc2626' : '1px solid #e2e8f0'
                  }}
                >
                  <Banknote size={18} />
                  <span>Cash Withdrawal (Debit)</span>
                </button>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '6px' }}>
                  Select or Enter Target Account
                </label>
                <select
                  value={cashAccountId}
                  onChange={(e) => setCashAccountId(e.target.value)}
                >
                  {allAccounts.map(acc => (
                    <option key={acc.accountId} value={acc.accountId}>
                      #{acc.accountId} &bull; {acc.accountNumber} ({acc.accountType}) &mdash; Balance: ₱{acc.balance.toFixed(2)}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '6px' }}>
                  Cash Amount (PHP ₱)
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    value={cashAmount}
                    onChange={(e) => setCashAmount(e.target.value)}
                    required
                    style={{ paddingLeft: '32px', fontSize: '16px', fontWeight: '600' }}
                  />
                  <span style={{ position: 'absolute', left: '12px', top: '10px', fontWeight: '700', color: '#64748b' }}>₱</span>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '6px' }}>
                  Teller Remarks
                </label>
                <input
                  type="text"
                  value={cashRemarks}
                  onChange={(e) => setCashRemarks(e.target.value)}
                  placeholder="e.g. Branch counter teller settlement"
                />
              </div>

              <button
                type="submit"
                disabled={cashLoading}
                className="banking-btn-primary"
                style={{
                  marginTop: '8px',
                  padding: '12px',
                  background: cashAction === 'DEPOSIT' ? '#059669' : '#dc2626'
                }}
              >
                {cashLoading ? 'Posting to Ledger...' : `Confirm Cash ${cashAction === 'DEPOSIT' ? 'Deposit' : 'Withdrawal'}`}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 4. OPEN & MANAGE ACCOUNTS                                                */}
      {/* ========================================================================= */}
      {activeTab === 'accounts' && (
        <div className="animate-fade">
          {/* Create Account Form */}
          <div className="banking-card" style={{ marginBottom: '24px' }}>
            <h3 style={{ fontSize: '17px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>
              Open New Customer Deposit Account
            </h3>

            <form onSubmit={handleOpenAccountSubmit} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr auto', gap: '14px', alignItems: 'flex-end' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                  Customer ID
                </label>
                <input
                  type="number"
                  min="1"
                  value={newAccCustomerId}
                  onChange={(e) => setNewAccCustomerId(e.target.value)}
                  placeholder="e.g. 1"
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                  Account Type
                </label>
                <select value={newAccType} onChange={(e) => setNewAccType(e.target.value)}>
                  <option value="SAVINGS">SAVINGS</option>
                  <option value="CHECKING">CHECKING</option>
                  <option value="PAYROLL">PAYROLL</option>
                  <option value="TIME_DEPOSIT">TIME_DEPOSIT</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                  Initial Deposit (₱)
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={newAccInitialDeposit}
                  onChange={(e) => setNewAccInitialDeposit(e.target.value)}
                  placeholder="0.00"
                />
              </div>

              <button
                type="submit"
                disabled={newAccLoading}
                className="banking-btn-primary"
                style={{ padding: '10px 18px' }}
              >
                <PlusCircle size={16} />
                <span>Open Account</span>
              </button>
            </form>
          </div>

          {/* All Accounts Listing & Status Toggles */}
          <div className="banking-card">
            <h3 style={{ fontSize: '17px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>
              System Accounts Directory & Status Maintenance
            </h3>

            <div className="banking-table-container">
              <table className="banking-table">
                <thead>
                  <tr>
                    <th>Account ID</th>
                    <th>Account Number</th>
                    <th>Customer</th>
                    <th>Type</th>
                    <th>Balance</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {allAccounts.map(acc => (
                    <tr key={acc.accountId}>
                      <td style={{ fontWeight: '600' }}>#{acc.accountId}</td>
                      <td style={{ fontWeight: '600', fontFamily: 'var(--font-mono)' }}>{acc.accountNumber}</td>
                      <td>{acc.customerName || `Customer #${acc.customerId}`}</td>
                      <td><span className="badge badge-neutral">{acc.accountType}</span></td>
                      <td style={{ fontWeight: '700' }}>₱{acc.balance.toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                      <td>
                        <span className={`badge badge-${acc.status.toLowerCase()}`}>{acc.status}</span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          {acc.status === 'ACTIVE' ? (
                            <button
                              type="button"
                              onClick={() => handleStatusChangeSubmit(acc.accountId, 'FROZEN')}
                              className="banking-btn-secondary"
                              style={{ padding: '4px 8px', fontSize: '11px', color: '#dc2626' }}
                              title="Freeze Account"
                            >
                              Freeze
                            </button>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handleStatusChangeSubmit(acc.accountId, 'ACTIVE')}
                              className="banking-btn-secondary"
                              style={{ padding: '4px 8px', fontSize: '11px', color: '#059669' }}
                              title="Activate Account"
                            >
                              Activate
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
