import React, { useState } from 'react';
import { BellRing, CheckCircle, Clock, ShieldAlert, ArrowUpRight, ArrowDownLeft, Radio } from 'lucide-react';
import type { NotificationItem } from '../types';

interface NotificationViewProps {
  notifications: NotificationItem[];
  onMarkAsRead: (id: number) => void;
  onRefresh: () => void;
}

export const NotificationView: React.FC<NotificationViewProps> = ({
  notifications,
  onMarkAsRead,
  onRefresh
}) => {
  const [filter, setFilter] = useState<'ALL' | 'UNREAD'>('ALL');

  const filtered = filter === 'UNREAD'
    ? notifications.filter(n => !n.isRead)
    : notifications;

  const getIcon = (type: string) => {
    switch (type) {
      case 'TRANSACTION_CREDIT':
        return <ArrowDownLeft size={18} color="var(--color-success)" />;
      case 'TRANSACTION_DEBIT':
      case 'TRANSACTION_TRANSFER':
        return <ArrowUpRight size={18} color="var(--accent-cyan)" />;
      case 'SECURITY_ALERT':
        return <ShieldAlert size={18} color="var(--color-warning)" />;
      default:
        return <BellRing size={18} color="var(--accent-indigo)" />;
    }
  };

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto', width: 'calc(100% - 32px)', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header and Filter Controls */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: '800', color: '#ffffff', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <BellRing size={22} color="var(--accent-cyan)" />
            <span>In-App Customer Notification Feed</span>
          </h2>
          <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginTop: '2px' }}>
            Real-time events consumed asynchronously from Apache Kafka KRaft cluster
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={() => setFilter('ALL')}
            className={filter === 'ALL' ? 'btn btn-primary' : 'btn btn-secondary'}
            style={{ padding: '6px 14px', fontSize: '12px' }}
          >
            All Alerts ({notifications.length})
          </button>
          <button
            onClick={() => setFilter('UNREAD')}
            className={filter === 'UNREAD' ? 'btn btn-primary' : 'btn btn-secondary'}
            style={{ padding: '6px 14px', fontSize: '12px' }}
          >
            Unread ({notifications.filter(n => !n.isRead).length})
          </button>
          <button
            onClick={onRefresh}
            className="btn btn-outline"
            style={{ padding: '6px 12px', fontSize: '12px' }}
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Kafka Broker Provenance Banner */}
      <div className="glass-card" style={{
        padding: '12px 18px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        background: 'rgba(2, 6, 23, 0.6)',
        borderColor: 'rgba(56, 189, 248, 0.2)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', color: 'var(--text-muted)' }}>
          <Radio size={14} color="var(--accent-cyan)" className="animate-pulse" />
          <span>Active Topics:</span>
          <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>
            ledger.mutation.completed.v1, ledger.transfer.completed.v1
          </span>
        </div>
        <span className="badge badge-success" style={{ fontSize: '10px' }}>
          CONSUMER GROUP: NOTIFICATION-SERVICE-GROUP
        </span>
      </div>

      {/* Notifications List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {filtered.length === 0 ? (
          <div className="glass-card" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-subtle)' }}>
            <p style={{ fontSize: '14px' }}>No notifications found in this feed.</p>
          </div>
        ) : (
          filtered.map(item => (
            <div
              key={item.id}
              className="glass-card"
              style={{
                padding: '18px 22px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                background: item.isRead ? 'rgba(15, 23, 42, 0.55)' : 'rgba(30, 41, 59, 0.8)',
                borderColor: item.isRead ? 'var(--border-subtle)' : 'rgba(56, 189, 248, 0.35)',
                boxShadow: item.isRead ? 'none' : '0 4px 15px rgba(56, 189, 248, 0.1)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '14px' }}>
                <div style={{
                  padding: '10px',
                  borderRadius: '10px',
                  background: 'rgba(15, 23, 42, 0.8)',
                  border: '1px solid var(--border-subtle)'
                }}>
                  {getIcon(item.type)}
                </div>

                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="badge badge-info" style={{ fontSize: '10px' }}>
                      {item.type}
                    </span>
                    {!item.isRead && (
                      <span className="badge badge-warning" style={{ fontSize: '10px' }}>
                        NEW
                      </span>
                    )}
                  </div>

                  <p style={{ marginTop: '6px', fontSize: '14px', color: '#ffffff', fontWeight: item.isRead ? '400' : '600' }}>
                    {item.message}
                  </p>

                  <div style={{ marginTop: '6px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: 'var(--text-subtle)' }}>
                    <Clock size={12} />
                    <span>{new Date(item.createdAt).toLocaleString()}</span>
                  </div>
                </div>
              </div>

              {!item.isRead && (
                <button
                  onClick={() => onMarkAsRead(item.id)}
                  className="btn btn-outline"
                  style={{ fontSize: '12px', padding: '6px 12px' }}
                >
                  <CheckCircle size={14} />
                  <span>Mark Read</span>
                </button>
              )}
            </div>
          ))
        )}
      </div>

    </div>
  );
};
