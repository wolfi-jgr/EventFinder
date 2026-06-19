import React, { useEffect } from "react";
import "./Modal.css";

/**
 * Reusable Modal component with backdrop overlay
 * 
 * Props:
 *   - isOpen (bool): Controls modal visibility
 *   - onClose (func): Callback when modal should close (backdrop click)
 *   - children (node): Modal content
 */
export default function Modal({ isOpen, onClose, children }) {
  useEffect(() => {
    if (!isOpen) return;

    const handleEscapeKey = (e) => {
      if (e.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleEscapeKey);
    return () => window.removeEventListener("keydown", handleEscapeKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}
