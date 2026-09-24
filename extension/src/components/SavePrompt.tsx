import React, { useState } from 'react';
import { AcceptedSolution } from '../types/solution';
import { SaveResult } from '../types/messages';

interface SavePromptProps {
  solution: AcceptedSolution;
  onSave: () => Promise<SaveResult>;
  onDismiss: () => void;
}

type UIState = 'prompt' | 'saving' | 'success' | 'error';

export const SavePrompt: React.FC<SavePromptProps> = ({ solution, onSave, onDismiss }) => {
  const [state, setState] = useState<UIState>('prompt');
  const [result, setResult] = useState<SaveResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');

  const handleConfirmSave = async () => {
    setState('saving');
    try {
      const res = await onSave();
      setResult(res);
      if (res.success) {
        setState('success');
      } else {
        setErrorMessage(res.error || 'Failed to save solution to GreenGrid.');
        setState('error');
      }
    } catch (e: unknown) {
      const err = e as Error;
      setErrorMessage(err?.message || 'Unexpected error occurred.');
      setState('error');
    }
  };

  return (
    <div style={styles.card}>
      {/* Header */}
      <div style={styles.header}>
        <div style={styles.brandRow}>
          <span style={styles.brandLogo}>🌱</span>
          <span style={styles.brandTitle}>GreenGrid</span>
        </div>
        <button
          onClick={onDismiss}
          style={styles.closeBtn}
          title="Dismiss"
          aria-label="Close GreenGrid prompt"
        >
          ✕
        </button>
      </div>

      {/* Body States */}
      {state === 'prompt' && (
        <div style={styles.content}>
          <div style={styles.statusBadge}>
            <span style={styles.checkIcon}>✓</span>
            <span>Solution Accepted</span>
          </div>

          <div style={styles.problemInfo}>
            <div style={styles.problemTitle}>{solution.problemName}</div>
            <div style={styles.metaRow}>
              <span style={styles.langBadge}>{solution.language}</span>
              <span style={styles.platformBadge}>{solution.platform}</span>
            </div>
          </div>

          <p style={styles.questionText}>Save this solution to your GreenGrid portfolio?</p>

          <div style={styles.buttonGroup}>
            <button onClick={onDismiss} style={styles.secondaryBtn}>
              No, thanks
            </button>
            <button onClick={handleConfirmSave} style={styles.primaryBtn}>
              Yes, Save
            </button>
          </div>
        </div>
      )}

      {state === 'saving' && (
        <div style={styles.content}>
          <div style={styles.loadingContainer}>
            <div style={styles.spinner} />
            <div style={styles.loadingText}>Saving to GreenGrid...</div>
            <div style={styles.loadingSubtext}>Committing and updating revision history</div>
          </div>
        </div>
      )}

      {state === 'success' && (
        <div style={styles.content}>
          <div style={styles.successIconContainer}>
            <span style={styles.bigCheck}>✓</span>
          </div>
          <div style={styles.successTitle}>Saved to GreenGrid!</div>
          <div style={styles.successDetails}>
            <div style={styles.problemTitle}>{result?.problemName || solution.problemName}</div>
            <div style={styles.metaRow}>
              <span style={styles.langBadge}>{solution.language}</span>
              {result?.isNewRevision ? (
                <span style={styles.revisionBadge}>
                  Revision #{result.revisionCount || 2}
                </span>
              ) : (
                <span style={styles.revisionBadge}>Revision #1</span>
              )}
              {result?.commitStatus && (
                <span style={styles.commitBadge}>
                  {result.commitStatus === 'COMMITTED' ? 'GitHub: Committed' : result.commitStatus}
                </span>
              )}
            </div>
          </div>
          <button onClick={onDismiss} style={styles.doneBtn}>
            Done
          </button>
        </div>
      )}

      {state === 'error' && (
        <div style={styles.content}>
          <div style={styles.errorIconContainer}>
            <span style={styles.errorMark}>!</span>
          </div>
          <div style={styles.errorTitle}>Save Failed</div>
          <p style={styles.errorMessage}>{errorMessage}</p>

          <div style={styles.buttonGroup}>
            <button onClick={onDismiss} style={styles.secondaryBtn}>
              Dismiss
            </button>
            <button onClick={handleConfirmSave} style={styles.primaryBtn}>
              Try Again
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  card: {
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    width: '320px',
    backgroundColor: '#0f172a',
    color: '#f8fafc',
    borderRadius: '14px',
    border: '1px solid rgba(16, 185, 129, 0.3)',
    boxShadow:
      '0 20px 25px -5px rgba(0, 0, 0, 0.6), 0 10px 10px -5px rgba(0, 0, 0, 0.5), 0 0 15px rgba(16, 185, 129, 0.15)',
    padding: '16px',
    boxSizing: 'border-box',
    userSelect: 'none',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '12px',
    paddingBottom: '8px',
    borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
  },
  brandRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
  },
  brandLogo: {
    fontSize: '18px',
    lineHeight: '1',
  },
  brandTitle: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#10b981',
    letterSpacing: '0.02em',
  },
  closeBtn: {
    background: 'transparent',
    border: 'none',
    color: '#94a3b8',
    cursor: 'pointer',
    fontSize: '14px',
    padding: '4px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: '4px',
  },
  content: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  statusBadge: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    backgroundColor: 'rgba(16, 185, 129, 0.12)',
    border: '1px solid rgba(16, 185, 129, 0.25)',
    color: '#34d399',
    borderRadius: '6px',
    padding: '4px 8px',
    fontSize: '12px',
    fontWeight: '600',
    alignSelf: 'flex-start',
  },
  checkIcon: {
    fontWeight: 'bold',
  },
  problemInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  problemTitle: {
    fontSize: '15px',
    fontWeight: '600',
    color: '#ffffff',
    lineHeight: '1.3',
    wordBreak: 'break-word',
  },
  metaRow: {
    display: 'flex',
    gap: '6px',
    flexWrap: 'wrap',
  },
  langBadge: {
    backgroundColor: 'rgba(59, 130, 246, 0.15)',
    border: '1px solid rgba(59, 130, 246, 0.3)',
    color: '#60a5fa',
    borderRadius: '4px',
    padding: '2px 6px',
    fontSize: '11px',
    fontWeight: '500',
  },
  platformBadge: {
    backgroundColor: 'rgba(245, 158, 11, 0.15)',
    border: '1px solid rgba(245, 158, 11, 0.3)',
    color: '#fbbf24',
    borderRadius: '4px',
    padding: '2px 6px',
    fontSize: '11px',
    fontWeight: '500',
  },
  revisionBadge: {
    backgroundColor: 'rgba(168, 85, 247, 0.15)',
    border: '1px solid rgba(168, 85, 247, 0.3)',
    color: '#c084fc',
    borderRadius: '4px',
    padding: '2px 6px',
    fontSize: '11px',
    fontWeight: '500',
  },
  commitBadge: {
    backgroundColor: 'rgba(16, 185, 129, 0.15)',
    border: '1px solid rgba(16, 185, 129, 0.3)',
    color: '#34d399',
    borderRadius: '4px',
    padding: '2px 6px',
    fontSize: '11px',
    fontWeight: '500',
  },
  questionText: {
    fontSize: '13px',
    color: '#94a3b8',
    margin: '0',
    lineHeight: '1.4',
  },
  buttonGroup: {
    display: 'flex',
    gap: '8px',
    marginTop: '4px',
  },
  secondaryBtn: {
    flex: '1',
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    color: '#cbd5e1',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    borderRadius: '8px',
    padding: '8px 12px',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'pointer',
    transition: 'background-color 0.15s ease',
  },
  primaryBtn: {
    flex: '1',
    backgroundColor: '#10b981',
    color: '#ffffff',
    border: 'none',
    borderRadius: '8px',
    padding: '8px 12px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    boxShadow: '0 2px 4px rgba(16, 185, 129, 0.3)',
    transition: 'background-color 0.15s ease',
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '16px 0',
    gap: '8px',
  },
  spinner: {
    width: '28px',
    height: '28px',
    border: '3px solid rgba(16, 185, 129, 0.2)',
    borderTop: '3px solid #10b981',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
  loadingText: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#f8fafc',
  },
  loadingSubtext: {
    fontSize: '12px',
    color: '#64748b',
  },
  successIconContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    width: '42px',
    height: '42px',
    borderRadius: '50%',
    backgroundColor: 'rgba(16, 185, 129, 0.15)',
    border: '2px solid #10b981',
    margin: '4px auto',
  },
  bigCheck: {
    fontSize: '22px',
    color: '#10b981',
    fontWeight: 'bold',
  },
  successTitle: {
    textAlign: 'center',
    fontSize: '15px',
    fontWeight: '700',
    color: '#10b981',
  },
  successDetails: {
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: '8px',
    padding: '10px',
    border: '1px solid rgba(255, 255, 255, 0.05)',
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  doneBtn: {
    width: '100%',
    backgroundColor: '#10b981',
    color: '#ffffff',
    border: 'none',
    borderRadius: '8px',
    padding: '8px 12px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    marginTop: '4px',
  },
  errorIconContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    border: '2px solid #ef4444',
    margin: '4px auto',
  },
  errorMark: {
    fontSize: '20px',
    color: '#ef4444',
    fontWeight: 'bold',
  },
  errorTitle: {
    textAlign: 'center',
    fontSize: '15px',
    fontWeight: '700',
    color: '#ef4444',
  },
  errorMessage: {
    fontSize: '13px',
    color: '#cbd5e1',
    textAlign: 'center',
    margin: '0',
    lineHeight: '1.4',
  },
};
