import React from "react";
import "./MenuItem.css";

/**
 * MenuItem Component - Individual menu item wrapper
 * 
 * Props:
 *   - icon (string): Icon/emoji to display
 *   - label (string): Menu item text
 *   - onClick (func): Click handler
 *   - type (string): 'button' (default) | 'divider' | 'toggle'
 *   - isActive (bool): For toggle items, whether they are active
 *   - disabled (bool): Whether item is disabled
 */
export default function MenuItem({
  icon,
  label,
  onClick,
  type = "button",
  isActive = false,
  disabled = false,
}) {
  if (type === "divider") {
    return <div className="menu-divider" />;
  }

  return (
    <button
      className={`menu-item ${type === "toggle" && isActive ? "active" : ""} ${disabled ? "disabled" : ""}`}
      onClick={onClick}
      disabled={disabled}
      role={type === "toggle" ? "switch" : "menuitem"}
      aria-pressed={type === "toggle" ? isActive : undefined}
    >
      {icon && <span className="menu-item-icon">{icon}</span>}
      <span className="menu-item-label">{label}</span>
    </button>
  );
}
